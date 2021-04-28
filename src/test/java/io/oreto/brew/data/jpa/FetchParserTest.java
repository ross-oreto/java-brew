package io.oreto.brew.data.jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FetchParserTest {

    @Test
    public void fetchTest1() {
        Fetch.Plan plan = Fetcher.parse("nickNames address orders[]{ items[] { attributes[] } }");
        assertArrayEquals(new String[]{"", "orders", "orders.items"}, plan.getJoinPaths());

        plan = Fetcher.parse("nickNames address orders[]{items[]{attributes[]}}");
        assertArrayEquals(new String[]{"", "orders", "orders.items"}, plan.getJoinPaths());

        assertTrue(plan.joins("").get(2).isCollection());
        assertTrue(plan.joins("orders").get(0).isCollection());
        assertTrue(plan.joins("orders.items").get(0).isCollection());
    }

    @Test
    public void fetchTest2() {
        Fetch.Plan plan = Fetcher.parse("nickNames address orders[1]{ items[1:2] { attributes[1:10] } }");
        assertArrayEquals(new String[]{"", "orders", "orders.items"}, plan.getQueryPaths());

        assertTrue(plan.queries("").get(0).isCollection());
        assertTrue(plan.queries("orders").get(0).isCollection());
        assertTrue(plan.queries("orders.items").get(0).isCollection());
    }
}
