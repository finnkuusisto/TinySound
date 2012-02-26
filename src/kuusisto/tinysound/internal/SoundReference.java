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

/**
 * The SoundReference class is the Mixers interface to the audio data of a Sound
 * object.  SoundReference is an internal class of the TinySound system and
 * should be of no real concern to the average user of TinySound.
 * 
 * @author Finn Kuusisto
 */
public class SoundReference {

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
	public SoundReference(byte[] left, byte[] right, double volume,
			int soundID) {
		this.left = left;
		this.right = right;
		this.volume = (volume >= 0.0) ? volume : 1.0;
		this.position = 0;
		this.SOUND_ID = soundID;
	}
	
	/**
	 * Gets the volume of this SoundReference.
	 * @return volume of this SoundReference
	 */
	public double getVolume() {
		return this.volume;
	}
	
	/**
	 * Get the number of bytes remaining for reading.
	 * @return number of bytes remaining
	 */
	public int bytesAvailable() {
		return this.left.length - this.position;
	}
	
	/**
	 * Get the next byte from the sound data.
	 * @param data length-2 array to write in next byte from each channel
	 */
	public void nextByte(byte[] data) {
		//left channel
		data[0] = this.left[position];
		//right channel
		data[1] = this.right[position];
		this.position++;
	}
	
	/**
	 * Get the next two bytes from the sound data in the specified endianness.
	 * @param data length-2 array to write in next two bytes from each channel
	 * @param bigEndian true if the bytes should be read big-endian
	 */
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

}
