<!--
Read AGENTS.md first: it applies to humans and AI agents alike.
This page is part of how this repository shows responsible AI-assisted development. Fill it in honestly.
-->

## What and why

<!-- What changes, and why? Link the issue if there is one. -->

## How AI was involved

- [ ] No AI was used for this change
- [ ] An AI assistant helped (suggestions, review, boilerplate)
- [ ] An AI agent wrote most or all of the change

Tool and model, if any:

## What was verified, and by whom

<!-- Tick only what actually happened. Unticked boxes are fine; they tell the reviewer where to look. -->

- [ ] I ran `./gradlew ktlintCheck detekt :detekt-rules:test testDebugUnitTest jacocoTestCoverageVerification lintDebug assembleDebug`
- [ ] I tried the change on an emulator or a device
- [ ] I read the code line by line (not only the diff summary)
- [ ] New behaviour has tests, including failure paths
- [ ] Documentation is updated (`README.md`, `CONTRIBUTING.md`, KDoc, ADR if a decision changed)

**Review depth** (who looked at the code, and how closely):

**Not verified or known gaps:**

## History and safety

- [ ] Each commit is one logical change, builds on its own and follows Conventional Commits (`tools/check-commits.sh`)
- [ ] No `fixup!`, `squash!` or "address review" commits are left
- [ ] A database schema change has a Room migration and a migration test
- [ ] No secrets, keystores or API keys, and no text from copyrighted books (code, tests, fixtures, screenshots)

## Screenshots

<!-- For UI changes: before and after. -->
