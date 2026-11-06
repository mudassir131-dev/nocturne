# KuGou Candidate Lyrics Selection Search and Access Key Payload Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **kugou candidate lyrics selection search and access key payload contract** within the Nocturne application.

## Key Principles & Invariants
- Endpoint: `GET http://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword={keyword}&duration={ms}`.
- Extraction of candidate list, accessKey, and id tokens.
- Secondary endpoint: `GET http://lyrics.kugou.com/download?ver=1&client=pc&id={id}&accesskey={key}&fmt=lrc`.
- Base64 decoding and charset conversion for East Asian character sets.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
