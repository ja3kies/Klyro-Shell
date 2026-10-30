# Oryn Terminal

An independent Android terminal app for personal use.

> **Status: technical evaluation / foundation phase.** No production-ready terminal environment is claimed yet.

## Product direction

Oryn Terminal is a standalone Android app designed to coexist with the user's existing Termux installation without modifying, replacing, migrating, or deleting Termux or its data.

- Application ID: `com.oryn.terminal`
- Platform: Android
- Primary implementation: Kotlin / Android
- UI direction: Jetpack Compose + Material 3
- Visual system: **Neon Obsidian**
- Default shell target: Bash
- Optional shell: Zsh

## Non-negotiable isolation rule

Oryn Terminal must use its own Android application sandbox, private storage, home directory, prefix, package state, configuration, and session data. It must never write to Termux's private directories or depend on Termux being installed.

## Visual foundation

| Token | Value |
|---|---|
| Background | `#0B0F19` |
| Surface | `#111827` |
| Secondary surface | `#1E293B` |
| Primary text | `#E2E8F0` |
| Secondary text | `#94A3B8` |
| Cyan | `#00F0FF` |
| Purple | `#8338EC` |
| Error | `#FF0055` |
| Success | `#00FF66` |

Use neon accents with restraint. Prioritize terminal readability, hierarchy, touch ergonomics, accessibility, and responsive behavior.

## First milestone

Validate a real ARM64 terminal build that installs beside Termux and can independently run a shell. Package-manager compatibility is a separate acceptance gate: a visible `pkg` command is not proof that package installation works.

## Technical evaluation

See:
- [Technical audit](docs/TECHNICAL_AUDIT.md)
- [Architecture decisions](docs/ARCHITECTURE_DECISIONS.md)

## V1 scope

Real shell and PTY sessions, horizontal session tabs, persistent session handling, history, ANSI support, copy/paste/selection, Bash, optional Zsh, a working package manager, and a reproducible ARM64 APK build.

Cloud sync, collaboration, IDE/editor features, and an AI assistant are out of scope for V1.
