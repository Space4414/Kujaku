# Kotatsu parser APK build handoff

Repo: [Space4414/Kujaku](https://github.com/Space4414/Kujaku)
Branch: `feature/kotatsu-parsers-redo` (open PR #2 into `main`).
Purpose: get the Kotatsu parsers integration to a CI-built APK so it can be installed and tested.

## Verified timeline and investigation log

1. Located the repository through the connected GitHub account. The PR branch was `feature/kotatsu-parsers-redo`; PR #2 is open and targets `main`.
2. Confirmed the JitPack credential is present in repository Actions secrets by name only (`JITPACK_AUTH_TOKEN`). Its value was not read, printed, or copied.
3. Reviewed the earlier CI history: build attempts 2–4 failed before APK upload. The user-reported failure in attempt 3 was HTTP 403 for `RepoDevil/AnimatedBottomBar:7fcb9af`.
4. Reviewed commits `9ed1d1b` (pass JitPack token to Gradle), `df20c88` (use public upstream AnimatedBottomBar), and `432f487` (move AnimatedBottomBar to Maven Central).
5. Verified the current dependency is `nl.joery.animatedbottombar:library:1.1.0`; its Maven Central POM is available and identifies the Droppers AnimatedBottomBar project. This confirms the coordinate resolves, not that the app compiles against it.
6. Inspected the latest Actions run for `432f487` (run #5, ID `40054885820`). Checkout and setup succeeded; `Build with Gradle` failed. APK artifact upload was skipped. The missing prior `last-sha` artifact also appeared, but that step has `continue-on-error: true` and is not the build blocker.
7. Tried the GitHub run/job log endpoints. The connector refused the complete archive because it was about 1.016 MB, above its 1,000,000-byte response limit. The public job page requires sign-in to display raw logs. Therefore the exact Gradle exception is still unverified; do not guess it from the generic exit-code annotation.
8. Added CI-only diagnostics on this branch: Gradle now writes to `gradle-build.log`, uses plain console plus stacktrace, and on failure emits a short annotation and uploads `gradle-build-failure.log` (failure-only, 3-day retention). The shell still exits with Gradle's original non-zero status. No application behavior or dependency versions were changed in this diagnostic step.
9. Added this handoff so the next agent can resume from the actual Actions result rather than repeat the credential/AnimatedBottomBar investigation.

## Current verification state

- Run #5 failed at Gradle; no APK was produced or uploaded.
- The branch has not yet been verified by a successful APK build after the diagnostic change. A push to this branch should trigger the `Build APK and Notify Discord` workflow.
- On failure, inspect the `Gradle failure detail` check annotation and download the `gradle-build-failure` artifact; do not conclude the Kotatsu integration compiles until `Dantotsu` APK artifact is present.
- On successful build, the expected artifact contains `app/build/outputs/apk/google/alpha/app-google-alpha.apk`. The fork's workflow does not post the APK to the upstream Discord/Telegram destinations.

## Copyable next-agent instructions

```text
Continue the APK build investigation in Space4414/Kujaku on feature/kotatsu-parsers-redo. Read AGENTS.md and KOTATSU_BUILD_HANDOFF.md first. The current head is the CI diagnostics commit after 432f487.

1. Inspect the newest 'Build APK and Notify Discord' workflow run for this branch.
2. If it fails, read the 'Gradle failure detail' annotation and download the 'gradle-build-failure' artifact. Diagnose the exact dependency-resolution or Kotlin compilation error from that evidence; do not guess and do not expose any secret values.
3. Make the smallest fix on this branch. Keep the change manga-only; do not raise minSdk above 21 or change anime/source behavior. Do not bump Kotlin, AGP, or Gradle without asking the owner.
4. Push the fix, wait for the workflow, and confirm the APK artifact exists. If it still fails, update this handoff with the exact error, attempted fix, and next concrete action.
5. If the Actions log archive is still too large for the connector, use the concise check annotation or the failure-only artifact rather than retrying the oversized log endpoint.
```

## Useful GitHub CLI commands (if `gh` is available)

```sh
gh run list --repo Space4414/Kujaku --branch feature/kotatsu-parsers-redo --workflow 'Build APK and Notify Discord' --limit 5
gh run view RUN_ID --repo Space4414/Kujaku --log-failed
gh run download RUN_ID --repo Space4414/Kujaku --name gradle-build-failure
gh run download RUN_ID --repo Space4414/Kujaku --name Dantotsu
```
