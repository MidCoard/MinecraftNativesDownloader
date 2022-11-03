package top.focess.minecraft.minecraftnativesdownloader;

import org.apache.commons.io.FileUtils;
import top.focess.minecraft.minecraftnativesdownloader.platform.Architecture;
import top.focess.minecraft.minecraftnativesdownloader.platform.Platform;
import top.focess.minecraft.minecraftnativesdownloader.platform.PlatformResolver;
import top.focess.minecraft.minecraftnativesdownloader.util.ZipUtil;
import top.focess.scheduler.Task;
import top.focess.scheduler.ThreadPoolScheduler;
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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class MinecraftNativesDownloader {

    private static final String PREFIX_URL = "https://repo1.maven.org/maven2/org/lwjgl/";

    private static final String LWJGL_SOURCE_URL = "https://github.com/LWJGL/lwjgl3/archive/refs/tags/";

    private static final ThreadPoolScheduler THREAD_POOL_SCHEDULER = new ThreadPoolScheduler(10, false, "MinecraftNativesDownloader", true);

    private static final List<Task> TASKS = new ArrayList<>();

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        Options options = Options.parse(args,
                new OptionParserClassifier("path", OptionType.DEFAULT_OPTION_TYPE),
                new OptionParserClassifier("no-change-mode"),
                new OptionParserClassifier("bridge"),
                new OptionParserClassifier("help"),
                new OptionParserClassifier("ignore-error"),
                new OptionParserClassifier("no-clean"),
                new OptionParserClassifier("clean"),
                new OptionParserClassifier("ignore-lwjgl"),
                new OptionParserClassifier("ignore-glfw"),
                new OptionParserClassifier("ignore-jemalloc"),
                new OptionParserClassifier("ignore-openal")
        );
        Option option = options.get("help");
        Option ignore = options.get("ignore-error");
        Option ignoreLwjgl = options.get("ignore-lwjgl");
        Option ignoreGlfw = options.get("ignore-glfw");
        Option ignoreJemalloc = options.get("ignore-jemalloc");
        Option ignoreOpenal = options.get("ignore-openal");
        if (option != null) {
            System.out.println("--path <Miencraft Version Path> Generate Minecraft naives by specified Minecraft version path");
            System.out.println("--no-change-mode Do not change mode of building files.");
            System.out.println("--bridge Build bridge for specified os.");
            System.out.println("--help Show this help message");
            System.out.println("--ignore-error Ignore error when building native files");
            System.out.println("--no-clean Do not clean the build files");
            System.out.println("--clean Clean the native files");
            System.out.println("--debug Show detailed message");
            System.out.println("--ignore-lwjgl Ignore building lwjgl");
            System.out.println("--ignore-glfw Ignore building glfw");
            System.out.println("--ignore-jemalloc Ignore building jemalloc");
            System.out.println("--ignore-openal Ignore building openal");
            return;
        }
        Platform platform = Platform.parse(System.getProperty("os.name"));
        Architecture arch = Architecture.parse(System.getProperty("os.arch"));
        System.out.println("Platform: " + platform);
        System.out.println("Architecture: " + arch);
        PlatformResolver platformResolver = PlatformResolver.getPlatformResolver(platform, arch);
        Scanner scanner = new Scanner(System.in);
        if (arch != Architecture.ARM64) {
            System.err.println("Architecture of your computer is not ARM64. If this is want you want, you can ignore this error.");
            System.err.println("But if your computer is ARM64, please make sure you are using ARM64 Java.");
            System.err.println("Please enter 'ENTER' key to continue.");
            scanner.nextLine();
        }
        option = options.get("path");
        String path = System.getProperty("user.dir");
        if (option != null)
            path = option.get(OptionType.DEFAULT_OPTION_TYPE);
        File file = new File(path);
        String filename = file.getName() + ".json";
        File jsonFile = new File(file, filename);
        Set<Pair<String, String>> libs = new HashSet<>();
        File parent = new File(file, "build");
        if (jsonFile.exists()) {
            System.out.println("Found json file: " + jsonFile.getAbsolutePath());
            option = options.get("clean");
            if (option != null) {
                for (File f : parent.listFiles())
                    FileUtils.forceDelete(f);
                return;
            }
            File natives = new File(parent, "natives");
            if (natives.exists())
                FileUtils.forceDelete(natives);
            natives.mkdirs();
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
            System.out.println("Collect finished. All libraries needed to download: " + libs.size());
            System.out.println("Needed libraries: " + libs);
            System.out.println("Start downloading...");
            List<Pair<String, String>> builtLibs = new ArrayList<>();
            for (Pair<String, String> lib : libs) {
                String type = lib.getFirst();
                String version = lib.getSecond();
                String url = PREFIX_URL + type + "/" + version + "/" + type + "-" + version + "-" + platform.getDownloadName(arch) + ".jar";
                System.out.println("Download " + url);
                try {
                    InputStream inputStream = new URL(url).openStream();
                    try (JarInputStream jarInputStream = new JarInputStream(inputStream)) {
                        JarEntry entry;
                        while ((entry = jarInputStream.getNextJarEntry()) != null) {
                            String name = entry.getName().substring(entry.getName().lastIndexOf(File.separatorChar) + 1);
                            if (name.endsWith(".dylib")) {
                                File newFile = new File(new File(parent, "natives"), name);
                                Files.copy(jarInputStream, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                        jarInputStream.closeEntry();
                    }
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
            if (ignoreLwjgl == null)
                for (Pair<String, String> lib : builtLibs) {
                    String name = lib.getFirst();
                    String version = lib.getSecond();
                    if (versions.contains(version))
                        continue;
                    if (name.equals("lwjgl") || name.equals("lwjgl-opengl") || name.equals("lwjgl-tinyfd") || name.equals("lwjgl-stb")) {
                        TASKS.add(THREAD_POOL_SCHEDULER.run(() -> {
                            try {
                                String url = LWJGL_SOURCE_URL + version + ".zip";
                                System.out.println("Download lwjgl...");
                                if (!new File(parent, "lwjgl3-" + version).exists()) {
                                    InputStream inputStream = new URL(url).openStream();
                                    ZipUtil.unzip(inputStream, parent);
                                }
                                File lwjgl3 = new File(parent, "lwjgl3-" + version);
                                System.out.println("Before building lwjgl...");
                                platformResolver.resolveBeforeLwjglBuild(lwjgl3);
                                System.out.println("Build lwjgl-" + version + "...");
                                Process process = new ProcessBuilder("ant", "compile-templates").redirectOutput(new File(parent, "lwjgl-compile-templates.txt")).redirectError(new File(parent, "lwjgl-compile-templates.txt")).directory(lwjgl3).start();
                                if (process.waitFor() != 0) {
                                    System.err.println("LWJGL compile templates failed. Please check the error above.");
                                    System.exit(-1);
                                }
                                System.out.println("Before linking lwjgl...");
                                platformResolver.resolveBeforeLwjglLink(lwjgl3);
                                process = new ProcessBuilder("ant", "compile-native").redirectOutput(new File(parent, "lwjgl-compile-native.txt")).redirectError(new File(parent, "lwjgl-compile-native.txt")).directory(lwjgl3).start();
                                if (process.waitFor() != 0 && ignore == null) {
                                    System.err.println("LWJGL compile native failed. Please add --ignore-error to ignore this error if this is a known error.");
                                    System.exit(-1);
                                }
                                System.out.println("Finish building lwjgl " + COUNTER.incrementAndGet() + "/" + TASKS.size());
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(-1);
                            }
                        }));
                        versions.add(version);
                    }
                }
            if (ignoreGlfw == null && find(builtLibs, "lwjgl-glfw"))
                TASKS.add(THREAD_POOL_SCHEDULER.run(()->{
                    try {
                        System.out.println("Download glfw...");
                        platformResolver.resolveDownloadGlfw(parent);
                        System.out.println("Finish building glfw " + COUNTER.incrementAndGet() + "/" + TASKS.size());
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }));
            if (ignoreJemalloc == null && find(builtLibs, "lwjgl-jemalloc"))
                TASKS.add(THREAD_POOL_SCHEDULER.run(()->{
                    try {
                        System.out.println("Download jemalloc...");
                        InputStream inputStream = new URL("https://github.com/jemalloc/jemalloc/archive/refs/heads/master.zip").openStream();
                        File jmalloc = new File(parent, "jemalloc-master");
                        ZipUtil.unzip(inputStream, parent);
                        // to avoid write or read permission denied
                        Option o = options.get("no-change-mode");
                        Process process;
                        if (o == null) {
                            process = new ProcessBuilder("chmod", "-R", "777", ".").redirectOutput(new File(parent, "jemalloc-chmod.txt")).redirectError(new File(parent, "jemalloc-chmod.txt")).directory(jmalloc).start();
                            if (process.waitFor() != 0) {
                                System.err.println("jemalloc: change mode of * failed. Please add --no-change-mode if there is no permission problem.");
                                System.exit(-1);
                            }
                        }
                        System.out.println("Build jemalloc...");
                        process = new ProcessBuilder("./autogen.sh").redirectOutput(new File(parent, "jemalloc-configure.txt")).redirectError(new File(parent, "jemalloc-configure.txt")).directory(jmalloc).start();
                        if (process.waitFor() != 0) {
                            System.err.println("jemalloc: autogen failed. Please check the error above.");
                            System.exit(-1);
                        }
                        process = new ProcessBuilder("make").redirectOutput(new File(parent, "jemalloc-make.txt")).redirectError(new File(parent, "jemalloc-make.txt")).directory(jmalloc).start();
                        if (process.waitFor() != 0 && ignore == null) {
                            System.err.println("jemalloc: make failed. Please add --ignore-error to ignore this error if this is a known error.");
                            System.exit(-1);
                        }
                        System.out.println("Finish building jemalloc " + COUNTER.incrementAndGet() + "/" + TASKS.size());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }));
            if (ignoreOpenal == null && find(builtLibs, "lwjgl-openal"))
                TASKS.add(THREAD_POOL_SCHEDULER.run(()->{
                    try {
                        System.out.println("Download openal...");
                        InputStream inputStream = new URL("https://github.com/kcat/openal-soft/archive/refs/heads/master.zip").openStream();
                        File openal = new File(parent, "openal-soft-master");
                        ZipUtil.unzip(inputStream, parent);
                        File buildFile = new File(openal, "build");
                        if (buildFile.exists())
                            FileUtils.forceDelete(buildFile);
                        buildFile.mkdirs();
                        System.out.println("Build openal-soft...");
                        Process process = new ProcessBuilder("cmake", "..").redirectOutput(new File(parent, "openal-cmake.txt")).redirectError(new File(parent, "openal-cmake.txt")).directory(buildFile).start();
                        if (process.waitFor() != 0) {
                            System.err.println("openal: cmake failed. Please check the error above.");
                            System.exit(-1);
                        }
                        process = new ProcessBuilder("make").redirectOutput(new File(parent, "openal-make.txt")).redirectError(new File(parent, "openal-make.txt")).directory(buildFile).start();
                        if (process.waitFor() != 0 && ignore == null) {
                            System.err.println("openal: make failed. Please add --ignore-error to ignore this error if this is a known error.");
                            System.exit(-1);
                        }
                        System.out.println("Finish building openal " + COUNTER.incrementAndGet() + "/" + TASKS.size());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }));
            option = options.get("bridge");
            if (option != null)
                TASKS.add(THREAD_POOL_SCHEDULER.run(()->{
                    try {
                        platformResolver.resolveBridge(parent);
                        System.out.println("Finish building bridge " + COUNTER.incrementAndGet() + "/" + TASKS.size());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }));
            for (Task task :TASKS)
                task.join();
            platformResolver.resolveMove(parent);
            System.out.println("All natives files are moving to " + parent.getAbsolutePath() + "/natives");
            option = options.get("no-clean");
            if (option == null) {
                System.out.println("Clean up...");
                for (File f : parent.listFiles())
                    if (!f.getName().equals("natives"))
                        FileUtils.forceDelete(f);
            }
            System.out.println("Finish");
        } else {
            System.out.println("Can't find json file: " + jsonFile.getAbsolutePath());
        }
    }

    private static boolean find(List<Pair<String, String>> libs, String name) {
        for (Pair<String, String > lib : libs)
            if (lib.getKey().equals(name))
                return true;
        return false;
    }

}
