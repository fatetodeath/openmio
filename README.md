# OpenMio 26.2

OpenMio is an open-source reconstruction of the Mio Client, ported from Minecraft 1.21.1 to Minecraft 26.2. This is an experimental Fabric client mod; gameplay and runtime compatibility still need testing.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 26.2 |
| Java JDK | 25 |
| Gradle | 9.5.1, included through the wrapper |
| Fabric Loom | 1.17.21 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.2 |
| MixinExtras | 0.5.5 |

The project uses Minecraft's unobfuscated names and `net.fabricmc.fabric-loom`. Version settings are maintained in `gradle.properties`.

## Build

Install JDK 25 and point `JAVA_HOME` to that installation. The first build needs internet access to download Gradle, Minecraft and Maven dependencies.

Linux / macOS:

```sh
chmod +x gradlew
./gradlew --no-daemon build
```

Windows PowerShell:

```powershell
.\gradlew.bat --no-daemon build
```

Outputs:

- `build/libs/mio-client-1.0.0.jar`: client mod.
- `build/libs/mio-client-1.0.0-sources.jar`: Java sources.

The mod version is `1.0.0`; `26.2` identifies the supported Minecraft version.

## Development

```sh
./gradlew runClient
```

`libs/baritone-fabric-26.2.jar` is the local Baritone dependency used by Gradle. Keep it in the source tree. The Fabric entrypoint is `nick.MioModInitializer`; the old `mio-loader.jar` is not required.

## Installation

Use Minecraft 26.2 with Fabric Loader 0.19.5 and Fabric API 0.161.0+26.2. Place the client JAR and `libs/baritone-fabric-26.2.jar` in the instance's `mods` directory. Source JARs are for development and should not be installed as mods.

The current build does not bundle the Discord IPC libraries into the client JAR. Discord integration may need additional runtime dependencies. A successful build does not establish that all reconstructed modules work in game.

## GitHub Actions

The `Build and Prerelease` workflow builds on every push, pull request and manual run using JDK 25 and the Gradle wrapper.

- Every successful build uploads the client and source JARs as an Actions artifact, retained for 14 days.
- A successful push or manual run on the repository's default branch publishes a GitHub prerelease.
- Pull requests, other branches and tag pushes only build; they do not publish releases.
- Prerelease tags use `26.2-build-<run_number>.<run_attempt>` and point to the commit that was built.
- Each prerelease includes both JARs and `SHA256SUMS.txt`; it does not replace the latest stable release.

Upload this project to a GitHub repository with Actions enabled. The workflow uses the built-in `GITHUB_TOKEN`; no custom secret is needed. Repository or organization policies must permit the release job's `contents: write` permission.

Download the client JAR from **Releases**, or download the build artifact from **Actions**. Baritone remains available in this repository's `libs` directory.

## Layout

```text
.github/workflows/build.yml
src/main/java/me/mioclient/
src/main/java/nick/
src/main/resources/
libs/baritone-fabric-26.2.jar
gradle/wrapper/
```

## License

GPL-3.0-or-later. See `LICENSE` and `NOTICE.md` for the existing license and provenance statements. This is a modified reconstruction and port, not the original client release, and is not affiliated with Mojang, Microsoft, Fabric or Baritone.
