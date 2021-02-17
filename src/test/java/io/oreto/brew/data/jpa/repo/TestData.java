package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.data.jpa.DataStore;
import io.oreto.brew.str.Str;
import org.hibernate.Session;

import javax.persistence.EntityManager;
import java.util.Random;

public class TestData {
    private static final Random rand = new Random();

    public static void random(int people, int orders, int items, EntityManager em) {
        for (int i = 0; i < people; i++) {
            Person person = new Person()
                    .withName(Str.random(10).toString())
                    .withAddress(new Address().withLine(Str.random(15).toString()));
            for (int j = 0; j < orders; j++) {
                Order order = new Order().withAmount(rand.nextDouble());
                for (int k = 0; k < items; k++) {
                    order.addItem(new Item().withName(Str.random(20).toString()));
                }
                person.addOrder(order);
            }
            DataStore.save(em, person);
        }
        Session session = em.unwrap(Session.class);
        session.clear();
    }

    public static void setupPeople(EntityManager em) {
        DataStore.save(em,
                new Person()
                        .withAddress(new Address().withLine("4th Ave Nashville, TN"))
                        .withName("Ross").addNickName("Ross Sauce", "Ross Sea").addOrder(
                                new Order()
                                        .withAmount(200.01)
                                        .addItem(new Item().withName("knife").addAttribute("type", "forged", "special")
                        )
                )
        );
        DataStore.save(em,
                new Person()
                        .withAddress(new Address().withLine("The Shire"))
                        .withName("Bilbo").addNickName("Barrel Rider", "Riddle Maker").addOrder(
                        new Order()
                                .withAmount(13000000.00)
                                .addItem(new Item().withName("The Ring").addAttribute("type", "forged"))
                                .addItem(new Item().withName("Sting").addAttribute("type", "forged")
                                        .addAttribute("special", "glows"))
                                .addItem(new Item().withName("Arkenstone").addAttribute("alias", "The King's Jewel"))
                )
        );
        DataStore.save(em,
                new Person()
                        .withAddress(new Address().withLine("Hogwarts, Gryffindor"))
                        .withName("Harry Potter").addNickName("The chosen one", "The boy who lived").addOrder(
                        new Order()
                                .withAmount(401000000.00)
                                .addItem(new Item().withName("Elder wand")
                                        .addAttribute("core", "tail hair of a Thestral")
                                        .addAttribute("exterior", "elder tree"))
                                .addItem(new Item().withName("Hedwig").addAttribute("species", "Snow Owl"))
                                .addItem(new Item().withName("Sword of Gryffindor")
                                        .addAttribute("special", "destroys horcruxes, willpower and loyality"))
                                .addItem(new Item().withName("eye glasses"))
                )
        );
        DataStore.save(em,
                new Person()
                        .withAddress(new Address().withLine("Hogwarts, Slug Club"))
                        .withName("Snape").addNickName("The Half Blood Prince").addOrder(
                        new Order()
                                .withAmount(541.00)
                                .addItem(new Item().withName("Ashwinder egg"))
                                .addItem(new Item().withName("Squill buld"))
                                .addItem(new Item().withName("Murtlap tentacle"))
                                .addItem(new Item().withName("Tincture of thyme"))
                                .addItem(new Item().withName("Occamy eggshell"))
                                .addItem(new Item().withName("Powdered common rue"))
                                .addItem(new Item().withName("glass bottle"))
                                .addItem(new Item().withName("Veritaserum").addAttribute("effect", "Truth serum"))
                )
        );
        DataStore.save(em,
                new Person()
                        .withAddress(new Address().withLine("Hogwarts, Chamber of Secrets"))
                        .withName("Tom Riddle").addNickName("Voldamort").addNickName("The Dark Lord").addOrder(
                        new Order()
                                .withAmount(766.60)
                                .addItem(new Item().withName("Diary"))
                                .addItem(new Item().withName("Ring"))
                                .addItem(new Item().withName("Locket"))
                                .addItem(new Item().withName("Cup"))
                                .addItem(new Item().withName("Diadem"))
                                .addItem(new Item().withName("Snake"))
                )
        );
        Session session = em.unwrap(Session.class);
        session.clear();
    }

    public static void setupVehicles(EntityManager em) {
        DataStore.save(em
                , new Vehicle()
                        .withMake("Mitsubishi")
                        .withModel("Outlander")
                        .withTire(new Tire().withId(new Tire.TireId("Goodyear", 20)))
        );
        DataStore.save(em
                , new Vehicle()
                        .withMake("Mitsubishi")
                        .withModel("Mirage")
                        .withTire(new Tire().withId(new Tire.TireId("Cooper", 13)))
        );
        Session session = em.unwrap(Session.class);
        session.clear();
    }
}
