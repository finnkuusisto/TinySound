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
import java.util.concurrent.atomic.AtomicBoolean;

import kuusisto.tinysound.TinySound;

/**
 * The UpdateRunner class implements Runnable and is what performs automatic
 * updates of the TinySound system.  UpdateRunner is an internal class of the
 * TinySound system and should be of no real concern to the average user of
 * TinySound.
 * 
 * @author Finn Kuusisto
 */
public class UpdateRunner implements Runnable {
		
		private AtomicBoolean running;
		private int updateRate;
		
		/**
		 * Constructs a new UpdateRunner to update the TinySound system at the
		 * specified rate.
		 * @param updateRate number of times to update the TinySound system
		 * every second
		 */
		public UpdateRunner(int updateRate) {
			this.running = new AtomicBoolean();
			this.updateRate = updateRate;
		}
		
		/**
		 * Stop this UpdateRunner from updating the TinySound system.
		 */
		public void stop() {
			this.running.set(false);
		}

		@Override
		public void run() {
			this.running.set(true);
			long nanosPerUpdate = 1000000000L / this.updateRate;
			long lastUpdate = 0;
			//keep running until told to stop
			while (this.running.get()) {
				long currTime = System.nanoTime();
				if ((currTime - lastUpdate) >= nanosPerUpdate) {
					TinySound.update();
				}
				//give the CPU back to the OS for a bit
				try {
					Thread.sleep(0, 100000);
				} catch (InterruptedException e) {}
			}
		}
		
	}