package algo.niveau2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import algo.SimilarityAlgorithm;
import algo.niveau1.Niveau1;
import algo.niveau1.Niveau1.Language;
import algo.niveau1.Niveau1.Token;

public class Niveau2 {

    private static final String ID = "ID";

    private Niveau2() {
    }

    public static double similarity(String code1, String fileName1,
                                    String code2, String fileName2) {
        Language l1 = Niveau1.detectLanguage(fileName1);
        Language l2 = Niveau1.detectLanguage(fileName2);

        if (l1 == Language.UNKNOWN || l2 == Language.UNKNOWN) {
            throw new IllegalArgumentException(
                    "Langage non supporté (java, c, cpp, py, js, dart)");
        }
        if (!Niveau1.sameFamily(l1, l2)) {
            throw new IllegalArgumentException("Les deux fichiers doivent avoir le même langage");
        }

        // même jeu de mots-clés pour les deux fichiers (cas C / C++)
        Language lang = (l1 == l2) ? l1 : Language.CPP;

        List<String[]> a = statements(code1, lang);
        List<String[]> b = statements(code2, lang);

        int sizeA = totalSize(a);
        int sizeB = totalSize(b);
        if (sizeA == 0 && sizeB == 0) {
            return 100.0;
        }

        int matched = matchStatements(a, b);
        return (200.0 * matched) / (sizeA + sizeB);
    }

    // ---------- Découpage en instructions + normalisation ----------

    static List<String[]> statements(String code, Language lang) {
        List<Token> tokens = Niveau1.scan(code, lang);
        List<String[]> result = new ArrayList<>();
        List<String> current = new ArrayList<>();

        if (lang == Language.PYTHON) {
            int depth = 0;
            int prevEndLine = -1;
            for (Token t : tokens) {
                // nouvelle ligne logique (hors parenthèses) : on coupe
                if (t.line != prevEndLine && depth == 0 && !current.isEmpty()) {
                    flush(current, result);
                }
                current.add(normalize(t, lang));
                prevEndLine = t.endLine;

                String s = t.text;
                if (s.equals("(") || s.equals("[") || s.equals("{")) {
                    depth++;
                } else if ((s.equals(")") || s.equals("]") || s.equals("}")) && depth > 0) {
                    depth--;
                }
            }
        } else {
            int paren = 0;
            for (Token t : tokens) {
                current.add(normalize(t, lang));

                String s = t.text;
                if (s.equals("(") || s.equals("[")) {
                    paren++;
                } else if ((s.equals(")") || s.equals("]")) && paren > 0) {
                    paren--;
                }
                // fin d'instruction : ; { }  (hors parenthèses, ex. for(;;))
                if (paren == 0 && (s.equals(";") || s.equals("{") || s.equals("}"))) {
                    flush(current, result);
                }
            }
        }
        flush(current, result);
        return result;
    }

    private static String normalize(Token t, Language lang) {
        if (t.identifier && !Keywords.isKeyword(t.text, lang)) {
            return ID; // variable / fonction renommable
        }
        return t.text;
    }

    private static void flush(List<String> current, List<String[]> result) {
        if (!current.isEmpty()) {
            result.add(current.toArray(new String[0]));
            current.clear();
        }
    }

    private static int totalSize(List<String[]> list) {
        int n = 0;
        for (String[] s : list) {
            n += s.length;
        }
        return n;
    }

    // ---------- Appariement des instructions (indépendant de l'ordre) ----------

    private static int matchStatements(List<String[]> a, List<String[]> b) {
        List<int[]> pairs = new ArrayList<>(); // {lcs, i, j, écart de taille}

        for (int i = 0; i < a.size(); i++) {
            for (int j = 0; j < b.size(); j++) {
                int l = lcs(a.get(i), b.get(j));
                if (l > 0) {
                    pairs.add(new int[] { l, i, j, Math.abs(a.get(i).length - b.get(j).length) });
                }
            }
        }

        // les meilleures correspondances d'abord
        pairs.sort((p, q) -> {
            if (p[0] != q[0]) return Integer.compare(q[0], p[0]);
            return Integer.compare(p[3], q[3]);
        });

        boolean[] usedA = new boolean[a.size()];
        boolean[] usedB = new boolean[b.size()];
        int total = 0;

        for (int[] p : pairs) {
            if (!usedA[p[1]] && !usedB[p[2]]) {
                usedA[p[1]] = true;
                usedB[p[2]] = true;
                total += p[0];
            }
        }
        return total;
    }

    private static int lcs(String[] x, String[] y) {
        if (Arrays.equals(x, y)) {
            return x.length;
        }
        return SimilarityAlgorithm.lcsLength(x, y);
    }
}