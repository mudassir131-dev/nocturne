# Networking 05: Exponential Backoff with Full Decorrelated Jitter Retry Curve

## Backoff Algorithm
$$Sleep(i) = \min(MaxDelay, 	ext{random}(MinDelay, Sleep(i-1) 	imes 3))$$
Prevents thundering herd problems when reconnecting after temporary network drops.
