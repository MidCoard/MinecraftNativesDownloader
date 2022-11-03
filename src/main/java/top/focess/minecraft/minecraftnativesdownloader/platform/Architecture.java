package top.focess.minecraft.minecraftnativesdownloader.platform;

public enum Architecture {

    X86,X86_64,ARM64;

    public static Architecture parse(String arch) {
        switch (arch) {
            case "x86":
                return X86;
            case "x86_64":
                return X86_64;
            case "arm64":
            case "aarch64":
                return ARM64;
            default:
                throw new IllegalArgumentException("Unknown architecture: " + arch);
        }
    }
}
