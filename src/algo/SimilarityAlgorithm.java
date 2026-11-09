package algo;

import java.util.Objects;

import model.SourceFile;
import algo.niveau1.Niveau1;
import algo.niveau2.Niveau2;


public class SimilarityAlgorithm {

    

    // ---------- Aiguillage selon le niveau ----------

    public static double similarity(SourceFile reference, SourceFile candidate, Level level) {
        Objects.requireNonNull(reference, "Le fichier de référence ne doit pas être null");
        Objects.requireNonNull(candidate, "Le fichier candidat ne doit pas être null");
        Objects.requireNonNull(level, "Le niveau ne doit pas être null");

        switch (level) {
    case NIVEAU_1:
        return Niveau1.similarity(
                reference.getContent(), reference.getName(),
                candidate.getContent(), candidate.getName());
    case NIVEAU_2:
        return Niveau2.similarity(
                reference.getContent(), reference.getName(),
                candidate.getContent(), candidate.getName());
    case BASIC:
    default:
        return similarity(reference.getContent(), candidate.getContent());
}
    }

    public static double similarity(SourceFile reference, SourceFile candidate) {
        return similarity(reference, candidate, Level.BASIC);
    }

    // ---------- Algorithme de base (espaces) ----------

    public static double similarity(String first, String second) {
        String[] a = tokenize(first);
        String[] b = tokenize(second);

        if (a.length == 0 && b.length == 0) {
            return 100.0;
        }
        int lcs = lcsLength(a, b);
        return (200.0 * lcs) / (a.length + b.length);
    }

    public static int lcsLength(String first, String second) {
        return lcsLength(tokenize(first), tokenize(second));
    }

    private static String[] tokenize(String text) {
        if (text == null) {
            return new String[0];
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split("\\s+");
    }

    // ---------- LCS (public pour que Niveau1 l'utilise) ----------

    public static int lcsLength(String[] a, String[] b) {
        if (a.length == 0 || b.length == 0) {
            return 0;
        }
        if (a.length < b.length) {
            String[] temp = a;
            a = b;
            b = temp;
        }

        int[] previous = new int[b.length + 1];
        int[] current = new int[b.length + 1];

        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                if (a[i - 1].equals(b[j - 1])) {
                    current[j] = previous[j - 1] + 1;
                } else {
                    current[j] = Math.max(previous[j], current[j - 1]);
                }
            }
            int[] tmp = previous;
            previous = current;
            current = tmp;
        }
        return previous[b.length];
    }
}