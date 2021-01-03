package io.oreto.brew.serialize.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.oreto.brew.serialize.JpaSerializer;

import javax.persistence.EntityManager;
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

    public static ObjectMapper mapper = mapper();

    public static ObjectMapper jpa(EntityManager entityManager) {
        mapper.setSerializerFactory(new JpaSerializer(entityManager));
        return mapper;
    }

    public static String asString(Object o, ObjectMapper mapper) throws JsonProcessingException {
        return mapper.writeValueAsString(o);
    }

    public static String asString(Object o) throws JsonProcessingException {
        return asString(o, mapper);
    }
    public static JsonNode asJson(Object o) { return mapper.valueToTree(o); }
    public static JsonNode asJson(String s) throws JsonProcessingException {
        return mapper.readTree(s);
    }
    public static Map<String, Object> asMap(String s) throws JsonProcessingException {
        return mapper().readValue(s, new TypeReference<Map<String, Object>>() {});
    }
    public static Map<String, Object> asMap(Object o) {
        return mapper().convertValue(o, new TypeReference<Map<String, Object>>() {});
    }

    public static <T> T from(Map<String, ?> o, Class<T> tClass) {
        return mapper.convertValue(o, tClass);
    }
    public static <T> T from(CharSequence s , Class<T> tClass) throws JsonProcessingException {
        return mapper().readValue(s.toString(), tClass);
    }
}
