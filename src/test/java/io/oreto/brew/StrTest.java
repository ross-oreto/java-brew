package io.oreto.brew;

import io.oreto.brew.num.Num;
import io.oreto.brew.str.Str;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


public class StrTest {
    @Test
    public void isNumber() {
        assertTrue(Str.isNumber("1"));
        assertTrue(Str.isNumber("10"));
        assertTrue(Str.isNumber("33.00"));
        assertTrue(Str.isNumber("-2.0"));
        assertTrue(Str.isNumber(".5"));
        assertTrue(Str.isNumber("-.3140"));
        assertTrue(Str.isNumber("-19.222222"));
        assertTrue(Str.isNumber("+5"));
        assertTrue(Str.isNumber("+.1"));

        assertFalse(Str.isNumber("two"));
        assertFalse(Str.isNumber("thirty-one"));
        assertFalse(Str.isNumber("5.0.0"));
        assertFalse(Str.isNumber("-2.-"));
        assertFalse(Str.isNumber("10."));
        assertFalse(Str.isNumber("-3-2"));
        assertFalse(Str.isNumber("0-"));
        assertFalse(Str.isNumber("-"));
        assertFalse(Str.isNumber(". "));
        assertFalse(Str.isNumber("-."));
        assertFalse(Str.isNumber(""));
        assertFalse(Str.isNumber("    "));
        assertFalse(Str.isNumber(null));
    }

    @Test
    public void isInteger() {
        assertTrue(Str.isInteger("9"));
        assertTrue(Str.isInteger("-9"));
        assertTrue(Str.isInteger("0"));

        assertFalse(Str.isInteger("9.0"));
        assertFalse(Str.isInteger("-0.0"));
    }

    @Test
    public void isNatural() {
        assertTrue(Str.isNumber("1", Num.Type.natural));
        assertTrue(Str.isNumber("00000001", Num.Type.natural));
        assertTrue(Str.isNumber("00023800", Num.Type.natural));
        assertTrue(Str.isNumber("100", Num.Type.natural));

        assertFalse(Str.isNumber("0", Num.Type.natural));
        assertFalse(Str.isNumber("9.0", Num.Type.natural));
        assertFalse(Str.isNumber("-0.0", Num.Type.natural));
        assertFalse(Str.isNumber("0000000000000", Num.Type.natural));
        assertFalse(Str.isNumber("00", Num.Type.natural));
        assertFalse(Str.isNumber("-1", Num.Type.natural));
    }

    @Test
    public void isWhole() {
        assertTrue(Str.isNumber("0", Num.Type.whole));
        assertTrue(Str.isNumber("00", Num.Type.whole));
        assertTrue(Str.isNumber("0000000000000", Num.Type.whole));
        assertTrue(Str.isNumber("1", Num.Type.whole));

        assertFalse(Str.isNumber("-1", Num.Type.whole));
        assertFalse(Str.isNumber("-01", Num.Type.whole));
        assertFalse(Str.isNumber("9.0", Num.Type.whole));
        assertFalse(Str.isNumber("-0.0", Num.Type.whole));
    }

    @Test
    public void isBoolean() {
        assertTrue(Str.isBoolean("true"));
        assertTrue(Str.isBoolean("false"));

        assertFalse(Str.isBoolean("0"));
        assertFalse(Str.isBoolean(""));
        assertFalse(Str.isBoolean("1"));
    }

    @Test
    public void isByte() {
        assertTrue(Str.isByte("1"));
        assertTrue(Str.isByte("+127"));
        assertTrue(Str.isByte("-128"));

        assertFalse(Str.isByte("1.1"));
        assertFalse(Str.isByte("+128"));
        assertFalse(Str.isByte("-129"));
    }

