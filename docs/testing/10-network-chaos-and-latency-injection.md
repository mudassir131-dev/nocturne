# Testing 10: Network Failure & Socket Timeout Chaos Injection Suite

## Test Scenarios
- High packet drop (20%), jitter (500ms), and abrupt airplane mode toggles during active playback.
- Ensures playback resumes gracefully from cached forward buffer without unhandled exceptions.
