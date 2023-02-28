package top.focess.minecraft.minecraftnativesdownloader.platform;

public enum Platform {

    WINDOWS("windows", "natives-windows", "win32"),
    MACOS("osx", "natives-macos", "darwin"),
    LINUX("linux", "natives-linux", "linux");


    private final String nativesName;
    private final String downloadName;
    private final String jnaName;

    Platform(String nativesName, String downloadName, String jnaName) {
        this.nativesName = nativesName;
        this.downloadName = downloadName;
        this.jnaName = jnaName;
    }

    public static Platform parse(String os) {
        if (os.equals("Mac OS X"))
            return MACOS;
        else if (os.startsWith("Windows"))
            return WINDOWS;
        else if (os.equals("Linux"))
            return LINUX;
        else
            throw new IllegalArgumentException("Unknown platform: " + os);
    }

    public String getNativesName() {
        return nativesName;
    }

    public String getDownloadName(Architecture arch) {
        if (this == MACOS && arch == Architecture.ARM64)
            return downloadName + "-arm64";
        return downloadName;
    }

    public String getJnaName() {
        return jnaName;
    }
}