    @Test
    public void to() {
        assertEquals (Optional.of(31), Str.toInteger("31"));
        assertEquals (Optional.of(2147483647), Str.toInteger("2147483647"));
        assertEquals (Optional.of(-2147483648), Str.toInteger("-2147483648"));
        assertEquals(Optional.empty(), Str.toInteger("2147483648"));
        assertEquals (Optional.empty(), Str.toInteger("31.2"));

        assertEquals (Optional.of(31L), Str.toLong("31"));
        assertEquals (Optional.of(9223372036854775807L), Str.toLong("9223372036854775807"));
        assertEquals (Optional.of(-9223372036854775808L), Str.toLong("-9223372036854775808"));
        assertEquals(Optional.empty(), Str.toLong("9223372036854775808"));
        assertEquals (Optional.empty(), Str.toLong("31.2L"));

        assertEquals (Optional.of((short) 31), Str.toShort("+31"));
        assertEquals (Optional.of((short) 32767), Str.toShort("32767"));
        assertEquals (Optional.of((short) -32768), Str.toShort("-32768"));
        assertEquals (Optional.empty(), Str.toShort("-1.1"));
        assertEquals (Optional.empty(), Str.toShort("1.0S"));
        assertEquals(Optional.empty(), Str.toShort("32768"));

        assertEquals (Optional.of(31.0F), Str.toFloat("31"));
        assertEquals (Optional.of(-31.2F), Str.toFloat("-31.2"));
        assertEquals (Optional.of(0F), Str.toFloat("0"));
        assertEquals (Optional.of(31.123457F), Str.toFloat("31.123456789"));
        assertEquals (Optional.empty(), Str.toFloat("1.0F"));

        assertEquals (Optional.of(10.0D), Str.toDouble("10"));
        assertEquals (Optional.of(-1.1D), Str.toDouble("-1.1"));
        assertEquals (Optional.of(100.123456789D), Str.toDouble("100.123456789"));
        assertEquals (Optional.empty(), Str.toDouble("100.123456789D"));

        assertEquals (Optional.of(true), Str.toBoolean("true"));
        assertEquals (Optional.of(false), Str.toBoolean("false"));
        assertEquals (Optional.empty(), Str.toBoolean("yes"));

        assertEquals(Optional.of((byte) 127), Str.toByte("127"));
        assertEquals(Optional.of((byte) -128), Str.toByte("-128"));
        assertEquals(Optional.empty(), Str.toByte("1.0"));
        assertEquals(Optional.empty(), Str.toByte("-129"));
        assertEquals(Optional.empty(), Str.toByte("128"));
    }

    @Test
    public void stringCollections() {
        assertArrayEquals(new char[]{ 'd', 'c', 'z', 'a', 'e', 'b' }, Str.of("d", "c", "z", "a", "e", "b").toArray());
        assertEquals(new ArrayList<Character>(){{ add('d'); add('c'); add('z'); add('a'); add('e'); add('b'); }}
        , Str.of("d", "c", "z", "a", "e", "b").toList());
        assertEquals("abcdez", Str.of("d", "c", "z", "a", "e", "b").sort().toString());
        assertEquals("pan", Str.of("nap").reverse().toString());
        Str str = Str.random(1000);
        assertArrayEquals(str.toString().getBytes(), str.getBytes());
    }

    @Test
    public void stringTypes() {
        assertTrue(Str.of(10).isEmpty());
        assertTrue(Str.empty().isEmpty());
        assertTrue(Str.of(" ").isBlank());
        assertTrue(Str.of("test").eq("test"));
        assertTrue(Str.of("in", "g").preface("test").eq("testing"));
        assertTrue(Str.of("0").isNum(Num.Type.whole));
        assertTrue(Str.of("0.5").isNum());
        assertTrue(Str.of('+').add(10).isInt());
        assertTrue(Str.of(1).preface('-').add(10, 20, 30).isInt());
    }

    @Test
    public void headsOrTails() {
        assertTrue(Str.of("s").head().isPresent());
        assertTrue(Str.of("s").tail().isPresent());
        assertEquals('r', (char) Str.of("Otter").tail().orElse(' '));
        assertEquals('O', (char) Str.of("Otter").head().orElse(' '));
        assertEquals("test string", Str.of("Some test strings").skip(5).take(12).drop(1).toString());
        assertEquals(Paths.get("some", "path").toString(), Str.of("some").sep().add("path").toString());
    }

    @Test
    public void sliceInclusive() {
        // include
        assertEquals("testin", Str.of("testing").slice(0, -2).toString());
        assertEquals("test", Str.of("testing").slice(0, 3).toString());
        assertEquals("sting", Str.of("testing").slice(2).toString());
        assertEquals("ng", Str.of("testing").slice(-2).toString());
        assertEquals("g", Str.of("ng").slice(-1, -1).toString());

        assertEquals("g", Str.of("testing").slice(-1, 1000).toString());
        assertEquals("", Str.of("ng").slice(1, 0).toString());
        assertThrows(StringIndexOutOfBoundsException.class
                , () ->  Str.of("in").slice(-1, 1000, true));
        assertThrows(StringIndexOutOfBoundsException.class
                , () ->  Str.of("in").slice(1, 0, true));
    }

