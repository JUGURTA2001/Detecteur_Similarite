package service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import algo.niveau1.Niveau1;
import algo.niveau1.Niveau1.Language;
import algo.niveau1.Niveau1.Token;
import algo.niveau2.Keywords;

/** Statistiques descriptives d'un fichier source. */
public final class CodeStats {

    public final int lines;
    public final int tokens;
    public final int keywords;
    public final int variables;
    public final int distinctVariables;
    public final int numbers;
    public final int strings;
    public final int symbols;
    /** Mots-clés utilisés, triés du plus fréquent au moins fréquent. */
    public final Map<String, Integer> keywordCounts;

    private CodeStats(int lines, int tokens, int keywords, int variables, int distinctVariables,
                      int numbers, int strings, int symbols, Map<String, Integer> keywordCounts) {
        this.lines = lines;
        this.tokens = tokens;
        this.keywords = keywords;
        this.variables = variables;
        this.distinctVariables = distinctVariables;
        this.numbers = numbers;
        this.strings = strings;
        this.symbols = symbols;
        this.keywordCounts = keywordCounts;
    }

    public static CodeStats of(String code, String fileName) {
        Language lang = Niveau1.detectLanguage(fileName);
        int kw = 0, vars = 0, nums = 0, strs = 0, syms = 0;
        Map<String, Integer> counts = new LinkedHashMap<>();
        Set<String> names = new HashSet<>();

        List<Token> tokens = Niveau1.scan(code, lang);
        for (Token t : tokens) {
            char c = t.text.charAt(0);
            if (t.identifier) {
                if (Keywords.isKeyword(t.text, lang)) {
                    kw++;
                    counts.merge(t.text, 1, Integer::sum);
                } else {
                    vars++;
                    names.add(t.text);
                }
            } else if (Character.isDigit(c)) {
                nums++;
            } else if (t.text.length() >= 2 && (c == '"' || c == '\'' || c == '`')) {
                strs++;
            } else {
                syms++;
            }
        }

        // tri décroissant
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        Map<String, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            sorted.put(e.getKey(), e.getValue());
        }

        int lines = 0;
        if (code != null) {
            for (String l : code.split("\\R")) {
                if (!l.trim().isEmpty()) {
                    lines++;
                }
            }
        }
        return new CodeStats(lines, tokens.size(), kw, vars, names.size(), nums, strs, syms, sorted);
    }
}