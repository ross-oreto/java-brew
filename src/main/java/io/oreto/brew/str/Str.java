package io.oreto.brew.str;

import io.oreto.brew.Range;
import io.oreto.brew.num.IntRange;
import io.oreto.brew.num.Num;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.oreto.brew.str.Word.reservedWords;

public class Str implements CharSequence, java.io.Serializable, Comparable<CharSequence> {

    public static final String EMPTY = "";
    public static final String SPACE = " ";
    public static final String DASH = "-";
    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String UNDER_SCORE = "_";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String EQUALS = "=";
    protected static final List<CharSequence> emptyList = new ArrayList<>();

    public static class Chars {
        public static final char NEGATIVE = '-';
        public static final char POSITIVE = '+';
        public static final char DECIMAL = '.';
        public static final char SPACE = ' ';
        public static final char DASH = '-';
        public static final char SLASH = '/';
        public static final char UNDER_SCORE = '_';
        public static final char ZERO = '0';
        public static final char EQUALS = '=';
    }

    /**
     * Compares two {@code CharSequence} instances lexicographically. Returns a
     * negative value, zero, or a positive value if the first sequence is lexicographically
     * less than, equal to, or greater than the second, respectively.
     *
     * <p>
     * The lexicographical ordering of {@code CharSequence} is defined as follows.
     * Consider a {@code CharSequence} <i>cs</i> of length <i>len</i> to be a
     * sequence of char values, <i>cs[0]</i> to <i>cs[len-1]</i>. Suppose <i>k</i>
     * is the lowest index at which the corresponding char values from each sequence
     * differ. The lexicographic ordering of the sequences is determined by a numeric
     * comparison of the char values <i>cs1[k]</i> with <i>cs2[k]</i>. If there is
     * no such index <i>k</i>, the shorter sequence is considered lexicographically
     * less than the other. If the sequences have the same length, the sequences are
     * considered lexicographically equal.
     *
     *
     * @param cs1 the first {@code CharSequence}
     * @param cs2 the second {@code CharSequence}
     *
     * @return  the value {@code 0} if the two {@code CharSequence} are equal;
     *          a negative integer if the first {@code CharSequence}
     *          is lexicographically less than the second; or a
     *          positive integer if the first {@code CharSequence} is
     *          lexicographically greater than the second.
     */
    @SuppressWarnings("unchecked")
    public static int compare(CharSequence cs1, CharSequence cs2) {
        if (Objects.requireNonNull(cs1) == Objects.requireNonNull(cs2)) {
            return 0;
        }

        if (cs1.getClass() == cs2.getClass() && cs1 instanceof Comparable) {
            return ((Comparable<Object>) cs1).compareTo(cs2);
        }

        for (int i = 0, len = Math.min(cs1.length(), cs2.length()); i < len; i++) {
            char a = cs1.charAt(i);
            char b = cs2.charAt(i);
            if (a != b) {
                return a - b;
            }
        }

        return cs1.length() - cs2.length();
    }

    /**
     * Determine if a given string is numeric.
     * This is better than relying on something like Integer.parseInt
     * to throw an Exception which has to be part of the normal method behavior.
     * Also better than a regular expression which is more difficult to maintain.
     * @param s A string.
     * @param type the type of number, natural, whole, integer, rational.
     * @return Return true if the value is numeric, false otherwise.
     */
    public static boolean isNumber(CharSequence s, Num.Type type) {
        // do some initial sanity checking to catch easy issues quickly and with very little processing
        // also this makes the state tracking easier below
        if (s == null)
            return false;

        int length = s.length();
        if (isBlank(s)
                || (length == 1 && !Character.isDigit(s.charAt(0)))
                ||  s.charAt(length - 1) == Chars.DECIMAL)
            return false;

        boolean dotted = false;
        int[] arr = s.chars().toArray();

        int len = arr.length;
        for (int i = 0; i < len; i++) {
            char c = (char) arr[i];

            switch (c) {
                // +/- can only be in the start position
                case Chars.NEGATIVE:
                    if (i > 0) return false;
                    if (type == Num.Type.natural || type == Num.Type.whole) return false;
                    break;
                case Chars.POSITIVE:
                    if (i > 0) return false;
                    break;
                case Chars.DECIMAL:
                    // only one decimal place allowed
                    if (dotted || type != Num.Type.rational) return false;
                    dotted = true;
                    break;
                default:
                    // this better be a digit
                    if (!Character.isDigit(c))
                        return false;
                    break;
            }
        }
        // make sure natural number type isn't assigned a 0
        return type != Num.Type.natural || !s.chars().allMatch(it -> (char) it == Chars.ZERO);
    }

    public static boolean isEmpty(final CharSequence s) {
        return s == null || s.length() == 0;
    }
    public static boolean isNotEmpty(final CharSequence s) {
        return !isEmpty(s);
    }

