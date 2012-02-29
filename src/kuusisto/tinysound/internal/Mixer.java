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
import java.util.ArrayList;
import java.util.List;

/**
 * The Mixer class is what does the audio data mixing for the TinySound system.
 * Mixer is an internal class of the TinySound system and should be of no real
 * concern to the average user of TinySound.
 * 
 * @author Finn Kuusisto
 */
public class Mixer {
	
	private List<MusicReference> musics;
	private List<SoundReference> sounds;
	private int[] dataBuf; //buffer for reading sound data
	
	/**
	 * Construct a new Mixer for TinySound system.
	 */
	public Mixer() {
		this.musics = new ArrayList<MusicReference>();
		this.sounds = new ArrayList<SoundReference>();
		this.dataBuf = new int[2]; //2-channel
	}
	
	/**
	 * Registers a MusicReference with this Mixer.
	 * @param music MusicReference to be registered
	 */
	public void registerMusicReference(MusicReference music) {
		synchronized (this.musics) {
			this.musics.add(music);
		}
	}
	
	/**
	 * Registers a SoundReference with this Mixer.
	 * @param sound SoundReference to be registered
	 */
	public void registerSoundReference(SoundReference sound) {
		synchronized (this.sounds) {
			this.sounds.add(sound);	
		}
	}
	
	/**
	 * Unregisters a MusicReference with this Mixer.
	 * @param music MusicReference to be unregistered
	 */
	public void unRegisterMusicReference(MusicReference music) {
		synchronized (this.musics) {
			this.musics.remove(music);
		}
	}
	
	/**
	 * Unregisters all SoundReferences with a given soundID.
	 * @param soundID ID of SoundReferences to be unregistered
	 */
	public void unRegisterSoundReference(int soundID) {
		synchronized (this.sounds) {
			//removal working backward is easier
			for (int i = this.sounds.size() - 1; i >= 0; i--) {
				if (this.sounds.get(i).SOUND_ID == soundID) {
					this.sounds.remove(i);
				}
			}
		}
	}
	
	/**
	 * Unregister all Music registered with this Mixer.
	 */
	public void clearMusic() {
		synchronized (this.musics) {
			this.musics.clear();
		}
	}
	
	/**
	 * Unregister all Sounds registered with this Mixer.
	 */
	public void clearSounds() {
		synchronized (this.sounds) {
			this.sounds.clear();
		}
	}
	
	/**
	 * Read bytes from this Mixer.
	 * @param data the buffer to read the bytes into
	 * @param offset the start index to read bytes into
	 * @param length the maximum number of bytes that should be read
	 * @return number of bytes read into buffer
	 */
	public int read(byte[] data, int offset, int length) {
		//*********************************************//
		//assume big-endian, stereo, 16-bit, signed PCM//
		//*********************************************//
		if (length < data.length) {
			length = data.length;
		}
		int numRead = 0;
		boolean bytesRead = true; //terminate early if out of bytes
		for (int i = offset; i < length && bytesRead; i += 4) {
			//first assume we are done
			bytesRead = false;
			//need to track value across audio sources
			double leftValue = 0.0;
			double rightValue = 0.0;
			//go through all the music first
			synchronized (this.musics) {
				for (int m = 0; m < this.musics.size(); m++) {
					MusicReference music = this.musics.get(m);
					//is the music playing and are there bytes available
					if (music.getPlaying() && music.bytesAvailable() > 0) {
						//add this music to the mix by volume
						music.nextTwoBytes(this.dataBuf, true);
						double volume = music.getVolume();
						leftValue += (this.dataBuf[0] * volume);
						rightValue += (this.dataBuf[1] * volume);
						//we know we aren't done yet now
						bytesRead = true;
					}
				}
			}
			//then go through all the sounds (backwards to remove completed)
			synchronized (this.sounds) {
				for (int s = this.sounds.size() - 1; s >= 0; s--) {
					SoundReference sound = this.sounds.get(s);
					//are there bytes available
					if (sound.bytesAvailable() > 0) {
						//add this sound to the mix by volume
						sound.nextTwoBytes(this.dataBuf, true);
						double volume = sound.getVolume();
						leftValue += (this.dataBuf[0] * volume);
						rightValue += (this.dataBuf[1] * volume);
						//we know we aren't done yet now
						bytesRead = true;
						//remove the reference if done
						if (sound.bytesAvailable() <= 0) {
							this.sounds.remove(s);
						}
					}
					else { //otherwise remove this reference
						this.sounds.remove(s);
					}
				}
			}
			//if we actually read bytes, store in the buffer
			if (bytesRead) {
				int finalLeftValue = (int)leftValue;
				int finalRightValue = (int)rightValue;
				//clipping
				if (finalLeftValue > Short.MAX_VALUE) {
					finalLeftValue = Short.MAX_VALUE;
				}
				else if (finalLeftValue < Short.MIN_VALUE) {
					finalLeftValue = Short.MIN_VALUE;
				}
				if (finalRightValue > Short.MAX_VALUE) {
					finalRightValue = Short.MAX_VALUE;
				}
				else if (finalRightValue < Short.MIN_VALUE) {
					finalRightValue = Short.MIN_VALUE;
				}
				//left channel bytes
				data[i] = (byte)((finalLeftValue >> 8) & 0xFF); //MSB
				data[i + 1] = (byte)(finalLeftValue & 0xFF); //LSB
				//then right channel bytes
				data[i + 2] = (byte)((finalRightValue >> 8) & 0xFF); //MSB
				data[i + 3] = (byte)(finalRightValue & 0xFF); //LSB
				numRead += 4;
			}
		}
		return numRead;
	}

}
