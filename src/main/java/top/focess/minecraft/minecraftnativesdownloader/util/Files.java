package top.focess.minecraft.minecraftnativesdownloader.util;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Files {

	public static String readString(Path path) throws IOException {
		return java.nio.file.Files.lines(path).collect(Collectors.joining("\n"));
	}

	public static Path writeString(Path path, String content, OpenOption... options) throws IOException {
		return java.nio.file.Files.write(path, content.getBytes(), options);
	}
}
