package algo.niveau1;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import algo.SimilarityAlgorithm;

public class Niveau1 {

    public enum Language { JAVA, C, CPP, PYTHON, UNKNOWN }

    private static final String STRING_C =
            "\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'";
    private static final String STRING_PY =
            "\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'";
    private static final String BODY =
            "|(?<id>[A-Za-z_]\\w*)|(?<num>\\d+(?:\\.\\d+)?)|(?<op>\\S)";

    private static final Pattern C_LIKE = Pattern.compile(
            "(?<comment>//[^\\n]*|/\\*[\\s\\S]*?\\*/)|(?<str>" + STRING_C + ")" + BODY);

    private static final Pattern PYTHON = Pattern.compile(
            "(?<comment>#[^\\n]*)|(?<str>" + STRING_PY + ")" + BODY);

    private Niveau1() {
    }

    public static double similarity(String code1, String fileName1,
                                    String code2, String fileName2) {
        Language l1 = detectLanguage(fileName1);
        Language l2 = detectLanguage(fileName2);

        if (l1 == Language.UNKNOWN || l2 == Language.UNKNOWN) {
            throw new IllegalArgumentException("Langage non supporté (java, c, cpp, py)");
        }
        if (family(l1) != family(l2)) {
            throw new IllegalArgumentException("Les deux fichiers doivent avoir le même langage");
        }

        String[] a = tokenize(code1, l1).toArray(new String[0]);
        String[] b = tokenize(code2, l2).toArray(new String[0]);

        if (a.length == 0 && b.length == 0) {
            return 100.0;
        }
        int lcs = SimilarityAlgorithm.lcsLength(a, b);
        return (200.0 * lcs) / (a.length + b.length);
    }

    public static List<String> tokenize(String code, Language lang) {
        List<String> tokens = new ArrayList<>();
        if (code == null) {
            return tokens;
        }
        Pattern p = (lang == Language.PYTHON) ? PYTHON : C_LIKE;
        Matcher m = p.matcher(code);
        while (m.find()) {
            if (m.group("comment") != null) {
                continue; // commentaires ignorés
            }
            tokens.add(m.group());
        }
        return tokens;
    }

    public static Language detectLanguage(String fileName) {
        String n = fileName == null ? "" : fileName.toLowerCase();
        if (n.endsWith(".java")) return Language.JAVA;
        if (n.endsWith(".c")) return Language.C;
        if (n.endsWith(".h") || n.endsWith(".cpp") || n.endsWith(".cc")
                || n.endsWith(".hpp") || n.endsWith(".cxx")) return Language.CPP;
        if (n.endsWith(".py")) return Language.PYTHON;
        return Language.UNKNOWN;
    }

    /** C et C++ sont comparables entre eux. */
    private static int family(Language l) {
        switch (l) {
            case C:
            case CPP:    return 1;
            case JAVA:   return 2;
            case PYTHON: return 3;
            default:     return 0;
        }
    }
}