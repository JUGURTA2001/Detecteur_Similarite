package algo.niveau3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import algo.niveau1.Niveau1;
import algo.niveau1.Niveau1.Language;
import algo.niveau1.Niveau1.Token;
import algo.niveau2.Keywords;

/**
 * Niveau 3 : comparaison entre langages différents.
 * Chaque code est traduit en une suite de symboles neutres (FUNC, LOOP, IF, ASSIGN...),
 * puis comparée par un LCS pondéré (ordre) et un appariement d'instructions (déplacements).
 */
public class Niveau3 {

    private static final double W_SEQUENCE = 0.5;
    private static final double W_STATEMENTS = 0.5;

    private static final String EOS = "|";       // fin d'instruction
    private static final String OPEN = "OPEN";   // début de bloc
    private static final String CLOSE = "CLOSE"; // fin de bloc

    private static final int ID = 0, NUM = 1, STR = 2, SYM = 3;

    // ---------- Tables de correspondance ----------

    private static final Map<String, String> WORDS = new HashMap<>();
    private static final Map<String, String> BUILTINS = new HashMap<>();
    private static final Set<String> ALWAYS = new HashSet<>(Arrays.asList("cin", "cout", "length"));
    private static final Set<String> NULLS =
            new HashSet<>(Arrays.asList("null", "none", "nil", "undefined", "nullptr"));
    private static final Set<String> IMPORTS =
            new HashSet<>(Arrays.asList("import", "package", "using"));
    private static final Set<String> NOT_TYPE_PREV = new HashSet<>(Arrays.asList(
            "return", "new", "else", "throw", "case", "in", "of", "and", "or", "not",
            "await", "yield", "typeof", "delete", "do"));
    private static final Set<String> DROP = new HashSet<>(Arrays.asList(
            "int", "long", "short", "byte", "char", "float", "double", "boolean", "bool", "void",
            "string", "String", "var", "let", "const", "final", "static", "public", "private",
            "protected", "auto", "unsigned", "signed", "dynamic", "num", "late", "required",
            "abstract", "override", "virtual", "inline", "extern", "register", "volatile",
            "mutable", "explicit", "constexpr", "new", "this", "self", "super", "do", "in", "of",
            "is", "as", "with", "throws", "extends", "implements", "async", "await", "export",
            "System", "out", "err", "Math", "std", "endl", "namespace", "pass", "global",
            "nonlocal", "Integer", "Double", "Long", "Object", "List", "ArrayList", "Map",
            "HashMap", "Set", "HashSet", "vector", "Scanner", "str"));
    private static final Set<String> MULTI = new HashSet<>(Arrays.asList(
            "===", "!==", "**=", "//=", "...",
            "==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=", "%=",
            "//", "**", "->", "=>", "::", "<<", ">>"));

    static {
        words("LOOP", "for", "foreach", "while");
        words("IF", "if");
        words("ELSE", "else");
        words("RET", "return", "yield");
        words("BRK", "break");
        words("CNT", "continue");
        words("FUNC", "def", "function", "lambda");
        words("CLASS", "class", "struct", "interface", "enum");
        words("TRY", "try");
        words("CATCH", "catch", "except");
        words("FIN", "finally");
        words("THROW", "throw", "raise");
        words("SWITCH", "switch", "match");
        words("CASE", "case");
        words("AND", "and");
        words("OR", "or");
        words("NOT", "not");

        builtins("IO", "print", "println", "printf", "fprintf", "puts", "write", "writeln",
                "echo", "scanf", "input", "readline", "readln", "nextint", "nextline",
                "nextdouble", "getline", "cin", "cout");
        builtins("MAX", "max");
        builtins("MIN", "min");
        builtins("ABS", "abs");
        builtins("SQRT", "sqrt");
        builtins("POW", "pow");
        builtins("LEN", "len", "length", "size", "count");
        builtins("PUSH", "append", "add", "push", "push_back", "addlast", "insert");
        builtins("POP", "pop", "remove", "poll");
        builtins("SORT", "sort", "sorted");
        builtins("SUM", "sum");
        builtins("SWAP", "swap");
    }

    private static void words(String symbol, String... names) {
        for (String n : names) WORDS.put(n, symbol);
    }

    private static void builtins(String symbol, String... names) {
        for (String n : names) BUILTINS.put(n, symbol);
    }

    private Niveau3() {
    }

    // =====================================================================
    //  API
    // =====================================================================

