package io.oreto.brew.data.jpa;

import io.jsonwebtoken.lang.Maps;
import io.oreto.brew.collections.Lists;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.jpa.repo.*;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceUnitUtil;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = true)
@TestPropertySource(locations = "classpath:application.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
public class JpaTest {
    private static EntityManager em;
    private static PersistenceUnitUtil util;

    @Resource private EntityManagerFactory entityManagerFactory;

    private final Random random = new Random();

    @BeforeEach
    public void init() {
        em = entityManagerFactory.createEntityManager();
        util = em.getEntityManagerFactory().getPersistenceUnitUtil();
        reset();
    }

    public void reset() {
        DataStore.deleteAll(em, Person.class);
        DataStore.deleteAll(em, Item.class);
        DataStore.deleteAll(em, Order.class);
        DataStore.deleteAll(em, Address.class);
    }

    @AfterEach
    public void close() {
        em.close();
    }

    @Test
    public void listFetch1() {
        DataStore.save(em,
                new Person()
                        .withAddress(new Address().withLine("4th Ave Nashville, TN"))
                        .withName("Ross").addNickName("Ross Sauce", "Ross Sea").addOrder(
                                new Order().withAmount(20.01).addItem(new Item().withName("knife")
                                        .addAttribute("type", "forged"))
                )
        );
        Session session = em.unwrap(Session.class);
        session.clear();

        Paged<Person> result = DataStore.list(em
                , Person.class
                , Fetch.join("nickNames","address", "orders[]")
                        .at("orders").join("items[]")
                        .at("orders.items").join("attributes").buildPlan());
        List<Person> page = result.getPage();
        session.clear();
        session.close();

        assertTrue(util.isLoaded(page.get(0).getOrders().get(0).getItems().get(0), "nickNames"));
        assertTrue(util.isLoaded(page.get(0), "attributes"));
        assertEquals(Maps.of("type", "forged").build()
                , page.get(0).getOrders().get(0).getItems().get(0).getAttributes());
        assertEquals(Lists.of("Ross Sauce", "Ross Sea"), page.get(0).getNickNames());
        assertEquals("4th Ave Nashville, TN", page.get(0).getAddress().getLine());
        assertEquals("knife", page.get(0).getOrders().get(0).getItems().get(0).getName());
    }

    @Test
    public void listFetch2() {
        TestData.random(10, 10, 10, em);
        Paged<Person> result = DataStore.list(em
                , Person.class
                , Fetch.get("address", "orders[8:9]").at("orders").get("items[0:5](name:asc)").buildPlan());

        List<Person> page = result.getPage();
        Session session = em.unwrap(Session.class);
        session.clear();
        session.close();

        assertTrue(util.isLoaded(page.get(0), "address"));
        assertTrue(util.isLoaded(page.get(0), "orders"));
        assertEquals(2, page.get(0).getOrders().size());
        assertEquals(5, page.get(0).getOrders().get(0).getItems().size());
        // make sure the sorting works
        assertEquals(page.get(0).getOrders().get(0).getItems().stream().map(Item::getName).collect(Collectors.toList())
                , page.get(0).getOrders().get(0).getItems().stream().sorted(Comparator.naturalOrder()).map(Item::getName)
                        .collect(Collectors.toList()));
    }

    @Test
    public void listFetch3() {
        TestData.random(10, 2, 2, em);
        Paged<Person> result = DataStore.list(em
                , Person.class
                , Fetch.get("address", "orders[]").at("orders").get("items[]").buildPlan()
        );
        List<Person> page = result.getPage();

        Session session = em.unwrap(Session.class);
        session.clear();
        session.close();

        assertTrue(util.isLoaded(page.get(0), "address"));
        assertFalse(page.get(0).getAddress().getLine().isEmpty());
        assertTrue(util.isLoaded(page.get(0), "orders"));
        assertEquals(2, page.get(0).getOrders().size());
        assertEquals(2, page.get(0).getOrders().get(0).getItems().size());
    }

    @Test
    public void listFetch4() {
        TestData.random(5, 1, 5, em);

        Paged<Person> result = DataStore.list(em
                , Person.class
                , Fetch.get("address", "orders[]")
                        .at("orders").get("items[]")
                        .at("orders.items").get("orders[]").buildPlan());
        List<Person> page = result.getPage();

        Session session = em.unwrap(Session.class);
        session.clear();
        session.close();

        assertTrue(util.isLoaded(page.get(0), "orders"));
        assertTrue(util.isLoaded(page.get(0), "address"));
        assertEquals(1, page.get(0).getOrders().size());
        assertEquals(5, page.get(0).getOrders().get(0).getItems().size());
        assertEquals(page.get(0).getOrders().get(0), page.get(0).getOrders().get(0).getItems().get(0).getOrders().get(0));
    }

    @Test
    public void query1() {
        TestData.setupPeople(em);

        Paged<Person> result = DataStore.Q.of(Person.class)
                .eq("orders.items.attributes.value", "forged")
                .find(em);
        List<Person> page = result.getPage();

        assertEquals(2, page.size());
        assertEquals("forged", page.get(0).getOrders().get(0).getItems().get(0).getAttributes().get("type"));
    }

    @Test
    public void query2() {
        TestData.setupPeople(em);

        Paged<Person> result = DataStore.Q.of(Person.class)
                .iContains("address.line", "hogwarts")
                .find(em);
        List<Person> page = result.getPage();

        assertEquals(3, page.size());
    }

    @Test
    public void query3() {
        TestData.setupPeople(em);

        Paged<Person> result = DataStore.Q.of(Person.class)
                .iContains("address.line", "hogwarts")
                .or()
                .lt(DataStore.Q.Func.of(Function.count, "orders.items"), 2)
                .find(em);
        List<Person> page = result.getPage();

        assertEquals(4, page.size());
    }

    @Test
    public void query4() {
        TestData.setupPeople(em);

        Paged<Person> result = DataStore.Q.of(Person.class)
                .iContains("orders.items.name", "ring")
                .order("name")
                .find(em);
        List<Person> page = result.getPage();

        assertEquals(2, page.size());
        assertEquals("Bilbo", page.get(0).getName());
        assertEquals("Tom Riddle", page.get(1).getName());
    }

    @Test
    public void query5() {
        TestData.setupPeople(em);

        Paged<Person> result = DataStore.Q.of(Person.class)
                .eq("orders.items.attributes.value", null)
                .find(em);
        List<Person> page = result.getPage();

        assertEquals(3, page.size());
    }

    @Test
    public void query6() {
        TestData.setupPeople(em);

        Paged<Person> result = DataStore.Q.of(Person.class)
                .eq("nickNames", "The Half Blood Prince")
                .find(em);
        List<Person> page = result.getPage();

        assertEquals(1, page.size());
        assertEquals("Snape", page.get(0).getName());
    }

    @Test
    public void query7() {
        TestData.setupPeople(em);
        Paged<Person> result = DataStore.findAll(em, Person.class, "name::in:['Bilbo', 'Ross', 'Snape']");
        assertEquals(3, result.getPage().size());

        result = DataStore.findAll(em, Person.class, "orders.items.name::in:['Hedwig']");
        assertEquals(1, result.getPage().size());
        assertEquals("Harry Potter", result.getPage().get(0).getName());
    }

    @Test
    public void query8() {
        TestData.setupPeople(em);
        long count = DataStore.Q.of(Person.class).count(em);

        assertEquals(count
                , DataStore.Q.of(Person.class).lt("orders.purchasedOn", LocalDateTime.now())
                        .find(em).getPager().getCount());
    }

    @Test
    public void query9() {
       TestData.setupVehicles(em);

       Optional<Vehicle> vehicle =
               DataStore.get(em, Vehicle.class, new Vehicle.VehicleId("Mitsubishi", "Outlander")
                       , Fetch.get("tire").buildPlan());
       assertTrue(vehicle.isPresent());
       assertEquals(20, vehicle.get().getTire().getId().getSize());

       Paged<Vehicle> result = DataStore.Q.of(Vehicle.class).eq("tire.id.make", "Goodyear").find(em);
       assertFalse(result.getPage().isEmpty());
    }

    @Test
    public void crud() {
        DataStore.save(em,
                new Person()
                        .withAddress(new Address().withLine("Kokiri Forest, Hyrule"))
                        .withName("Link").addNickName("Hero of Time", "Legendary Hero", "Hero of Light").addOrder(
                        new Order()
                                .withAmount(1000.01)
                                .addItem(new Item().withName("Master Sword").addAttribute("type", "forged")
                                        .addAttribute("special", "Bane of evil"))
                                .addItem(new Item().withName("Silver Arrow"))
                                .addItem(new Item().withName("Fairy Bow"))
                                .addItem(new Item().withName("Hylian Shield"))
                )
        );

        Person u = DataStore.findOne(em, Person.class, "").orElse(null);
        assert u != null;
        Long id = u.getId();
        u.setName("Zelda");
        DataStore.update(em, u);
        assertEquals("Zelda", DataStore.get(em, Person.class, id).get().getName());

        Paged<Person> paged = DataStore.findAll(em, Person.class, "name:Zelda");
        assertEquals(1, paged.getPager().getPage());
        DataStore.delete(em, paged.getPage().get(0));
        assertTrue(DataStore.findAll(em, Person.class,"name:Zelda").getPage().isEmpty());
    }

    @Test
    public void txTest() {
        try {
            EntityTransaction trx = em.getTransaction();
            trx.begin();
            DataStore.save(em, new Person().withName("Link"));
            DataStore.save(em, new Person());
            trx.commit();
        } catch (Exception ignored) {
        }
        assertEquals(0, DataStore.count(em, Person.class));
    }
}
