package top.focess.minecraft.mclwjglnativesdownloader.platform;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

public class MacosArm64Resolver extends PlatformResolver {


    @Override
    public void resolvePrebuild(File lwjgl3) throws IOException {
        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("config/build-definitions.xml"), new File(lwjgl3, "config/build-definitions.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("config/macos/arm64/build.xml"), new File(lwjgl3, "config/macos/build.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void resolvePredownload(File lwjgl3) throws IOException {
        InputStream inputStream = new URL("https://www.dyncall.org/r1.2/dyncall-1.2-darwin-20.2.0-arm64-r.tar.gz").openStream();
        File targetDir = new File(lwjgl3, "bin/libs/macox/x64");
        targetDir.mkdirs();
        GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
        try(TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream)) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tarArchiveInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".a")) {
                    File newFile = new File(targetDir, entry.getName());
                    Files.copy(tarArchiveInputStream, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
