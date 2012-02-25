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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import kuusisto.tinysound.internal.Mixer;
import kuusisto.tinysound.internal.UpdateRunner;

/**
 * TinySound is the main class of the TinySound system.  In order to use the
 * TinySound system, it must be initialized.  After that, Music and Sound
 * objects can be loaded and used.  When finished with the TinySound system, it
 * must be shutdown.  Shutdown is especially important if the system has been
 * initialized to update automatically.
 * 
 * @author Finn Kuusisto
 */
public class TinySound {

	//The system only supports one format, but converts as needed
	public static final AudioFormat FORMAT = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED, //linear signed PCM
			44100, //44.1kHz sampling rate
			16, //16-bit
			1, //only 1 channel fool
			2, //frame size 2 bytes (16-bit, 1 channel)
			44100, //same as sampling rate
			true //big-endian
			);
	
	//the system has only one mixer for both music and sounds
	private static Mixer mixer;
	//the system has a buffer size based on a specified update rate
	private static int updateRate;
	private static byte[] audioBuffer;
	private static int numBytesRead;
	//need a line to the speakers
	private static SourceDataLine outLine;
	//see if the system has been initialized
	private static boolean inited = false;
	//see if the system is auto-updating
	private static boolean autoUpdate;
	private static UpdateRunner autoUpdater;
	
	/**
	 * Initialize the AudioSystem with the default update rate of 40Hz and
	 * auto-updating on.  This is probably sufficient for most users.
	 */
	public static void init() {
		//default to 40Hz, auto-update
		TinySound.init(40, true);
	}
	
	/**
	 * Initialize the AudioSystem with a desired update rate and desired
	 * auto-update setting.  The AudioSystem will attempt to allocate an audio
	 * buffer that is most appropriate for the specified update rate.  If the
	 * AudioSystem is not set to auto-update, the user must update the
	 * AudioSystem at the specified update rate.
	 * @param updateRate the desired update rate of the AudioSystem, in updates
	 * per second
	 * @param autoUpdate true if auto-updating is desired
	 */
	public static void init(int updateRate, boolean autoUpdate) {
		if (TinySound.inited) {
			return;
		}
		//first try to open a line to the speakers
		DataLine.Info info = new DataLine.Info(SourceDataLine.class,
				TinySound.FORMAT);
		if (!javax.sound.sampled.AudioSystem.isLineSupported(info)) {
		    System.err.println("Unsupported output format!");
		    return;
		}
		try {
		    TinySound.outLine = 
		    	(SourceDataLine)javax.sound.sampled.AudioSystem.getLine(info);
		    TinySound.outLine.open(TinySound.FORMAT);
		}
		catch (LineUnavailableException e) {
		    System.err.println("Output line unavailable!");
		    return;
		}
		TinySound.outLine.start();
		//now initialize the other stuff
		TinySound.mixer = new Mixer();
		TinySound.updateRate = updateRate;
		//make the buffer the size needed for update rate
		int bufSize = (int)((TinySound.FORMAT.getSampleRate() *
				TinySound.FORMAT.getFrameSize()) / updateRate) + 1;
		bufSize += (bufSize % 2 == 0) ? 0 : 1; //make it even
		TinySound.audioBuffer = new byte[bufSize];
		TinySound.numBytesRead = 0;
		//start the updater if set to autoUpdate
		if (autoUpdate) {
			TinySound.autoUpdater = new UpdateRunner(updateRate);
			new Thread(TinySound.autoUpdater).start();
		}
		TinySound.autoUpdate = autoUpdate;
		TinySound.inited = true;
	}
	
	/**
	 * Shutdown the AudioSystem.
	 */
	public static void shutdown() {
		if (!TinySound.inited) {
			return;
		}
		TinySound.inited = false;
		TinySound.outLine.stop();
		TinySound.outLine.flush();
		TinySound.mixer.clearMusic();
		TinySound.mixer.clearSounds();
		TinySound.mixer = null;
		TinySound.updateRate = 0;
		TinySound.audioBuffer = null;
		TinySound.numBytesRead = 0;
		//stop the auto-updater if running
		if (TinySound.autoUpdate) {
			TinySound.autoUpdater.stop();
			TinySound.autoUpdater = null;
			TinySound.autoUpdate = false;
		}
	}
	
	/**
	 * Get the current TinySound audio update rate.
	 * @return the current audio update rate
	 */
	public static int getUpdateRate() {
		return TinySound.updateRate;
	}
	
	/**
	 * Determine whether the TinySound is set to auto-update.
	 * @return true if the TinySound is set to auto-update
	 */
	public static boolean isAutoUpdating() {
		return TinySound.autoUpdate;
	}
	
	/**
	 * Write a buffer of audio data to the speakers and fill the audio buffer
	 * for the next update.  This or the other update method must be called
	 * regularly if not auto-updating.
	 */
	public static void update() {
		if (!TinySound.inited) {
			System.err.println("TinySound not initialized!");
			return;
		}
		TinySound.update(true);
	}
	
	/**
	 * Write a buffer of audio data to the speakers and fill the audio buffer
	 * for the next update as specified.  This or the other update method must
	 * be called regularly if not auto-updating.
	 * @param fillNextBuffer true if the next audio buffer should be filled
	 */
	public static void update(boolean fillNextBuffer) {
		if (!TinySound.inited) {
			System.err.println("TinySound not initialized!");
			return;
		}		//read from the mixer (maybe this should be synchronized)
		if (TinySound.numBytesRead <= 0) {
			TinySound.numBytesRead =
				TinySound.mixer.read(TinySound.audioBuffer);
		}
		if (TinySound.numBytesRead <= 0) {
			return;
		}
		//and write to the speakers
		TinySound.outLine.write(TinySound.audioBuffer, 0,
				TinySound.numBytesRead);
		TinySound.numBytesRead = 0;
		//now refill the buffer if desired
		if (fillNextBuffer) {
			TinySound.numBytesRead =
				TinySound.mixer.read(TinySound.audioBuffer);
		}
	}
	
	/**
	 * Load a Music by a resource name.  The resource must be on the classpath
	 * for this to work.
	 * @param name name of the Music resource
	 * @return Music resource as specified, null if not found/loaded
	 */
	public static Music loadMusic(String name) {
		//check if the system is initialized
		if (!TinySound.inited) {
			System.err.println("TinySound not initialized!");
			return null;
		}
		//check for failure
		if (name == null) {
			return null;
		}
		//check for correct naming
		if (!name.startsWith("/")) {
			name = "/" + name;
		}
		InputStream stream = TinySound.class.getResourceAsStream(name);
		return TinySound.loadMusic(stream);
	}
	
	/**
	 * Load a Music by a File.
	 * @param file the Music file to load
	 * @return Music from file as specified, null if not found/loaded
	 */
	public static Music loadMusic(File file) {
		//check if the system is initialized
		if (!TinySound.inited) {
			System.err.println("TinySound not initialized!");
			return null;
		}
		//check for failure
		if (file == null) {
			return null;
		}
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open file " + file + "!");
			return null;
		}
		return TinySound.loadMusic(stream);
	}
	
	/**
	 * Load a Music by a URL.
	 * @param url the URL of the Music
	 * @return Music from URL as specified, null if not found/loaded
	 */
	public static Music loadMusic(URL url) {
		//check if the system is initialized
		if (!TinySound.inited) {
			System.err.println("TinySound not initialized!");
			return null;
		}
		//check for failure
		if (url == null) {
			return null;
		}
		InputStream stream = null;
		try {
			stream = url.openStream();
		} catch (IOException e) {
			System.err.println("Unable to open URL " + url + "!");
			return null;
		}
		return TinySound.loadMusic(stream);
	}
	
	/**
	 * Load a Music by an InputStream
	 * @param stream stream of the Music
	 * @return Music from the stream as specified, null if not found/loaded
	 */
	public static Music loadMusic(InputStream stream) {
		//check if the system is initialized
		if (!TinySound.inited) {
			System.err.println("TinySound not initialized!");
			return null;
		}
		//check for failure
		if (stream == null) {
			return null;
		}
		AudioInputStream audioStream = TinySound.getValidAudioStream(stream);
		//check for failure
		if (audioStream == null) {
			return null;
		}
		//try to read all the bytes
		byte[] data = TinySound.readAllBytes(audioStream);
		//check for failure
		if (data == null) {
			return null;
		}
		//construct the Music object and register it with the mixer
		return new Music(data, TinySound.mixer);
	}
	
	/**
	 * Load a Sound by a resource name.  The resource must be on the classpath
	 * for this to work.
	 * @param name name of the Sound resource
	 * @return Sound resource as specified, null if not found/loaded
	 */
	public static Sound loadSound(String name) {
		//check if the system is initialized
		if (!TinySound.inited) {
			System.err.println("TinySound not initialized!");
			return null;
		}
		//check for failure
		if (name == null) {
			return null;
		}
		//check for correct naming
		if (!name.startsWith("/")) {
			name = "/" + name;
		}
		InputStream stream = TinySound.class.getResourceAsStream(name);
		return TinySound.loadSound(stream);

	}
	
	/**
	 * Load a Sound by a File.
	 * @param file the Sound file to load
	 * @return Sound from file as specified, null if not found/loaded
	 */
	public static Sound loadSound(File file) {
		//check if the system is initialized
		if (!TinySound.inited) {
			System.err.println("TinySound not initialized!");
			return null;
		}
		//check for failure
		if (file == null) {
			return null;
		}
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open file " + file + "!");
			return null;
		}
		return TinySound.loadSound(stream);
	}
	
	/**
	 * Load a Sound by a URL.
	 * @param url the URL of the Sound
	 * @return Sound from URL as specified, null if not found/loaded
	 */
	public static Sound loadSound(URL url) {
		//check if the system is initialized
		if (!TinySound.inited) {
			System.err.println("TinySound not initialized!");
			return null;
		}
		//check for failure
		if (url == null) {
			return null;
		}
		InputStream stream = null;
		try {
			stream = url.openStream();
		} catch (IOException e) {
			System.err.println("Unable to open URL " + url + "!");
			return null;
		}
		return TinySound.loadSound(stream);
	}
	
	/**
	 * Load a Sound by an InputStream.
	 * @param stream stream of the Sound
	 * @return Sound from stream as specified, null if not found/loaded
	 */
	public static Sound loadSound(InputStream stream) {
		//check if the system is initialized
		if (!TinySound.inited) {
			System.err.println("TinySound not initialized!");
			return null;
		}
		//check for failure
		if (stream == null) {
			return null;
		}
		AudioInputStream audioStream =
			TinySound.getValidAudioStream(stream);
		//check for failure
		if (audioStream == null) {
			return null;
		}
		//try to read all the bytes
		byte[] data = TinySound.readAllBytes(audioStream);
		//check for failure
		if (data == null) {
			return null;
		}
		//construct the Sound object
		return new Sound(data, TinySound.mixer);
	}
	
	/**
	 * Reads all of the bytes from an AudioInputStream.
	 * @param stream the stream to read
	 * @return all bytes from the stream, null if error
	 */
	private static byte[] readAllBytes(AudioInputStream stream) {
		//read all the bytes (assuming 16-bit, 1 channel)
		int numBytes = (int)stream.getFrameLength() *
			TinySound.FORMAT.getFrameSize();
		byte[] data = new byte[numBytes];
		try {
			int numRead = stream.read(data);
			if (numRead != numBytes) {
				//didn't read all of the data for some reason
				System.err.println("Failed to read all data from stream!\n" +
						numRead + " of " + numBytes + " read");
				//return what we did read I guess
				data = Arrays.copyOf(data, numRead);
			}
		}
		catch (IOException e) {
			System.err.println("Error reading all bytes from stream!");
			return null;
		}
		finally {
			try {
				stream.close();
			} catch (IOException e) {}
		}
		return data;
	}
	
	/**
	 * Gets and AudioInputStream in the TinySound system format.
	 * @param stream InputStream of the resource
	 * @return the specified stream as an AudioInputStream stream, null if
	 * failure
	 */
	private static AudioInputStream getValidAudioStream(InputStream stream) {
		AudioInputStream audioStream = null;
		try {
			audioStream = AudioSystem.getAudioInputStream(stream);
			//now see if it is the correct format
			if (audioStream.getFormat().equals(TinySound.FORMAT)) {
				return audioStream;
			}
			//if not, see if it can be converted
			if (!AudioSystem.isConversionSupported(TinySound.FORMAT,
					audioStream.getFormat())) {
				System.err.println("Unable to convert audio resource!");
				System.err.println(audioStream.getFormat());
				audioStream.close();
				return null;
			}
			//otherwise convert it
			audioStream = AudioSystem.getAudioInputStream(TinySound.FORMAT,
					audioStream);
		}
		catch (UnsupportedAudioFileException e) {
			System.err.println("Unsupported audio resource!\n" +
					e.getMessage());
			try {
				stream.close();
			} catch (IOException e1) {}
			return null;
		}
		catch (IOException e) {
			System.err.println("Error getting resource stream!\n" +
					e.getMessage());
			try {
				stream.close();
			} catch (IOException e1) {}
			return null;
		}
		return audioStream;
	}
	
}
