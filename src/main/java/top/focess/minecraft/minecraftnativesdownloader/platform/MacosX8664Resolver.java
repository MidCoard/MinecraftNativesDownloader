package top.focess.minecraft.minecraftnativesdownloader.platform;

import org.apache.commons.io.FileUtils;
import top.focess.minecraft.minecraftnativesdownloader.util.ZipUtil;
import top.focess.util.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class MacosX8664Resolver extends PlatformResolver {
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
                    File jar = find(new File(file, "target"), "jar");
                    File target = new File(natives, "java-objc-bridge-" + bridgeVersion + "-natives-osx.jar");
                    File targetJar = new File(natives, "java-objc-bridge-" + bridgeVersion + ".jar");
                    if (dylib != null && !target.exists()) {
                        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(target))) {
                            JarEntry jarEntry = new JarEntry("libjcocoa.dylib");
                            jarOutputStream.putNextEntry(jarEntry);
                            jarOutputStream.write(Files.readAllBytes(dylib.toPath()));
                            jarOutputStream.closeEntry();
                        }
                    }
                    if (jar != null && !targetJar.exists())
                        Files.copy(jar.toPath(), targetJar.toPath());
                }
    }

    @Override
    public void resolveBridge(File parent, JSONObject json) throws IOException, InterruptedException {
        PlatformResolver.getPlatformResolver(Platform.MACOS, Architecture.ARM64).resolveBridge(parent, json);
    }

    @Override
    public void resolveBeforeLwjglBuild(File lwjgl) throws IOException {

    }
}
