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

import kuusisto.tinysound.Music;
import kuusisto.tinysound.TinySound;

public class StreamMusic implements Music {
	
	private URL dataURL;
	private Mixer mixer;
	private MusicReference reference;
	
	public StreamMusic(URL dataURL, long numBytesPerChannel, Mixer mixer) {
		this.dataURL = dataURL;
		this.mixer = mixer;
		try {
			//TODO should this be thrown from here?
			this.reference = new StreamMusicReference(this.dataURL, false,
					false, 0, 0, numBytesPerChannel, 1.0);
			this.mixer.registerMusicReference(this.reference);
		} catch (IOException e) {
			System.err.println("Failed to open stream for Music");
		}
	}

	/**
	 * Play this Music and loop if specified.
	 * @param loop if this Music should loop
	 */
	public void play(boolean loop) {
		this.reference.setPlaying(true);
		this.reference.setLoop(loop);
	}
	
	/**
	 * Play this Music at the specified volume and loop if specified.
	 * @param loop if this Music should loop
	 * @param volume the volume to play the this Music
	 */
	public void play(boolean loop, double volume) {
		this.reference.setPlaying(true);
		this.setLoop(loop);
		this.setVolume(volume);
	}
	
	/**
	 * Stop playing this Music and set its position to the beginning.
	 */
	public void stop() {
		this.reference.setPlaying(false);
		this.rewind();
	}
	
	/**
	 * Stop playing this Music and keep its current position.
	 */
	public void pause() {
		this.reference.setPlaying(false);
	}
	
	/**
	 * Play this Music from its current position.
	 */
	public void resume() {
		this.reference.setPlaying(true);
	}
	
	/**
	 * Set this Music's position to the beginning.
	 */
	public void rewind() {
		this.reference.setPosition(0);
	}
	
	/**
	 * Set this Music's position to the loop position.
	 */
	public void rewindToLoopPosition() {
		long byteIndex = this.reference.getLoopPosition();
		this.reference.setPosition(byteIndex);
	}
	
	/**
	 * Determine if this Music is playing.
	 * @return true if this Music is playing
	 */
	public boolean playing() {
		return this.reference.getPlaying();
	}
	
	/**
	 * Determine if this Music will loop.
	 * @return true if this Music will loop
	 */
	public boolean loop() {
		return this.reference.getLoop();
	}
	
	/**
	 * Set whether this Music will loop.
	 * @param loop whether this Music will loop
	 */
	public void setLoop(boolean loop) {
		this.reference.setLoop(loop);
	}

	@Override
	public int getLoopPositionByFrame() {
		int bytesPerChannelForFrame = TinySound.FORMAT.getFrameSize() /
			TinySound.FORMAT.getChannels();
		long byteIndex = this.reference.getLoopPosition();
		return (int)(byteIndex / bytesPerChannelForFrame);
	}

	@Override
	public double getLoopPositionBySeconds() {
		int bytesPerChannelForFrame = TinySound.FORMAT.getFrameSize() /
			TinySound.FORMAT.getChannels();
		long byteIndex = this.reference.getLoopPosition();
		return (byteIndex / (TinySound.FORMAT.getFrameRate() *
			bytesPerChannelForFrame));
	}

	@Override
	public void setLoopPositionByFrame(int frameIndex) {
		//get the byte index for a channel
		int bytesPerChannelForFrame = TinySound.FORMAT.getFrameSize() /
			TinySound.FORMAT.getChannels();
		long byteIndex = (long)(frameIndex * bytesPerChannelForFrame);
		this.reference.setLoopPosition(byteIndex);
	}

	@Override
	public void setLoopPositionBySeconds(double seconds) {
		//get the byte index for a channel
		int bytesPerChannelForFrame = TinySound.FORMAT.getFrameSize() /
			TinySound.FORMAT.getChannels();
		long byteIndex = (long)(seconds * TinySound.FORMAT.getFrameRate() *
			bytesPerChannelForFrame);
		this.reference.setLoopPosition(byteIndex);
	}
	
	/**
	 * Get the volume of this Music.
	 * @return volume of this Music
	 */
	public double getVolume() {
		return this.reference.getVolume();
	}
	
	/**
	 * Set the volume of this Music.
	 * @param volume the desired volume of this Music
	 */
	public void setVolume(double volume) {
		if (volume >= 0.0) {
			this.reference.setVolume(volume);
		}
	}

	@Override
	public void unload() {
		//unregister the reference
		this.mixer.unRegisterMusicReference(this.reference);
		this.reference.dispose();
		this.mixer = null;
		this.dataURL = null;
		this.reference = null;
	}
	
