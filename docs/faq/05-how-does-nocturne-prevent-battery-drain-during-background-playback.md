# FAQ: How does Nocturne prevent battery drain during background playback?

## Question
How does Nocturne prevent battery drain during background playback?

## Answer
Nocturne runs as a foreground service with a partial CPU WakeLock held only while audio is playing. Screen rendering, UI recomposition, and video loops are completely paused when the screen is turned off. Audio chunks are pre-buffered so the device cellular radio can return to low-power sleep states between tracks.
