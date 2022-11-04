package top.focess.minecraft.minecraftnativesdownloader.platform;

import top.focess.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;

public abstract class PlatformResolver {

    public static final Map<Pair<Platform, Architecture>, Supplier<PlatformResolver>> PLATFORM_RESOLVER_MAP = new HashMap<>();


    static {
        PLATFORM_RESOLVER_MAP.put(new Pair<>(Platform.MACOS, Architecture.ARM64), MacosArm64Resolver::new);
        PLATFORM_RESOLVER_MAP.put(new Pair<>(Platform.MACOS, Architecture.X86_64), MacosX8664Resolver::new);
    }

    public static PlatformResolver getPlatformResolver(Platform platform, Architecture architecture) {
        return PLATFORM_RESOLVER_MAP.getOrDefault(Pair.of(platform, architecture),EmptyPlatformResolver::new).get();
    }

    public abstract void resolveBeforeLwjglLink(File lwjgl) throws IOException;

    public abstract void resolveDownloadGlfw(File parent) throws IOException;

    public abstract void resolveMove(File parent, File natives, String bridgeVersion) throws IOException;

    public abstract void resolveBridge(File parent) throws IOException, InterruptedException;

    public abstract void resolveBeforeLwjglBuild(File lwjgl) throws IOException;

    private static long size(File file) {
        try {
            return Files.size(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    protected static File find(File file, String suffix) {
        List<File> files = new ArrayList<>();
        if (file.isDirectory())
            for (File f : file.listFiles())
                if (f.getName().endsWith("." + suffix))
                    files.add(f);
        if (files.size() == 0)
            return null;
        else return files.stream().max(Comparator.comparingLong(PlatformResolver::size)).orElse(null);
    }

    public static class EmptyPlatformResolver extends PlatformResolver {

        @Override
        public void resolveBeforeLwjglLink(File lwjgl) {

        }

        @Override
        public void resolveDownloadGlfw(File parent) {

        }

        @Override
        public void resolveMove(File parent, File natives, String bridgeVersion) throws IOException {

        }

        @Override
        public void resolveBridge(File parent) {

        }

        @Override
        public void resolveBeforeLwjglBuild(File lwjgl) {

        }
    }
}
