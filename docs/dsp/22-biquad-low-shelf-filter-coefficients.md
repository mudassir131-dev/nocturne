# Biquad Low Shelf Filter Coefficient Formulas and Gain Response Curves

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **biquad low shelf filter coefficient formulas and gain response curves** within the Nocturne application.

## Key Principles & Invariants
- RBJ Audio EQ Cookbook low-shelf filter coefficient equations.
- Intermediate calculations incorporating shelf slope parameter S and gain multiplier A.
- Smooth transition between baseline unity gain and target low-frequency shelf boost/cut.
- Zero phase distortion above the transition corner frequency.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
