# FAQ: What happens when audio chunks fail to download mid-song?

## Question
What happens when audio chunks fail to download mid-song?

## Answer
ExoPlayer DataSource intercepts network interruptions and executes an exponential backoff retry with decorrelated jitter. The player continues playback from its internal look-ahead buffer (up to 30 seconds of preloaded audio). If network recovery fails before the buffer exhausts, playback pauses gracefully and resumes as soon as connectivity is restored.
