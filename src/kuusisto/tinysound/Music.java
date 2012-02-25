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
import kuusisto.tinysound.internal.MusicReference;

/**
 * The Music class is an abstraction for music.  Music objects should only be
 * loaded via the TinySound <code>loadMusic()</code> functions.  Music can be
 * played, paused, resumed, stopped and looped from specified positions.
 * 
 * @author Finn Kuusisto
 */
public class Music {
	
	private byte[] data;
	private Mixer mixer;
	private MusicReference reference;
	
	/**
	 * Construct a new Music with the given music data and the Mixer with which
	 * to register this Music.
	 * @param data music data
	 * @param mixer Mixer with which this Music is registered
	 */
	public Music(byte[] data, Mixer mixer) {
		this.data = data;
		this.mixer = mixer;
		this.reference = new MusicReference(this.data, false, false, 0, 0, 1.0);
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
		int byteIndex = this.reference.getLoopPosition();
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
		int byteIndex = this.reference.getLoopPosition();
		return (byteIndex / TinySound.FORMAT.getFrameSize());
	}
	
	/**
	 * Get the loop position of this Music by seconds.
	 * @return loop position by seconds
	 */
	public double getLoopPositionBySeconds() {
		int byteIndex = this.reference.getLoopPosition();
		return (int)(byteIndex / (TinySound.FORMAT.getFrameRate() *
				TinySound.FORMAT.getFrameSize()));
	}
	
	/**
	 * Set the loop position of this Music by sample frame.
	 * @param frameIndex sample frame loop position to set
	 */
	public void setLoopPositionByFrame(int frameIndex) {
		int byteIndex = frameIndex * TinySound.FORMAT.getFrameSize();
		this.reference.setLoopPosition(byteIndex);
	}
	
	/**
	 * Set the loop position of this Music by seconds.
	 * @param seconds loop position to set by seconds
	 */
	public void setLoopPositionBySeconds(double seconds) {
		int byteIndex = (int)(seconds * TinySound.FORMAT.getFrameRate() *
			TinySound.FORMAT.getFrameSize());
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
		this.mixer = null;
		this.data = null;
		this.reference = null;
	}

}
