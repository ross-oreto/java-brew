package io.oreto.brew.serialize.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.oreto.brew.serialize.JpaSerializer;

import javax.persistence.PersistenceUnitUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JSON {

    protected static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new ParameterNamesModule());

        return mapper;
    }

    public static final ObjectMapper mapper = mapper();
    public static final Map<String, ObjectMapper> mappers = new HashMap<String, ObjectMapper>(){{ put("", mapper); }};

    public static ObjectReader reader() {
        return mapper.reader();
    }

    public static ObjectWriter writer() {
        return mapper.writer();
    }

    public static ObjectReader reader(String name) {
        return mappers.get(name).reader();
    }

    public static ObjectWriter writer(String name) {
        return mappers.get(name).writer();
    }

    public static ObjectMapper jpa(String unit, PersistenceUnitUtil util) {
        ObjectMapper unitMapper = mapper();
        unitMapper.setSerializerFactory(new JpaSerializer(util));
        mappers.put(unit, unitMapper);
        return unitMapper;
    }

    public static ObjectMapper jpa(PersistenceUnitUtil util) {
        mapper.setSerializerFactory(new JpaSerializer(util));
        return mapper;
    }

    public static String asString(Object o, boolean pretty) throws JsonProcessingException {
        return pretty
                ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o)
                : mapper.writer().writeValueAsString(o);
    }

    public static String asString(Object o) throws JsonProcessingException {
        return asString(o, false);
    }

    public static JsonNode asJson(Object o) { return mapper.valueToTree(o); }
    public static JsonNode asJson(String s) throws JsonProcessingException {
        return mapper.reader().readTree(s);
    }
    public static Map<String, Object> asMap(String s) throws JsonProcessingException {
        return mapper.readValue(s, new TypeReference<Map<String, Object>>() {});
    }
    public static Map<String, Object> asMap(Object o) {
        return mapper.convertValue(o, new TypeReference<Map<String, Object>>() {});
    }

    public static <T> T from(Map<String, ?> o, Class<T> tClass) {
        return mapper.convertValue(o, tClass);
    }
    public static <T> T from(CharSequence s , Class<T> tClass) throws IOException {
        return mapper.reader().readValue(s.toString(), tClass);
    }
}
