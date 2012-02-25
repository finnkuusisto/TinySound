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
	
	/**
	 * Construct a new Mixer for TinySound system.
	 */
	public Mixer() {
		this.musics = new ArrayList<MusicReference>();
		this.sounds = new ArrayList<SoundReference>();
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
	 * @return number of bytes read into buffer
	 */
	public int read(byte[] data) {
		//*****************************************//
		//  assume big-endian, 16-bit, signed PCM  //
		//*****************************************//
		int numRead = 0;
		boolean bytesRead = true; //terminate early if out of bytes
		for (int i = 0; i < data.length && bytesRead; i += 2) {
			//first assume we are done
			bytesRead = false;
			//need to track value across audio sources
			double value = 0.0;
			//go through all the music first
			synchronized (this.musics) {
				for (int m = 0; m < this.musics.size(); m++) {
					MusicReference music = this.musics.get(m);
					//is the music playing and are there bytes available
					if (music.getPlaying() && music.bytesAvailable() > 0) {
						//add this music to the mix by volume
						int next = music.nextTwoBytes(true);
						double volume = music.getVolume();
						value += (next * volume);
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
						int next = ((sound.nextByte() << 8) | 
								(sound.nextByte() & 0xFF));
						double volume = sound.getVolume();
						value += (next * volume);
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
				int finalValue = (int)value;
				//clipping
				if (finalValue > Short.MAX_VALUE) {
					finalValue = Short.MAX_VALUE;
				}
				else if (finalValue < Short.MIN_VALUE) {
					finalValue = Short.MIN_VALUE;
				}
				data[i] = (byte)((finalValue >> 8) & 0xFF); //left byte
				data[i + 1] = (byte)(finalValue & 0xFF); //right byte
				numRead += 2;
			}
		}
		return numRead;
	}

}