    @Test
    public void sliceExclusive() {
        // exclude
        assertEquals("", Str.of("ng").slice(1, 1, Str.Slice.EXCLUDE).toString());
        assertEquals("b", Str.of("abc").slice(0, 2, Str.Slice.EXCLUDE).toString());
        assertEquals("", Str.of("ng").slice(0, 1, Str.Slice.EXCLUDE).toString());
        assertEquals("", Str.of("ng").slice(0, -1, Str.Slice.EXCLUDE).toString());
        assertEquals("b", Str.of("abc").slice(0, -1, Str.Slice.EXCLUDE).toString());
        assertEquals("bc", Str.of("abcd").slice(0, -1, Str.Slice.EXCLUDE).toString());
        assertEquals("", Str.of("abcd").slice(-1, -1, Str.Slice.EXCLUDE).toString());
        assertEquals("testing", Str.of(":testing:").slice(0, -1, Str.Slice.EXCLUDE).toString());
        assertEquals("", Str.of("ng").slice(1, 0, Str.Slice.EXCLUDE).toString());
        assertThrows(StringIndexOutOfBoundsException.class
                , () ->  Str.of("ng").slice(1, 10, Str.Slice.EXCLUDE,true));
        assertThrows(StringIndexOutOfBoundsException.class
                , () ->  Str.of("ng").slice(1, 0, Str.Slice.EXCLUDE,true));
    }

    @Test
    public void sliceIncludeExclude() {
        // include_exclude
        assertEquals("", Str.of("in").slice(1, 1, Str.Slice.INCLUDE_EXCLUDE).toString());
        assertEquals("i", Str.of("in").slice(0, 1, Str.Slice.INCLUDE_EXCLUDE).toString());
        assertEquals("tes", Str.of("test").slice(0, -1, Str.Slice.INCLUDE_EXCLUDE).toString());
        assertEquals("sting", Str.of("testing").slice(2, 7, Str.Slice.INCLUDE_EXCLUDE).toString());
        assertEquals("", Str.of("t").slice(0, 0, Str.Slice.INCLUDE_EXCLUDE).toString());

        assertEquals("", Str.of("testing").slice(-1000, 0, Str.Slice.INCLUDE_EXCLUDE).toString());
        assertEquals("", Str.of("in").slice(1, 0, Str.Slice.INCLUDE_EXCLUDE).toString());
        assertThrows(StringIndexOutOfBoundsException.class
                , () ->  Str.of("in").slice(1, 0, Str.Slice.INCLUDE_EXCLUDE,true));
    }

    @Test
    public void sliceExcludeInclude() {
        // exclude_include
        assertEquals("n", Str.of("in").slice(1, 1, Str.Slice.EXCLUDE_INCLUDE).toString());
        assertEquals("n", Str.of("in").slice(0, 1, Str.Slice.EXCLUDE_INCLUDE).toString());
        assertEquals("est", Str.of("test").slice(0, -1, Str.Slice.EXCLUDE_INCLUDE).toString());
        assertEquals("t", Str.of("t").slice(0, -1, Str.Slice.EXCLUDE_INCLUDE).toString());
        assertEquals("sting", Str.of("testing").slice(1, -1, Str.Slice.EXCLUDE_INCLUDE).toString());
        assertEquals("esting", Str.of("testing").slice(0, 10, Str.Slice.EXCLUDE_INCLUDE).toString());
        assertEquals("ing", Str.of("testing").slice(-4, -1, Str.Slice.EXCLUDE_INCLUDE).toString());
        assertEquals("", Str.of("testing").slice(7, 8, Str.Slice.EXCLUDE_INCLUDE).toString());
        assertThrows(StringIndexOutOfBoundsException.class
                , () ->  Str.of("testing").slice(7, 8, Str.Slice.INCLUDE_EXCLUDE,true));
    }

    @Test
    public void trimming() {
        assertEquals("test", Str.of("         test    ").trim().toString());
        assertEquals("test", Str.of("         test    ").trim(' ').toString());
        assertEquals("test    ", Str.of("         test    ").ltrim().toString());
        assertEquals("         test", Str.of("         test    ").rtrim().toString());
        assertEquals("999", Str.of("000000000999000000000").trim('0').toString());
        assertEquals("09990", Str.of("000000000999000000000").trim("00").toString());
        assertEquals("999000000000", Str.of("000000000999000000000").ltrim(new String[]{"00", "0"}).toString());
        assertEquals("000000000999", Str.of("000000000999000000000").rtrim(new String[]{"00", "0"}).toString());
    }

    @Test
    public void replace() {
        assertEquals("test ing  abc  123 ", Str.of("test,ing,\nabc\r\n123\t")
                .replace("[,\r\n\t]", " ").toString());
        assertEquals("wha o hat time"
                , Str.of("what to that time").replace("t", "", 3).toString());
        assertEquals("***es***in***", Str.of("testing").replace("[tg]", "***").toString());
        assertEquals("esting", Str.of("testing").replaceFirst("t", "").toString());
        assertEquals("tesing", Str.of("testing").replaceLast("t", "").toString());
        assertEquals("ssss", Str.of("test test test test").replace("[et ]", "").toString());
    }

