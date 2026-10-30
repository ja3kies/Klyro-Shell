# Architecture decisions

## ADR-001 — Candidate base

**Status:** Provisional  
**Date:** 2026-10-30

### Context

Oryn Terminal must be a separate Android app that coexists with the user's current Termux installation. It must have a real terminal, a maintainable session lifecycle, and a usable Termux-compatible package environment.

### Decision

Use **Chimera Terminal as the first prototype candidate**, subject to the P0 validation gates in the technical audit.

Do not adopt NewTermux as the direct base: its production app identity is `com.termux`, while its separate coexistence build intentionally disables real shell command execution.

Keep official Termux and NewTermux as upstream references for package/bootstrap integration, terminal behavior, and UI patterns.

### Consequences

- Oryn's Android application ID will be `com.oryn.terminal`.
- The real Termux installation and its private data must remain untouched.
- The Termux userland will run from Oryn-owned private storage, with proot mapping only the paths required by Termux-built binaries.
- Bootstrap freshness, integrity verification, package operations, and lifecycle behavior are release blockers.
- This decision may be reversed if the proot/bootstrap path fails on a real ARM64 device.

### Explicit non-goals

The assistant's design and engineering reference methods (Apple Design principles, Superpowers, Ponytail, and Claude Code best practices) are working methods, not runtime dependencies or project components. They must not be added to the app as dependencies.
