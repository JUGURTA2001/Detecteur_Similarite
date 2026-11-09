package algo.niveau2;

import java.util.Set;

import algo.niveau1.Niveau1.Language;

public final class Keywords {

    private Keywords() { }

    private static final Set<String> JAVA = Set.of(
        "abstract","assert","boolean","break","byte","case","catch","char","class","const",
        "continue","default","do","double","else","enum","extends","final","finally","float",
        "for","goto","if","implements","import","instanceof","int","interface","long","native",
        "new","package","private","protected","public","return","short","static","strictfp",
        "super","switch","synchronized","this","throw","throws","transient","try","void",
        "volatile","while","true","false","null","var","record");

    private static final Set<String> C = Set.of(
        "auto","break","case","char","const","continue","default","do","double","else","enum",
        "extern","float","for","goto","if","inline","int","long","register","restrict","return",
        "short","signed","sizeof","static","struct","switch","typedef","union","unsigned",
        "void","volatile","while");

    private static final Set<String> CPP = Set.of(
        "auto","bool","break","case","catch","char","class","const","constexpr","continue",
        "default","delete","do","double","else","enum","explicit","extern","false","float",
        "for","friend","goto","if","inline","int","long","mutable","namespace","new","nullptr",
        "operator","override","private","protected","public","return","short","signed","sizeof",
        "static","struct","switch","template","this","throw","true","try","typedef","typename",
        "union","unsigned","using","virtual","void","volatile","while");

    private static final Set<String> PYTHON = Set.of(
        "False","None","True","and","as","assert","async","await","break","class","continue",
        "def","del","elif","else","except","finally","for","from","global","if","import","in",
        "is","lambda","nonlocal","not","or","pass","raise","return","try","while","with","yield");

    private static final Set<String> JS = Set.of(
        "async","await","break","case","catch","class","const","continue","debugger","default",
        "delete","do","else","export","extends","false","finally","for","function","if","import",
        "in","instanceof","let","new","null","of","return","static","super","switch","this",
        "throw","true","try","typeof","undefined","var","void","while","with","yield");

    private static final Set<String> DART = Set.of(
        "abstract","as","assert","async","await","bool","break","case","catch","class","const",
        "continue","covariant","default","deferred","do","double","dynamic","else","enum",
        "export","extends","extension","external","factory","false","final","finally","for",
        "get","if","implements","import","in","int","interface","is","late","library","mixin",
        "new","null","num","on","operator","part","required","rethrow","return","set","show",
        "static","super","switch","sync","this","throw","true","try","typedef","var","void",
        "while","with","yield");

    public static boolean isKeyword(String token, Language lang) {
        switch (lang) {
            case JAVA:   return JAVA.contains(token);
            case C:      return C.contains(token);
            case CPP:    return CPP.contains(token);
            case PYTHON: return PYTHON.contains(token);
            case JS:     return JS.contains(token);
            case DART:   return DART.contains(token);
            default:     return false;
        }
    }
}