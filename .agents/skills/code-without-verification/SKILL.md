---
name: code-without-verification
description: Implement code changes on the user's instruction without running post-change verification unless the user explicitly asks for it.
---

# Code changes without verification

When the user asks for code work, implement the requested change and inspect the relevant source needed to make it correctly. Do not run post-change verification: no tests, builds, linters, type checks, format checks, application launches, smoke checks, or other commands intended to validate the change.

If the user explicitly asks to verify or test, that instruction overrides this preference for that request. Otherwise, finish after editing and report what was changed without implying that it was tested. If verification is needed to diagnose an issue, ask the user before running it.

This skill applies to code modification tasks in this project. It does not prohibit reading source, configuration, or documentation before editing, nor does it apply to non-code deliverables.
