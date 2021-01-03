package io.oreto.brew.data.jpa;

import io.oreto.brew.collections.Lists;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.jpa.repo.*;
import io.oreto.brew.web.rest.RestResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestPropertySource(locations = "classpath:application.properties")
@ContextConfiguration
public class JpaTest {
    private static EntityManager em;

    @Resource private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    public void setup() {
        em = entityManagerFactory.createEntityManager();
        DataStore.save(em,
                new Entity1("test")
                        .withCreatedBy("me")
                        .withStrings("test", "ing", "io", "oreto", "brew", "key")
                        .withEntries(new HashMap<String, String>() {{
                            put("key", "value");
                        }})
                        .withEntity2(new Entity2("oneE2").withEntity3(new Entity3("e2e3a")))
                        .withEntity2s(Lists.of(
                                new Entity2("e1")
                                        .withEntity3(new Entity3("oneE3"))
                                        .withEntity3s(Lists.of(new Entity3("e3"), new Entity3("e03")))
                                , new Entity2("e4")
                                        .withEntity3s(Lists.of(new Entity3("e5")))
                                , new Entity2("e2"))
                        )
        );

        DataStore.save(em,
                new Entity1("test2").withCreatedBy("Tyson")
                        .withEntity2s(Lists.of(new Entity2("ee")))
        );

        DataStore.save(em, new Entity4(new Entity4.CompId(1, "abc"), "t1")
                .withDateTime(LocalDateTime.of(2020, 12, 14, 1, 21, 33)));
        DataStore.save(em, new Entity4(new Entity4.CompId(2, "d"), "t1"));
    }

    @AfterEach
    public void reset() {
        DataStore.deleteAll(em, Entity1.class);
        DataStore.deleteAll(em, Entity2.class);
        DataStore.deleteAll(em, Entity3.class);
        DataStore.deleteAll(em, Entity4.class);
        DataStore.deleteAll(em, Entity5.class);
        em.close();
    }

    @Test
    public void query0() {
        Paged<Entity1> query =
            DataStore.Q.of(Entity1.class).eq("name", "test").find(em);
        List<Entity1> result = query.getPage();
        assertEquals(3, result.get(0).getEntity2s().size());
    }

    @Test
    public void query1() {
        Paged<Entity1> query = DataStore.Q.of(Entity1.class)
                .eq(DataStore.Q.Func.of(Function.count, "entity2s"), 1)
                .or()
                .eq("entity2s.entity3s.name", "e5")
                .find(em);
        List<Entity1> result = query.getPage();
        assertEquals(2, result.size());
    }

    @Test
    public void query2() {
        Paged<Entity2> query = DataStore.Q.of(Entity2.class)
                .eq("entity3s.name", "e03")
                .or()
                .eq(DataStore.Q.Func.of(Function.count, "entity3s"), 0)
                .find(em);

        List<Entity2> result = query.getPage();
        assertEquals(4, result.size());
    }

    @Test
    public void query3() {
        Paged<Entity2> query = DataStore.Q.of(Entity2.class)
                .eq(DataStore.Q.Func.of(Function.count, "entity3s.id"), 2)
                .or()
                .eq(DataStore.Q.Func.of(Function.count, "entity3s"), 1)
                .eq(DataStore.Q.Func.of(Function.count, "entity3s"), 0)
                .find(em);
        List<Entity2> result = query.getPage();
        assertEquals(5, result.size());
    }

    @Test
    public void query4() {
        Paged<Entity2> query = DataStore.Q.of(Entity2.class)
                .endsWith("entity3s.name", "03")
                .find(em);
        List<Entity2> result = query.getPage();
        assertEquals(1, result.size());
    }

    @Test
    public void query5() {
        Paged<Entity1> query = DataStore.Q.of(Entity1.class)
                .eq("entries", "key")
                .find(em, "entries");
        List<Entity1> result = query.getPage();
        assertEquals("value", result.get(0).getEntries().get("key"));
    }

