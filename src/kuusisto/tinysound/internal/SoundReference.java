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
	
	private byte[] data;
	private int position;
	private double volume;
	
	/**
	 * Construct a new SoundReference with the given reference data.
	 * @param data sound data
	 */
	public SoundReference(byte[] data, double volume, int soundID) {
		this.data = data;
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
		return this.data.length - this.position;
	}
	
	/**
	 * Get the next byte from the sound data.
	 * @return the next byte
	 */
	public byte nextByte() {
		byte ret = this.data[this.position];
		this.position++;
		return ret;
	}
	
	/**
	 * Get the next two bytes from the sound data in the specified endianness.
	 * @param bigEndian true if the bytes should be read big-endian
	 * @return the next two bytes in the specified endianness
	 */
	public int nextTwoBytes(boolean bigEndian) {
		int ret = 0;
		if (bigEndian) {
			ret = ((this.data[this.position] << 8) |
					(this.data[this.position + 1] & 0xFF));
		}
		else {
			ret = ((this.data[this.position + 1] << 8) |
					(this.data[this.position] & 0xFF));
		}
		this.position += 2;
		return ret;
	}

}
