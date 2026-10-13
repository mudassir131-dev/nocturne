# FAQ: Why does Nocturne use a custom C++ audio engine?

## Question
Why does Nocturne use a custom C++ audio engine?

## Answer
Android standard AudioFX API is notoriously inconsistent across manufacturers (Samsung, Xiaomi, Google Pixel), frequently introducing audio latency, clipping, or phase distortion. Nocturne C++ JNI audio engine (jni_bridge.cpp) processes 32-bit floating-point audio directly, guaranteeing high-fidelity, bit-accurate equalization, bass boost, and virtualizer processing across all hardware.