    public static double similarity(String code1, String fileName1,
                                    String code2, String fileName2) {
        Language l1 = Niveau1.detectLanguage(fileName1);
        Language l2 = Niveau1.detectLanguage(fileName2);

        // (a) Plus de test UNKNOWN ici : un fichier lisible n'est jamais refusé.
        // Le test UNKNOWN reste uniquement dans abstractStream() si besoin.

        List<String> s1 = build(code1, l1);
        List<String> s2 = build(code2, l2);

        String[] flat1 = flat(s1);
        String[] flat2 = flat(s2);
        double totalFlat = total(flat1) + total(flat2);
        if (totalFlat == 0) {
            return 100.0;
        }
        double seq = 2.0 * weightedLcs(flat1, flat2) / totalFlat;

        List<String[]> st1 = statements(s1);
        List<String[]> st2 = statements(s2);
        double totalSt = totalStatements(st1) + totalStatements(st2);
        double stmt = (totalSt == 0) ? 1.0 : 2.0 * matchStatements(st1, st2) / totalSt;

        return 100.0 * (W_SEQUENCE * seq + W_STATEMENTS * stmt);
    }

    /** Séquence abstraite d'un fichier (utile pour déboguer et pour le rapport). */
    public static List<String> abstractStream(String code, String fileName) {
        Language lang = Niveau1.detectLanguage(fileName);
        if (lang == Language.UNKNOWN) {
            throw new IllegalArgumentException("Langage non supporté : " + fileName);
        }
        return build(code, lang);
    }

    // =====================================================================
    //  Représentation interne des tokens
    // =====================================================================

    private static final class Item {
        final String text;
        final int kind;
        final int line;
        final int endLine;
        final int col;

        Item(String text, int kind, int line, int endLine, int col) {
            this.text = text;
            this.kind = kind;
            this.line = line;
            this.endLine = endLine;
            this.col = col;
        }

        static Item of(Token t) {
            char c = t.text.charAt(0);
            int kind;
            if (t.identifier) {
                kind = ID;
            } else if (Character.isDigit(c)) {
                kind = NUM;
            } else if (t.text.length() >= 2 && (c == '"' || c == '\'' || c == '`')) {
                kind = STR;
            } else {
                kind = SYM;
            }
            return new Item(t.text, kind, t.line, t.endLine, t.col);
        }
    }

    /** Fusionne les symboles adjacents : '=' '=' devient '==', '&' '&' devient '&&', etc. */
    private static List<Item> toItems(List<Token> tokens) {
        List<Item> out = new ArrayList<>();
        int i = 0;
        while (i < tokens.size()) {
            Token t = tokens.get(i);
            Item it = Item.of(t);
            if (it.kind == SYM) {
                boolean merged = false;
                for (int len = 3; len >= 2 && !merged; len--) {
                    String cand = adjacent(tokens, i, len);
                    if (cand != null && MULTI.contains(cand)) {
                        out.add(new Item(cand, SYM, t.line, tokens.get(i + len - 1).endLine, t.col));
                        i += len;
                        merged = true;
                    }
                }
                if (merged) {
                    continue;
                }
            }
            out.add(it);
            i++;
        }
        return out;
    }

    private static String adjacent(List<Token> tokens, int i, int len) {
        if (i + len > tokens.size()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < len; k++) {
            Token t = tokens.get(i + k);
            if (t.identifier || t.text.length() != 1 || Character.isLetterOrDigit(t.text.charAt(0))) {
                return null;
            }
            if (k > 0) {
                Token p = tokens.get(i + k - 1);
                if (t.line != p.line || t.col != p.col + 1) {
                    return null;
                }
            }
            sb.append(t.text);
        }
        return sb.toString();
    }

    // =====================================================================
    //  Traduction vers la séquence abstraite
    // =====================================================================

