package top.focess.minecraft.minecraftnativesdownloader.platform;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import top.focess.minecraft.minecraftnativesdownloader.util.ZipUtil;
import top.focess.util.json.JSON;
import top.focess.util.json.JSONList;
import top.focess.util.json.JSONObject;
import top.focess.util.network.NetworkHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class MacosArm64Resolver extends PlatformResolver {


    @Override
    public void resolveBeforeLwjglLink(File lwjgl) throws IOException {
        //use higher version of macos sdk, sprintf is not supported in 10.13
        //replace it and make build.xml not automatically generate the new file.
        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("lwjgl/modules/lwjgl/core/src/generated/c/org_lwjgl_system_libc_LibCStdio.c"), new File(lwjgl, "modules/lwjgl/core/src/generated/c/org_lwjgl_system_libc_LibCStdio.c").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("lwjgl/build.xml"), new File(lwjgl, "build.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);

        InputStream inputStream = new URL("https://www.dyncall.org/r1.2/dyncall-1.2-darwin-20.2.0-arm64-r.tar.gz").openStream();
        File targetDir = new File(lwjgl, "bin/libs/macos/x64");
        targetDir.mkdirs();
        GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
        try(TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream)) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tarArchiveInputStream.getNextEntry()) != null) {
                String name = entry.getName().substring(entry.getName().lastIndexOf(File.separatorChar) + 1);
                if (!entry.isDirectory() && name.endsWith(".a")) {
                    File newFile = new File(targetDir,name);
                    Files.copy(tarArchiveInputStream, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    @Override
    public void resolveDownloadGlfw(File parent) throws IOException {
        NetworkHandler networkHandler = new NetworkHandler();
        JSON json = networkHandler.get("https://api.github.com/repos/glfw/glfw/releases/latest", Map.of(), Map.of()).getAsJSON();
        JSONList assets = json.getList("assets");
        for (JSONObject asset : assets) {
            String name = asset.get("name");
            if (name.toUpperCase().contains("MACOS")) {
                String url = asset.get("browser_download_url");
                InputStream inputStream = new URL(url).openStream();
                ZipUtil.unzip(inputStream, parent);
                break;
            }
        }
    }

    @Override
    public void resolveMove(File parent, File natives) throws IOException {
        for (File file : parent.listFiles()) {
            if (file.isDirectory()) {
                if (file.getName().contains("glfw")) {
                    File dylib = find(new File(file, "lib-arm64"), "dylib");
                    File target = new File(natives, "libglfw.dylib");
                    if (dylib != null && !target.exists())
                        Files.copy(dylib.toPath(), target.toPath());
                } else if (file.getName().contains("lwjgl")) {
                    File dir = new File(file, "bin/libs");
                    File lwjgl = new File(natives, "liblwjgl.dylib");
                    File opengl = new File(natives, "liblwjgl_opengl.dylib");
                    File stb = new File(natives, "liblwjgl_stb.dylib");
                    File tinyfd = new File(natives, "liblwjgl_tinyfd.dylib");
                    if (!lwjgl.exists())
                        Files.copy(new File(dir, "liblwjgl.dylib").toPath(), lwjgl.toPath());
                    if (!opengl.exists())
                        Files.copy(new File(dir, "liblwjgl_opengl.dylib").toPath(), opengl.toPath());
                    if (!stb.exists())
                        Files.copy(new File(dir, "liblwjgl_stb.dylib").toPath(), stb.toPath());
                    if (!tinyfd.exists())
                        Files.copy(new File(dir, "liblwjgl_tinyfd.dylib").toPath(), tinyfd.toPath());
                } else if (file.getName().contains("jemalloc")) {
                    File dylib = find(new File(file, "lib"), "dylib");
                    File target = new File(natives, "libjemalloc.dylib");
                    if (dylib != null && !target.exists())
                        Files.copy(dylib.toPath(), target.toPath());
                } else if (file.getName().contains("openal")) {
                    File dylib = find(new File(file, "build"), "dylib");
                    File target = new File(natives, "libopenal.dylib");
                    if (dylib != null && !target.exists())
                        Files.copy(dylib.toPath(), target.toPath());
                } else if (file.getName().contains("Bridge")) {
                    File dylib = find(new File(file, "target/classes"), "dylib");
                    File target = new File(natives, "libjcocoa.dylib");
                    if (dylib != null && !target.exists())
                        Files.copy(dylib.toPath(), target.toPath());
                }
            }
        }
    }

    @Override
    public void resolveBridge(File parent) throws IOException,InterruptedException {
        File dir = new File(parent, "Java-Objective-C-Bridge-master");
        if (!dir.exists()) {
            System.out.println("Download Java-Objective-C-Bridge...");
            InputStream inputStream = new URL("https://github.com/shannah/Java-Objective-C-Bridge/archive/refs/heads/master.zip").openStream();
            ZipUtil.unzip(inputStream, parent);
        }
        File target = new File(dir, "target");
        if (target.exists())
            FileUtils.forceDelete(target);
        System.out.println("Build Java-Objective-C-Bridge...");
        Process process = new ProcessBuilder("mvn","package").redirectOutput(new File(parent, "bridge.txt")).redirectError(new File(parent, "bridge.txt")).directory(dir).start();
        if (process.waitFor() != 0) {
            System.err.println("Java-Objective-C-Bridge: mvn package failed. Please check the error above.");
            System.exit(-1);
        }
    }

    @Override
    public void resolveBeforeLwjglBuild(File lwjgl) throws IOException {
        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("lwjgl/config/build-definitions.xml"), new File(lwjgl, "config/build-definitions.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("lwjgl/config/macos/arm64/build.xml"), new File(lwjgl, "config/macos/build.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
