package io.oreto.brew.serialize.json;

import io.oreto.brew.collections.Lists;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.Pager;
import io.oreto.brew.web.rest.RestResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class jsonTest {

    @Test
    public void select1() {
        RestResponse<Paged<Pojo>> items = RestResponse.ok(Paged.of(Lists.of(
                new Pojo("test1")
                , new Pojo("test2")
        ), Pager.of())).view("body.page[1]").drop("pojos");

        JsonRenderer json = new JsonRenderer();

        String s = json.render(items);
        assertEquals("{\"name\":\"test1\",\"description\":null}", s);

        items.view("body.page");
        assertEquals("[{\"name\":\"test1\",\"description\":null},{\"name\":\"test2\",\"description\":null}]"
                , json.render(items));
    }

    @Test
    public void select2() {
        List<Pojo> pojos = Lists.of(new Pojo("test1", "a")
                , new Pojo("test2", "b")
                , new Pojo("test3", "c"));
        JsonRenderer json = new JsonRenderer();

        assertEquals("{\"name\":\"test1\",\"description\":\"a\"}"
                , json.render(pojos, "[1]", null, "pojos"));
        assertEquals("[{\"name\":\"test1\"},{\"name\":\"test2\"}]"
                , json.render(pojos, "[1:2]", "name", "pojos"));
    }

    @Test
    public void select3() {
        List<Pojo> pojos = Lists.of(new Pojo("test1", "a")
                , new Pojo("test2", "b")
                , new Pojo("test3", "c"));

        JsonRenderer json = new JsonRenderer();
        assertEquals("[{\"description\":\"a\"},{\"description\":\"b\"},{\"description\":\"c\"}]"
                , json.render(pojos, null, "description", "pojos"));
        assertEquals("[{\"description\":\"a\"},{\"description\":\"b\"},{\"description\":\"c\"}]"
                , json.render(pojos, null, "", "name pojos"));
    }

    @Test
    public void select4() {
        RestResponse<Paged<Pojo>> items = RestResponse.ok(Paged.of(Lists.of(
                new Pojo("test1", "a")
                , new Pojo("test2", "b")
        ), Pager.of())).select("body { page { name } }");

        JsonRenderer json = new JsonRenderer();

        assertEquals("{\"body\":{\"page\":[{\"name\":\"test1\"},{\"name\":\"test2\"}]}}"
                , json.render(items));

        assertEquals("{\"body\":{\"page\":[{\"description\":\"a\"}]}}"
                , json.render(items.select("body { page[1] { description } }")));
    }

    @Test
    public void select5() {
        RestResponse<Paged<Pojo>> items = RestResponse.ok(Paged.of(Lists.of(
                new Pojo("test1", "a")
                , new Pojo("test2", "b")
        ), Pager.of())).select("{ body{page pager { page } } status }").drop("body.page.pojos");

        JsonRenderer json = new JsonRenderer();
        assertEquals("{\"status\":200,\"body\":{\"page\":[{\"name\":\"test1\",\"description\":\"a\"}," +
                        "{\"name\":\"test2\",\"description\":\"b\"}],\"pager\":{\"page\":1}}}"
                , json.render(items));

        assertEquals("{\"body\":{\"page\":[{\"name\":\"test1\"}]}}"
                , json.render(items.select("body { page[1]{name} }")));
    }

    @Test
    public void select6() {
        RestResponse<Paged<Pojo>> items = RestResponse.ok(Paged.of(Lists.of(
                new Pojo("test1", "a")
                        .withPojos(new Pojo1("a").withPojos("1", "2")
                                , new Pojo1("b").withPojos("3", "4")
                                , new Pojo1("c").withPojos("5", "6"))
                , new Pojo("test2", "b")
                        .withPojos(new Pojo1("d").withPojos("7", "8"), new Pojo1("e"), new Pojo1("f"))
        ), Pager.of())).select("{body \t{ page {\n\rname\npojos{\r\nname\r} \t} } }");

        JsonRenderer json = new JsonRenderer();
        assertEquals("{\"body\":{\"page\":[{\"name\":\"test1\",\"pojos\":[{\"name\":\"a\"},{\"name\":\"b\"}," +
                        "{\"name\":\"c\"}]},{\"name\":\"test2\",\"pojos\":[{\"name\":\"d\"},{\"name\":\"e\"},{\"name\":\"f\"}]}]}}"
                , json.render(items));

        assertEquals( "{\"body\":{\"page\":[{\"name\":\"test1\",\"pojos\":[{\"name\":\"a\",\"pojos\":" +
                        "[{\"name\":\"1\"},{\"name\":\"2\"}]},{\"name\":\"b\",\"pojos\":" +
                        "[{\"name\":\"3\"},{\"name\":\"4\"}]},{\"name\":\"c\",\"pojos\":" +
                        "[{\"name\":\"5\"},{\"name\":\"6\"}]}]},{\"name\":\"test2\",\"pojos\":" +
                        "[{\"name\":\"d\",\"pojos\":[{\"name\":\"7\"},{\"name\":\"8\"}]},{\"name\":\"e\",\"pojos\":" +
                        "[]},{\"name\":\"f\",\"pojos\":[]}]}]}}"
                , json.render(items.select("{body { page { name pojos{ name pojos {name} }}}}")));
    }

    @Test
    public void select7() {
        RestResponse<Paged<Pojo3>> items = RestResponse.ok(Paged.of(Lists.of(
                new Pojo3("pojo1", new Pojo("test1"))
                , new Pojo3("pojo2", new Pojo("test2").withPojos("a", "b"))
        ), Pager.of())).select("body{ page { name pojo { name pojos {name} } } }");

        JsonRenderer json = new JsonRenderer();

        assertEquals("{\"body\":{\"page\":[{\"name\":\"pojo1\",\"pojo\":{\"name\":\"test1\",\"pojos\":[]}}" +
                        ",{\"name\":\"pojo2\",\"pojo\":{\"name\":\"test2\",\"pojos\":[{\"name\":\"a\"},{\"name\":\"b\"}]}}]}}"
                , json.render(items));
    }

    public static class Pojo {
        private final String name;
        private String description;
        private final List<Pojo1> pojos;

        public Pojo(String name) {
            this.name = name;
            pojos = new ArrayList<>();
        }

        public Pojo(String name, String description) {
            this(name);
            this.description = description;
        }

        public Pojo withPojos(String... names) {
            for (String name : names) {
                pojos.add(new Pojo1(name));
            }
            return this;
        }

        public Pojo withPojos(Pojo1... pojos) {
            Collections.addAll(this.pojos, pojos);
            return this;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class Pojo1 {
        private final String name;
        private String description;
        private final List<Pojo2> pojos;

        public Pojo1(String name) {
            this.name = name;
            pojos = new ArrayList<>();
        }

        public Pojo1(String name, String description) {
            this(name);
            this.description = description;
        }

        public Pojo1 withPojos(String... names) {
            for (String name : names) {
                pojos.add(new Pojo2(name));
            }
            return this;
        }

        public Pojo1 withPojos(Pojo2... pojos) {
            Collections.addAll(this.pojos, pojos);
            return this;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public List<Pojo2> getPojos() {
            return pojos;
        }
    }

    public static class Pojo2 {
        private final String name;

        public Pojo2(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }

    public static class Pojo3 {
        private final String name;
        private final Pojo pojo;

        public Pojo3(String name, Pojo pojo) {
            this.name = name;
            this.pojo = pojo;
        }
        public String getName() {
            return name;
        }
        public Pojo getPojo() {
            return pojo;
        }
    }
}