	/////////////
	//Reference//
	/////////////

	private static class StreamMusicReference implements MusicReference {
		
		private URL url;
		private InputStream data;
		private long numBytesPerChannel; //not per frame, but the whole sound
		private byte[] buf;
		private byte[] skipBuf;
		private boolean playing;
		private boolean loop;
		private long loopPosition;
		private long position;
		private double volume;
		
		public StreamMusicReference(URL dataURL, boolean playing, boolean loop,
				long loopPosition, long position, long numBytesPerChannel,
				double volume) throws IOException {
			this.url = dataURL;
			this.playing = playing;
			this.loop = loop;
			this.loopPosition = loopPosition;
			this.position = position;
			this.numBytesPerChannel = numBytesPerChannel;
			this.volume = volume;
			this.buf = new byte[4];
			this.skipBuf = new byte[50];
			//now get the data stream
			this.data = this.url.openStream();
		}

		@Override
		public synchronized boolean getPlaying() {
			return this.playing;
		}

		@Override
		public synchronized boolean getLoop() {
			return this.loop;
		}
		
		@Override
		public synchronized long getPosition() {
			return this.position;
		}
		
		@Override
		public synchronized long getLoopPosition() {
			return this.loopPosition;
		}

		/**
		 * Get the number of bytes remaining for each channel until the end of
		 * this Music.
		 * @return number of bytes remaining for each channel
		 */
		@Override
		public synchronized long bytesAvailable() {
			return this.numBytesPerChannel - this.position;
		}

		@Override
		public synchronized double getVolume() {
			return this.volume;
		}

		@Override
		public synchronized void setPlaying(boolean playing) {
			this.playing = playing;
		}

		@Override
		public synchronized void setLoop(boolean loop) {
			this.loop = loop;
		}

		@Override
		public synchronized void setPosition(long position) {
			if (position >= 0 && position < this.numBytesPerChannel) {
				//if it's later, skip
				if (position >= this.position) {
					this.skipBytes(position - this.position);
				}
				else { //otherwise skip from the beginning
					//first close our current stream
					try {
						this.data.close();
					} catch (IOException e) {
						//whatever...
					}
					//open a new stream
					try {
						this.data = this.url.openStream();
						this.position = 0;
						this.skipBytes(position);
					} catch (IOException e) {
						System.err.println("Failed to open stream for Music");
						this.playing = false;
					}
				}
			}
		}

		@Override
		public synchronized void setLoopPosition(long loopPosition) {
			if (loopPosition >= 0 && loopPosition < this.numBytesPerChannel) {
				this.loopPosition = loopPosition;
			}
		}

		@Override
		public synchronized void setVolume(double volume) {
			this.volume = volume;
		}

		@Override
		public synchronized void nextTwoBytes(int[] data, boolean bigEndian) {
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
				//this should never happen
				this.position = this.numBytesPerChannel;
			}
			else {
				this.position += 2;
			}
			//wrap if looping
			if (this.loop && this.position >= this.numBytesPerChannel) {
				this.setPosition(this.loopPosition);
			}
		}

		@Override
		public synchronized void skipBytes(long num) {
			//couple of shortcuts if we are going to complete the stream
			if ((this.position + num) >= this.numBytesPerChannel) {
				//if we're not looping, nothing special needs to happen
				if (!this.loop) {
					this.position += num;
					return;
				}
				else {
					//compute the next position
					long loopLength = this.numBytesPerChannel -
						this.loopPosition;
					long bytesOver = (this.position + num) -
						this.numBytesPerChannel;
					long nextPosition = this.loopPosition +
						(bytesOver % loopLength);
					//and set us there
					this.setPosition(nextPosition);
					return;
				}
			}
			//this is the number of bytes to skip per channel, so double it
			long numSkip = num * 2;
			//spin read since skip is not always supported apparently and won't
			//guarantee a correct skip amount
			int tmpRead = 0;
			int numRead = 0;
			try {
				while (numRead < numSkip && tmpRead != -1) {
					//determine safe length to read
					long remaining = numSkip - numRead;
					int len = remaining > this.skipBuf.length ?
							this.skipBuf.length : (int)remaining;
					//and read
					tmpRead = this.data.read(this.skipBuf, 0, len);
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

		/**
		 * Does any cleanup necessary to dispose of resources in use by this
		 * MusicReference.
		 */
		@Override
		public synchronized void dispose() {
			this.playing = false;
			this.position = this.numBytesPerChannel;
			this.url = null;
			try {
				this.data.close();
			} catch (IOException e) {
				//whatever... this should never happen
			}
		}
	}
}