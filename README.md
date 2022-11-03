# MinecraftNativesDownloader - Generate Minecraft Natives Especially for Unsupported Platforms.

# Description

This is a tool to generate Minecraft Natives especially for unsupported platforms.

Why use native libraries? Even with powerful Rosetta 2, running Minecraft on Apple Silicon is still slow. Using native libraries can improve the performance of Minecraft on Apple Silicon.

As other platforms like Linux and Windows, there is no Rosetta 2 with their arm64 version. So we need to generate native libraries for them. But this work has not been done yet.


# Usage

todo

# Screenshots

The image below shows the performance of Minecraft on Apple Silicon with and without native libraries.

![Run without Rosetta 2](macos_arm64.png)

Minimal fps is 100.

![Run with Rosetta 2](macos_x86_64.png)

Minimal fps is 33.

It runs on the same modpack with 165 mods, and arm64 version running fps is three times as much as x86_64 version.

# Supported Platform

|        | Windows | Linux | Macos  |
|:------:|:-------:|:-----:|:------:|
|  X86   |    -    |   -   |   ✅    |
| X86_64 |    -    |   -   |   ❌❌   |
| arm64  |    ❌    |   ❌   |   ✅    |

❌❌ means not planned,
❌ means not supported , ✅ means supported, - means not tested or not matter.

# Contribution

If you have any ideas or suggestions, please open an issue or pull request.

Platforms with the unsupported architecture such as **Windows arm64** or **Linux arm64** is welcome to contribute.

# License

This project is licensed under the AGPL-3.0 License - see the [LICENSE](LICENSE) file for details