package algo.niveau1;

import java.util.Set;

public final class Keywords {

    private Keywords() { }

    public static final Set<String> JAVA = Set.of(
        "abstract","assert","boolean","break","byte","case","catch","char","class","const",
        "continue","default","do","double","else","enum","extends","final","finally","float",
        "for","goto","if","implements","import","instanceof","int","interface","long","native",
        "new","package","private","protected","public","return","short","static","strictfp",
        "super","switch","synchronized","this","throw","throws","transient","try","void",
        "volatile","while","true","false","null","var","record");

    public static final Set<String> C = Set.of(
        "auto","break","case","char","const","continue","default","do","double","else","enum",
        "extern","float","for","goto","if","inline","int","long","register","restrict","return",
        "short","signed","sizeof","static","struct","switch","typedef","union","unsigned",
        "void","volatile","while");

    public static final Set<String> CPP = Set.of(
        "auto","bool","break","case","catch","char","class","const","constexpr","continue",
        "default","delete","do","double","else","enum","explicit","extern","false","float",
        "for","friend","goto","if","inline","int","long","mutable","namespace","new","nullptr",
        "operator","override","private","protected","public","return","short","signed","sizeof",
        "static","struct","switch","template","this","throw","true","try","typedef","typename",
        "union","unsigned","using","virtual","void","volatile","while");

    public static final Set<String> PYTHON = Set.of(
        "False","None","True","and","as","assert","async","await","break","class","continue",
        "def","del","elif","else","except","finally","for","from","global","if","import","in",
        "is","lambda","nonlocal","not","or","pass","raise","return","try","while","with","yield");

    public static boolean isKeyword(String token, Niveau1.Language lang) {
        switch (lang) {
            case JAVA:   return JAVA.contains(token);
            case C:      return C.contains(token);
            case CPP:    return CPP.contains(token);
            case PYTHON: return PYTHON.contains(token);
            default:     return false;
        }
    }
}