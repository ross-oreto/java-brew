package io.oreto.brew;

import io.oreto.brew.map.MultiInt;
import io.oreto.brew.map.MultiString;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiMapTest {

    @Test
    public void intMapTest() {
        MultiInt<String> multiInt = new MultiInt<>();
        assertEquals(new ArrayList<Object>(){{ add(1); add(2); }}
        , multiInt.put("test", 1).put("test", 2).get("test"));

        multiInt.disallowEmptyValues();
        assertEquals(new ArrayList<Object>(){{ add(1); add(2); }}
                , multiInt.put("test", null).get("test"));

        assertEquals(3, multiInt.sum().get("test").intValue());
    }

    @Test
    public void StringMapTest() {
        MultiString<String> multiString = new MultiString<>();
        assertEquals(new ArrayList<Object>(){{ add("Michael"); add("Ross"); }}
                , multiString.put("test", "Michael").put("test", "Ross").get("test"));

        multiString.disallowEmptyValues();
        assertEquals(new ArrayList<Object>(){{ add("Michael"); add("Ross"); }}
                , multiString.put("test", "").get("test"));

        assertEquals("Michael Ross", multiString.join(" ").get("test"));
    }
}
