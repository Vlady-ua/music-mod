# Afrikalify (Fabric 1.21.11)

Client-side mod: drop .mp3 files in `<minecraft folder>/afrikalify/music`, press **M**
(rebind under Options > Controls > Afrikalify) to open the player.

Features: library with search + reload, Play / Stop, queue (click a queued song to remove it),
loop-current-song toggle, Now tab (progress, pause/resume, next, volume), "Open folder" button.

## Build
1. Install JDK 21.
2. Get a Gradle wrapper (copy `gradlew`, `gradlew.bat`, `gradle/` from the official
   Fabric example mod, or run `gradle wrapper` if you have Gradle installed).
3. Check https://fabricmc.net/develop and update the versions in `gradle.properties` if needed.
4. `./gradlew build` -> the jar is `build/libs/afrikalify-1.0.0.jar` (not the -sources one).
5. Put it in your instance's `mods` folder together with Fabric API.
