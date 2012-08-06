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

import kuusisto.tinysound.Music;
import kuusisto.tinysound.TinySound;

/**
 * The Music class is an abstraction for music.  Music objects should only be
 * loaded via the TinySound <code>loadMusic()</code> functions.  Music can be
 * played, paused, resumed, stopped and looped from specified positions.
 * 
 * @author Finn Kuusisto
 */
public class MemMusic implements Music {
	
	private byte[] left;
	private byte[] right;
	private Mixer mixer;
	private MusicReference reference;
	
	/**
	 * Construct a new Music with the given music data and the Mixer with which
	 * to register this Music.
	 * @param left left channel of music data
	 * @param right right channel of music data
	 * @param mixer Mixer with which this Music is registered
	 */
	public MemMusic(byte[] left, byte[] right, Mixer mixer) {
		this.left = left;
		this.right = right;
		this.mixer = mixer;
		this.reference = new MemMusicReference(this.left, this.right, false,
				false, 0, 0, 1.0);
		this.mixer.registerMusicReference(this.reference);
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
	
	/**
	 * Get the loop position of this Music by sample frame.
	 * @return loop position by sample frame
	 */
	public int getLoopPositionByFrame() {
		int bytesPerChannelForFrame = TinySound.FORMAT.getFrameSize() /
			TinySound.FORMAT.getChannels();
		long byteIndex = this.reference.getLoopPosition();
		return (int)(byteIndex / bytesPerChannelForFrame);
	}
	
	/**
	 * Get the loop position of this Music by seconds.
	 * @return loop position by seconds
	 */
	public double getLoopPositionBySeconds() {
		int bytesPerChannelForFrame = TinySound.FORMAT.getFrameSize() /
			TinySound.FORMAT.getChannels();
		long byteIndex = this.reference.getLoopPosition();
		return (byteIndex / (TinySound.FORMAT.getFrameRate() *
				bytesPerChannelForFrame));
	}
	
	/**
	 * Set the loop position of this Music by sample frame.
	 * @param frameIndex sample frame loop position to set
	 */
	public void setLoopPositionByFrame(int frameIndex) {
		//get the byte index for a channel
		int bytesPerChannelForFrame = TinySound.FORMAT.getFrameSize() /
			TinySound.FORMAT.getChannels();
		long byteIndex = (long)(frameIndex * bytesPerChannelForFrame);
		this.reference.setLoopPosition(byteIndex);
	}
	
	/**
	 * Set the loop position of this Music by seconds.
	 * @param seconds loop position to set by seconds
	 */
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
	
	/**
	 * Unload this Music from the system.  Attempts to use this Music after
	 * unloading will result in error.
	 */
	public void unload() {
		//unregister the reference
		this.mixer.unRegisterMusicReference(this.reference);
		this.reference.dispose();
		this.mixer = null;
		this.left = null;
		this.right = null;
		this.reference = null;
	}
	
	/////////////
	//Reference//
	/////////////
	
	/**
	 * The MusicReference class is the Mixers interface to the audio data of a
	 * Music object.  MusicReference is an internal class of the TinySound
	 * system and should be of no real concern to the average user of TinySound.
	 * 
	 * @author Finn Kuusisto
	 */
	private static class MemMusicReference implements MusicReference {

		private byte[] left;
		private byte[] right;
		private boolean playing;
		private boolean loop;
		private int loopPosition;
		private int position;
		private double volume;
		
		/**
		 * Construct a new MusicReference with the given audio data and
		 * settings.
		 * @param left left channel of music data
		 * @param right right channel of music data
		 * @param playing true if the music should be playing
		 * @param loop true if the music should loop
		 * @param position byte index position in music data
		 * @param volume volume to play the music
		 */
		public MemMusicReference(byte[] left, byte[] right, boolean playing,
				boolean loop, int loopPosition, int position, double volume) {
			this.left = left;
			this.right = right;
			this.playing = playing;
			this.loop = loop;
			this.loopPosition = loopPosition;
			this.position = position;
			this.volume = volume;
		}
		
		/**
		 * Get the playing setting of this MusicReference.
		 * @return true if this MusicReference is set to play
		 */
		@Override
		public synchronized boolean getPlaying() {
			return this.playing;
		}
		
		/**
		 * Get the loop setting of this MusicReference.
		 * @return true if this MusicReference is set to loop
		 */
		@Override
		public synchronized boolean getLoop() {
			return this.loop;
		}
		
		/**
		 * Get the byte index of this MusicReference.
		 * @return byte index of this MusicReference
		 */
		@Override
		public synchronized long getPosition() {
			return this.position;
		}
		
		/**
		 * Get the loop-position byte index of this MusicReference.
		 * @return loop-position byte index of this MusicReference
		 */
		@Override
		public synchronized long getLoopPosition() {
			return this.loopPosition;
		}
		
		/**
		 * Get the volume of this MusicReference.
		 * @return volume of this MusicReference
		 */
		@Override
		public synchronized double getVolume() {
			return this.volume;
		}
		
		/**
		 * Set whether this MusicReference is playing.
		 * @param playing whether this MusicReference is playing
		 */
		@Override
		public synchronized void setPlaying(boolean playing) {
			this.playing = playing;
		}
		
		/**
		 * Set whether this MusicReference will loop.
		 * @param loop whether this MusicReference will loop
		 */
		@Override
		public synchronized void setLoop(boolean loop) {
			this.loop = loop;
		}
		
		/**
		 * Set the byte index of this MusicReference.
		 * @param position the byte index to set
		 */
		@Override
		public synchronized void setPosition(long position) {
			if (position >= 0 && position < this.left.length) {
				this.position = (int)position;
			}
		}
		
		/**
		 * Set the loop-position byte index of this MusicReference.
		 * @param loopPosition the loop-position byte index to set
		 */
		@Override
		public synchronized void setLoopPosition(long loopPosition) {
			if (loopPosition >= 0 && loopPosition < this.left.length) {
				this.loopPosition = (int)loopPosition;
			}
		}
		
		/**
		 * Set the volume of this MusicReference.
		 * @param volume the desired volume of this MusicReference
		 */
		@Override
		public synchronized void setVolume(double volume) {
			this.volume = volume;
		}
		
		/**
		 * Get the number of bytes remaining for each channel until the end of this
		 * Music.
		 * @return number of bytes remaining for each channel
		 */
		@Override
		public synchronized long bytesAvailable() {
			return this.left.length - this.position;
		}
		
		/**
		 * Skip a specified number of bytes of the audio data.
		 * @param num number of bytes to skip
		 */
		@Override
		public synchronized void skipBytes(long num) {
			for (int i = 0; i < num; i++) {
				this.position++;
				//wrap if looping
				if (this.loop && this.position >= this.left.length) {
					this.position = this.loopPosition;
				}
			}
		}
		
		/**
		 * Get the next two bytes from the music data in the specified
		 * endianness.
		 * @param data length-2 array to write in next two bytes from each
		 * channel
		 * @param bigEndian true if the bytes should be read big-endian
		 */
		@Override
		public synchronized void nextTwoBytes(int[] data, boolean bigEndian) {
			if (bigEndian) {
				//left
				data[0] = ((this.left[this.position] << 8) |
						(this.left[this.position + 1] & 0xFF));
				//right
				data[1] = ((this.right[this.position] << 8) |
						(this.right[this.position + 1] & 0xFF));
			}
			else {
				//left
				data[0] = ((this.left[this.position + 1] << 8) |
						(this.left[this.position] & 0xFF));
				//right
				data[1] = ((this.right[this.position + 1] << 8) |
						(this.right[this.position] & 0xFF));
			}
			this.position += 2;
			//wrap if looping
			if (this.loop && this.position >= this.left.length) {
				this.position = this.loopPosition;
			}
		}

		/**
		 * Does any cleanup necessary to dispose of resources in use by this
		 * MusicReference.
		 */
		@Override
		public synchronized void dispose() {
			this.playing = false;
			this.position = this.left.length + 1;
			this.left = null;
			this.right = null;
		}
		
	}

}
