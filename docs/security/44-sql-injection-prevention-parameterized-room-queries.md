# SQL Injection Prevention Parameterized Room Queries

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **sql injection prevention parameterized room queries** within the Nocturne application.

## Key Principles & Invariants
- Enforces 100% parameterized SQLite statements across all Room DAO queries and SQLite query builders.
- Rejects dynamic string concatenation of user-provided search keywords or playlist names into raw SQL strings.
- Escapes special SQLite LIKE wildcard characters in search terms via dedicated SQL query sanitization utility.
- Automated Android Lint checks in CI flagging any un-parameterized raw SQL query invocations.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