    public static boolean isBlank(CharSequence s) {
       return isEmpty(s) || s.chars().allMatch(Character::isWhitespace);
    }

    public static boolean isNumber(CharSequence s) {
        return isNumber(s, Num.Type.rational);
    }

    public static boolean isInteger(CharSequence s) {
        return isNumber(s, Num.Type.integer);
    }

    public static boolean isByte(CharSequence s) {
        if (isInteger(s)) {
            int i = Integer.parseInt(s.toString());
            return i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE;
        } else {
         return false;
        }
    }

    public static boolean isBoolean(CharSequence s) {
        return s != null && (s.equals(TRUE) || s.equals(FALSE));
    }

    public static boolean isAlphaNumeric(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c);
    }

    // A valid email address consists of an email prefix and an email domain, both in acceptable formats.
    // The prefix appears to the left of the @ symbol.
    // The domain appears to the right of the @ symbol.
    // Allowed characters: letters (a-z), numbers, underscores, periods, and dashes.
    // An underscore, period, or dash must be followed by one or more letter or number.
    public static boolean isEmail(CharSequence s) {
        if (s == null)
            return false;
        String str = s.toString().trim();
        s = str;
        int len = s.length();
        // can't start or end with [_.-@]
        if (len == 0 || !isAlphaNumeric(s.charAt(0)) || !isAlphaNumeric(s.charAt(len - 1)))
            return false;
        // make sure final domain is at least two characters long
        int i = str.lastIndexOf('.');
        if (i < 0)
            return false; // must have one dot at least
        if (len - i <= 2)
            return false;

        boolean at = false;      // encountered @ symbol
        boolean alpha = true;    // [_.-@] expect an alpha numeric char after
        boolean dotted = false;  // dot in domain
        for (i = 0; i < len; i++) {
            char c = s.charAt(i);
            boolean alphaNumeric = isAlphaNumeric(c);
            if (alpha) {
                if (alphaNumeric) {
                    alpha = false;
                } else
                    return false;
            } else if (c == '@') {
                if (at)
                    return false; // can't have two @ symbols
                at = true;
                alpha = true;
            } else {
                if (c == '.') {
                    alpha = true;
                    if (at)
                        dotted = true;
                } else if (c == '_') {
                    if (at) // underscores are not permitted in domain names
                        return false;
                    else
                        alpha = true;
                }
                else if (c == '-') {
                    alpha = true;
                } else if (!alphaNumeric)
                    return false;
            }
        }
        return dotted;
    }

    public static Optional<Boolean> toBoolean(CharSequence s) {
        try {
            return isBoolean(s) ? Optional.of(Boolean.parseBoolean(s.toString())) : Optional.empty();
        } catch (Exception ignored) { }
        return Optional.empty();
    }

    public static Optional<Byte> toByte(CharSequence s) {
        try {
            return isByte(s) ? Optional.of(Byte.parseByte(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    public static Optional<Short> toShort(CharSequence s) {
        try {
            return isInteger(s) ? Optional.of(Short.parseShort(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    public static Optional<Integer> toInteger(CharSequence s) {
        try {
            return isInteger(s) ? Optional.of(Integer.parseInt(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    public static Optional<Long> toLong(CharSequence s) {
        try {
            return isNumber(s) ? Optional.of(Long.parseLong(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    public static Optional<Float> toFloat(CharSequence s) {
        try {
            return isNumber(s) ? Optional.of(Float.parseFloat(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    public static Optional<Double> toDouble(CharSequence s) {
        try {
            return isNumber(s) ? Optional.of(Double.parseDouble(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    public static char[] toArray(CharSequence s) {
        int len = s.length();
        char[] arr = new char[len];
        for (int i = 0; i < len; i++) {
            arr[i] = s.charAt(i);
        }
        return arr;
    }

    public static List<Character> toList(CharSequence s) {
        int len = s.length();
        List<Character> list = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            list.add(s.charAt(i));
        }
        return list;
    }

    public static Optional<Character> head(CharSequence s) {
        return isEmpty(s) ? Optional.empty() : Optional.of(s.charAt(0));
    }

    public static Optional<Character> tail(CharSequence s) {
        return isEmpty(s) ? Optional.empty() : Optional.of(s.charAt(s.length() - 1));
    }

    public static String findAndReplace(CharSequence s, CharSequence search, CharSequence replacement, int max) {
        return Str.of(s).findAndReplace(search, replacement, max).toString();
    }

    public static String findAndReplace(CharSequence s, CharSequence search, CharSequence replacement) {
        return Str.of(s).findAndReplace(search, replacement).toString();
    }

    public static String replace(CharSequence s, CharSequence search, CharSequence replacement, int max) {
        return Str.of(s).replace(search, replacement, max).toString();
    }

    public static String replace(CharSequence s, CharSequence search, CharSequence replacement) {
        return Str.of(s).replace(search, replacement).toString();
    }

    public static String slice(CharSequence s, int from, int to, Slice policy, boolean failOnOutOfBounds) {
        return Str.of(s).slice(from, to, policy, failOnOutOfBounds).toString();
    }

    public static String slice(CharSequence s, int from, int to, Slice policy) {
        return Str.of(s).slice(from, to, policy).toString();
    }

    public static String slice(CharSequence s, int from, int to, boolean failOnOutOfBounds) {
        return Str.of(s).slice(from, to, failOnOutOfBounds).toString();
    }

    public static String slice(CharSequence s, int from, int to) {
        return Str.of(s).slice(from, to).toString();
    }

    public static String slice(CharSequence s, int from, boolean failOnOutOfBounds) {
        return Str.of(s).slice(from, failOnOutOfBounds).toString();
    }

    public static String slice(CharSequence s, int from) {
        return Str.of(s).slice(from).toString();
    }

    public static String ltrim(CharSequence s, CharSequence trim) {
        return Str.of(s).ltrim(trim).toString();
    }

    public static String rtrim(CharSequence s, CharSequence trim) {
        return Str.of(s).rtrim(trim).toString();
    }

    public static String trim(CharSequence s, CharSequence trim) {
        return Str.of(s).trim(trim).toString();
    }

    public static String ltrim(CharSequence s, char trim) {
        return Str.of(s).ltrim(trim).toString();
    }

    public static String rtrim(CharSequence s, char trim) {
        return Str.of(s).rtrim(trim).toString();
    }

    public static String trim(CharSequence s, char trim) {
        return Str.of(s).trim(trim).toString();
    }

    static private Map<Integer, List<CharSequence>> groupBySizes(CharSequence[] search) {
        Map<Integer, List<CharSequence>> strings = new HashMap<>();
        Arrays.stream(search).forEach(it -> {
            if (strings.containsKey(it.length()))
                strings.get(it.length()).add(it);
            else
                strings.put(it.length(), new ArrayList<CharSequence>() {{ add(it); }});
        });
        return strings;
    }

    public static String removeWhiteSpace(CharSequence s) {
        return Str.of(s).removeWhiteSpace().toString();
    }

    // --------------------------------- WORDS ---------------------------------
    static final char[] delimiters = new char[] { Chars.SPACE, Chars.DASH, Chars.DECIMAL, Chars.UNDER_SCORE};
    static boolean isDelimiter(char c) { return Arrays.binarySearch(delimiters, c) > -1; }

    public static String quote(CharSequence s) {
        return Str.of(s).quote().toString();
    }

    public static String singleQuote(CharSequence s) {
        return Str.of(s).singleQuote().toString();
    }

    public static String capitalize(CharSequence s) {
        return Str.of(s).capitalize().toString();
    }

    public static String unCapitalize(CharSequence s) {
        return Str.of(s).unCapitalize().toString();
    }

    public static String toVariableName(CharSequence s) {
        return Str.of(s).toVariableName().toString();
    }

    public static String toCamel(CharSequence s) {
        return Str.of(s).toCamel().toString();
    }

    public static String toPascal(CharSequence s) {
        return Str.of(s).toPascal().toString();
    }

    public static String toKebab(CharSequence s) {
        return Str.of(s).toKebab().toString();
    }

    public static String toSnake(CharSequence s) {
        return Str.of(s).toSnake().toString();
    }

    public static String toTitleCase(CharSequence s) {
        return Str.of(s).toTitleCase().toString();
    }

    public static String toWords(CharSequence s) {
        return Str.of(s).toWords().toString();
    }

    public static String toUri(CharSequence s) {
        return Str.of(s).toUri().toString();
    }

    public static String toUpper(CharSequence s) {
        return Str.of(s).toUpper().toString();
    }

    public static String toLower(CharSequence s) {
        return Str.of(s).toLower().toString();
    }

    public static String sort(CharSequence s, Sort direction) {
        return Str.of(s).sort(direction).toString();
    }

    public static String sort(CharSequence s) {
        return Str.of(s).sort().toString();
    }

    public static String reverse(CharSequence s) {
        return Str.of(s).reverse().toString();
    }

    // --------------------------------- static constructors ---------------------------------

    public static Str of(CharSequence... s) {
        return new Str(s);
    }

    public static Str of(char c) {
        return new Str(c);
    }

    public static Str of(int capacity) {
        return new Str(capacity);
    }

    public static Str empty() {
        return new Str(EMPTY);
    }

    public static Str random(int size) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(size)
                .collect(Str::empty, (a, b) -> a.sb.appendCodePoint(b), Str::add);
    }

    public static Str random() {
        return random(new Random().nextInt(1000));
    }

    public static Optional<String> salt(final int length) {
        if (length < 1) {
            System.err.println("error in salt: length must be > 0");
            return Optional.empty();
        }

        byte[] salt = new byte[length];
        new SecureRandom().nextBytes(salt);

        return Optional.of(Base64.getEncoder().encodeToString(salt));
    }

    public static Optional<String> hash(String password, String salt) {
        char[] chars = password.toCharArray();
        byte[] bytes = salt.getBytes();

        PBEKeySpec spec = new PBEKeySpec(chars, bytes, 10001, 512);
        Arrays.fill(chars, Character.MIN_VALUE);

        try {
            SecretKeyFactory fac = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] securePassword = fac.generateSecret(spec).getEncoded();
            return Optional.of(Base64.getEncoder().encodeToString(securePassword));

        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            System.err.println("Exception encountered in key hashing");
        }
        spec.clearPassword();
        return Optional.empty();
    }

    public enum Slice {
        INCLUDE, INCLUDE_EXCLUDE, EXCLUDE_INCLUDE, EXCLUDE
    }

    public enum Sort {
        ASC, DESC
    }

    // --------------------------------- END STATIC ---------------------------------

    private final StringBuilder sb;

    private Str(CharSequence... charSequences) {
        this.sb = new StringBuilder();
        add(charSequences);
    }

    private Str(char...chars) {
        this.sb = new StringBuilder();
        for(char c : chars)
            sb.append(c);
    }

    private Str(int capacity) {
        this.sb = new StringBuilder(capacity);
    }

    @Override
    public int length() {
        return sb.length();
    }

    @Override
    public char charAt(int index) {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return sb.subSequence(start, end);
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    public String fmt(Object...args) {
        return String.format(toString(), args);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CharSequence && this.compareTo((CharSequence) obj) == 0;
    }

    @Override
    public int compareTo(CharSequence o) {
        return Str.compare(this, o);
    }

    public byte[] getBytes() {
        char[] chars = toArray();
        ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return b;
    }

    public Str andThen(Consumer<Str> consumer) {
        consumer.accept(this);
        return this;
    }

    public Optional<Integer> indexOf(CharSequence s, int from) {
        int i = sb.indexOf(s.toString(), from);
        return i > -1 ? Optional.of(i) : Optional.empty();
    }

    public Optional<Integer> indexOf(CharSequence s) {
        return indexOf(s, 0);
    }

    public Optional<Integer> indexOf(char c, int from) {
        return indexOf(String.valueOf(c), from);
    }

    public Optional<Integer> indexOf(char c) {
        return indexOf(String.valueOf(c), 0);
    }

    public Optional<Integer> lastIndexOf(CharSequence s, int from) {
        int i = sb.lastIndexOf(s.toString(), from);
        return i > -1 ? Optional.of(i) : Optional.empty();
    }

    public Optional<Integer> lastIndexOf(CharSequence s) {
        return lastIndexOf(s, 0);
    }

    public Optional<Integer> lastIndexOf(char c, int from) {
        return lastIndexOf(String.valueOf(c), from);
    }

    public Optional<Integer> lastIndexOf(char c) {
        return lastIndexOf(String.valueOf(c), 0);
    }

    /**
     * An equals method which takes a String/CharSequence to avoid cast warnings.
     * @param s The string to compare.
     * @return True if the strings are equal
     */
    public boolean eq(CharSequence s) {
        return this.equals(s);
    }

    /**
     * Case insensitive equals.
     * @param s The string to compare.
     * @return True if the strings are equal regardless of case.
     */
    public boolean ieq(CharSequence s) {
        return toString().equalsIgnoreCase(s.toString());
    }

    public boolean contains(CharSequence s) {
        return sb.indexOf(s.toString()) > -1;
    }

    public boolean containsAny(CharSequence[] s) {
        for (CharSequence c : s) {
            if (eq(c)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(CharSequence[] s) {
        for (CharSequence c : s) {
            if (!eq(c)) {
                return false;
            }
        }
        return true;
    }

    public boolean startsWith(CharSequence s) {
        return subSequence(0, Math.min(s.length(), length())).equals(s);
    }

    public boolean endsWith(CharSequence s) {
        return subSequence(Math.max(length() - s.length(), 0), length()).equals(s);
    }

    public boolean matches(CharSequence regex) {
        return toString().matches(regex.toString());
    }

    public Str findAndReplace(CharSequence search, CharSequence replacement, int max) {
        if (isNotEmpty(search)) {
            String r = replacement.toString();
            int searchLength = search.length();
            int replacementLength = replacement.length();

            int count = 0;
            if (max < 0) {
                max = Math.abs(max);

                for (int i = length(); count < max && i >= searchLength; i--) {
                    int from = i - searchLength;
                    if (charAt(from) == search.charAt(0) && subSequence(from, i).equals(search)) {
                        sb.replace(from, i, r);
                        count++;
                    }
                }
            } else {
                String s = search.toString();
                int i = sb.indexOf(s);
                while (count < max && i > -1) {
                    sb.replace(i, i + searchLength, r);
                    i = sb.indexOf(s, i + replacementLength);
                    count++;
                }
            }
        }
        return this;
    }

    public Str findAndReplace(CharSequence search, CharSequence replacement) {
        return findAndReplace(search, replacement, length());
    }

    public Str findAndReplaceFirst(CharSequence search, CharSequence replacement) {
        return findAndReplace(search, replacement, 1);
    }

    public Str findAndReplaceLast(CharSequence search, CharSequence replacement) {
        return findAndReplace(search, replacement, -1);
    }

    public Str findAndReplace(CharSequence[] search, CharSequence replacement, int max) {
        if (search.length == 1) { // if only one search term call the appropriate method
            return findAndReplace(search[0], replacement, max);
        }

        Map<Integer, List<CharSequence>> strings = groupBySizes(search);

        List<Integer> sizes = strings.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .filter(it -> it > 0 && it <= length())
                .collect(Collectors.toList());

        if (!sizes.isEmpty()) {
            int minLength = sizes.get(sizes.size() - 1);

            String r = replacement.toString();
            int replacementLength = replacement.length();

            int count = 0;
            // search from the tail
            if (max < 0) {
                max = Math.abs(max);
                for (int i = length(); count < max && i >= minLength; i--) {
                    for (int size : sizes) {
                        int from = i - size;
                        if (from >= 0 && strings.get(size).contains(subSequence(from, i))) {
                            sb.replace(from, i, r);
                            count++;
                        }
                    }
                }
            } else { // search from the head
                for (int i = 0; count < max && i <= length() - minLength; i++) {
                    for (int size : sizes) {
                        int to = i + size;
                        if (to <= length() && strings.get(size).contains(subSequence(i, to))) {
                            sb.replace(i, to, r);
                            i += replacementLength - 1;
                            count++;
                        }
                    }
                }
            }
        }
        return this;
    }

    public Str findAndReplace(CharSequence[] search, CharSequence replacement) {
        return findAndReplace(search, replacement, length());
    }

    public Str findAndReplaceFirst(CharSequence[] search, CharSequence replacement) {
        return findAndReplace(search, replacement, 1);
    }

    public Str findAndReplaceLast(CharSequence[] search, CharSequence replacement) {
        return findAndReplace(search, replacement, -1);
    }

    public Str replace(CharSequence regex, CharSequence replacement, int max) {
        Matcher matcher = Pattern.compile(regex.toString()).matcher(toString());
        boolean found = matcher.find();
        int count = 0;
        if (found && count < max) {
            String r = replacement.toString();
            int replacementLength = replacement.length();
            int offset = 0;
            while(found) {
                int from = matcher.start();
                int to = matcher.end();
                sb.replace(from + offset, to + offset, r);
                offset += replacementLength - (to - from);
                count++;
                found = count < max && matcher.find();
            }
        }
        return this;
    }

    public Str replace(CharSequence regex, CharSequence replacement) {
        return replace(regex, replacement, length());
    }

    public Str replaceFirst(CharSequence regex, CharSequence replacement) {
        return replace(regex, replacement, 1);
    }

    public Str replaceLast(CharSequence regex, CharSequence replacement) {
        Matcher matcher = Pattern.compile(regex.toString()).matcher(this);
        Range<Integer> range = Range.of();
        while(matcher.find()) {
            if (range.isEmpty() || matcher.start() > range.from()){
                range.from(matcher.start());
                range.to(matcher.end());
            }
        }
        if (range.isPresent()) {
            sb.replace(range.from(), range.to(), replacement.toString());
        }
        return this;
    }

    public Str delete(CharSequence search) {
        return findAndReplace(search, EMPTY);
    }

    public Str delete(CharSequence... search) {
        return findAndReplace(search, EMPTY);
    }

    public Str delete(CharSequence[] search, int max) {
        return findAndReplace(search, EMPTY, max);
    }

    public Str deleteFirst(CharSequence search) {
        return findAndReplace(search, EMPTY, 1);
    }

    public Str deleteFirst(CharSequence... search) {
        return findAndReplace(search, EMPTY, 1);
    }

    public Str deleteLast(CharSequence search) {
        return findAndReplace(search, EMPTY, -1);
    }

    public Str deleteLast(CharSequence... search) {
        return findAndReplace(search, EMPTY, -1);
    }

    public Str delete() {
        sb.setLength(0);
        return this;
    }

    public Str removeWhiteSpace() {
        trim();
        int len = length();
        for (int i = 0; i < len; i++) {
            if (Character.isWhitespace(charAt(i))) {
                sb.deleteCharAt(i);
                i--;
                len--;
            }
        }
        return this;
    }

    public Str setTo(CharSequence s) {
        return delete().add(s);
    }

    public boolean isEmpty() {
        return isEmpty(this);
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public boolean isBlank() {
        return isBlank(this);
    }

    public Str add(CharSequence... charSequences) {
        for(CharSequence cs : charSequences)
            sb.append(cs);
        return this;
    }

    public Str preface(CharSequence... charSequences) {
        for(CharSequence cs : charSequences)
            sb.insert(0, cs);
        return this;
    }

    public Str add(char... chars) {
        for(char c : chars)
            sb.append(c);
        return this;
    }

    public Str preface(char... chars) {
        for(char c : chars)
            sb.insert(0, c);
        return this;
    }

    public Str add(Number... numbers) {
        for(Number n : numbers)
            sb.append(n);
        return this;
    }

    public Str preface(Number... numbers) {
        for(Number n : numbers)
            sb.insert(0, n);
        return this;
    }

    public Str quote() {
        return preface('"').add('"');
    }

    public Str singleQuote() {
        return preface("'").add("'");
    }

    public Str repeat(CharSequence s, int len) {
        if (len > 0)
            sb.append(String.join(EMPTY, Collections.nCopies(len, s)));
        else if (len < 0)
            sb.insert(0, String.join(EMPTY, Collections.nCopies(Math.abs(len), s)));

        return this;
    }

    public Str repeat(char c, int len) {
        return repeat(String.valueOf(c), len);
    }

    public Str rotateRight(int i) {
        int length = length();
        i = i > length ? i % length : i;
        return i == length ? this : preface(sb.subSequence(length() - i, length())).drop(i);
    }

    public Str rotateLeft(int i) {
        int length = length();
        i = i > length ? i % length : i;
        return i == length ? this : add(sb.subSequence(0, i)).skip(i);
    }

    public Str space(int spaces) {
        return repeat(Chars.SPACE, spaces);
    }

    public Str space() {
        return space(1);
    }

    public Str tab(int tabs) {
        return repeat(Chars.SPACE, tabs * 4);
    }

    public Str tab() {
        return space(4);
    }

    public Str br(int lines) {
        return repeat(System.lineSeparator(), lines);
    }

    public Str br() {
        return br(1);
    }

    public Str dot(int dots) {
        return repeat(Chars.DECIMAL, dots);
    }

    public Str dot() {
        return dot(1);
    }

    public Str sep(int separators) {
        return repeat(File.separatorChar, separators);
    }

    public Str sep() {
        return sep(1);
    }

    public Str path(CharSequence...s) {
        return add(String.join(File.separator, s));
    }

    public Str skip(int i) {
        sb.delete(0, i);
        return this;
    }

    public Str take(int i) {
        sb.delete(i, length());
        return this;
    }

    public Str drop(int i) {
        sb.delete(length() - i, length());
        return this;
    }

    /**
     * Flexible substring method which supports multiple inclusion policies and negative indexes.
     * INCLUDE from and to are included in the result
     * INCLUDE_EXCLUDE from is included to is excluded
     * EXCLUDE_INCLUDE from is excluded to is included
     * EXCLUDE from and to are excluded
     * NOTE: from greater than to is always undefined and return will result is an empty string or an exception.
     * @param from The start index inclusive or exclusive. If negative the index will count backwards from the tail.
     * @param to The final index inclusive or exclusive. If negative the index will count backwards from the tail.
     * @param policy Determines which indexes should be included or excluded
     * @param failOnOutOfBounds Throws StringIndexOutOfBounds Exception if true and indexes are violated.
     *                          If false out of bounds indexes are moved to the ends of the valid range.
     * @return A self referencing Str to support a fluent api.
     */
    public Str slice(int from, int to, Slice policy, boolean failOnOutOfBounds) {
        int len = length();

        from = from >= 0 ? from : len + from;
        to = to >= 0 ? to : len + to;

        if (failOnOutOfBounds && from > to) {
            throw new StringIndexOutOfBoundsException(String.format("begin %s, end %s, length %s", from, to, len));
        }

        switch (policy) {
            case INCLUDE:
                to++;
                break;
            case INCLUDE_EXCLUDE:
                break;
            case EXCLUDE_INCLUDE:
                if (from != to) from++;
                to++;
                break;
            case EXCLUDE:
                from++;
                break;
        }

        if (failOnOutOfBounds) {
            if (!IntRange.From(0).to(len).allIn(from, to))
                throw new StringIndexOutOfBoundsException(String.format("begin %s, end %s, length %s", from, to, len));
        }

        if (from > to || to <= 0 || from >= len) {
            return delete();
        } else {
            return skip(Math.max(Math.min(from, len), 0))
                    .take(Math.min(to - from, length()));
        }
    }

    public Str slice(int from, int to, boolean failOnOutOfBounds) {
        return slice(from, to, Slice.INCLUDE, failOnOutOfBounds);
    }

    public Str slice(int from, int to, Slice policy) {
        return slice(from, to, policy, false);
    }

    public Str slice(int from, int to) {
        return slice(from, to, Slice.INCLUDE, false);
    }

    public Str slice(int from, boolean failOnOutOfBounds) {
        return slice(from, length(), Slice.INCLUDE_EXCLUDE, failOnOutOfBounds);
    }

    public Str slice(int from) {
        return slice(from, length(), Slice.INCLUDE_EXCLUDE, false);
    }

    /**
     * Trim s [-1 == left, 1 == right, 0 == left and right]
     * @param lr Indicates which direction to trim -1 for left, 0 for left and right, and 1 for right.
     * @param s The string to trim.
     * @return A self referencing Str to support a fluent api.
     */
    protected Str lrtrim(int lr, CharSequence... s) {
        Map<Integer, List<CharSequence>> strings = groupBySizes(s);

        List<Integer> sizes = strings.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .filter(it -> it > 0 && it <= length())
                .collect(Collectors.toList());

        int i = 0;
        boolean trim;

        // left trim or both
        if (lr == 0 || lr == -1) {
            int len = length();
            do {
                trim = false;
                for (int size : sizes) {
                    int to = i + size;
                    if (to < len && strings.getOrDefault(size, emptyList).contains(subSequence(i, to)) ) {
                        i = to;
                        trim = true;
                        break;
                    }
                }
            } while(trim);

            if (i > 0) sb.delete(0, i);
        }

        // right trim or both
        if (lr == 0 || lr == 1) {
            int len = length();
            i = len;
            do {
                trim = false;
                for (int size : sizes) {
                    int from = i - size;
                    if (from >= 0 && strings.getOrDefault(size, emptyList).contains(subSequence(from, i))) {
                        i = from;
                        trim = true;
                        break;
                    }
                }
            } while(trim);

            if (i < len) sb.delete(i, len);
        }
        return this;
    }

    public Str ltrim(CharSequence[] s) {
        if (s.length == 0) {
            ltrim();
        } else {
            lrtrim(-1, s);
        }
        return this;
    }

    public Str rtrim(CharSequence[] s) {
        if (s.length == 0) {
            rtrim();
        } else {
            lrtrim(1, s);
        }
        return this;
    }

    public Str trim(CharSequence[] s) {
        return s.length == 0 ? ltrim().rtrim() : lrtrim(0, s);
    }

    public Str ltrim(CharSequence s) {
        return lrtrim(-1, s);
    }

    public Str rtrim(CharSequence s) {
        return lrtrim(1, s);
    }

    public Str trim(CharSequence s) {
        return lrtrim(0, s);
    }

    public Str ltrim() {
        int length = length();
        if (length > 0 && Character.isWhitespace(charAt(0))) {
            int i = 1;
            for (; i < length; i++) {
                if (!Character.isWhitespace(charAt(i))) {
                    break;
                }
            }
            sb.delete(0, i);
        }
        return this;
    }

    public Str rtrim() {
        int length = length();
        int last = length - 1;
        if (length > 0 && Character.isWhitespace(charAt(last))) {
            int i = last - 1;
            for (; i >= 0; i--) {
                if (!Character.isWhitespace(charAt(i))) {
                    break;
                }
            }
            sb.delete(i + 1, length);
        }
        return this;
    }

    public Str trim() {
        return ltrim().rtrim();
    }

    public Str ltrim(char c) {
        int length = length();
        if (length > 0 && charAt(0) == c) {
            int i = 1;
            for (; i < length; i++) {
                if (charAt(i) != c) {
                    break;
                }
            }
            sb.delete(0, i);
        }
        return this;
    }

    public Str rtrim(char c) {
        int length = length();
        int last = length - 1;
        if (length > 0 && charAt(last) == c) {
            int i = last - 1;
            for (; i >= 0; i--) {
                if (charAt(i) != c) {
                    break;
                }
            }
            sb.delete(i + 1, length);
        }
        return this;
    }

    public Str trim(char c) {
        return ltrim(c).rtrim(c);
    }

    public Optional<Character> head() {
        return head(this);
    }

    public Optional<Character> tail() {
        return tail(this);
    }

    public Str reverse() {
        sb.reverse();
        return this;
    }

    public Str sort(Sort direction) {
        CharSequence sorted =
                chars()
                        .mapToObj(i -> (char) i)
                        .map(String::valueOf)
                        .sorted(direction == Sort.ASC ? Comparator.naturalOrder() : Comparator.reverseOrder())
                        .collect(Collectors.joining(EMPTY));
        return delete().add(sorted);
    }

    public Str sort() {
        return sort(Sort.ASC);
    }

    public Str capitalize() {
        if (isNotEmpty()) {
            char c = charAt(0);
            int i = 1;
            int len = length();
            while(!Character.isAlphabetic(c) && i < len) {
                c = charAt(i);
                i++;
            }

            if (Character.isLowerCase(c)) {
                sb.setCharAt(i - 1, Character.toUpperCase(c));
            }
        }
        return this;
    }

    public Str unCapitalize() {
        if (isNotEmpty()) {
            char c = charAt(0);
            int i = 1;
            int len = length();
            while(!Character.isAlphabetic(c) && i < len) {
                c = charAt(i);
                i++;
            }

            if (Character.isUpperCase(c)) {
                sb.setCharAt(i - 1, Character.toLowerCase(c));
            }
        }
        return this;
    }

    public Str toVariableName() {
        Optional<Character> c = head();
        if (c.isPresent()) {
            if (Character.isDigit(c.get()))
                preface(Chars.UNDER_SCORE);

            replace("[^0-9_$\\w]", UNDER_SCORE);
            for(String word : reservedWords) {
                if (eq(word)) {
                    preface(Chars.UNDER_SCORE);
                    break;
                }
            }
        }
        return this;
    }

    protected Str toCamel(boolean pascal) {
        boolean dotted = false;

        for (int i = 0; i < length(); i++) {
            char c = charAt(i);
            if (isDelimiter(c)) {
                sb.deleteCharAt(i);
                // update the loop b/c we are looping and modifying
                // generally don't do this, however this is really efficient string manipulation
                i--;
                dotted = true;
            } else if(dotted) {
                sb.setCharAt(i, Character.toUpperCase(c));
                dotted = false;
            }
        }
        return pascal ? capitalize() : unCapitalize();
    }

    public Str toCamel() {
        return toCamel(false);
    }

    public Str toPascal() {
        return toCamel(true);
    }

    protected Str toDelimitedName(char delimiter, boolean lowerCase) {
        boolean dotted = false;

        for (int i = 0; i < length(); i++) {
            char c = charAt(i);
            if (isDelimiter(c)) {
                if (dotted || i == 0 || i >= length() - 1) {
                    sb.deleteCharAt(i);
                    // update the loop b/c we are looping and modifying
                    // generally don't do this, however this is really efficient string manipulation
                    i--;
                } else {
                    sb.setCharAt(i, delimiter);
                }
                dotted = true;
            } else {
                if (Character.isUpperCase(c)) {
                    if (lowerCase) {
                        sb.setCharAt(i, Character.toLowerCase(c));
                    }
                    if (!dotted && i > 0 && i <= length() - 1) {
                        sb.insert(i, delimiter);
                        i++;
                    }
                } else if (!lowerCase && dotted) {
                    sb.setCharAt(i, Character.toUpperCase(c));
                }
                dotted = false;
            }
        }
        return rtrim(delimiter);
    }

    public Str toKebab() {
        return toDelimitedName(Chars.DASH, true);
    }

    public Str toSnake() {
        return toDelimitedName(Chars.UNDER_SCORE, true);
    }

    public Str toTitleCase() {
        return toDelimitedName(Chars.SPACE, false).capitalize();
    }

    public Str toWords() {
        return toDelimitedName(Chars.SPACE, true);
    }

    public Str toUri() {
        return toDelimitedName(Chars.SLASH, true);
    }

    public Str toUpper() {
        for (int i = 0; i < length(); i++) {
            char c = charAt(i);
            if (Character.isLowerCase(c))
                sb.setCharAt(i, Character.toUpperCase(c));
        }
        return this;
    }

    public Str toLower() {
        for (int i = 0; i < length(); i++) {
            char c = charAt(i);
            if (Character.isUpperCase(c))
                sb.setCharAt(i, Character.toLowerCase(c));
        }
        return this;
    }

    public boolean isNum(Num.Type type) {
        return isNumber(this, type);
    }

    public boolean isNum() {
        return isNumber(this);
    }

    public boolean isInt() {
        return isInteger(this);
    }

    public boolean isBoolean() {
        return isBoolean(this);
    }

    public boolean isByte() {
        return isByte(this);
    }

    public Optional<Boolean> toBoolean() {
        return toBoolean(this);
    }

    public Optional<Byte> toByte() {
        return toByte(this);
    }

    public Optional<Short> toShort() {
        return toShort(this);
    }

    public Optional<Integer> toInteger() {
        return toInteger(this);
    }

    public Optional<Long> toLong() {
        return toLong(this);
    }

    public Optional<Float> toFloat() {
        return toFloat(this);
    }

    public Optional<Double> toDouble() {
        return toDouble(this);
    }

    public char[] toArray() {
        return toArray(this);
    }

    public List<Character> toList() {
        return toList(this);
    }
}
