package top.focess.minecraft.mclwjglnativesdownloader;

import top.focess.minecraft.mclwjglnativesdownloader.platform.Architecture;
import top.focess.minecraft.mclwjglnativesdownloader.platform.Platform;
import top.focess.util.Pair;
import top.focess.util.json.JSON;
import top.focess.util.json.JSONList;
import top.focess.util.json.JSONObject;
import top.focess.util.option.Option;
import top.focess.util.option.OptionParserClassifier;
import top.focess.util.option.Options;
import top.focess.util.option.type.OptionType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MCLWJGLNativesDownloader {

    private static final String PREFIX_URL = "https://repo1.maven.org/maven2/org/lwjgl/";

    private static final String LWJGL_SOURCE_URL = "https://github.com/LWJGL/lwjgl3/archive/refs/tags/";

    public static void main(String[] args) throws IOException, InterruptedException {
        Platform platform = Platform.parse(System.getProperty("os.name"));
        Architecture arch = Architecture.parse(System.getProperty("os.arch"));
        System.out.println("Platform: " + platform);
        System.out.println("Architecture: " + arch);
        Options options = Options.parse(args, new OptionParserClassifier("path", OptionType.DEFAULT_OPTION_TYPE));
        Option option = options.get("path");
        String path = System.getProperty("user.dir");
        if (option != null)
            path = option.get(OptionType.DEFAULT_OPTION_TYPE);
        File file = new File(path);
        String filename = file.getName() + ".json";
        File jsonFile = new File(file, filename);
        Set<Pair<String, String>> libs = new HashSet<>();
        if (jsonFile.exists()) {
            System.out.println("Found json file: " + jsonFile.getAbsolutePath());
            JSONObject json = JSON.parse(Files.readString(jsonFile.toPath()));
            JSONList libraries = json.getList("libraries");
            System.out.println("Start collecting libraries needed to download...");
            for (JSONObject library : libraries) {
                String name = library.get("name");
                String[] arguments = name.split(":");
                String group = arguments[0];
                String type = arguments[1];
                String version = arguments[2];
                if (group.equals("org.lwjgl")) {
                    JSONList rules = library.getList("rules");
                    boolean allowed = true;
                    for (JSONObject object : rules) {
                        JSON rule = (JSON) object;
                        if (rule.get("action").equals("disallow"))
                            if (rule.contains("os")) {
                                JSON os = rule.getJSON("os");
                                if (os.get("name").equals(platform.getNativesName()))
                                    allowed = false;
                            }
                    }
                    if (allowed)
                        libs.add(Pair.of(type, version));
                }
            }
            System.out.println("Collecting finished. All libraries needed to download: " + libs.size());
            System.out.println("Start downloading...");
            List<Pair<String, String>> builtLibs = new ArrayList<>();
            for (Pair<String, String> lib : libs) {
                String type = lib.getFirst();
                String version = lib.getSecond();
                String url = PREFIX_URL + type + "/" + version + "/" + type + "-" + version + "-" + platform.getDownloadName(arch) + ".jar";
                System.out.println("Downloading " + url);
                try {
                    InputStream inputStream = new URL(url).openStream();
                } catch (FileNotFoundException e) {
                    builtLibs.add(lib);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
            System.out.println("Downloading finished. All libraries downloaded: " + (libs.size() - builtLibs.size()));
            System.out.println("Start building libraries: " + builtLibs.size());
            Set<String> versions = new HashSet<>();
            for (Pair<String, String> lib : builtLibs) {
                String version = lib.getSecond();
                if (versions.contains(version))
                    continue;
                versions.add(version);
                String url = LWJGL_SOURCE_URL + version + ".zip";
                System.out.println("Downloading built library: " + url);
                if (!new File(file, "lwjgl3-" + version).exists())
                    try {
                        InputStream inputStream = new URL(url).openStream();
                        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                        ZipEntry entry;
                        while((entry = zipInputStream.getNextEntry()) != null) {
                            File newFile = new File(file, entry.getName());
                            if (entry.isDirectory()) {
                                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                                    throw new IOException("Failed to create directory " + newFile);
                                }
                            } else {
                                // fix for Windows-created archives
                                File parent = newFile.getParentFile();
                                if (!parent.isDirectory() && !parent.mkdirs()) {
                                    throw new IOException("Failed to create directory " + parent);
                                }

                                // write file content
                                Files.copy(zipInputStream, newFile.toPath());
                            }
                        }
                        zipInputStream.closeEntry();
                        zipInputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                File lwjgl3 = new File(file, "lwjgl3-" + version);
                System.out.println("Replace necessary files...");
                Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("config/build-definitions.xml"), new File(lwjgl3, "config/build-dependencies.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("config/macos/build.xml"), new File(lwjgl3, "config/macos/build.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Building library: " + version);
                Process process = new ProcessBuilder("ant","compile-templates").redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).directory(lwjgl3).start();
                process.waitFor();
            }
        } else {
            System.out.println("Can't find json file: " + jsonFile.getAbsolutePath());
        }
    }
}
