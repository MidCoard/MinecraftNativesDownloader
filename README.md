# MinecraftNativesDownloader - Generate Minecraft Natives Especially for Unsupported Platforms.

# Description

This is a tool to generate Minecraft Natives especially for unsupported platforms.

Why use native libraries? Even with powerful Rosetta 2, running Minecraft on Apple Silicon is still slow. Using native libraries can improve the performance of Minecraft on Apple Silicon.

As other platforms like Linux and Windows, there is no Rosetta 2 with their arm64 version. So we need to generate native libraries for them. But this work has not been done yet.


# Usage


## Requirements

arm64 Java 11

todo

## Warning

# Screenshots

The image below shows the performance of Minecraft on Apple Silicon with and without native libraries.

![Run without Rosetta 2](macos_arm64.png)

Minimal fps is 100.

![Run with Rosetta 2](macos_x86_64.png)

Minimal fps is 33.

It runs on the same modpack with 165 mods, and arm64 version running fps is three times as much as x86_64 version.

The comparison of the performance of Minecraft on Apple Silicon with and without native libraries is not official. It is just a test result of my own.

# Supported Platform

|        | Windows | Linux | Macos  |
|:------:|:-------:|:-----:|:------:|
|  X86   |    -    |   -   |   ✅    |
| X86_64 |    -    |   -   |   ❌❌   |
| arm64  |    ❌    |   ❌   |   ✅    |

❌❌ means not planned,
❌ means not supported , ✅ means supported, - means not tested or not matter.

# Supported Minecraft Versions

We do not care about the versions higher than 1.19, which are officially supported. You can still generate native libraries for higher versions, but it is meaningless.

The above table shows the supported Minecraft versions, which are tested.
If you have tested other versions, please tell me through issues.

- 1.18.2
- 1.16.5
- 1.13.2

# Contribution

If you have any ideas or suggestions, please open an issue or pull request.

Platforms with the unsupported architecture such as **Windows arm64** or **Linux arm64** is welcome to contribute.

# License

This project is licensed under the AGPL-3.0 License - see the [LICENSE](LICENSE) file for details