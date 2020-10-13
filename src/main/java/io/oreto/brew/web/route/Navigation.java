package io.oreto.brew.web.route;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class Navigation {
    @JsonIgnore private Routing routing;
    private String name;
    private List<Item> items;

    public Navigation(Routing routing) {
        this.routing = routing;
        this.items = new ArrayList<>();
    }

    public Navigation withItem(String name, String defaultUri, String... args) {
        Item newItem = new Item();
        newItem.name = name;
        NamedRoute namedRoute = routing.at(name);
        newItem.uri = namedRoute == null ? defaultUri : routing.at(name).with(args);
        newItem.selected = newItem.uri.equals(routing.getInfo().getPath());
        items.add(newItem);
        return this;
    }

    public List<Item> items(){
        return items;
    }

    public Navigation withName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public static class Item {
        public String name;
        public String uri;
        public boolean selected;
    }
}
