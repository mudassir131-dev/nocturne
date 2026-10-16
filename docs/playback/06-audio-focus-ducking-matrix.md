# Playback 06: Audio Focus Request Matrix for Phone Calls, Alarms & Nav Prompts

## Focus Rules
- `AUDIOFOCUS_LOSS`: Pause and release audio sinks.
- `AUDIOFOCUS_LOSS_TRANSIENT`: Pause playback temporarily, resume automatically on focus regain.
- `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`: Lower volume to 20% during GPS turn-by-turn prompts.
