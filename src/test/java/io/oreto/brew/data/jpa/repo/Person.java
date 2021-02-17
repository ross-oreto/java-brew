package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.data.Model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
@Table
public class Person implements Model<Long>, Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(nullable = false) private String name;

    @ElementCollection(fetch = FetchType.LAZY)
    private final List<String> nickNames;

    @OneToMany(cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, fetch = FetchType.LAZY, mappedBy = "person")
    private final List<Order> orders;

    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    @MapsId
    private Address address;

    public Person() {
        orders = new ArrayList<>();
        nickNames = new ArrayList<>();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders.clear();
        addOrder(orders);
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<String> getNickNames() {
        return nickNames;
    }

    public void setNickNames(List<String> nickNames) {
        this.nickNames.clear();
        addNickName(nickNames);
    }

    public Person withId(Long id) {
        this.id = id;
        return this;
    }

    public Person withName(String name) {
        this.name = name;
        return this;
    }

    public Person withOrders(List<Order> orders) {
        setOrders(orders);
        return this;
    }

    public Person withAddress(Address address) {
        setAddress(address);
        return this;
    }

    public Person withNickNames(List<String> nickNames) {
        setNickNames(nickNames);
        return this;
    }

    public Person addNickName(String... nickNames) {
        this.nickNames.addAll(Arrays.asList(nickNames));
        return this;
    }

    public Person addNickName(Collection<String> nickNames) {
        this.nickNames.addAll(nickNames);
        return this;
    }

    public Person addOrder(Order... orders) {
        for(Order order : orders) {
            order.setPerson(this);
            this.orders.add(order);
        }
        return this;
    }

    public Person addOrder(Collection<Order> orders) {
        return addOrder(orders.toArray(new Order[0]));
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Person)) {
            return false;
        }
        Person person = (Person) o;
        return Objects.equals(person.getId(), getId())
                && Objects.equals(person.getName(), getName());
    }
}
