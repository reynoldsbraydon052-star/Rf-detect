import re

with open('app/src/main/java/com/example/AudioRadarTracker.kt', 'r') as f:
    content = f.read()

# Replace the loop in AudioRadarTracker to support state machine and continuous tone.
new_loop = """
                var lastState = -1 // 0=idle, 1=pulse, 2=continuous
                
                while (isActive && isPlaying) {
                    val dist = proximityDistance
                    val isClose = dist <= 2.0 // proximity threshold
                    
                    if (isClose) {
                        if (lastState != 2) {
                            // Start continuous tone
                            lastState = 2
                            val continuousSamples = sampleRate * 5 // 5 seconds
                            val buffer = ShortArray(continuousSamples)
                            var ph = 0.0
                            val phInc = 2.0 * Math.PI * 1800.0 / sampleRate
                            for (i in buffer.indices) {
                                buffer[i] = (sin(ph) * 22000 * volumeLevel).toInt().toShort()
                                ph += phInc
                            }
                            // Run in background so we don't block the loop entirely? Actually AudioTrack blocking is fine if we stop it on state change
                            // Better: write smaller chunks so we can interrupt it, or just use ToneGenerator
                        }
                    }
                }
"""

# ToneGenerator is much easier for state machine than manual AudioTrack. Let's rewrite AudioRadarTracker to use ToneGenerator, similar to what I did in SonarEngine, but keeping the AudioRadarTracker name.