    @Test
    public void query6() {
        Paged<Entity1> query = DataStore.Q.of(Entity1.class)
                .eq("strings", "brew")
                .find(em, "strings", "entries");
        List<Entity1> result = query.getPage();
        assertEquals("value", result.get(0).getEntries().get("key"));
    }

    @Test
    public void query7() {
        Paged<Entity3> query = DataStore.Q.of(Entity3.class)
                .isNotNull("name")
                .find(em);
        List<Entity3> result = query.getPage();
        assertEquals(5, result.size());
    }

    @Test
    public void crud() {
        DataStore.save(em,
            new Entity1("test3").withCreatedBy("Link").withEntity2s(Lists.of(new Entity2("e20")))
        );

        Entity1 u = DataStore.findOne(em, Entity1.class, "").orElse(null);
        assert u != null;
        Long id = u.getId();
        u.setName("update");
        DataStore.update(em, u);
        assertEquals("update", DataStore.get(em, Entity1.class, id).get().getName());

        Paged<Entity1> paged = DataStore.findAll(em, Entity1.class, "name:test3");
        assertEquals(1, paged.getPager().getPage());
        DataStore.delete(em, Entity1.class, paged.getPage().get(0).getId());
        assertTrue(DataStore.findAll(em, Entity1.class,"name:test3").getPage().isEmpty());
    }

    @Test
    public void list1() {
        Paged<Entity1> query = DataStore.list(em, Entity1.class, "entity2.entity3s", "strings", "entries");
        List<Entity1> result = query.getPage();
        assertEquals("oneE2", result.get(0).getEntity2().getName());
    }

    @Test
    public void restTest1() {
        Entity1Store entity1Store = new Entity1Store(em);
        RestResponse<Entity1> result;

        result = entity1Store.replace(1L, new Entity1("update").withCreatedBy("me"));
        assertEquals(true, result.isOk());
        assertEquals("update", result.getBody().getName());
        assertEquals("me", result.getBody().getCreatedBy());

        result = entity1Store.update(result.getBody().withName("test"));
        assertEquals(true, result.isOk());
        assertEquals("test", result.getBody().getName());
        assertEquals("me", result.getBody().getCreatedBy());

        assertEquals( new Long(2), DataStore.count(em, Entity1.class));
    }

    @Test
    public void restTest2() {
        Entity5Store entity5Store = new Entity5Store(em);

        RestResponse<Entity5> restResponse = entity5Store.replace(1L, new Entity5());
        assertEquals(false, restResponse.isOk());
        restResponse = entity5Store.replace(2L, new Entity5("test"));
        assertEquals(true, restResponse.isOk());
    }

    @Test
    public void idTest() {
        Optional<Entity4> entity4 = DataStore.get(em, Entity4.class, Entity4.CompId.of(1, "abc"));
        assertTrue(entity4.isPresent());
        assertEquals("t1", entity4.get().getTest());

        entity4 = DataStore.Q.of(Entity4.class).eq("dateTime"
                , LocalDateTime.of(2020, 12, 14, 1, 21, 33)).findOne(em);
        assertEquals("t1", entity4.get().getTest());
    }

    @Test
    public void txTest() {
        try {
            EntityTransaction trx = em.getTransaction();
            trx.begin();
            DataStore.save(em, new Entity5("t1"));
            DataStore.save(em, new Entity5("t2"));
            assertEquals(2, DataStore.count(em, Entity5.class));
            DataStore.save(em, new Entity5());
            trx.commit();
        } catch (Exception ignored) {
        }
        assertEquals(0, DataStore.count(em, Entity5.class));

        DataStore.save(em, new Entity5("t1"));
        DataStore.save(em, new Entity5("t2"));
        DataStore.save(em, new Entity5("t3"));
        assertEquals(3, DataStore.count(em, Entity5.class));
    }
}
