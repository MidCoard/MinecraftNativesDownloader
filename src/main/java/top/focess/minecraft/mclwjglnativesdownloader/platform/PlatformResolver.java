package top.focess.minecraft.mclwjglnativesdownloader.platform;

import top.focess.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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

    public abstract void resolvePrebuild(File lwjgl3) throws IOException;

    public abstract void resolvePredownload(File lwjgl3) throws IOException;

    public static class EmptyPlatformResolver extends PlatformResolver {

        @Override
        public void resolvePrebuild(File lwjgl3) {

        }

        @Override
        public void resolvePredownload(File lwjgl3) {

        }
    }
}
