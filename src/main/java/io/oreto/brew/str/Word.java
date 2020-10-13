package io.oreto.brew.str;

import io.oreto.brew.collections.Lists;
import io.oreto.brew.map.Maps;

import java.util.*;
import java.util.stream.Collectors;
import static io.oreto.brew.map.Maps.E;

public class Word {

    public enum ZeroTo9 {
        zero(0)
        , one(1)
        , two(2)
        , three(3)
        , four(4)
        , five(5)
        , six(6)
        , seven(7)
        , eight(8)
        , nine(9);

        public static ZeroTo9 of(String s) {
            for (ZeroTo9 value : values()) {
                if (value.numberString.equals(s)) return value;
            }
            return null;
        }

        public static ZeroTo9 of(int i) {
            for (ZeroTo9 value : values()) {
                if (value.num == i) return value;
            }
            return null;
        }

        private final int num;
        private final String numberString;

        ZeroTo9(int num) {
            this.num = num;
            this.numberString = String.valueOf(num);
        }
        public int getNum() { return num; }
        @Override public String toString() { return numberString; }
    }

    public enum TenTo19 {
        ten(10)
        , eleven(11)
        , twelve(12)
        , thirteen(13)
        , fourteen(14)
        , fifteen(15)
        , sixteen(16)
        , seventeen(17)
        , eighteen(18)
        , nineteen(19);

        public static TenTo19 of(String s) {
            for (TenTo19 value : values()) {
                if (value.numberString.equals(s)) return value;
            }
            return null;
        }

        public static TenTo19 of(int i) {
            for (TenTo19 value : values()) {
                if (value.num == i) return value;
            }
            return null;
        }

        private final int num;
        private final String numberString;

        TenTo19(int num) {
            this.num = num;
            this.numberString = String.valueOf(num);
        }
        public int getNum() { return num; }

        @Override public String toString() { return numberString; }
    }

    public enum TwentyTo90 {
        twenty(20)
        , thirty(30)
        , forty(40)
        , fifty(50)
        , sixty(60)
        , seventy(70)
        , eighty(80)
        , ninety(90);

        public static TwentyTo90 of(String s) {
            for (TwentyTo90 value : values()) {
                if (value.numberString.equals(s)) return value;
            }
            return null;
        }

        public static TwentyTo90 of(int i) {
            for (TwentyTo90 value : values()) {
                if (value.num == i) return value;
            }
            return null;
        }

        private final int num;
        private final String numberString;

        TwentyTo90(int num) {
            this.num = num;
            this.numberString = String.valueOf(num);
        }
        public int getNum() { return num; }
        @Override public String toString() { return numberString; }
    }

    public static String[] reservedWords = {
            "abstract",	"assert", "boolean", "break", "byte", "case", "catch"
            , "char", "class", "const", "continue", "default"
            ,"double", "do", "else", "enum", "extends", "false", "final"
            , "finally", "float", "for", "goto", "if"
            ,"implements", "import", "instanceof", "int", "interface", "long"
            ,"native", "new", "null", "package", "private", "protected"
            , "public", "return", "short", "static", "strictfp", "super"
            ,"switch", "synchronized", "this", "throw", "throws", "transient"
            ,"true", "try", "void", "volatile",	"while", "_"
    };

    public static final Map<Integer, String> zeroTo99 = new HashMap<Integer, String>() {{
        put(ZeroTo9.zero.num, ZeroTo9.zero.name());
        put(ZeroTo9.one.num, ZeroTo9.one.name());
        put(ZeroTo9.two.num, ZeroTo9.two.name());
        put(ZeroTo9.three.num, ZeroTo9.three.name());
        put(ZeroTo9.four.num, ZeroTo9.four.name());
        put(ZeroTo9.five.num, ZeroTo9.five.name());
        put(ZeroTo9.six.num, ZeroTo9.six.name());
        put(ZeroTo9.seven.num, ZeroTo9.seven.name());
        put(ZeroTo9.eight.num, ZeroTo9.eight.name());
        put(ZeroTo9.nine.num, ZeroTo9.nine.name());
        put(TenTo19.ten.num, TenTo19.ten.name());
        put(TenTo19.eleven.num, TenTo19.eleven.name());
        put(TenTo19.twelve.num, TenTo19.twelve.name());
        put(TenTo19.thirteen.num, TenTo19.thirteen.name());
        put(TenTo19.fourteen.num, TenTo19.fourteen.name());
        put(TenTo19.fifteen.num, TenTo19.fifteen.name());
        put(TenTo19.sixteen.num, TenTo19.sixteen.name());
        put(TenTo19.seventeen.num, TenTo19.seventeen.name());
        put(TenTo19.eighteen.num, TenTo19.eighteen.name());
        put(TenTo19.nineteen.num, TenTo19.nineteen.name());
        put(TwentyTo90.twenty.num, TwentyTo90.twenty.name());
        put(TwentyTo90.thirty.num, TwentyTo90.thirty.name());
        put(TwentyTo90.forty.num, TwentyTo90.forty.name());
        put(TwentyTo90.fifty.num, TwentyTo90.fifty.name());
        put(TwentyTo90.sixty.num, TwentyTo90.sixty.name());
        put(TwentyTo90.seventy.num, TwentyTo90.seventy.name());
        put(TwentyTo90.eighty.num, TwentyTo90.eighty.name());
        put(TwentyTo90.ninety.num, TwentyTo90.ninety.name());
    }};

