# Subsystem API Specification 13: Media3 MediaSession Callback and Custom Player Command Contract

## Custom Session Commands
- `ACTION_TOGGLE_LIKE`: Favorite/Unfavorite current song.
- `ACTION_SET_SLEEP_TIMER`: Schedule playback stop.
- `ACTION_CYCLE_EQ_PRESET`: Switch active DSP equalizer profile.

## Callback Conformance
Must respond to standard Android media button keyevents (`KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_MEDIA_NEXT`).\n