About
-----
TinySound is a simple sound system that wraps the standard Java sound libraries.
It is "tiny" in that it is intended to have a small, easy-to-use interface with
everything that you need to play sounds and music, and nothing that you don't.

Releases
--------
If you would just like to download the jar files, see the releases page:

http://finnkuusisto.github.com/TinySound/releases

License
-------
TinySound is licensed under the BSD 2-Clause license.  A copy of the license can
be found in the header of every source file as well as in the LICENSE file
included with the TinySound system.

Audio Formats
-------------
TinySound stores all audio as 16-bit, 44.1kHz, 2-channel, linear PCM data
internally.  It makes an effort to convert other formats, but will not be able
to handle all formats.  As for container formats, TinySound should be able to
load any container types supported by your version of Java.  This should include
WAV at the very least.
TinySound can also load Ogg files containing audio in the Vorbis format with the
inclusion of the libraries found in the lib directory.  If you intend to use Ogg
Vorbis files with TinySound just include the jorbis, tritonus_share and
vorbisspi jar files on your CLASSPATH along with TinySound.

Javadocs
--------
You should only be concerned with the classes in the `kuusisto.tinysound`
package.

http://finnkuusisto.github.com/TinySound/doc

Using TinySound
---------------
There are 3 classes that you need to know when using TinySound: `TinySound`,
`Music` and `Sound`.  TinySound is the main system class, Music is an
abstraction for music, and Sound is an abstraction for a sound effect.  Simple.

__TinySound__
There are really only 2 steps you need to worry about with the TinySound class.
1. Initialization
2. Shutdown

1. Initialization is accomplished via the `init()` function.  It takes no
arguments and sets up the system for you to play audio data.  TinySound creates
a daemon thread to automatically write audio data to the speakers.  For those
with special requirements and who are very familiar with the Java sound
libraries, there is an alternative `init()` function which allows selection of
how a line is opened to the speakers.  See the Javadocs for more detail.

2. Shutdown is accomplished via the `shutdown()` function.  This shuts down the
update thread and clears resources in use.

__Music__
You load Music objects via the TinySound `loadMusic()` functions.  Music objects
can be started, stopped, paused, resumed, and looped from specified positions.
If you are done using a particular Music object, you can also unload its sound
data from the system via its `unload()` method.  See the Javadocs for more
detail.

__Sound__
You load Sound objects via the TinySound `loadSound()` functions.  Sound objects
work differently from Music objects as you can only play them (no pausing etc.).
When a Sound is played it is queued to be played from the speakers once.  Of
course, you can play a Sound multiple times in an overlapping fashion so it is
generally useful for sound effects.  See the Javadocs for more detail.

Memory Usage
------------
The basic loading functions for Music and Sound objects produce implementations
that store all audio data in memory.  This is good for maintaining low latency,
but can also require a lot of heap space if you load many, or particularly long,
audio resources.  There are loading functions available that allow you to
request that the audio data be streamed from a file.  If this is requested, the
audio data will first be converted as usual and then written to a temporary file
from which it will be streamed.  This will dramatically reduce the overall
memory usage (after loading), but can potentially introduce occasional latency
when reading from disk.

Example
-------
There is a very simple example provided in the example directory.  You'll need
sound resources with the specified names on the classpath if you want to try the
example without modifying it.  Note that the example does not demonstrate all of
TinySound's features.  See the Javadocs for more detail.