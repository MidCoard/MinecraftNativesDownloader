package top.focess.minecraft.minecraftnativesdownloader.platform;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.*;
import top.focess.minecraft.minecraftnativesdownloader.util.ZipUtil;
import top.focess.util.json.JSON;
import top.focess.util.json.JSONList;
import top.focess.util.json.JSONObject;
import top.focess.util.network.NetworkHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;

public class MacosArm64Resolver extends PlatformResolver {

    private static final String LATEST_GLFW_VERSION = "3.3.8";


    @Override
    public void resolveBeforeLwjglLink(File lwjgl) throws IOException {
        //use higher version of macos sdk, sprintf is not supported in 10.13
        File file = new File(lwjgl, "modules/lwjgl/core/src/generated/c/org_lwjgl_system_libc_LibCStdio.c");
        if (file.exists())
            Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("lwjgl/modules/lwjgl/core/src/generated/c/org_lwjgl_system_libc_LibCStdio.c"), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

        InputStream inputStream = new URL("https://www.dyncall.org/r1.2/dyncall-1.2-darwin-20.2.0-arm64-r.tar.gz").openStream();
        File targetDir = new File(lwjgl, "bin/libs/macos/x64");
        targetDir.mkdirs();
        GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
        try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream)) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tarArchiveInputStream.getNextEntry()) != null) {
                String name = entry.getName().substring(entry.getName().lastIndexOf(File.separatorChar) + 1);
                if (!entry.isDirectory() && name.endsWith(".a")) {
                    File newFile = new File(targetDir, name);
                    Files.copy(tarArchiveInputStream, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    @Override
    public void resolveDownloadGlfw(File parent) throws IOException {
        NetworkHandler networkHandler = new NetworkHandler();
        try {
            JSON json = networkHandler.get("https://api.github.com/repos/glfw/glfw/releases/latest", new HashMap<>(), new HashMap<>()).getAsJSON();
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
        } catch (Exception e) {
            String url = "https://github.com/glfw/glfw/releases/download/" + LATEST_GLFW_VERSION + "/glfw-" + LATEST_GLFW_VERSION + ".bin.MACOS.zip";
            InputStream inputStream = new URL(url).openStream();
            ZipUtil.unzip(inputStream, parent);
        }
    }

    @Override
    public void resolveMove(File parent, File natives, String bridgeVersion) throws IOException {
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
                    File sourceLwjgl = new File(dir, "liblwjgl.dylib");
                    File sourceOpengl = new File(dir, "liblwjgl_opengl.dylib");
                    File sourceStb = new File(dir, "liblwjgl_stb.dylib");
                    File sourceTinyfd = new File(dir, "liblwjgl_tinyfd.dylib");
                    if (!lwjgl.exists() && sourceLwjgl.exists())
                        Files.copy(sourceLwjgl.toPath(), lwjgl.toPath());
                    if (!opengl.exists() && sourceOpengl.exists())
                        Files.copy(sourceOpengl.toPath(), opengl.toPath());
                    if (!stb.exists() && sourceStb.exists())
                        Files.copy(sourceStb.toPath(), stb.toPath());
                    if (!tinyfd.exists() && sourceTinyfd.exists())
                        Files.copy(sourceTinyfd.toPath(), tinyfd.toPath());
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
        }
    }

    @Override
    public void resolveBridge(File parent,JSONObject json) throws IOException, InterruptedException {
        File dir = new File(parent, "Java-Objective-C-Bridge-master");
        if (!dir.exists()) {
            System.out.println("Download Java-Objective-C-Bridge...");
            InputStream inputStream = new URL("https://github.com/MidCoard/Java-Objective-C-Bridge/archive/refs/heads/master.zip").openStream();
            ZipUtil.unzip(inputStream, parent);
        }
        File target = new File(dir, "target");
        if (target.exists())
            FileUtils.forceDelete(target);
        System.out.println("Build Java-Objective-C-Bridge...");
        Process process = new ProcessBuilder("mvn", "package").redirectOutput(new File(parent, "bridge.txt")).redirectError(new File(parent, "bridge.txt")).directory(dir).start();
        if (process.waitFor() != 0) {
            System.err.println(top.focess.minecraft.minecraftnativesdownloader.util.Files.readString(new File(parent, "bridge.txt").toPath()));
            System.err.println("Java-Objective-C-Bridge: mvn package failed. Please check the error above.");
            System.exit(-1);
        }
        JSONList list = json.getList("libraries");
        for (JSONObject library : list) {
            String name = library.get("name");
            String[] arguments = name.split(":");
            String group = arguments[0];
            String type = arguments[1];
            JSON j = (JSON) library;
            if (group.equals("net.java.dev.jna") && type.equals("jna")) {
                j.set("name", "net.java.dev.jna:jna:5.10.0");
                JSON downloads = j.getJSON("downloads");
                JSON artifact = downloads.getJSON("artifact");
                artifact.set("path", "net/java/dev/jna/jna/5.10.0/jna-5.10.0.jar");
                artifact.set("url", "https://libraries.minecraft.net/net/java/dev/jna/jna/5.10.0/jna-5.10.0.jar");
                artifact.set("sha1", "7cf4c87dd802db50721db66947aa237d7ad09418");
                artifact.set("size", 1756400);
            } else if (group.equals("net.java.dev.jna") && type.equals("jna-platform")) {
                j.set("name", "net.java.dev.jna:jna-platform:5.10.0");
                JSON downloads = j.getJSON("downloads");
                JSON artifact = downloads.getJSON("artifact");
                artifact.set("path", "net/java/dev/jna/jna-platform/5.10.0/jna-platform-5.10.0.jar");
                artifact.set("url", "https://libraries.minecraft.net/net/java/dev/jna/jna-platform/5.10.0/jna-platform-5.10.0.jar");
                artifact.set("sha1", "fbed7d9669dba47714ad0d4f4454290a997aee69");
                artifact.set("size", 1343495);
            }
        }
    }

    @Override
    public void resolveBeforeLwjglBuild(File lwjgl) throws IOException {
        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("lwjgl/config/build-definitions.xml"), new File(lwjgl, "config/build-definitions.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("lwjgl/config/macos/arm64/build.xml"), new File(lwjgl, "config/macos/build.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
        try {
            //replace it and make build.xml not automatically generate the new file when compile-native.
            File build = new File(lwjgl, "build.xml");
            DocumentBuilder documentBuilderFactor = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilderFactor.parse(build);
            document.normalize();
            NodeList nodeList = document.getElementsByTagName("target");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                NamedNodeMap attributes = node.getAttributes();
                if (attributes != null) {
                    String name = attributes.getNamedItem("name").getNodeValue();
                    if (name.equals("compile-native")) {
                        attributes.getNamedItem("depends").setNodeValue("init");
                    } else if (name.equals("compile-templates")) {
                        Element element = document.createElement("antcall");
                        element.setAttribute("target", "compile");
                        node.appendChild(element);
                    }
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(build);
            transformer.transform(source, result);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
