package top.focess.minecraft.mclwjglnativesdownloader.platform;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import top.focess.minecraft.mclwjglnativesdownloader.util.ZipUtil;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class MacosArm64Resolver extends PlatformResolver {


    @Override
    public void resolvePrebuild(File parent) throws IOException {
        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("lwjgl/config/build-definitions.xml"), new File(parent, "config/build-definitions.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("lwjgl/config/macos/arm64/build.xml"), new File(parent, "config/macos/build.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void resolvePredownload(File parent) throws IOException {
        InputStream inputStream = new URL("https://www.dyncall.org/r1.2/dyncall-1.2-darwin-20.2.0-arm64-r.tar.gz").openStream();
        File targetDir = new File(parent, "bin/libs/macos/x64");
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
    public void resolveDownloadGLFW(File parent) throws IOException {
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
    public void resolveMove(File parent) throws IOException {
        File natives = new File(parent, "natives");
        natives.mkdirs();
        for (File file : parent.listFiles()) {
            if (file.isDirectory()) {
                if (file.getName().contains("glfw")) {
                    File dylib = find(new File(file, "lib-arm64"), "dylib");
                    if (dylib != null)
                        Files.copy(dylib.toPath(),new File(natives, "libglfw.dylib").toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else if (file.getName().contains("lwjgl")) {
                    File dir = new File(file, "bin/libs");
                    Files.copy(new File(dir, "liblwjgl.dylib").toPath(), new File(natives, "liblwjgl.dylib").toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(new File(dir, "liblwjgl_opengl.dylib").toPath(), new File(natives, "liblwjgl_opengl.dylib").toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(new File(dir, "liblwjgl_stb.dylib").toPath(), new File(natives, "liblwjgl_stb.dylib").toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(new File(dir, "liblwjgl_tinyfd.dylib").toPath(), new File(natives, "liblwjgl_tinyfd.dylib").toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else if (file.getName().contains("jemalloc")) {
                    File dylib = find(new File(file, "lib"), "dylib");
                    if (dylib != null)
                        Files.copy(dylib.toPath(),new File(natives, "libjemalloc.dylib").toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else if (file.getName().contains("openal")) {
                    File dylib = find(new File(file, "build"), "dylib");
                    if (dylib != null)
                        Files.copy(dylib.toPath(),new File(natives, "libopenal.dylib").toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else if (file.getName().contains("Bridge")) {
                    File dylib = find(new File(file, "target/classes"), "dylib");
                    if (dylib != null)
                        Files.copy(dylib.toPath(),new File(natives, "libjcocoa.dylib").toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static long size(File file) {
        try {
            return Files.size(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File find(File file, String suffix) {
        List<File> files = new ArrayList<>();
        if (file.isDirectory())
            for (File f : file.listFiles())
                if (f.getName().endsWith("." + suffix))
                    files.add(f);
        if (files.size() == 0)
            return null;
        else return files.stream().max(Comparator.comparingLong(MacosArm64Resolver::size)).orElse(null);
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
        Process process = new ProcessBuilder("mvn","package").redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).directory(dir).start();
        if (process.waitFor() != 0) {
            System.err.println("Java-Objective-C-Bridge: mvn package failed. Please add --no-bridge to skip this step if you don't need it.");
            System.exit(-1);
        }
    }
}
