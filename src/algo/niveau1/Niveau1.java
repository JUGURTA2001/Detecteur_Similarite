package algo.niveau1;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import algo.SimilarityAlgorithm;

public class Niveau1 {

    public enum Language { JAVA, C, CPP, PYTHON, JS, DART, PASCAL, TEXT, UNKNOWN }

    /** Un token avec sa position (utile pour les niveaux 2 et 3). */
    public static final class Token {
        public final String text;
        public final int line;      // ligne de début
        public final int endLine;   // ligne de fin
        public final int col;       // colonne de début
        public final boolean identifier;

        Token(String text, int line, int endLine, int col, boolean identifier) {
            this.text = text;
            this.line = line;
            this.endLine = endLine;
            this.col = col;
            this.identifier = identifier;
        }
    }

    // ---------- Chaînes de caractères ----------

    private static final String STRING_C =
            "\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'";

    private static final String STRING_PY =
            "\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'";

    private static final String STRING_WEB =
            "\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|`[\\s\\S]*?`"
            + "|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'";

    private static final String STRING_PASCAL = "'(?:''|[^'\\n])*'";

    // ---------- Identifiants, nombres, symboles ----------

    private static final String BODY =
            "|(?<id>[A-Za-z_]\\w*)|(?<num>\\d+(?:\\.\\d+)?)|(?<op>\\S)";

    private static final String BODY_WEB =
            "|(?<id>[A-Za-z_$][\\w$]*)|(?<num>\\d+(?:\\.\\d+)?)|(?<op>\\S)";

    // ---------- Patterns ----------

    private static final Pattern C_LIKE = Pattern.compile(
            "(?<comment>//[^\\n]*|/\\*[\\s\\S]*?\\*/)|(?<str>" + STRING_C + ")" + BODY);

    private static final Pattern PYTHON = Pattern.compile(
            "(?<comment>#[^\\n]*)|(?<str>" + STRING_PY + ")" + BODY);

    private static final Pattern JS_DART = Pattern.compile(
            "(?<comment>//[^\\n]*|/\\*[\\s\\S]*?\\*/)|(?<str>" + STRING_WEB + ")" + BODY_WEB);

    private static final Pattern PASCAL = Pattern.compile(
            "(?<comment>//[^\\n]*|\\{[\\s\\S]*?\\}|\\(\\*[\\s\\S]*?\\*\\))"
            + "|(?<str>" + STRING_PASCAL + ")" + BODY);

    /** Texte libre : pas de commentaires ni de chaînes, mots Unicode (accents), nombres, symboles. */
    private static final Pattern TEXT = Pattern.compile(
            "(?<comment>(?!))|(?<str>(?!))"
            + "|(?<id>[\\p{L}_][\\p{L}\\p{N}_]*)|(?<num>\\d+(?:[.,]\\d+)?)|(?<op>\\S)");

    private Niveau1() {
    }

    // ---------- Similarité niveau 1 ----------

    public static double similarity(String code1, String fileName1,
                                    String code2, String fileName2) {
        Language l1 = detectLanguage(fileName1);
        Language l2 = detectLanguage(fileName2);

        if (!sameFamily(l1, l2)) {
            throw new IllegalArgumentException("Les deux fichiers doivent avoir le même langage");
        }

        // les deux fichiers sont découpés avec les mêmes règles
        Language lang = common(l1, l2);
        String[] a = tokenize(code1, lang).toArray(new String[0]);
        String[] b = tokenize(code2, lang).toArray(new String[0]);

        if (a.length == 0 && b.length == 0) {
            return 100.0;
        }
        int lcs = SimilarityAlgorithm.lcsLength(a, b);
        return (200.0 * lcs) / (a.length + b.length);
    }

    // ---------- Tokenisation ----------

    public static List<String> tokenize(String code, Language lang) {
        List<String> texts = new ArrayList<>();
        for (Token t : scan(code, lang)) {
            texts.add(t.text);
        }
        return texts;
    }

    /** Tokens (sans commentaires) avec leur position. */
    public static List<Token> scan(String code, Language lang) {
        List<Token> tokens = new ArrayList<>();
        if (code == null) {
            return tokens;
        }

        boolean ignoreCase = (lang == Language.PASCAL || lang == Language.TEXT);
        //choisir le bon pattern pour un langage.
        Matcher m = patternFor(lang).matcher(code);
        int cursor = 0;
        int line = 0;
        int lineStart = 0;

        while (m.find()) {
            for (; cursor < m.start(); cursor++) {
                if (code.charAt(cursor) == '\n') {
                    line++;
                    lineStart = cursor + 1;
                }
            }
            int startLine = line;
            int col = m.start() - lineStart;

            for (; cursor < m.end(); cursor++) {
                if (code.charAt(cursor) == '\n') {
                    line++;
                    lineStart = cursor + 1;
                }
            }

            if (m.group("comment") != null) {
                continue; // commentaires ignorés
            }
            boolean isId = m.group("id") != null;
            String text = m.group();
            if (isId && ignoreCase) {
                text = text.toLowerCase();   // Pascal et texte : casse sans importance
            }
            tokens.add(new Token(text, startLine, line, col, isId));
        }
        return tokens;
    }

    private static Pattern patternFor(Language lang) {
        switch (lang) {
            case PYTHON:
                return PYTHON;
            case JS:
            case DART:
                return JS_DART;
            case PASCAL:
                return PASCAL;
            case TEXT:
                return TEXT;
            default:
                return C_LIKE;
        }
    }

    // ---------- Détection du langage ----------

    public static Language detectLanguage(String fileName) {
        if (fileName == null) {
            return Language.UNKNOWN;
        }
        String n = fileName.toLowerCase();
        if (n.endsWith(".java")) return Language.JAVA;
        if (n.endsWith(".c")) return Language.C;
        if (n.endsWith(".h") || n.endsWith(".cpp") || n.endsWith(".cc")
                || n.endsWith(".hpp") || n.endsWith(".cxx")) return Language.CPP;
        if (n.endsWith(".py")) return Language.PYTHON;
        if (n.endsWith(".js") || n.endsWith(".mjs")) return Language.JS;
        if (n.endsWith(".dart")) return Language.DART;
        if (n.endsWith(".pas") || n.endsWith(".pp") || n.endsWith(".dpr")) return Language.PASCAL;
        return Language.TEXT;   // .txt et tout autre fichier lisible dans le Bloc-notes
    }

    /**
     * Deux fichiers sont comparables s'ils sont du même langage (C et C++ ensemble),
     * ou si l'un des deux est un fichier TEXT (il prend alors le langage de l'autre).
     */
    //dire si deux fichiers peuvent être comparés aux niveaux 1 et 2.
    public static boolean sameFamily(Language a, Language b) {
        if (a == Language.UNKNOWN || b == Language.UNKNOWN) {
            return false;
        }
        if (a == Language.TEXT || b == Language.TEXT) {
            return true;
        }
        return family(a) == family(b);
    }

    /** Langage utilisé pour découper les deux fichiers. */
    public static Language common(Language a, Language b) {
        if (a == b) return a;
        if (a == Language.TEXT) return b;
        if (b == Language.TEXT) return a;
        return Language.CPP;   // C avec C++
    }

    private static int family(Language l) {
        switch (l) {
            case C:
            case CPP:    return 1;
            case JAVA:   return 2;
            case PYTHON: return 3;
            case JS:     return 4;
            case DART:   return 5;
            case PASCAL: return 6;
            default:     return 0;
        }
    }
}