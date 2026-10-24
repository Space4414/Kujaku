# Kotatsu parser APK build handoff

Repo: [Space4414/Kujaku](https://github.com/Space4414/Kujaku)
Branch: `feature/kotatsu-parsers-redo` (open PR #2 into `main`).
Goal: produce a CI-built APK to install and test the Kotatsu parsers integration.

## Verified timeline and investigation log

1. Located the repo through the connected GitHub account and confirmed PR #2 targets `main` from `feature/kotatsu-parsers-redo`.
2. Confirmed the repository Actions secret named `JITPACK_AUTH_TOKEN` exists. Its value was not read or printed.
3. Reviewed build attempts 2–4. The user-reported attempt 3 failure was HTTP 403 for `RepoDevil/AnimatedBottomBar:7fcb9af`.
4. Reviewed commits `9ed1d1b` (passes the JitPack token into Gradle), `df20c88` (changed AnimatedBottomBar to its public upstream), and `432f487` (switched it to Maven Central).
5. Verified `nl.joery.animatedbottombar:library:1.1.0` has a Maven Central POM. That proves the coordinate is published, not that it compiles in the app.
6. Run #5 (`40054885820`, head `432f487`) checked out the project and reached `Build with Gradle`, then failed. APK upload was skipped. The absent previous `last-sha` artifact is not the blocker: its download step has `continue-on-error: true`.
7. The original run/job log archive exceeded the GitHub connector's 1,000,000-byte response limit. Added failure-only Gradle diagnostics in commit `fae91ff`.
8. Run #6 (`40057295044`, head `fae91ff`) failed in Gradle and uploaded artifact `gradle-build-failure` (ID `12546147187`). Its excerpt showed `:app:processGoogleAlphaResources FAILED` and `java.lang.OutOfMemoryError` in a Gradle worker thread. The specific heap/native-memory exhaustion mechanism was not recorded. No APK was uploaded.
9. The checked-in Gradle heap cap was 2 GiB. This follow-up raises it to 4 GiB, caps Gradle workers at two, and improves the failure excerpt to retain matching leading errors as well as the log tail. This is a targeted memory mitigation, not a confirmed fix until CI produces an APK.

## Current verification state

- No successful APK build has been verified yet; the captured excerpt does not establish whether Kotlin compilation completed.
- A push to this branch runs `Build APK and Notify Discord`.
- On failure, inspect the `Gradle failure detail` annotation and download `gradle-build-failure` (three-day retention).
- On success, expected artifact `Dantotsu` contains `app/build/outputs/apk/google/alpha/app-google-alpha.apk`. Fork builds do not post APKs to upstream Discord/Telegram destinations.
- Do not merge PR #2 or claim the integration runtime-tested until a successful APK artifact is confirmed and the user installs/tests it.

## Copyable next-agent instructions

```text
Continue in Space4414/Kujaku on feature/kotatsu-parsers-redo. Read AGENTS.md and KOTATSU_BUILD_HANDOFF.md first. Review the newest GitHub Actions run for this branch; current work includes a Gradle memory mitigation (4 GiB heap, max 2 workers) and failure diagnostics.

1. If the run succeeds, verify that artifact Dantotsu contains app/build/outputs/apk/google/alpha/app-google-alpha.apk. Report the run and artifact; do not claim the parser integration itself was runtime-tested.
2. If it fails, inspect the Gradle failure detail annotation and download gradle-build-failure. Diagnose the exact first cause from that evidence; do not repeat the old AnimatedBottomBar/JitPack fix without fresh evidence.
3. Make the smallest supported fix on this branch and rerun CI. Keep changes manga-only, keep minSdk at 21 or lower, and do not change anime behavior. Do not bump Kotlin, AGP, or Gradle without asking the owner.
4. Never reveal or print secret values. The repository secret name JITPACK_AUTH_TOKEN is configured; if fresh logs show another JitPack 403, verify the authorized account can read that specific repository without exposing the token.
5. If still blocked, update this handoff with exact evidence, attempted fixes, and one concrete next step so another agent can continue without repeating investigation.
```

## Useful commands (if `gh` is available)

```sh
gh run list --repo Space4414/Kujaku --branch feature/kotatsu-parsers-redo --workflow 'Build APK and Notify Discord' --limit 5
gh run view RUN_ID --repo Space4414/Kujaku --log-failed
gh run download RUN_ID --repo Space4414/Kujaku --name gradle-build-failure
gh run download RUN_ID --repo Space4414/Kujaku --name Dantotsu
```
