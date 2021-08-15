![Kores-BytecodeWriter](https://github.com/JonathanxD/Kores-BytecodeWriter/blob/version/4.0.0/Kores-bytecode.png?raw=true)

[![jitpack](https://jitpack.io/v/JonathanxD/Kores-BytecodeWriter.svg)](https://jitpack.io/#JonathanxD/Kores-BytecodeWriter)
[![Discord](https://img.shields.io/discord/291407467286364164.svg)](https://discord.gg/3cQWmtj)
[![Actions](https://img.shields.io/github/workflow/status/koresframework/Kores-BytecodeWriter/Gradle%20Package)](https://github.com/koresframework/Kores-BytecodeWriter/actions)
[![Packages](https://img.shields.io/github/v/tag/koresframework/Kores-BytecodeWriter)](https://github.com/orgs/koresframework/packages?repo_name=Kores-BytecodeWriter)

## How to use Kores-BytecodeWriter

Kores-BytecodeWriter is now using [GitHub Packages](https://github.com/orgs/koresframework/packages?repo_name=Kores-BytecodeWriter) to distribute its binary files instead of [jitpack.io](https://jitpack.io) (because jitpack still not support all JDK versions and sometimes `jitpack.yml` simply do not work).

In order to be able to download Kores-BytecodeWriter Artifacts, you will need to configure your global `$HOME/.gradle/gradle.properties` to store your username and a [PAT](https://github.com/settings/tokens) with `read:packages` permission:

```properties
USERNAME=GITHUB_USERNAME
TOKEN=PAT
```

Then configure your `build.gradle` as the following:

```gradle
def GITHUB_USERNAME = project.findProperty("USERNAME") ?: System.getenv("USERNAME")
def GITHUB_PAT = project.findProperty("TOKEN") ?: System.getenv("TOKEN")

repositories {
    mavenCentral()
    maven {
        url "https://maven.pkg.github.com/jonathanxd/jwiutils"
        credentials {
            username = GITHUB_USERNAME
            password = GITHUB_PAT
        }
    }
    maven {
        url "https://maven.pkg.github.com/jonathanxd/bytecodedisassembler"
        credentials {
            username = GITHUB_USERNAME
            password = GITHUB_PAT
        }
    }
    maven {
        url "https://maven.pkg.github.com/koresframework/kores"
        credentials {
            username = GITHUB_USERNAME
            password = GITHUB_PAT
        }
    }
    maven {
        url "https://maven.pkg.github.com/koresframework/kores-bytecodewriter"
        credentials {
            username = GITHUB_USERNAME
            password = GITHUB_PAT
        }
    }
}

dependencies {
    implementation("com.koresframework:kores:4.2.1.base") // Replace the version with the latest or a preferred one
    implementation("com.koresframework:kores-bytecodewriter:4.2.1.bytecode") // Replace the version with the latest or a preferred one
}
```

This is only needed because **GitHub** still not support unauthenticated artifact access.