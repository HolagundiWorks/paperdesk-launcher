# Carbon app forks

This workspace maintains a managed-device fork of Fossify Commons and the Fossify apps used on the SM-A107F. The shared theme uses IBM Carbon Gray 100 colors, support-status tokens, and zero-radius Material 3 shapes.

## Source worktrees

The large upstream repositories are kept outside the OneDrive workspace:

- `D:\Samsung-A10s-Recovery\source\FossifyCommons`
- `D:\Samsung-A10s-Recovery\source\Gallery`

Both forks use the `codex/carbon-ui` branch. Commons is published locally at the upstream-compatible `org.fossify:commons:6.1.6` coordinate, and app repositories resolve `mavenLocal()` first.

## Rebuild

```powershell
Set-Location D:\Samsung-A10s-Recovery\source\FossifyCommons
.\gradlew.bat :commons:publishToMavenLocal --project-prop VERSION=6.1.6

Set-Location D:\Samsung-A10s-Recovery\source\Gallery
$env:GRADLE_USER_HOME = 'D:\Samsung-A10s-Recovery\gradle-home'
.\gradlew.bat :app:assembleFossRelease
```

Keeping `GRADLE_USER_HOME` on `D:` avoids a Windows KSP cross-volume path defect.

## Device install status

Gallery is the validation app. Its custom release retains `org.fossify.gallery`, is signed with the project development key, installed on the test A10s, and verified to launch. Replacing an upstream-signed installation clears that app's private data; shared media is unaffected.

The remaining Fossify apps follow the same shared-library workflow. Their app-specific icon replacement is tracked separately from the shared Carbon color/shape layer.
