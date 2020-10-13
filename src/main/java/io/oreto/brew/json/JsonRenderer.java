package io.oreto.brew.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.oreto.brew.map.MultiString;
import io.oreto.brew.str.Str;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class JsonRenderer {

    public JsonRenderer() { }

    public String render(Object o, String view, String selectValue, String dropValue) {
        if (!Str.isBlank(view)) {
            List<JsonNode> picks = new ArrayList<>();

            ObjectNode element = o instanceof ObjectNode ? (ObjectNode) o : (ObjectNode) JSON.asJson(o);
            track(new ArrayList<ObjectNode>() {{
                add(element);
            }}, "", picker(view), picks);
            int size = picks.size();
            if (size == 1) o = picks.get(0);
            else if (size > 1) {
                ArrayNode jsonArray = JSON.mapper.createArrayNode();
                picks.forEach(jsonArray::add);
                o = jsonArray;
            }
        }
        if ((Str.isBlank(dropValue) || o == null) && Str.isBlank(selectValue)) {
            return o instanceof ObjectNode ? o.toString() : JSON.asString(o);
        } else {
            ObjectNode element = o instanceof ObjectNode ? (ObjectNode) o : (ObjectNode) JSON.asJson(o);
            List<ObjectNode> json = element.isArray()
                    ? StreamSupport.stream((element).spliterator(), false)
                    .map(it -> (ObjectNode) it).collect(Collectors.toList())
                    : new ArrayList<ObjectNode>() {{
                add(element);
            }};

            if (!Str.isBlank(selectValue) && !Str.isBlank(dropValue)) {
                JsonNode copy = json.size() == 1 ? JSON.mapper.createObjectNode() : JSON.mapper.createArrayNode();
                walk(json, "", picker(selectValue), copy);

                json = copy instanceof ObjectNode
                        ? new ArrayList<ObjectNode>() {{
                    add((ObjectNode) copy);
                }}
                        : StreamSupport.stream((copy).spliterator(), false)
                        .map(it -> (ObjectNode) it)
                        .collect(Collectors.toList());

                track(json, "", picker(dropValue), null);
                return json.size() == 1 ? json.get(0).toString() : json.toString();

            } else if (!Str.isBlank(selectValue)) {
                JsonNode copy = json.size() == 1 ? JSON.mapper.createObjectNode() : JSON.mapper.createArrayNode();
                walk(json, "", picker(selectValue), copy);
                return copy.toString();
            } else {
                track(json, "", picker(dropValue), null);
                return json.size() == 1 ? json.get(0).toString() : json.toString();
            }
        }
    }

    public String render(Object o)  {
        return JSON.asString(o);
    }


    protected Map<String, List<String>> picker(String dsl) {
        MultiString<String> picks = new MultiString<>();
        StringBuilder sb = new StringBuilder();
        Stack<String> address = new Stack<>();
        String currentAddress = "";
        int dotted = 0;

        int len = dsl.length();
        for (int i = 0; i < len; i++){
            char c = dsl.charAt(i);
            switch (c) {
                case '{':
                    currentAddress = open(sb, address, picks, currentAddress);
                    dotted = 0;
                    break;
                case '.':
                    currentAddress = open(sb, address, picks, currentAddress);
                    dotted = 1;
                    break;
                case '}':
                    if (dotted > 0) {
                        currentAddress = close(sb, address, picks, currentAddress);
                        dotted = 0;
                    }
                    currentAddress = close(sb, address, picks, currentAddress);
                    break;
                default:
                    if (dotted == 1 && c == ' ') {
                            dotted = 2;
                    } else if (dotted == 2 && c != ' ') {
                        currentAddress = close(sb, address, picks, currentAddress);
                        dotted = 0;
                    }
                    sb.append(c);
                    if (i == len - 1) {
                        addFields(sb.toString(), picks, currentAddress);
                        sb.setLength(0);
                    }
                    break;
            }
        }

        Map<String, Long> hits = new HashMap<>();
        picks.asMap().values().stream().flatMap(Collection::stream)
                .filter(it -> it.startsWith("*")).forEach(it -> {
            String path = it.substring(1);
            hits.put(it, picks.asMap().keySet().stream().filter(key -> key.startsWith(path)).count());
        });

        return picks.sort((o1, o2) -> {
            boolean isPointer = hits.containsKey(o1);
            int comp = Boolean.compare(isPointer, hits.containsKey(o2));
            return comp == 0 && isPointer ? hits.get(o1).compareTo(hits.get(o2)) : comp;
        }).asMap();
    }

    protected String open(StringBuilder sb, Stack<String> address, MultiString<String> picks, String currentAddress) {
        List<String> fields = Arrays.stream(sb.toString().trim().split("[ \n\r]"))
                .collect(Collectors.toList());
        int size = fields.size();
        if (size > 1) {
            for (String field : fields.subList(0, size - 1)) picks.put(currentAddress, field.trim());
        }
        String pointer = fields.get(size - 1).trim();
        address.push(pointer);
        picks.put(currentAddress, "*"+pointer);
        sb.setLength(0);
        return address(address);
    }

    protected String close(StringBuilder sb, Stack<String> address, MultiString<String> picks, String currentAddress) {
        addFields(sb.toString(), picks, currentAddress);
        sb.setLength(0);
        address.pop();
        return address(address);
    }

    protected Map.Entry<String, Subset> parseIndex(String property) {
        if (property.matches(".*\\[[0-9]+]$")) {
            int start = property.indexOf('[');
            String name = property.substring(0, start).trim();
            String index = property.substring(start + 1, property.length() - 1).trim();
            start = Integer.parseInt(index);
            return new AbstractMap.SimpleEntry<>(name, Subset.of(start - 1));
        } else if (property.matches(".*\\[[0-9]*:[0-9]*]$")) {
            int start = property.indexOf('[');
            String name = property.substring(0, start).trim();
            String index = property.substring(start + 1, property.length() - 1).trim();

            String[] range = index.split(":");
            String a = range[0].trim();
            String b = range.length > 1 ? range[1].trim() : "";
            return new AbstractMap.SimpleEntry<>(name
                    , Subset.of(
                    a.equals("") ? null : Integer.parseInt(a) - 1
                    , b.equals("") ? null : Integer.parseInt(b) - 1));
        }
        return null;
    }

    private String resolveAddress(String path, String address) {
        return "".equals(path) ? address : String.format("%s.%s", path, address);
    }
    private String address(Stack<String> stack) { return String.join(".", stack); }
    private void addFields(String fields, MultiString<String> picks, String currentAddress) {
        for (String field : fields.trim().split("[ \n\r]")) {
            picks.put(currentAddress, field.trim());
        }
    }

    protected void track(List<ObjectNode> nodes, String path, Map<String, List<String>> pathMap, List<JsonNode> picks) {
        for (String address : pathMap.get(path)) {
            boolean isPointer = address.startsWith("*");
            address = isPointer ? address.substring(1) : address;
            String property = address;
            Map.Entry<String, Subset> subset = parseIndex(property);
            if (subset != null) { property = subset.getKey(); }

            for (ObjectNode node : nodes) {
                JsonNode element = node.get(property);
                if (isPointer) {
                    List<ObjectNode> newNodes = new ArrayList<>();
                    if (element instanceof ArrayNode) {
                        ArrayNode jsonArray = (ArrayNode) element;
                        if (subset == null)
                            jsonArray.forEach(it -> newNodes.add((ObjectNode) it));
                        else
                            subset.getValue().size(jsonArray.size()).stream()
                                    .forEach(it -> newNodes.add((ObjectNode) jsonArray.get(it)));
                    } else if (element instanceof ObjectNode) {
                        newNodes.add((ObjectNode) element);
                    }
                    track(newNodes, resolveAddress(path, address), pathMap, picks);
                } else {
                    if (element instanceof ArrayNode && subset != null) {
                        ArrayNode jsonArray = (ArrayNode) node.get(property);
                        if (picks == null)
                            subset.getValue().size(jsonArray.size()).streamStart().forEach(jsonArray::remove);
                        else
                            subset.getValue().size(jsonArray.size()).streamStart().forEach(it -> picks.add(jsonArray.get(it)));
                    } else {
                        if (picks == null)
                            node.remove(property);
                        else
                            picks.add(node.get(property));
                    }
                }
            }
        }
    }

    protected void walk(ObjectNode node, ObjectNode copy
            , String path, Map<String, List<String>> pathMap, List<ObjectNode> newNodes
            , String property, String address) {
        newNodes.add(node);
        ObjectNode newObject = JSON.mapper.createObjectNode();
        copy.set(property, newObject);
        walk(newNodes, resolveAddress(path, address), pathMap, newObject);
    }

    protected void walk(ObjectNode node, ArrayNode copy, String path, Map<String, List<String>> pathMap
            , List<ObjectNode> newNodes, String property, String address) {
        newNodes.add(node);
        ObjectNode newObject = JSON.mapper.createObjectNode();
        addElement(copy, property, newObject);
        walk(newNodes, resolveAddress(path, address), pathMap, newObject);
    }

    protected void walk(ArrayNode node, ArrayNode copy
            , String path, Map<String, List<String>> pathMap
            , Map.Entry<String, Subset> subset, List<ObjectNode> newNodes
            , String property, String address) {

        if (subset == null) node.forEach(it -> newNodes.add((ObjectNode) it));
        else subset.getValue().size(node.size()).stream()
                .forEach(it -> newNodes.add((ObjectNode) node.get(it)));

        for (JsonNode ignored : node) {
            ObjectNode newObject = JSON.mapper.createObjectNode();
            addElement(copy, property, newObject);
            walk(newNodes, resolveAddress(path, address), pathMap, newObject);
        }
    }

    protected void walk(ArrayNode node, ObjectNode copy
            , String path, Map<String, List<String>> pathMap
            , Map.Entry<String, Subset> subset, List<ObjectNode> newNodes
            , String property, String address) {
        if (subset == null) node.forEach(it -> newNodes.add((ObjectNode) it));
        else subset.getValue().size(node.size()).stream()
                .forEach(it -> newNodes.add((ObjectNode) node.get(it)));

        ArrayNode newArray = JSON.mapper.createArrayNode();
        if (copy.has(property)) {
            newArray = (ArrayNode) copy.get(property);
        } else
            copy.set(property, newArray);
        walk(newNodes, resolveAddress(path, address), pathMap, newArray);
    }

    protected void walk(List<ObjectNode> nodes, String path, Map<String, List<String>> pathMap, JsonNode copy) {
        for (ObjectNode node : nodes) {
            List<Map.Entry<String, JsonNode>> elements = pathMap.get(path)
                    .stream().filter(it -> !it.startsWith("*")).map(property -> {
                        Map.Entry<String, Subset> subset = parseIndex(property);
                        if (subset != null) { property = subset.getKey(); }
                        JsonNode element = node.get(property);
                        if (element instanceof ArrayNode && subset != null) {
                            ArrayNode jsonArray = (ArrayNode) element;
                            ArrayNode newArray = JSON.mapper.createArrayNode();
                            for (int it : subset.getValue().size(jsonArray.size()).stream().toArray()) {
                                newArray.add(jsonArray.get(it));
                            }
                            return new AbstractMap.SimpleEntry<String, JsonNode>(property, newArray);
                        } else
                            return new AbstractMap.SimpleEntry<>(property, element);
                    }).collect(Collectors.toList());

            if (elements.size() > 0) {
                if (copy instanceof ObjectNode) {
                    addElement((ObjectNode) copy, elements);
                } else if (copy instanceof ArrayNode) {
                    addElement((ArrayNode) copy, elements);
                }
            }
        }

        for (String address : pathMap.get(path)
                .stream().filter(it -> it.startsWith("*")).collect(Collectors.toList())) {
            address = address.substring(1);
            String property = address;
            Map.Entry<String, Subset> subset = parseIndex(property);
            if (subset != null) { property = subset.getKey(); }

            for (ObjectNode node: nodes) {
                if (!node.has(property)) continue;

                JsonNode element = node.get(property);

                if (element instanceof ObjectNode) {
                    if (copy instanceof ObjectNode)
                        walk((ObjectNode) element, (ObjectNode) copy, path, pathMap
                                , new ArrayList<>(), property, address);
                    else if (copy instanceof ArrayNode)
                        walk((ObjectNode) element, (ArrayNode) copy, path, pathMap, new ArrayList<>(), property, address);
                } else if (element instanceof ArrayNode) {
                    if (copy instanceof ObjectNode)
                        walk((ArrayNode) element, (ObjectNode) copy, path, pathMap, subset
                                , new ArrayList<>(), property, address);
                    else if (copy instanceof ArrayNode)
                        walk((ArrayNode) element, (ArrayNode) copy, path, pathMap, subset
                                , new ArrayList<>(), property, address);
                }
            }
        }
    }

    private void addElement(ArrayNode jsonArray, List<Map.Entry<String, JsonNode>> elements) {
        ObjectNode jsonObject = JSON.mapper.createObjectNode();
        elements.forEach(it -> jsonObject.set(it.getKey(), it.getValue()));
        jsonArray.add(jsonObject);
    }

    private void addElement(ObjectNode jsonObject, List<Map.Entry<String, JsonNode>> elements) {
        elements.forEach(it -> addElement(jsonObject, it.getKey(), it.getValue()));
    }

    private void addElement(ArrayNode jsonArray, String property, JsonNode element) {
        ObjectNode jsonObject = JSON.mapper.createObjectNode();
        jsonObject.set(property, element);
        jsonArray.add(jsonObject);
    }

    private void addElement(ObjectNode jsonObject, String property, JsonNode element) {
        if (jsonObject.has(property) && element instanceof ArrayNode)
            ((ArrayNode)jsonObject.get(property)).addAll((ArrayNode) element);
        else
            jsonObject.set(property, element);
    }

    static class Subset {
        public static Subset of(Integer a, Integer b) { return new Subset().start(a).end(b); }
        public static Subset of(Integer a) { return new Subset().start(a).end(a); }
        public static Subset of() { return new Subset(); }

        private Integer start, end, max, min;

        protected Subset() {
            start = end = null;
            min = max = 0;
        }

        public Subset start(Integer i) { this.start = i == null ? min : i; return this; }
        public Subset end(Integer i) { this.end = i; return this; }
        public Subset size(Integer i) {
            this.max = i - 1; return this;
        }
        public Subset min(Integer i) { this.min = i; return this; }

        public long size() { return stream().count(); }
        public long complimentSize() { return compliment().count(); }

        public boolean in(Integer i) {
            return i >= start && i <= end;
        }

        public boolean notIn(Integer i) { return !in(i); }

        public IntStream stream() {
            return IntStream.range(computeStart(), computeEnd() + 1);
        }
        public IntStream streamStart() {
            int start = computeStart();
            return stream().map(i -> start);
        }
        public IntStream streamReversed() {
            return stream().boxed().sorted(Collections.reverseOrder()).mapToInt(Integer::intValue);
        }
        public IntStream compliment() {
            return IntStream.concat(IntStream.range(min, computeStart()), IntStream.range(computeEnd() + 1, max + 1));
        }
        public IntStream complimentStart() {
            final int[] count = {min - 1};
            return compliment().map(i -> {
                count[0]++;
                return i - count[0];
            });
        }
        public IntStream complimentReversed() {
            return compliment().boxed().sorted(Collections.reverseOrder()).mapToInt(Integer::intValue);
        }

        protected int computeStart() { return start < min ? min : start; }
        protected int computeEnd() { return end == null || end > max ? max : end; }
    }
}
