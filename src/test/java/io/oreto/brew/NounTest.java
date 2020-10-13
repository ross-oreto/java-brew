package io.oreto.brew;

import io.oreto.brew.str.Noun;
import org.junit.Test;

import static org.junit.Assert.*;

public class NounTest {
    @Test
    public void plural() {
        assertEquals("mens", Noun.plural("men"));
        assertEquals("types", Noun.plural("type"));
        assertEquals("Categories", Noun.plural("Category"));
        assertEquals("elves", Noun.plural("elf"));
        assertEquals("cats", Noun.plural("cat"));
        assertEquals("fish", Noun.plural("fish"));
        assertEquals("wolves", Noun.plural("wolf"));
        assertEquals("cacti", Noun.plural("cactus"));
        assertEquals("appendices", Noun.plural("appendix"));
        assertEquals("oxen", Noun.plural("ox"));
        assertEquals("mice", Noun.plural("mouse"));
        assertEquals("loaves", Noun.plural("loaf"));
        assertEquals("indices", Noun.plural("index"));
        assertEquals("next", Noun.plural("next"));
        assertEquals("Tees", Noun.plural("Tee"));
        assertEquals("saves", Noun.plural("save"));
        assertEquals("conclaves", Noun.plural("conclave"));
        assertEquals("sleaves", Noun.plural("sleave"));
        assertEquals("things", Noun.plural("things"));
        assertEquals("Categories", Noun.plural("Categories"));
        assertEquals("cacti", Noun.plural("cacti"));
        assertEquals("quizzes", Noun.plural("quiz"));
        assertEquals("soliloquies", Noun.plural("soliloquy"));
        assertEquals("yeses", Noun.plural("yes"));
        assertEquals("buses", Noun.plural("bus"));
        assertEquals("Next", Noun.plural("Next"));
        assertEquals("addresses", Noun.plural("address"));
        assertEquals("people", Noun.plural("person"));

        assertTrue(Noun.isPlural("things"));
        assertTrue(Noun.isPlural("cacti"));
        assertTrue(Noun.isPlural("saves"));
        assertTrue(Noun.isPlural("loaves"));
        assertTrue(Noun.isPlural("sleaves"));
        assertTrue(Noun.isPlural("appendices"));
        assertTrue(Noun.isPlural("wolves"));
        assertTrue(Noun.isPlural("conclaves"));
        assertTrue(Noun.isPlural("quizzes"));
        assertTrue(Noun.isPlural("addresses"));
    }

    @Test
    public void singular() {
        assertEquals("man", Noun.singular("men"));
        assertEquals("Category", Noun.singular("Categories"));
        assertEquals("elf", Noun.singular("elves"));
        assertEquals("fish", Noun.singular("fish"));
        assertEquals("wolf", Noun.singular("wolves"));
        assertEquals("cactus", Noun.singular("cacti"));
        assertEquals("appendix", Noun.singular("appendices"));
        assertEquals("ox", Noun.singular("oxen"));
        assertEquals("mouse", Noun.singular("mice"));
        assertEquals("index", Noun.singular("indices"));
        assertEquals("next", Noun.singular("nexts"));
        assertEquals("next", Noun.singular("next"));
        assertEquals("Tee", Noun.singular("Tees"));
        assertEquals("save", Noun.singular("save"));
        assertEquals("save", Noun.singular("saves"));
        assertEquals("conclave", Noun.singular("conclaves"));
        assertEquals("loaf", Noun.singular("loaves"));
        assertEquals("cave", Noun.singular("caves"));
        assertEquals("sleave", Noun.singular("sleaves"));
        assertEquals("quiz", Noun.singular("quizzes"));
        assertEquals("soliloquy", Noun.singular("soliloquies"));
        assertEquals("yes", Noun.singular("yeses"));
        assertEquals("yes", Noun.singular("yes"));
        assertEquals("no", Noun.singular("no"));

        assertTrue(Noun.isSingular("thing"));
        assertTrue(Noun.isSingular("ox"));
        assertTrue(Noun.isSingular("elf"));
        assertTrue(Noun.isSingular("wolf"));
        assertTrue(Noun.isSingular("quiz"));
        assertTrue(Noun.isSingular("yes"));
        assertTrue(Noun.isSingular("address"));

        assertFalse(Noun.isSingular("saves"));
    }
}
