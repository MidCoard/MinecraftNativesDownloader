package top.focess.minecraft.minecraftnativesdownloader.platform;

import org.apache.commons.io.FileUtils;
import top.focess.minecraft.minecraftnativesdownloader.util.ZipUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class MacosX8664Resolver extends PlatformResolver{
    @Override
    public void resolveBeforeLwjglLink(File lwjgl) throws IOException {

    }

    @Override
    public void resolveDownloadGlfw(File parent) throws IOException {

    }

    @Override
    public void resolveMove(File parent, File natives, String bridgeVersion) throws IOException {
        for (File file : parent.listFiles())
            if (file.isDirectory())
                if (file.getName().contains("Bridge")) {
                    File dylib = find(new File(file, "target/classes"), "dylib");
                    File target = new File(natives, "java-objc-bridge-" + bridgeVersion + "-natives-osx.jar");
                    if (dylib != null && !target.exists()) {
                        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(target))) {
                            JarEntry jarEntry = new JarEntry("libjcocoa.dylib");
                            jarOutputStream.putNextEntry(jarEntry);
                            jarOutputStream.write(Files.readAllBytes(dylib.toPath()));
                            jarOutputStream.closeEntry();
                        }
                    }
                }
    }

    @Override
    public void resolveBridge(File parent) throws IOException, InterruptedException {
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

    }
}