    private static List<String> build(String code, Language lang) {
        List<Item> items = toItems(Niveau1.scan(code, lang));
        int n = items.size();

        // (b) TEXT est traité comme Python pour le découpage par ligne.
        boolean py = (lang == Language.PYTHON);
        boolean lineBased = py || lang == Language.TEXT;
        boolean[] startsLine = lineStarts(items, lineBased);

        List<String> out = new ArrayList<>();
        List<Integer> indents = new ArrayList<>();
        indents.add(0);

        int i = 0;
        while (i < n) {
            Item it = items.get(i);
            String x = it.text;

            // Découpage par ligne : Python via indentation, Texte : une instruction par ligne.
            if (lineBased && startsLine[i]) {
                eos(out);
                if (py) {
                    if (it.col > indents.get(indents.size() - 1)) {
                        indents.add(it.col);
                        out.add(OPEN);
                        eos(out);
                    } else {
                        while (indents.size() > 1 && it.col < indents.get(indents.size() - 1)) {
                            indents.remove(indents.size() - 1);
                            out.add(CLOSE);
                            eos(out);
                        }
                    }
                }
            }

            if (it.kind == NUM) {
                out.add("NUM");
                i++;
                continue;
            }
            if (it.kind == STR) {
                out.add("STR");
                i++;
                continue;
            }

            // ----- symboles -----
            if (it.kind == SYM) {
                // (c) Pascal : "<>" est l'opérateur de différence
                if (lang == Language.PASCAL
                        && x.equals("<")
                        && i + 1 < n
                        && items.get(i + 1).text.equals(">")) {
                    out.add("CMP");
                    i += 2;
                    continue;
                }

                switch (x) {
                    case "{":
                        if (!py) { eos(out); out.add(OPEN); eos(out); }
                        break;
                    case "}":
                        if (!py) { eos(out); out.add(CLOSE); eos(out); }
                        break;
                    case ";":
                        eos(out);
                        break;
                    case "&&": out.add("AND"); break;
                    case "||": out.add("OR"); break;
                    case "!":  out.add("NOT"); break;
                    case "==": case "!=": case "<": case ">": case "<=": case ">=":
                    case "===": case "!==":
                        out.add("CMP");
                        break;
                    case "=": case "+=": case "-=": case "*=": case "/=": case "%=":
                    case "++": case "--": case "//=": case "**=":
                        out.add("ASSIGN");
                        break;
                    case "+": case "-": case "*": case "/": case "%": case "//": case "**":
                        out.add("AR");
                        break;
                    case "#":            // directive de préprocesseur
                        i = skipLine(items, i);
                        continue;
                    case "@":            // annotation / décorateur
                        i += 2;
                        continue;
                    default:             // ( ) [ ] , . : ? & | -> => :: << >> ... ignorés
                        break;
                }
                i++;
                continue;
            }

            // (d) Pascal : mots-clés structurels + opérateurs div / mod
            if (lang == Language.PASCAL) {
                if (x.equals("begin"))  { out.add(OPEN);  i++; continue; }
                if (x.equals("end"))    { out.add(CLOSE); i++; continue; }
                if (x.equals("repeat")) { out.add("LOOP"); i++; continue; }
                if (x.equals("div") || x.equals("mod")) { out.add("AR"); i++; continue; }
                if (x.equals("procedure") || x.equals("function")) {
                    out.add("FUNC");
                    i++;
                    if (i < n && items.get(i).kind == ID) {
                        i++;   // nom de la fonction ignoré
                    }
                    continue;
                }
            }

            // ----- mots -----
            String lower = x.toLowerCase();
            boolean nextParen = (i + 1 < n && items.get(i + 1).text.equals("("));

            if (x.equals("elif")) {
                out.add("ELSE");
                out.add("IF");
                i++;
                continue;
            }

            String w = WORDS.get(x);
            if (w != null) {
                out.add(w);
                if (w.equals("LOOP")) {
                    i = skipLoopHeader(items, i, py);
                } else if (w.equals("FUNC") && (x.equals("def") || x.equals("function"))) {
                    i++;
                    if (i < n && items.get(i).kind == ID) {
                        i++;
                    }
                } else {
                    i++;
                }
                continue;
            }

            if (lower.equals("true") || lower.equals("false")) {
                out.add("BOOL");
                i++;
                continue;
            }
            if (NULLS.contains(lower)) {
                out.add("NULL");
                i++;
                continue;
            }
            if (IMPORTS.contains(x) || (py && x.equals("from"))) {
                i = skipLine(items, i);
                continue;
            }
            if (lower.equals("console") && i + 2 < n && items.get(i + 1).text.equals(".")) {
                out.add("IO");
                i += 3;
                continue;
            }

            String b = BUILTINS.get(lower);
            if (b != null && (nextParen || ALWAYS.contains(lower))) {
                out.add(b);
                i++;
                continue;
            }

            // types génériques : List<Integer>, vector<int>, new ArrayList<>()
            if (!py && i + 1 < n && items.get(i + 1).text.equals("<")) {
                int g = skipGenerics(items, i + 1);
                if (g > 0) {
                    i = g;
                    continue;
                }
            }

            if (DROP.contains(x) || Keywords.isKeyword(x, lang)) {
                i++;
                continue;
            }

            // nom de classe après 'new'
            if (i > 0 && items.get(i - 1).text.equals("new")) {
                i++;
                continue;
            }

            // type utilisateur dans une déclaration
            if (Character.isUpperCase(x.charAt(0)) && i + 1 < n) {
                Item nx = items.get(i + 1);
                if (nx.kind == ID && nx.line == it.endLine && !reserved(nx.text, lang)) {
                    i++;
                    continue;
                }
            }

            // définition de fonction/méthode
            if (!py && nextParen && isFunctionDefinition(items, i)) {
                out.add("FUNC");
                i++;
                continue;
            }

            out.add(nextParen ? "CALL" : "VAR");
            i++;
        }

        if (py) {
            while (indents.size() > 1) {
                indents.remove(indents.size() - 1);
                eos(out);
                out.add(CLOSE);
                eos(out);
            }
        }
        return out;
    }

