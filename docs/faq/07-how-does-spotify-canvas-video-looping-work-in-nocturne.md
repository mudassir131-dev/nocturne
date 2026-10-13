# FAQ: How does Spotify Canvas video looping work in Nocturne?

## Question
How does Spotify Canvas video looping work in Nocturne?

## Answer
Nocturne queries Canvas video URLs associated with the track, caches the short MP4 loop, and renders it onto a hardware-accelerated SurfaceView behind the player controls. Canvas playback automatically mutes audio and loops seamlessly without stuttering or impacting music playback performance.
