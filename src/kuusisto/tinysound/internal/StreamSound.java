/*
 * Copyright (c) 2012, Finn Kuusisto
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *     
 *     Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package kuusisto.tinysound.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import kuusisto.tinysound.Sound;

public class StreamSound implements Sound {
	
	private URL dataURL;
	private long numBytesPerChannel;
	private Mixer mixer;
	private final int ID;
	
	public StreamSound(URL dataURL, long numBytesPerChannel, Mixer mixer,
			int id) {
		this.dataURL = dataURL;
		this.numBytesPerChannel = numBytesPerChannel;
		this.mixer = mixer;
		this.ID = id;
		//TODO open and close a stream to check for immediate I/O issues?
	}

	@Override
	public void play() {
		this.play(1.0);
	}

	@Override
	public void play(double volume) {
		//dispatch a SoundReference to the mixer
		SoundReference ref;
		try {
			ref = new StreamSoundReference(this.dataURL.openStream(),
					this.numBytesPerChannel, volume, this.ID);
			this.mixer.registerSoundReference(ref);
		} catch (IOException e) {
			System.err.println("Failed to open stream for Sound");
		}
	}

	@Override
	public void stop() {
		this.mixer.unRegisterSoundReference(this.ID);
	}

	@Override
	public void unload() {
		this.mixer.unRegisterSoundReference(this.ID);
		this.mixer = null;
		this.dataURL = null;
	}
	
	/////////////
	//Reference//
	/////////////
	
	private static class StreamSoundReference implements SoundReference {
		
		public final int SOUND_ID;
		
		private InputStream data;
		private long numBytesPerChannel; //not per frame, but the whole sound
		private long position;
		private double volume;
		private byte[] buf;
		private byte[] skipBuf;
		
		public StreamSoundReference(InputStream data, long numBytesPerChannel,
				double volume, int soundID) {
			this.data = data;
			this.numBytesPerChannel = numBytesPerChannel;
			this.volume = (volume >= 0.0) ? volume : 1.0;
			this.position = 0;
			this.buf = new byte[4];
			this.skipBuf = new byte[20];
			this.SOUND_ID = soundID;
		}

		/**
		 * Get the ID of the Sound that produced this SoundReference.
		 * @return the ID of this SoundReference's parent Sound
		 */
		@Override
		public int getSoundID() {
			return this.SOUND_ID;
		}

		/**
		 * Get the number of bytes remaining for each channel.
		 * @return number of bytes remaining for each channel
		 */
		@Override
		public long bytesAvailable() {
			return this.numBytesPerChannel - this.position;
		}

		@Override
		public double getVolume() {
			return this.volume;
		}

		@Override
		public void nextTwoBytes(int[] data, boolean bigEndian) {
			//try to read audio data
			int tmpRead = 0;
			int numRead = 0;
			try {
				while (numRead < this.buf.length && tmpRead != -1) {
					tmpRead = this.data.read(this.buf, numRead,
							this.buf.length - numRead);
					numRead += tmpRead;
				}
			} catch (IOException e) {
				//this shouldn't happen if the bytes were written correctly to
				//the temp file, but this sound should now be invalid at least
				this.position = this.numBytesPerChannel;
				System.err.println("Failed reading bytes for stream sound");
			}
			//copy the values into the caller buffer
			if (bigEndian) {
				//left
				data[0] = ((this.buf[0] << 8) |
						(this.buf[1] & 0xFF));
				//right
				data[1] = ((this.buf[2] << 8) |
						(this.buf[3] & 0xFF));
			}
			else {
				//left
				data[0] = ((this.buf[1] << 8) |
						(this.buf[0] & 0xFF));
				//right
				data[1] = ((this.buf[3] << 8) |
						(this.buf[2] & 0xFF));
			}
			//increment the position appropriately
			if (tmpRead == -1) { //reached end of file in the middle of reading
				this.position = this.numBytesPerChannel;
			}
			else {
				this.position += 2;
			}
		}

		@Override
		public void skipBytes(long num) {
			//terminate early if it would finish the sound
			if (this.position + num >= this.numBytesPerChannel) {
				this.position = this.numBytesPerChannel;
				return;
			}
			//this is the number of bytes to skip per channel, so double it
			long numSkip = num * 2;
			//spin read since skip is not always supported apparently and won't
			//guarantee a correct skip amount
			int tmpRead = 0;
			int numRead = 0;
			try {
				while (numRead < numSkip && tmpRead != -1) {
					tmpRead = this.data.read(this.skipBuf, numRead,
							this.skipBuf.length - numRead);
					numRead += tmpRead;
				}
			} catch (IOException e) {
				//hmm... I guess invalidate this reference
				this.position = this.numBytesPerChannel;
			}
			//increment the position appropriately
			if (tmpRead == -1) { //reached end of file in the middle of reading
				this.position = this.numBytesPerChannel;
			}
			else {
				this.position += num;
			}
		}

		@Override
		public void dispose() {
			this.position = this.numBytesPerChannel;
			try {
				this.data.close();
			} catch (IOException e) {
				//whatever... this shouldn't happen
			}
			this.buf = null;
			this.skipBuf = null;
		}

	}

}