    private static void eos(List<String> out) {
        if (!out.isEmpty() && !out.get(out.size() - 1).equals(EOS)) {
            out.add(EOS);
        }
    }

    private static boolean reserved(String x, Language lang) {
        String lower = x.toLowerCase();
        return WORDS.containsKey(x) || x.equals("elif") || DROP.contains(x)
                || IMPORTS.contains(x) || NULLS.contains(lower)
                || lower.equals("true") || lower.equals("false")
                || Keywords.isKeyword(x, lang);
    }

    /** Repère les premiers tokens de chaque ligne logique (hors parenthèses). */
    private static boolean[] lineStarts(List<Item> items, boolean lineBased) {
        boolean[] r = new boolean[items.size()];
        if (!lineBased) {
            return r;
        }
        int depth = 0;
        int prevEnd = -1;
        for (int k = 0; k < items.size(); k++) {
            Item t = items.get(k);
            r[k] = (depth == 0 && t.line != prevEnd);
            prevEnd = t.endLine;
            if (t.kind == SYM) {
                String s = t.text;
                if (s.equals("(") || s.equals("[") || s.equals("{")) {
                    depth++;
                } else if ((s.equals(")") || s.equals("]") || s.equals("}")) && depth > 0) {
                    depth--;
                }
            }
        }
        return r;
    }

    private static int skipLine(List<Item> items, int i) {
        int line = items.get(i).line;
        int j = i + 1;
        while (j < items.size() && items.get(j).line == line) {
            j++;
        }
        return j;
    }

    /** for / while : l'en-tête est résumé en LOOP. */
    private static int skipLoopHeader(List<Item> items, int i, boolean py) {
        int n = items.size();
        int j = i + 1;
        if (py) {
            int depth = 0;
            while (j < n) {
                String t = items.get(j).text;
                if (t.equals("(") || t.equals("[") || t.equals("{")) {
                    depth++;
                } else if (t.equals(")") || t.equals("]") || t.equals("}")) {
                    depth--;
                } else if (t.equals(":") && depth == 0) {
                    return j + 1;
                }
                j++;
            }
            return n;
        }
        if (j < n && items.get(j).text.equals("(")) {
            int c = matchParen(items, j);
            return (c < 0) ? j : c + 1;
        }
        return j;
    }

    private static int matchParen(List<Item> items, int open) {
        int depth = 0;
        for (int k = open; k < items.size(); k++) {
            String s = items.get(k).text;
            if (s.equals("(")) {
                depth++;
            } else if (s.equals(")")) {
                depth--;
                if (depth == 0) {
                    return k;
                }
            }
        }
        return -1;
    }

    private static int skipGenerics(List<Item> items, int start) {
        int depth = 0;
        for (int k = start; k < items.size() && k < start + 20; k++) {
            Item t = items.get(k);
            String s = t.text;
            if (s.equals("<")) {
                depth++;
            } else if (s.equals(">")) {
                depth--;
            } else if (s.equals(">>")) {
                depth -= 2;
            } else if (s.equals("?") || s.equals(",") || s.equals(".") || s.equals("[")
                    || s.equals("]") || s.equals("&") || s.equals("*")) {
                // autorisés
            } else if (t.kind != ID) {
                return -1;
            }
            if (depth < 0) {
                return -1;
            }
            if (depth == 0) {
                int after = k + 1;
                if (after < items.size()) {
                    Item nx = items.get(after);
                    if (nx.kind == ID || nx.text.equals("(")) {
                        return after;
                    }
                }
                return -1;
            }
        }
        return -1;
    }

