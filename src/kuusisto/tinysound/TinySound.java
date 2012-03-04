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

import java.io.ByteArrayInputStream;
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
 * must be shutdown.
 * 
 * @author Finn Kuusisto
 */
public class TinySound {

	/**
	 * The internal format used by TinySound.
	 */
	public static final AudioFormat FORMAT = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED, //linear signed PCM
			44100, //44.1kHz sampling rate
			16, //16-bit
			2, //2 channels fool
			4, //frame size 4 bytes (16-bit, 2 channel)
			44100, //same as sampling rate
			true //big-endian
			);
	
	//the system has only one mixer for both music and sounds
	private static Mixer mixer;
	//need a line to the speakers
	private static SourceDataLine outLine;
	//see if the system has been initialized
	private static boolean inited = false;
	//auto-updater for the system
	private static UpdateRunner autoUpdater;
	
	/**
	 * Initialize the AudioSystem.  This must be called before loading audio.
	 */
	public static void init() {
		if (TinySound.inited) {
			return;
		}
		//try to open a line to the speakers
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
		//now initialize the mixer
		TinySound.mixer = new Mixer();
		//initialize and start the updater
		TinySound.autoUpdater = new UpdateRunner(TinySound.mixer,
				TinySound.outLine);
		Thread updateThread = new Thread(TinySound.autoUpdater);
		updateThread.setDaemon(true);
		TinySound.inited = true;
		updateThread.start();
	}
	
	/**
	 * Shutdown the AudioSystem.
	 */
	public static void shutdown() {
		if (!TinySound.inited) {
			return;
		}
		TinySound.inited = false;
		//stop the auto-updater if running
		TinySound.autoUpdater.stop();
		TinySound.autoUpdater = null;
		TinySound.outLine.stop();
		TinySound.outLine.flush();
		TinySound.mixer.clearMusic();
		TinySound.mixer.clearSounds();
		TinySound.mixer = null;
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
		byte[][] data = TinySound.readAllBytes(audioStream);
		//check for failure
		if (data == null) {
			return null;
		}
		//construct the Music object and register it with the mixer
		return new Music(data[0], data[1], TinySound.mixer);
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
		byte[][] data = TinySound.readAllBytes(audioStream);
		//check for failure
		if (data == null) {
			return null;
		}
		//construct the Sound object
		return new Sound(data[0], data[1], TinySound.mixer);
	}
	
	/**
	 * Reads all of the bytes from an AudioInputStream.
	 * @param stream the stream to read
	 * @return all bytes from the stream, null if error
	 */
	private static byte[][] readAllBytes(AudioInputStream stream) {
		//left and right channels
		byte[][] data = null;
		int numChannels = stream.getFormat().getChannels();
		//handle 1-channel
		if (numChannels == 1) {
			byte[] left = TinySound.readAllBytesOneChannel(stream);
			//check failure
			if (left == null) {
				return null;
			}
			data = new byte[2][];
			data[0] = left;
			data[1] = left; //don't copy for the right channel
		} //handle 2-channel
		else if (numChannels == 2) {
			data = TinySound.readAllBytesTwoChannel(stream);
		}
		else { //wtf?
			System.err.println("Unable to read " + numChannels + " channels!");
		}
		return data;
	}
	
	/**
	 * Reads all of the bytes from a 1-channel AudioInputStream.
	 * @param stream the stream to read
	 * @return all bytes from the stream, null if error
	 */
	private static byte[] readAllBytesOneChannel(AudioInputStream stream) {
		//read all the bytes (assuming 1-channel)
		int numBytes = (int)stream.getFrameLength() *
			stream.getFormat().getFrameSize();
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
			try { stream.close(); } catch (IOException e) {}
		}
		return data;
	}
	
	/**
	 * Reads all of the bytes from a 2-channel AudioInputStream.
	 * @param stream the stream to read
	 * @return all bytes from the stream, null if error
	 */
	private static byte[][] readAllBytesTwoChannel(AudioInputStream stream) {
		//read all the bytes (assuming 16-bit, 2-channel)
		int numBytesPerChannel = (int)stream.getFrameLength() *
			(stream.getFormat().getFrameSize() / 2);
		byte[] left = new byte[numBytesPerChannel];
		byte[] right = new byte[numBytesPerChannel];
		byte[][] data = null;
		try {
			//read one frame at a time
			byte[] frame = new byte[stream.getFormat().getFrameSize()];
			for (int i = 0; i < numBytesPerChannel; i += 2) {
				int bytesRead = stream.read(frame);
				//hope it actually reads a full frame (it should)
				if (bytesRead != stream.getFormat().getFrameSize()) {
					System.err.println("Failed to read full frame of data " +
							"from stream!");
				}
				//interleaved left then right
				left[i] = frame[0];
				left[i + 1] = frame[1];
				right[i] = frame[2];
				right[i + 1] = frame[3];
			}
			data = new byte[2][];
			data[0] = left;
			data[1] = right;
		}
		catch (IOException e) {
			System.err.println("Error reading all bytes from stream!");
			return null;
		}
		finally {
			try { stream.close(); } catch (IOException e) {}
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
			//1-channel can also be treated as stereo
			AudioFormat mono = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
					44100, 16, 1, 2, 44100, true);
			//1 or 2 channel 8-bit may be easy to convert
			AudioFormat mono8 =	new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
					44100, 8, 1, 1, 44100, true);
			AudioFormat stereo8 =
				new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 8, 2, 2,
					44100, true);
			//now check formats (attempt conversion as needed)
			if (audioStream.getFormat().equals(TinySound.FORMAT) ||
					audioStream.getFormat().equals(mono)) {
				return audioStream;
			} //check conversion to TinySound format
			else if (AudioSystem.isConversionSupported(TinySound.FORMAT,
					audioStream.getFormat())) {
				audioStream = AudioSystem.getAudioInputStream(TinySound.FORMAT,
						audioStream);
			} //check conversion to mono alternate
			else if (AudioSystem.isConversionSupported(mono,
					audioStream.getFormat())) {
				audioStream = AudioSystem.getAudioInputStream(mono,
						audioStream);
			} //try convert from 8-bit, 2-channel
			else if (audioStream.getFormat().equals(stereo8) ||
					AudioSystem.isConversionSupported(stereo8,
							audioStream.getFormat())) {
				//convert to 8-bit stereo first?
				if (!audioStream.getFormat().equals(stereo8)) {
					audioStream = AudioSystem.getAudioInputStream(stereo8,
							audioStream);
				}
				audioStream = TinySound.getTwoChannel8Bit(audioStream);
			} //try convert from 8-bit, 1-channel
			else if (audioStream.getFormat().equals(mono8) ||
					AudioSystem.isConversionSupported(mono8,
							audioStream.getFormat())) {
				//convert to 8-bit mono first?
				if (!audioStream.getFormat().equals(mono8)) {
					audioStream = AudioSystem.getAudioInputStream(mono8,
							audioStream);
				}
				audioStream = TinySound.getOneChannel8Bit(audioStream);
			} //it's time to give up
			else {
				System.err.println("Unable to convert audio resource!");
				System.err.println(audioStream.getFormat());
				audioStream.close();
				return null;
			}
		}
		catch (UnsupportedAudioFileException e) {
			System.err.println("Unsupported audio resource!\n" +
					e.getMessage());
			try { stream.close(); } catch (IOException e1) {}
			return null;
		}
		catch (IOException e) {
			System.err.println("Error getting resource stream!\n" +
					e.getMessage());
			try { stream.close(); } catch (IOException e1) {}
			return null;
		}
		return audioStream;
	}
	
	/**
	 * Converts an 8-bit, signed, 1-channel AudioInputStream to 16-bit, signed,
	 * 1-channel.
	 * @param stream stream to convert
	 * @return converted stream
	 */
	private static AudioInputStream getOneChannel8Bit(AudioInputStream stream) {
		//assuming 8-bit, 1-channel to 16-bit, 1-channel
		int numFrames = (int)stream.getFrameLength();
		int numBytes = numFrames * 2;
		byte[] data = new byte[numBytes];
		try {
			//read bytes one-by-one, convert to int, and then to 16-bit
			byte[] buf = new byte[1];
			for (int i = 0; i < numBytes; i += 2) {
				//read a byte
				int numRead = stream.read(buf);
				if (numRead <= 0) {
					System.err.println("Failed to read full frame of data " +
					"from stream!");
				}
				//convert it to an double
				double floatVal = (double)buf[0];
				floatVal /= (floatVal < 0) ? 128 : 127;
				if (floatVal < -1.0) { //just in case
					floatVal = -1.0;
				}
				else if (floatVal > 1.0) {
					floatVal = 1.0;
				}
				//convert it to an int and then to 2 bytes
				int val = (int)(floatVal * Short.MAX_VALUE);
				data[i] = (byte)((val >> 8) & 0xFF); //MSB
				data[i + 1] = (byte)(val & 0xFF); //LSB
			}
		}
		catch (IOException e) {
			System.err.println("Error reading all bytes from stream!");
			return null;
		}
		finally {
			try { stream.close(); } catch (IOException e) {}
		}
		AudioFormat mono16 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				44100, 16, 1, 2, 44100, true);
		return new AudioInputStream(new ByteArrayInputStream(data), mono16,
				numFrames);
	}
	
	/**
	 * Converts an 8-bit, signed, 2-channel AudioInputStream to 16-bit, signed,
	 * 2-channel.
	 * @param stream stream to convert
	 * @return converted stream
	 */
	private static AudioInputStream getTwoChannel8Bit(AudioInputStream stream) {
		//assuming 8-bit, 2-channel to 16-bit, 2-channel
		int numFrames = (int)stream.getFrameLength();
		int numBytes = numFrames * 2 * 2; //2-bytes, 2-channels
		byte[] data = new byte[numBytes];
		try {
			//read frames one-by-one, convert to ints, and then to 16-bit
			byte[] buf = new byte[2];
			for (int i = 0; i < numBytes; i += 4) {
				//read a frame
				int numRead = stream.read(buf);
				if (numRead <= 1) {
					System.err.println("Failed to read full frame of data " +
					"from stream!");
				}
				//convert them to doubles
				double leftFloatVal = (double)buf[0];
				double rightFloatVal = (double)buf[1];
				leftFloatVal /= (leftFloatVal < 0) ? 128 : 127;
				rightFloatVal /= (rightFloatVal < 0) ? 128 : 127;
				if (leftFloatVal < -1.0) { //just in case
					leftFloatVal = -1.0;
				}
				else if (leftFloatVal > 1.0) {
					leftFloatVal = 1.0;
				}
				if (rightFloatVal < -1.0) { //just in case
					rightFloatVal = -1.0;
				}
				else if (rightFloatVal > 1.0) {
					rightFloatVal = 1.0;
				}
				//convert them to ints and then to 2 bytes each
				int leftVal = (int)(leftFloatVal * Short.MAX_VALUE);
				int rightVal = (int)(rightFloatVal * Short.MAX_VALUE);
				//left channel bytes
				data[i] = (byte)((leftVal >> 8) & 0xFF); //MSB
				data[i + 1] = (byte)(leftVal & 0xFF); //LSB
				//then right channel bytes
				data[i + 2] = (byte)((rightVal >> 8) & 0xFF); //MSB
				data[i + 3] = (byte)(rightVal & 0xFF); //LSB
			}
		}
		catch (IOException e) {
			System.err.println("Error reading all bytes from stream!");
			return null;
		}
		finally {
			try { stream.close(); } catch (IOException e) {}
		}
		AudioFormat stereo16 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				44100, 16, 2, 4, 44100, true);
		return new AudioInputStream(new ByteArrayInputStream(data), stereo16,
				numFrames);
	}
	
}
