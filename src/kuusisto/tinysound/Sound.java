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
package kuusisto.tinysound;
import kuusisto.tinysound.internal.Mixer;
import kuusisto.tinysound.internal.SoundReference;

/**
 * The Sound class is an abstraction for sound effects.  Sound objects should
 * only be loaded via the TinySound <code>loadSound()</code> functions.  Sound
 * can be played repeatedly in an overlapping fashion.
 * 
 * @author Finn Kuusisto
 */
public class Sound {
	
	private static int soundCount = 0;
	
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
	public Sound(byte[] left, byte[] right, Mixer mixer) {
		this.left = left;
		this.right = right;
		this.mixer = mixer;
		//get the next ID
		this.ID = Sound.soundCount;
		Sound.soundCount++;
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
		SoundReference ref = new SoundReference(this.left, this.right, volume,
				this.ID);
		this.mixer.registerSoundReference(ref);
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

}
