package io.oreto.brew;

import io.oreto.brew.collections.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ListTest {
    @Test
    public void order() {
        List<Entity> entities = Lists.of(
                new Entity(1L, 2, "ross", "oreto")
                , new Entity(10L, 0, "ross", "test")
                , new Entity(3L, 0, "ross", "michael")
                , new Entity(6L, 1, "alpha", "beta")
                , new Entity(12L, 2, "foo", "bar")
                , new Entity(11L, 1, "foo", "fighters")
        );

        assertEquals(6L, (long) Lists.orderBy(entities, Lists.of("name,asc")).get(0).getId());
        assertEquals(10L, (long) Lists.orderBy(entities, Lists.of("name,desc", "f1,desc")).get(0).getId());
        assertEquals(3L, (long) Lists.orderBy(entities, Lists.of("name,desc", "f1")).get(0).getId());

        Lists.orderBy(entities, Lists.of("name", "i,desc", "f1"));
        List<Entity> ordered = Lists.orderBy(entities, Lists.of("name", "i,desc", "f1"));
        assertArrayEquals(new Long[]{ 6L, 12L, 11L, 1L, 3L, 10L }, ordered.stream().map(Entity::getId).toArray(Long[]::new));
    }

    public static class Entity {
        private Long id;
        private Integer i;
        private String name;
        private String f1;

        public Entity(Long id, Integer i, String name, String f1) {
            this.id = id;
            this.i = i;
            this.name = name;
            this.f1 = f1;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getI() {
            return i;
        }

        public void setI(Integer i) {
            this.i = i;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getF1() {
            return f1;
        }

        public void setF1(String f1) {
            this.f1 = f1;
        }
    }
}
