package top.focess.minecraft.minecraftnativesdownloader.platform;

import top.focess.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class PlatformResolver {

    public static final Map<Pair<Platform, Architecture>, Supplier<PlatformResolver>> PLATFORM_RESOLVER_MAP = new HashMap<>();


    static {
        PLATFORM_RESOLVER_MAP.put(new Pair<>(Platform.MACOS, Architecture.ARM64), MacosArm64Resolver::new);
    }

    public static PlatformResolver getPlatformResolver(Platform platform, Architecture architecture) {
        return PLATFORM_RESOLVER_MAP.getOrDefault(Pair.of(platform, architecture),EmptyPlatformResolver::new).get();
    }

    public abstract void resolveBeforeLink(File lwjgl) throws IOException;

    public abstract void resolveDownloadGLFW(File parent) throws IOException;

    public abstract void resolveMove(File parent) throws IOException;

    public abstract void resolveBridge(File parent) throws IOException, InterruptedException;

    public static class EmptyPlatformResolver extends PlatformResolver {

        @Override
        public void resolveBeforeLink(File lwjgl) {

        }

        @Override
        public void resolveDownloadGLFW(File parent) {

        }

        @Override
        public void resolveMove(File parent) {

        }

        @Override
        public void resolveBridge(File parent) {

        }
    }
}
