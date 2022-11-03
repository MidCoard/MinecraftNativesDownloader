package top.focess.minecraft.minecraftnativesdownloader.platform;

public enum Platform {

    WINDOWS("windows", "natives-windows"),
    MACOS("osx", "natives-macos"),
    LINUX("linux", "natives-linux");


    private final String nativesName;
    private final String downloadName;

    Platform(String nativesName, String downloadName) {
        this.nativesName = nativesName;
        this.downloadName = downloadName;
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
}
