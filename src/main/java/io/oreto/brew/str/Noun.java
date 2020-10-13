package io.oreto.brew.str;

import io.oreto.brew.collections.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Noun {
    private static final Set<String> uncountable = Lists.set(
            "equipment", "information", "rice", "money", "monies", "species", "series", "fish"
            , "sheep", "deer", "next", "full", "moose", "bison", "everything"
            , "tea", "sugar", "water", "air", "knowledge", "beauty", "anger", "fear", "love"
            , "research", "safety", "evidence", "engineering", "pants"
            , "flounder", "pliers", "bream", "gallows", "proceedings", "breeches", "graffiti"
            , "rabies", "britches", "headquarters", "salmon", "carp", "herpes", "scissors", "chassis"
            , "high-jinks", "sea-bass", "clippers", "homework", "cod", "shears", "contretemps"
            , "jackanapes", "corps", "mackerel", "swine", "debris", "measles", "trout", "diabetes", "mews"
            , "tuna", "djinn", "mumps", "whiting", "eland", "news", "wildebeest", "elk", "pincers"
            // below are prepositions that don't get pluralized b/c they aren't nouns
            , "beside", "near", "to", "between", "of", "towards", "across",
            "off", "under", "after", "by", "on", "against", "despite", "onto", "unlike", "along",
            "down", "until", "among", "during", "out", "up", "around", "except",
            "upon", "as", "for", "over", "via", "at", "from", "with", "before",
            "in", "within", "behind", "since", "without", "below", "into", "than",
            "beneath", "through"
            // definite articles
            , "the", "an"
    );
    private static final List<String[]> singulars;
    private static final List<String[]> plurals;
    private static final List<String[]> irregulars;

    static {
        singulars = new ArrayList<>();
        plurals = new ArrayList<>();
        irregulars = new ArrayList<>();

        addPlural("$", "s");
        addPlural("s$", "s");
        addPlural("(ax|test)is$", "$1es");
        addPlural("(octop|vir|cact)(us|i)$", "$1i");
        addPlural("(alias|status)$", "$1es");
        addPlural("(bu)s$", "$1ses");
        addPlural("(buffal|tomat)o$", "$1oes");
        addPlural("([ti])um$", "$1a");
        addPlural("sis$", "ses");
        addPlural("(.*)loaf$", "$1loaves");
        addPlural("(?:([^f])fe|([lr])f)$", "$1$2ves");
        addPlural("(hive)$", "$1s");
        addPlural("([^aeiouy]|qu)y$", "$1ies");
        addPlural("(x|ch|ss|sh)$", "$1es");
        addPlural("(matr|vert|ind|append)(?:ix|ex)$", "$1ices");
        addPlural("([m|l])ouse$", "$1ice");
        addPlural("^(ox)$", "$1en");
        addPlural("(quiz)$", "$1zes");

        addSingular("s$", "");
        addSingular("(n)ews$", "$1ews");
        addSingular("([ti])a$", "$1um");
        addSingular("((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$", "$1sis");
        addSingular("(^analy)ses$", "$1sis");
        addSingular("([^fa])ves$", "$1fe");
        addSingular("^yes$", "yes");
        addSingular("(hive)s$", "$1");
        addSingular("(tive)s$", "$1");
        addSingular("(.*)loaves$", "$1loaf");
        addSingular("([lr])aves$", "$1ave");
        addSingular("([lr])ves$", "$1f");
        addSingular("([^aeiouy]|qu)ies$", "$1y");
        addSingular("(s)eries$", "$1eries");
        addSingular("(m)ovies$", "$1ovie");
        addSingular("(x|ch|ss|sh)es$", "$1");
        addSingular("([m|l])ice$", "$1ouse");
        addSingular("(bus)es$", "$1");
        addSingular("(o)es$", "$1");
        addSingular("(shoe)s$", "$1");
        addSingular("(cris|ax|test)es$", "$1is");
        addSingular("(octop|vir)i$", "$1us");
        addSingular("(alias|status)es$", "$1");
        addSingular("^(ox)en", "$1");
        addSingular("(.*)cacti", "$1cactus");
        addSingular("^appendices", "appendix");
        addSingular("(vert|ind)ices$", "$1ex");
        addSingular("(matr)ices$", "$1ix");
        addSingular("(quiz)zes$", "$1");
        addSingular("(database)s$", "$1");

        addIrregular("person", "people");
        addIrregular("man", "men");
        addIrregular("child", "children");
        addIrregular("sex", "sexes");
        addIrregular("move", "moves");
        addIrregular("yes", "yeses");
    }

    static void addPlural(String rule, String replacement){
        plurals.add(0, new String[]{rule, replacement});
    }

    static void addSingular(String rule, String replacement){
        singulars.add(0, new String[]{rule, replacement});
    }

    static void addIrregular(String rule, String replacement){
        irregulars.add(new String[]{rule, replacement});
    }

    /**
     * Replaces a found pattern in a word and returns a transformed word.
     * @param word The word to transform
     * @param rule The rule pattern to apply
     * @param replacement The replacement for the given rule
     * @return Replaces a found pattern in a word and returns a transformed word. Null is pattern does not match.
     */
    static String applyRule(String word, String rule, String replacement) {
        Pattern pattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(word);
        return matcher.find() ? matcher.replaceFirst(replacement) : null;
    }

    public static boolean isCountable(String s) {
        return !uncountable.contains(s);
    }

    public static String plural(String word) {
        if(uncountable.contains(word.toLowerCase())) return word;

        for (String[] irregular : irregulars) {
            if (irregular[0].equalsIgnoreCase(word)) {
                return irregular[1];
            }
        }

        for (String[] pair: plurals) {
            String plural = applyRule(word, pair[0], pair[1]);
            if (plural != null)
                return plural;
        }

        return word;
    }

    public static String singular(String word) {
        if(uncountable.contains(word.toLowerCase())) return word;

        for (String[] irregular : irregulars) {
            if (irregular[1].equalsIgnoreCase(word)) {
                return irregular[0];
            }
        }

        for (String[] pair: singulars) {
            String singular = applyRule(word, pair[0], pair[1]);
            if (singular != null)
                return singular;
        }

        return word;
    }

    public static boolean isPlural(String word) {
        String w = word.toLowerCase();
        return uncountable.contains(w) || plural(w).equals(w);
    }

    public static boolean isSingular(String word) {
        String w = word.toLowerCase();
        return uncountable.contains(w) || !isPlural(w);
    }
}
