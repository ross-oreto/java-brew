package io.oreto.brew;

import io.oreto.brew.obj.Safe;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class io {
    public static void print(String s) { System.out.print(s); }
    public static void println(String s) { System.out.println(s); }
    public static void print(char s) { System.out.print(s); }
    public static void println(char s) { System.out.println(s); }
    public static void print(Number n) { System.out.print(n); }
    public static void println(Number n) { System.out.println(n); }

    // check for Windows OS which always is a concern.
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    // any non windows OS, all of which will be more unix based.
    public static boolean isUnix() {
        return !isWindows();
    }

    public static File loadResourceFile(ClassLoader classLoader, String path, String... resourcePath) {
        File file = new File(
                Safe.of(classLoader.getResource(path)).q(URL::getFile)
                        .orElse(Paths.get(Paths.get(".", resourcePath).toString(), path).toString())
        );
        if (!file.exists())
            file = new File(path);
        return file;
    }

    public static File loadResourceFile(String path, String... resourcePath) {
        return loadResourceFile(io.class.getClassLoader(), path, resourcePath);
    }

    public static Optional<String> fileText(String path) {
        try {
            return Optional.of(String.join("\n", Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static Optional<String> fileText(File file) {
        return fileText(file.getPath());
    }
}
