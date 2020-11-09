package io.oreto.brew.data.jpa;

import io.oreto.brew.collections.Lists;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.jpa.repo.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = { JpaConfig.class },
        loader = AnnotationConfigContextLoader.class)
@Transactional
@Rollback(false)
public class JpaTest {
    private static EntityManager em;
    private static boolean setup;

    @Resource private EntityManagerFactory entityManagerFactory;
    @Resource private Entity1Repository entity1Repository;
    @Resource private Entity2Repository entity2Repository;
    @Resource private Entity3Repository entity3Repository;

    @Before
    public void setup() {
        if (!setup) {
            setup = true;
            em = entityManagerFactory.createEntityManager();
            DataStore.save(em,
                    new Entity1("test")
                            .withStrings("test", "ing", "io", "oreto", "brew", "key")
                            .withEntries(new HashMap<String, String>(){{
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
                    new Entity1("test2")
                            .withEntity2s(Lists.of(new Entity2("ee")))
            );
        }
    }

    @AfterClass
    public static void tearDown() {
        em.close();
    }

    @Test
    public void query0() {
        Paged<Entity1> query =
            DataStore.Q.of(Entity1.class).eq("name", "test").find(em);
        List<Entity1> result = query.getList();
        assertEquals(3, result.get(0).getEntity2s().size());
    }

    @Test
    public void query1() {
        Paged<Entity1> query = DataStore.Q.of(Entity1.class)
                .eq(DataStore.Q.Func.of(Function.count, "entity2s"), 1)
                .or()
                .eq("entity2s.entity3s.name", "e5")
                .find(em);
        List<Entity1> result = query.getList();
        assertEquals(2, result.size());
    }

    @Test
    public void query2() {
        Paged<Entity2> query = DataStore.Q.of(Entity2.class)
                .eq("entity3s.name", "e03")
                .or()
                .eq(DataStore.Q.Func.of(Function.count, "entity3s"), 0)
                .find(em);

        List<Entity2> result = query.getList();
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
        List<Entity2> result = query.getList();
        assertEquals(5, result.size());
    }

    @Test
    public void query4() {
        Paged<Entity2> query = DataStore.Q.of(Entity2.class)
                .endsWith("entity3s.name", "03")
                .find(em);
        List<Entity2> result = query.getList();
        assertEquals(1, result.size());
    }

    @Test
    public void query5() {
        Paged<Entity1> query = DataStore.Q.of(Entity1.class)
                .eq("entries", "key")
                .find(em, "entries");
        List<Entity1> result = query.getList();
        assertEquals("value", result.get(0).getEntries().get("key"));
    }

    @Test
    public void query6() {
        Paged<Entity1> query = DataStore.Q.of(Entity1.class)
                .eq("strings", "brew")
                .find(em, "strings", "entries");
        List<Entity1> result = query.getList();
        assertEquals("value", result.get(0).getEntries().get("key"));
    }

    @Test
    public void crud() {
        DataStore.save(em,
            new Entity1("test3").withEntity2s(Lists.of(new Entity2("e20")))
        );

        Entity1 u = DataStore.get(em, Entity1.class,1L).orElse(null);
        assert u != null;
        u.setName("update");
        DataStore.update(em, u);
        assertEquals("update", DataStore.get(em, Entity1.class, 1L).get().getName());

        Paged<Entity1> paged = DataStore.findAll(em, Entity1.class, "name:test3");
        assertEquals(1, paged.getPage().getNumber());
        DataStore.delete(em, Entity1.class, paged.getList().get(0).getId());
        assertTrue(DataStore.findAll(em, Entity1.class,"name:test3").getList().isEmpty());
    }

    @Test
    public void list1() {
        Paged<Entity1> query = DataStore.list(em, Entity1.class, "entity2.entity3s", "strings", "entries");
        List<Entity1> result = query.getList();
        assertEquals("oneE2", result.get(0).getEntity2().getName());
    }
}