    private static boolean isFunctionDefinition(List<Item> items, int i) {
        if (i == 0) {
            return false;
        }
        int close = matchParen(items, i + 1);
        if (close < 0) {
            return false;
        }
        Item prev = items.get(i - 1);
        boolean prevOk = (prev.kind == ID && !NOT_TYPE_PREV.contains(prev.text))
                || prev.text.equals(">") || prev.text.equals("*")
                || prev.text.equals("&") || prev.text.equals("]");
        if (!prevOk) {
            return false;
        }
        int k = close + 1;
        int limit = Math.min(items.size(), k + 8);
        while (k < limit && (items.get(k).kind == ID || items.get(k).text.equals(","))) {
            k++;
        }
        if (k >= items.size()) {
            return false;
        }
        String t = items.get(k).text;
        return t.equals("{") || t.equals("=>");
    }

    // =====================================================================
    //  Comparaison des séquences abstraites
    // =====================================================================

    private static String[] flat(List<String> stream) {
        List<String> r = new ArrayList<>();
        for (String s : stream) {
            if (!s.equals(EOS)) {
                r.add(s);
            }
        }
        return r.toArray(new String[0]);
    }

    private static List<String[]> statements(List<String> stream) {
        List<String[]> res = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        for (String s : stream) {
            if (s.equals(EOS) || s.equals(OPEN) || s.equals(CLOSE)) {
                if (!cur.isEmpty()) {
                    res.add(cur.toArray(new String[0]));
                    cur.clear();
                }
            } else {
                cur.add(s);
            }
        }
        if (!cur.isEmpty()) {
            res.add(cur.toArray(new String[0]));
        }
        return res;
    }

    private static double weight(String s) {
        switch (s) {
            case "FUNC": case "LOOP": case "IF": case "ELSE": case "RET": case "BRK":
            case "CNT": case "TRY": case "CATCH": case "FIN": case "THROW":
            case "SWITCH": case "CASE": case "CLASS":
                return 3.0;
            case "IO": case "MAX": case "MIN": case "ABS": case "SQRT": case "POW":
            case "LEN": case "PUSH": case "POP": case "SORT": case "SUM": case "SWAP":
                return 2.0;
            case "VAR": case "NUM": case "STR": case "OPEN": case "CLOSE":
                return 0.5;
            default:
                return 1.0;
        }
    }

    private static double total(String[] a) {
        double t = 0;
        for (String s : a) {
            t += weight(s);
        }
        return t;
    }

    private static double totalStatements(List<String[]> list) {
        double t = 0;
        for (String[] s : list) {
            t += total(s);
        }
        return t;
    }

    private static double weightedLcs(String[] a, String[] b) {
        if (a.length == 0 || b.length == 0) {
            return 0;
        }
        if (a.length < b.length) {
            String[] tmp = a;
            a = b;
            b = tmp;
        }
        double[] previous = new double[b.length + 1];
        double[] current = new double[b.length + 1];

        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                if (a[i - 1].equals(b[j - 1])) {
                    current[j] = previous[j - 1] + weight(a[i - 1]);
                } else {
                    current[j] = Math.max(previous[j], current[j - 1]);
                }
            }
            double[] t = previous;
            previous = current;
            current = t;
        }
        return previous[b.length];
    }

    private static double matchStatements(List<String[]> a, List<String[]> b) {
        List<double[]> pairs = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            String[] x = a.get(i);
            for (int j = 0; j < b.size(); j++) {
                String[] y = b.get(j);
                double s = Arrays.equals(x, y) ? total(x) : weightedLcs(x, y);
                if (s > 0) {
                    pairs.add(new double[] { s, i, j, Math.abs(total(x) - total(y)) });
                }
            }
        }
        pairs.sort((p, q) -> {
            int c = Double.compare(q[0], p[0]);
            return (c != 0) ? c : Double.compare(p[3], q[3]);
        });

        boolean[] usedA = new boolean[a.size()];
        boolean[] usedB = new boolean[b.size()];
        double sum = 0;
        for (double[] p : pairs) {
            int i = (int) p[1];
            int j = (int) p[2];
            if (!usedA[i] && !usedB[j]) {
                usedA[i] = true;
                usedB[j] = true;
                sum += p[0];
            }
        }
        return sum;
    }

    // =====================================================================
    //  Test en ligne de commande :
    //  java -cp target/classes algo.niveau3.Niveau3 data/n3_max.java data/n3_max.py
    // =====================================================================

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage : Niveau3 <fichier1> <fichier2>");
            return;
        }
        String c1 = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        String c2 = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8);
        System.out.println(args[0] + " : " + String.join(" ", abstractStream(c1, args[0])));
        System.out.println(args[1] + " : " + String.join(" ", abstractStream(c2, args[1])));
        System.out.printf("Score niveau 3 : %.2f %%%n", similarity(c1, args[0], c2, args[1]));
    }
}