    @Test
    public void findReplace() {
        assertEquals("", Str.of("test").findAndReplace("test", "").toString());

        assertEquals("The taxi cab is heeereee"
                , Str.of("The taxi cab is here").findAndReplace("e", "eee", -2).toString());

        assertEquals("catsicats", Str.of("kediked").findAndReplace("ked", "cats").toString());

        assertEquals("test ----s--"
                , Str.of("test test").findAndReplace(new String[]{  "e", "t", " " }, "--", -3).toString());

        assertEquals("Moonl drowns o all b the brest stars"
                , Str.of("Moonlight drowns out all but the brightest stars")
                        .findAndReplace(new String[]{  "ut", "ight", "" }, "").toString());

        assertEquals("It's the job that**** never started **** takes longest **** finish."
                , Str.of("It's the job that's never started as takes longest to finish.")
                        .findAndReplace(new String[]{  "as", "to", "'s" }, "****", -3).toString());
    }

    @Test
    public void delete() {
        assertEquals("thisisatest", Str.of(" this is a test  ").removeWhiteSpace().toString());
        assertEquals("thisisatest", Str.removeWhiteSpace(" this\n\n\nis a\t\ttest\t\r\n\r \n \r\n"));
    }

    @Test
    public void rotate() {
        assertEquals("cab", Str.of("abc").rotateRight(1).toString());
        assertEquals("bca", Str.of("abc").rotateRight(2).toString());
        assertEquals("bca", Str.of("abc").rotateLeft(1).toString());
        assertEquals("cab", Str.of("abc").rotateLeft(2).toString());

        assertEquals("stingte", Str.of("testing").rotateRight(5).toString());
        assertEquals("testing", Str.of("testing").rotateRight(7).toString());
        assertEquals("testing", Str.of("testing").rotateRight(28).toString());
    }

    @Test
    public void words() {
        assertEquals("kebabToCamelCase", Str.toCamel("kebab-to-camel_Case"));
        assertEquals("testSomeSTUFF", Str.toCamel("test_some-STUFF"));
        assertEquals("aBCE", Str.toCamel("A-B_C-E"));
        assertEquals("aB", Str.toCamel("a-b"));
        assertEquals("whatIsThisAbout", Str.toCamel("What isThis  -   _about"));

        assertEquals("w-hat-is-this-about", Str.toKebab("WHat_is_this_about"));
        assertEquals("pre-first-name-thing", Str.toKebab("pre_firstNameThing"));
        assertEquals("a-b", Str.toKebab("AB"));
        assertEquals("a-b-c-d-e", Str.toKebab("aBCDE"));

        assertEquals("w_hat_is_this_about", Str.toSnake("WHat_is_this_about"));
        assertEquals("pre_first_name_thing", Str.toSnake("pre_firstNameThing"));
        assertEquals("a_b", Str.toSnake("AB"));
        assertEquals("a_b_c_d_e", Str.toSnake("aBCDE"));

        assertEquals("W Hat Is This About", Str.toTitleCase("WHat_is_this_about"));
        assertEquals("Pre First Name Thing", Str.toTitleCase("pre_firstNameThing"));
        assertEquals("A B", Str.toTitleCase("AB"));
        assertEquals("A B C D E", Str.toTitleCase("aBCDE"));
        assertEquals("W Hat Is This About", Str.toTitleCase("WHat  is    _this_----   about"));
        assertEquals("Title Case In Camel", Str.toTitleCase("titleCaseInCamel"));

        assertEquals("what is this about", Str.toWords("What IsThis  -   _About"));
        assertEquals("usr/home/dev/folder/file", Str.toUri(" usr-home_devFolderFile"));

        assertEquals("_page", Str.toVariableName("*page"));
        assertEquals("__one_", Str.toVariableName("  one "));
        assertEquals("_11test", Str.toVariableName("11test"));
        assertEquals("__", Str.toVariableName("_"));
        assertEquals("$t3__", Str.toVariableName("$t3#!"));
        assertEquals("_final", Str.toVariableName("final"));
    }

    @Test
    public void saltAndHash() {
        String salt = Str.salt(512).orElse(null);
        assert salt != null;

        String hash = Str.hash("testing123", salt).orElse(null);

        assertEquals(Str.hash("testing123", salt).orElse(null), hash);
        assertNotEquals(Str.hash("testing12", salt).orElse(null), hash);
    }
}
