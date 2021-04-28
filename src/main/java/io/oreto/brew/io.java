package io.oreto.brew;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public static Optional<InputStream> loadResource(ClassLoader classLoader, String path, String... resourcePath) {
        InputStream stream = classLoader.getResourceAsStream(Paths.get(path, resourcePath).toString());
        return stream == null ? Optional.empty() : Optional.of(stream);
    }

    public static Optional<InputStream> loadResource(String path, String... resourcePath) {
        return loadResource(io.class.getClassLoader(), path, resourcePath);
    }

    public static Optional<List<String>> resourceLines(ClassLoader classLoader, String path, String... resourcePath) {
        return loadResource(classLoader, path, resourcePath)
                .map(is -> new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.toList()));
    }

    public static Optional<List<String>> resourceLines(String path, String... resourcePath) {
        return resourceLines(io.class.getClassLoader(), path, resourcePath);
    }

    public static Optional<String> resourceText(ClassLoader classLoader, String path, String... resourcePath) {
        return loadResource(classLoader, path, resourcePath)
                .map(is -> new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n")));
    }

    public static Optional<String> resourceText(String path, String... resourcePath) {
        return resourceText(io.class.getClassLoader(), path, resourcePath);
    }

    public static Optional<String> fileText(File file) {
        try {
            return file.exists()
                    ? Optional.of(String.join("\n", Files.readAllLines(file.toPath())))
                    : Optional.empty();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }
}
