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

import kuusisto.tinysound.Sound;

/**
 * The Sound class is an abstraction for sound effects.  Sound objects should
 * only be loaded via the TinySound <code>loadSound()</code> functions.  Sound
 * can be played repeatedly in an overlapping fashion.
 * 
 * @author Finn Kuusisto
 */
public class MemSound implements Sound {
	
	private byte[] left;
	private byte[] right;
	private Mixer mixer;
	private final int ID; //unique ID to match references
	
	/**
	 * Construct a new Sound with the given data and Mixer which will handle
	 * handle this Sound.
	 * @param left left channel of sound data
	 * @param right right channel of sound data
	 * @param mixer Mixer that will handle this Sound
	 */
	public MemSound(byte[] left, byte[] right, Mixer mixer, int id) {
		this.left = left;
		this.right = right;
		this.mixer = mixer;
		this.ID = id;
	}
	
	/**
	 * Plays this Sound.
	 */
	public void play() {
		this.play(1.0);
	}
	
	/**
	 * Plays this Sound with a specified volume.
	 * @param volume the volume at which to play this Sound
	 */
	public void play(double volume) {
		//dispatch a SoundReference to the mixer
		SoundReference ref = new MemSoundReference(this.left, this.right,
				volume, this.ID);
		this.mixer.registerSoundReference(ref);
	}
	
	/**
	 * Stops this Sound from playing.  Note that if this Sound was played
	 * repeatedly in an overlapping fashion, all instances of this Sound still
	 * playing will be stopped.
	 */
	public void stop() {
		this.mixer.unRegisterSoundReference(this.ID);
	}
	
	/**
	 * Unloads this Sound from the system.  Attempts to use this Sound after
	 * unloading will result in error.
	 */
	public void unload() {
		this.mixer.unRegisterSoundReference(this.ID);
		this.mixer = null;
		this.left = null;
		this.right = null;
	}
	
	/////////////
	//Reference//
	/////////////
	
	/**
	 * The SoundReference class is the Mixers interface to the audio data of a
	 * Sound object.  SoundReference is an internal class of the TinySound
	 * system and should be of no real concern to the average user of TinySound.
	 * 
	 * @author Finn Kuusisto
	 */
	private static class MemSoundReference implements SoundReference {

		public final int SOUND_ID; //parent Sound
		
		private byte[] left;
		private byte[] right;
		private int position;
		private double volume;
		
		/**
		 * Construct a new SoundReference with the given reference data.
		 * @param left left channel of sound data
		 * @param right right channel of sound data
		 * @param volume volume at which to play the sound
		 * @param soundID ID of the Sound for which this is a reference
		 */
		public MemSoundReference(byte[] left, byte[] right, double volume,
				int soundID) {
			this.left = left;
			this.right = right;
			this.volume = (volume >= 0.0) ? volume : 1.0;
			this.position = 0;
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
		 * Gets the volume of this SoundReference.
		 * @return volume of this SoundReference
		 */
		@Override
		public double getVolume() {
			return this.volume;
		}
		
		/**
		 * Get the number of bytes remaining for each channel.
		 * @return number of bytes remaining for each channel
		 */
		@Override
		public long bytesAvailable() {
			return this.left.length - this.position;
		}
		
		/**
		 * Skip a specified number of bytes of the audio data.
		 * @param num number of bytes to skip
		 */
		@Override
		public synchronized void skipBytes(long num) {
			this.position += num;
		}
		
		/**
		 * Get the next two bytes from the sound data in the specified
		 * endianness.
		 * @param data length-2 array to write in next two bytes from each
		 * channel
		 * @param bigEndian true if the bytes should be read big-endian
		 */
		@Override
		public void nextTwoBytes(int[] data, boolean bigEndian) {
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
		}

		/**
		 * Does any cleanup necessary to dispose of resources in use by this
		 * SoundReference.
		 */
		@Override
		public void dispose() {
			this.position = this.left.length + 1;
			this.left = null;
			this.right = null;
		}
	}

}
