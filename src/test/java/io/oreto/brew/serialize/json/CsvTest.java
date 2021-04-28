package io.oreto.brew.serialize.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.oreto.brew.collections.Lists;
import io.oreto.brew.io;
import io.oreto.brew.serialize.Csv;
import org.junit.jupiter.api.Test;

import javax.naming.NameNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CsvTest {

    @Test
    public void read() throws NameNotFoundException {
        List<Map<String, ?>> stats = Csv.from(io.resourceText("biostats.csv").orElse(""));
        assert stats != null;
        assertEquals(18, stats.size());
        assertEquals("38", stats.stream()
                .filter(it -> it.get("Name").equals("Omar"))
                .findFirst().orElseThrow(NameNotFoundException::new).get("Age"));
    }

    @Test
    public void write() throws IOException {
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(new LinkedHashMap<String, Object>(){{ put("name", "ross"); put("address", "Nashville, TN"); }});
        String csv = Csv.asCsv(elements);
        assertEquals("name,address\n" +
                "ross,\"Nashville, TN\"\n", csv);
    }

    @Test
    public void writeNoHeader() throws IOException {
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(new LinkedHashMap<String, Object>(){{ put("name", "ross"); put("address", "Nashville, TN"); }});
        String csv = Csv.asCsv(elements, Csv.Options.noHeader());
        assertEquals("ross,\"Nashville, TN\"\n", csv);
    }

    @Test
    public void writeObject() throws IOException {
        List<Person> elements = new ArrayList<>();
        elements.add(new Person("ross", "Nashville, TN"));
        String csv = Csv.toCsv(elements);
        assertEquals("name,address\n" +
                "ross,\"Nashville, TN\"\n", csv);
    }

    @Test
    public void writeEmpty() throws IOException {
        assertEquals("", Csv.toCsv(new ArrayList<>()));
    }

    @Test
    public void writeList() throws IOException {
        assertEquals("a,b,c\n", Csv.toCsv(Lists.of(Lists.of("a", "b", "c"))));
    }

    @Test
    public void writer1() throws IOException {
        List<Person> elements = new ArrayList<>();
        elements.add(new Person("ross", "Nashville, TN"));
        String csv = Csv.of(elements, Csv.Options.header().drop("name")).writeString();
        assertEquals("address\n" +
                "\"Nashville, TN\"\n", csv);
    }

    @Test
    public void writer2() throws IOException {
        List<Person> elements = new ArrayList<>();
        elements.add(new Person("ross", "Nashville, TN"));
        String csv = Csv.of(elements, Csv.Options.header().order("address")).writeString();
        assertEquals("address,name\n" +
                "\"Nashville, TN\",ross\n", csv);
    }

    @Test
    public void writer3() throws IOException {
        List<Person2> elements = new ArrayList<>();
        LocalDateTime dateTime = LocalDateTime.of(2020, 12, 5, 13, 46, 31);
        elements.add(new Person2("ross", "Nashville, TN")
                .date(dateTime));
        String csv = Csv.of(elements, Csv.Options.header().asc()).writeString();
        assertEquals("address,dateTime,name\n" +
                "\"Nashville, TN\",20201205134631,ross\n", csv);
    }

    @Test
    public void writer4() throws IOException {
        List<Person2> elements = new ArrayList<>();
        elements.add(new Person2("ross", "Nashville, TN")
                .withChild(new Person("Brandon", "Dickerson")));
        String csv = Csv.of(elements, Csv.Options.header().drop("child.address")).writeString();
        assertEquals("name,address,child.name\n" +
                "ross,\"Nashville, TN\",Brandon\n", csv);

        csv = Csv.of(elements, Csv.Options.header().drop("child")).writeString();
        assertEquals("name,address\n" +
                "ross,\"Nashville, TN\"\n", csv);
    }

    @Test
    public void writer5() throws IOException {
        List<Person2> elements = new ArrayList<>();
        elements.add(new Person2("ross", "Nashville, TN")
                .withChild(new Person("Brandon", "Dickerson")));

        String csv = Csv.of(elements, Csv.Options.header().select("child")).writeString();
        assertEquals("name,address\n" +
                "Brandon,Dickerson\n", csv);
    }

    @Test
    public void writer6() throws IOException {
        List<Person2> elements = new ArrayList<>();
        elements.add(new Person2("ross", "Nashville, TN")
                .withChild(new Person("Brandon", "Dickerson")));
        String csv = Csv.of(elements, Csv.Options.noHeader()).writeString();
        assertEquals("ross,\"Nashville, TN\",Brandon,Dickerson\n", csv);
    }

    static class Person {
        public String name;
        public String address;

        public Person(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }

    static class Person2 {
        public String name;
        public String address;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Person child;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMddHHmmss")
        public LocalDateTime dateTime;

        public Person2(String name, String address) {
            this.name = name;
            this.address = address;
        }

        public Person2 withChild(Person child) {
            this.child = child;
            return this;
        }

        public Person2 date(LocalDateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public Person2 date() {
            return date(LocalDateTime.now());
        }
    }
}
