package io;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import model.SourceFile;

public class FileLoader {

    private static final Set<String> EXTENSIONS =
            Set.of(".java", ".py", ".c", ".h", ".cpp", ".hpp", ".js", ".cs");

    public static SourceFile load(Path path) throws IOException {
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            content = Files.readString(path, StandardCharsets.ISO_8859_1);
        }
        return new SourceFile(path, content);
    }

    /** Charge tous les fichiers de code d'un dossier (sans les sous-dossiers). */
    public static List<SourceFile> loadFolder(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files
                .filter(Files::isRegularFile)
                .filter(p -> EXTENSIONS.stream()
                        .anyMatch(ext -> p.getFileName().toString().toLowerCase().endsWith(ext)))
                .sorted()
                .map(p -> {
                    try { return load(p); }
                    catch (IOException e) { return null; }
                })
                .filter(s -> s != null)
                .toList();
        }
    }
}
