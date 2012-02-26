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
 * The MusicReference class is the Mixers interface to the audio data of a Music
 * object.  MusicReference is an internal class of the TinySound system and
 * should be of no real concern to the average user of TinySound.
 * 
 * @author Finn Kuusisto
 */
public class MusicReference {

	private byte[] left;
	private byte[] right;
	private boolean playing;
	private boolean loop;
	private int loopPosition;
	private int position;
	private double volume;
	
	/**
	 * Construct a new MusicReference with the given audio data and settings.
	 * @param left left channel of music data
	 * @param right right channel of music data
	 * @param playing true if the music should be playing
	 * @param loop true if the music should loop
	 * @param position byte index position in music data
	 * @param volume volume to play the music
	 */
	public MusicReference(byte[] left, byte[] right, boolean playing,
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
	public synchronized boolean getPlaying() {
		return this.playing;
	}
	
	/**
	 * Get the loop setting of this MusicReference.
	 * @return true if this MusicReference is set to loop
	 */
	public synchronized boolean getLoop() {
		return this.loop;
	}
	
	/**
	 * Get the byte index of this MusicReference.
	 * @return byte index of this MusicReference
	 */
	public synchronized int getPosition() {
		return this.position;
	}
	
	/**
	 * Get the loop-position byte index of this MusicReference.
	 * @return loop-position byte index of this MusicReference
	 */
	public synchronized int getLoopPosition() {
		return this.loopPosition;
	}
	
	/**
	 * Get the volume of this MusicReference.
	 * @return volume of this MusicReference
	 */
	public synchronized double getVolume() {
		return this.volume;
	}
	
	/**
	 * Set whether this MusicReference is playing.
	 * @param playing whether this MusicReference is playing
	 */
	public synchronized void setPlaying(boolean playing) {
		this.playing = playing;
	}
	
	/**
	 * Set whether this MusicReference will loop.
	 * @param loop whether this MusicReference will loop
	 */
	public synchronized void setLoop(boolean loop) {
		this.loop = loop;
	}
	
	/**
	 * Set the byte index of this MusicReference.
	 * @param position the byte index to set
	 */
	public synchronized void setPosition(int position) {
		if (position >= 0 && position < this.left.length) {
			this.position = position;
		}
	}
	
	/**
	 * Set the loop-position byte index of this MusicReference.
	 * @param loopPosition the loop-position byte index to set
	 */
	public synchronized void setLoopPosition(int loopPosition) {
		if (loopPosition >= 0 && loopPosition < this.left.length) {
			this.loopPosition = loopPosition;
		}
	}
	
	/**
	 * Set the volume of this MusicReference.
	 * @param volume the desired volume of this MusicReference
	 */
	public synchronized void setVolume(double volume) {
		this.volume = volume;
	}
	
	/**
	 * Get the number of bytes remaining until the end of this Music.
	 * @return number of bytes remaining
	 */
	public synchronized int bytesAvailable() {
		return this.left.length - this.position;
	}
	
	/**
	 * Get the next byte from the music data.
	 * @param data length-2 array to write in next byte from each channel
	 */
	public synchronized void nextByte(byte[] data) {
		//left channel
		data[0] = this.left[position];
		//right channel
		data[1] = this.right[position];
		this.position++;
		//wrap if looping
		if (this.loop && this.position >= this.left.length) {
			this.position = this.loopPosition;
		}
	}
	
	/**
	 * Get the next two bytes from the music data in the specified endianness.
	 * @param data length-2 array to write in next two bytes from each channel
	 * @param bigEndian true if the bytes should be read big-endian
	 */
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
	
}
