package io.oreto.brew.json;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.oreto.brew.io;

import javax.naming.NameNotFoundException;

import static org.junit.Assert.assertEquals;

public class CsvTest {

    @Test
    public void read() throws NameNotFoundException {
        List<Map<String, ?>> stats = JSON.fromCsv(io.loadResourceFile("biostats.csv"));
        assert stats != null;
        assertEquals(18, stats.size());
        assertEquals("38", stats.stream()
                .filter(it -> it.get("Name").equals("Omar"))
                .findFirst().orElseThrow(NameNotFoundException::new).get("Age"));
    }

    @Test
    public void write() {
        List<Map<String, ?>> elements = new ArrayList<>();
        elements.add(new LinkedHashMap<String, Object>(){{ put("name", "ross"); put("address", "Nashville, TN"); }});
        String csv = JSON.toCsv(elements);
        assertEquals("name,address\n" +
                "ross,\"Nashville, TN\"\n", csv);
    }

    @Test
    public void writeObject() {
        List<Person> elements = new ArrayList<>();
        elements.add(new Person("ross", "Nashville, TN"));
        String csv = JSON.asCsv(elements);
        assertEquals("name,address\n" +
                "ross,\"Nashville, TN\"\n", csv);
    }

    static class Person {
        public String name;
        public String address;

        public Person(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }
}
