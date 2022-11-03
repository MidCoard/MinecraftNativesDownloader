package top.focess.minecraft.mclwjglnativesdownloader.platform;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
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
    public void resolveMove(File parent) {

    }

    @Override
    public void resolveBridge(File parent) throws IOException,InterruptedException {
        File dir = new File(parent, "Java-Objective-C-Bridge-master");
        if (!dir.exists()) {
            InputStream inputStream = new URL("https://github.com/shannah/Java-Objective-C-Bridge/archive/refs/heads/master.zip").openStream();
            ZipUtil.unzip(inputStream, parent);
        }
        Process process = new ProcessBuilder("mvn","package").redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).directory(dir).start();
        if (process.waitFor() != 0) {
            System.err.println("Java-Objective-C-Bridge: mvn package failed. Please add --no-bridge to skip this step if you don't need it.");
//            System.exit(-1);
        }
    }
}
