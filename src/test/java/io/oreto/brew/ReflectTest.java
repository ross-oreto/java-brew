package io.oreto.brew;

import io.oreto.brew.collections.Lists;
import io.oreto.brew.data.jpa.repo.Address;
import io.oreto.brew.data.jpa.repo.Item;
import io.oreto.brew.data.jpa.repo.Order;
import io.oreto.brew.data.jpa.repo.Person;
import io.oreto.brew.obj.Reflect;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReflectTest {

    @Test
    public void setter1() throws ReflectiveOperationException {
        Setter setter = new Setter();

        Reflect.setFieldValue(setter, "test", "hello");
        assertEquals("hello", Reflect.getFieldValue(setter, "test"));
    }

    @Test
    public void setter2() throws ReflectiveOperationException {
        Setter setter = new Setter();

        Reflect.setFieldValue(setter, "i", 20);
        assertEquals(20, Reflect.getFieldValue(setter, "i"));
    }

    @Test
    public void copy() throws ReflectiveOperationException {
        Setter setter1 = new SubSetter("s1", 1);
        Setter setter2 = new SubSetter("s2", 2);

        Reflect.copy(setter1, setter2, "i");
        assertEquals(2, setter1.getI());

        setter1 = new SubSetter("s1", 1);
        setter2 = new SubSetter("s2", 2);
        Reflect.copy(setter1, setter2, Lists.of("i"), Reflect.CopyOptions.create().exclusion());
        assertEquals(1, setter1.getI());
        assertEquals("s2", setter1.getTest());
    }

    @Test
    void adder() throws ReflectiveOperationException {
        Setter setter = new Setter();
        assertTrue(Reflect.getAdder("strings", setter).isPresent());
        assertTrue(Reflect.getAdder("integers", setter).isPresent());
        Reflect.addFieldValue(setter, "strings", "test");
        assertEquals(Lists.of("test"), setter.strings);
    }

    @Test
    void remover() {
        assertTrue(Reflect.getRemover("strings", new Setter()).isPresent());
        assertTrue(Reflect.getRemover("integers", new Setter()).isPresent());
    }

    @Test
    void parameterNames() throws ReflectiveOperationException {
        Map<String, Object> request = new HashMap<String, Object>() {{
            put("name", "Ross Oreto");
            put("orders"
                    , new ArrayList<Object>() {{
                        add(
                                new HashMap<String, Object>() {{
                                    put("amount", 12.01);
                                    put("person", new HashMap<String, Object>() {{ put("name", "Michael Oreto"); }});
                                }}
                        );
                    }});
            put("nickNames", new ArrayList<String>(){{ add("rossSauce");}});
            put("address", new HashMap<String, Object>() {{
                put("line", "1st st");
            }});
        }};
        assertEquals(Lists.of("address.line"
                , "name"
                , "orders.amount"
                , "orders.person.name"
                , "nickNames")
                , Reflect.parameterNames(request));

        Person person1 = new Person()
                .withAddress(new Address().withLine("1st Ave Nashville, TN"))
                .withName("Ross Oreto").addNickName("Ross Sauce", "Ross Sea").addOrder(
                        new Order()
                                .withAmount(20.01)
                                .addItem(new Item().withName("knife").addAttribute("type", "forged", "special"))
                );
        Person person2 = new Person()
                .withAddress(new Address().withLine("3rd Ave Nashville, TN"))
                .withName("Michael Oreto").addNickName("RossSauce", "Rossi").addOrder(
                        new Order().withAmount(200.01)
                                .addItem(new Item().withName("sword"))
                );
        Reflect.copy(person1, person2, Reflect.parameterNames(request), Reflect.CopyOptions.create().updateCollections());
        assertEquals(person1, person2);

        assertEquals("3rd Ave Nashville, TN", person1.getAddress().getLine());
        assertEquals(200.01, person1.getOrders().get(0).getAmount());
        assertEquals(Lists.of("RossSauce", "Rossi"), person1.getNickNames());
        assertEquals("sword", person1.getOrders().get(0).getItems().get(0).getName());
    }

    public static class Setter {
        private String test;
        private int i;
        private final List<String> strings = new ArrayList<>();
        private final Set<Integer> integers = new HashSet<>();

        public Setter(String test, int i) {
            this.test = test;
            this.i = i;
        }

        public Setter() { }

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public void addString(String... strings) {
            this.strings.addAll(Arrays.asList(strings));
        }

        public void addInteger(int integer) {
            this.integers.add(integer);
        }

        public void removeString(String... strings) {
            this.strings.removeAll(Arrays.asList(strings));
        }

        public void removeInteger(int integer) {
            this.integers.remove(integer);
        }
    }

    public static class SubSetter extends Setter {
        public SubSetter(String s1, int i) {
            super(s1, i);
        }
    }
}
