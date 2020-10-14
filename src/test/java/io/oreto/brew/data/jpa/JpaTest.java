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
                            .withEntity2s(Lists.of(
                                    new Entity2("e1")
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
    public void query1() {
        Paged<Entity1> query = QueryParser.query(
                "count{entity2s}::eq:1 or entity2s.entity3s.name:e5"
                , em
                , Entity1.class);
        List<Entity1> result = query.getList();
        assertEquals(2, result.size());
    }

    @Test
    public void query2() {
        Paged<Entity2> query = QueryParser.query(
                "entity3s.name:e03 or count{entity3s}:0"
                , em
                , Entity2.class);
        List<Entity2> result = query.getList();
        assertEquals(3, result.size());
    }

    @Test
    public void query3() {
        Paged<Entity2> query = QueryParser.query(
                "count{entity3s.id}:2 or count{entity3s}:1 or count{entity3s}:0"
                , em
                , Entity2.class);
        List<Entity2> result = query.getList();
        assertEquals(4, result.size());
    }

    @Test
    public void query4() {
        Paged<Entity2> query = QueryParser.query(
                "entity3s.name::endswith:03"
                , em
                , Entity2.class);
        List<Entity2> result = query.getList();
        assertEquals(1, result.size());
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

        Paged<Entity1> paged = DataStore.find(em, Entity1.class, "name:test3");
        assertEquals(1, paged.getPage().getNumber());
        DataStore.delete(em, Entity1.class, paged.getList().get(0).getId());
        assertTrue(DataStore.find(em, Entity1.class,"name:test3").getList().isEmpty());
    }
}
