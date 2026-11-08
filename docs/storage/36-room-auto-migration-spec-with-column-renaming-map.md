# Room Auto Migration Spec with Column Renaming Map

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **room auto migration spec with column renaming map** within the Nocturne application.

## Key Principles & Invariants
- Implements Room AutoMigration specifications with custom RenameColumn and DeleteColumn mapping classes.
- Retains user playlist data, play counts, and downloaded media references across major app version upgrades.
- Automated schema JSON generation in schemas directory committed to version control for CI validation.
- Fallback destructive migration explicitly disabled to guarantee zero data loss on database schema evolution.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
