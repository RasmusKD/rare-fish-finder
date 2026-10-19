# Rare Fish Finder

Client-only Fabric mod that finds and tracks rare tropical fish variants (glow, toasts, collection screen, advancement tab). GitHub RasmusKD/rare-fish-finder, Modrinth slug rare-fish-finder, branch main. This is the current clone; `C:\Users\Rasmus\Desktop\gg\rare-fish-finder` is stale.

Read the `minecraft-mods` skill first (`C:\Users\Rasmus\.claude\skills\minecraft-mods\SKILL.md`). It holds the cross-mod rules: dual-version builds, release flow, CurseForge/Modrinth, testing in instances, config and icon style. Memory note for this mod: `rare-fish-finder.md`.

## Build

- Java 25: `JAVA_HOME=C:\Users\Rasmus\.jdks\jdk-25.0.3+9`, then `./gradlew build`. The jar is written to `build/libs/`.
- No unit tests; the check is a clean build plus a run in a Modrinth App instance (see the skill).
- Two jars from one tree: `./gradlew build` makes `rarefishfinder-X.Y.Z+26.1.jar` (26.1.x and 26.2), `./gradlew build -PmcTarget=26.3` makes `+26.3` (pins in `gradle-26.3.properties`; files in `src/mc26_3` replace same-named `src/main` files, `remove.txt` drops some). Compile the 26.1 jar against 26.2 too: `./gradlew build -Pminecraft_version=26.2 -Pfabric_version=0.158.0+26.2 -Pclothconfig_version=26.2.155 -Pmodmenu_version=20.0.3 -Ploader_version=0.19.5`. In PowerShell quote every `-P` argument with a version, or `26.2` is cut at the dot.
- Key codes: never a bare number or `GLFW_KEY_*`. 26.3 uses SDL scancodes, so use `InputConstants.KEY_*` (inlined per jar).
- Static proof: `python C:\Users\Rasmus\.claude\skills\minecraft-mods\scripts\linkcheck.py <jar> <mapped minecraft jar>` per jar per claimed version. Renamed vanilla members are listed in `VERSIONS.md`; add new finds there.
- The server syncs the glow flag, so the mixin must always decide the answer for tropical fish and never fall through to vanilla.

## Release

Tag only: `git tag vX.Y.Z` then push the tag. The tag must be exactly `vX.Y.Z` (a suffix becomes part of the Modrinth version name). `.github/workflows/publish.yml` builds both jars against every version they claim, gates on them, then publishes each jar to GitHub and Modrinth with its own game-versions. Never hand-publish; check the workflow first. Pushing a tag is a public release: only on Rasmus' word.

## Keep skills and docs current (same rule as Nordvec)

When you learn something new, update the doc the next agent reads in the same change: this file for build and release facts, `README.md` for user-facing behavior, `VERSIONS.md` for 26.1/26.2/26.3 splits, and the `minecraft-mods` skill for cross-mod lessons. Correct or delete outdated text instead of adding a contradicting note. Then run `python C:\Users\Rasmus\.claude\skills\minecraft-mods\scripts\check_citations.py`.

No em or en dashes in any text. Commit messages describe the code change.