    static private final Map<Integer, String> magnitudeToWord = Maps.of(
            E.of(0, "hundred")
            , E.of(1, "thousand")
            , E.of(2, "million")
            , E.of(3, "billion")
            , E.of(4, "trillion")
    );

    public static String groupToWord(ZeroTo9 h, ZeroTo9 t, ZeroTo9 o) {
        if ( (h == null || h.num == 0) && (t == null || t.num == 0) && (o == null || o.num == 0) )
            return zeroTo99.get(0);

        Str str = Str.of();
        if (h != null && h.num > 0) {
            str.add(zeroTo99.get(h.num)).space().add(magnitudeToWord.get(0)).space();
        }
        if (t != null && t.num == 1) {
            str.add(zeroTo99.get(10 + o.num));
        } else {
            if (t != null && t.num > 1) {
                str.add(zeroTo99.get(t.num * 10)).space();
            }
            if (o.num > 0) {
                str.add(zeroTo99.get(o.num));
            }
        }
        return str.trim().toString();
    }

    public static Optional<String> fromNumber(CharSequence s) {
        if (s.length() == 0) return Optional.empty();

        boolean isNegative;
        Str number = Str.of(s).trim();
        Optional<Long> optionalInteger = number.toLong();
        if (optionalInteger.isPresent()) {
            if (optionalInteger.get() == 0) return Optional.of(ZeroTo9.zero.name());
            isNegative = optionalInteger.get() < 0;
        } else {
            return Optional.empty();
        }
        number.ltrim(new String[]{"-", "+"}).ltrim(ZeroTo9.zero.name());
        int size = number.length();
        List<CharSequence> sequences = new ArrayList<>();
        int batches = size / 3;
        for (int i = 0; i < batches; i++) {
            sequences.add(number.subSequence(number.length() - 3, number.length()));
            number.slice(0, -3, Str.Slice.INCLUDE_EXCLUDE);
        }
        int rem = size % 3;
        if (rem > 0) {
            sequences.add(number.subSequence(0, rem));
        }
        Collections.reverse(sequences);
        List<String> groups = sequences.stream().map(it -> {
            int len = it.length();
            return groupToWord(
                    len > 2 ? ZeroTo9.of(it.subSequence(len - 3, len - 2).toString()) : ZeroTo9.zero
                    , len > 1 ? ZeroTo9.of(it.subSequence(len - 2, len - 1).toString()) : ZeroTo9.zero
                    , ZeroTo9.of(it.subSequence(len - 1, len).toString())
            );
        }).filter(Objects::nonNull).collect(Collectors.toList());

        int len = groups.size();
        return Optional.of( (isNegative ? "negative " : Str.EMPTY) +
                Lists.withIndex(groups).stream()
                .map(it -> {
                    int i = (len - it.getIndex() - 1);
                    return i == 0 ? it.getValue() : String.format("%s %s"
                        , it.getValue()
                        , magnitudeToWord.get(i));
                }).filter(it -> !it.startsWith(ZeroTo9.zero.name())).collect(Collectors.joining(Str.SPACE))
        );
    }

    public static final Set<String> prepositions = Lists.set(
           "about", "beside", "near", "to", "above", "between", "of", "towards", "across", "beyond",
           "off", "under", "after", "by", "on", "underneath", "against", "despite", "onto", "unlike", "along",
           "down", "opposite", "until", "among", "during", "out", "up", "around", "except", "outside",
           "upon", "as", "for", "over", "via", "at", "from", "past", "with", "before",
           "in", "round", "within", "behind", "inside", "since", "without", "below", "into", "than",
           "beneath", "like", "through"
    );

    public static final Set<String> definiteArticles = Lists.set(
            "a", "the", "an"
    );
}
