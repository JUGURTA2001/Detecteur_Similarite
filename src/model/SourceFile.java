package model;


import java.nio.file.Path;

public class SourceFile {
    private final String name;
    private final Path path;
    private final String language;
    private final String content;

    public SourceFile(Path path, String content) {
        this.path = path;
        this.name = path.getFileName().toString();
        this.content = content;
        this.language = detectLanguage(name);
    }

    private static String detectLanguage(String fileName) {
        String n = fileName.toLowerCase();
        if (n.endsWith(".java")) return "Java";
        if (n.endsWith(".py")) return "Python";
        if (n.endsWith(".c") || n.endsWith(".h")) return "C";
        if (n.endsWith(".cpp") || n.endsWith(".hpp")) return "C++";
        if (n.endsWith(".js")) return "JavaScript";
        if (n.endsWith(".cs")) return "C#";
        return "Inconnu";
    }

    public String getName()     { return name; }
    public Path getPath()       { return path; }
    public String getLanguage() { return language; }
    public String getContent()  { return content; }

    @Override
    public String toString() { return name + "  [" + language + "]"; }
}
