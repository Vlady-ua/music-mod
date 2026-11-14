package com.afrikalify;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MusicLibrary {
    private MusicLibrary() {}

    /** <instance>/afrikalify/music */
    public static Path folder() {
        return FabricLoader.getInstance().getGameDir().resolve("afrikalify").resolve("music");
    }

    public static void ensureFolder() {
        try {
            Files.createDirectories(folder());
        } catch (IOException e) {
            System.err.println("[Afrikalify] Could not create music folder: " + e);
        }
    }

    public static List<Path> scan() {
        ensureFolder();
        try (Stream<Path> s = Files.list(folder())) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp3"))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("[Afrikalify] Could not scan music folder: " + e);
            return new ArrayList<>();
        }
    }

    public static String displayName(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }
}
