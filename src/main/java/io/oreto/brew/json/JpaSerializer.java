package io.oreto.brew.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import java.io.IOException;
import java.util.Objects;

/**
 * This is the key class in enabling graceful handling of Hibernate managed entities when
 * serializing them to JSON.
 * The key features are:
 * Non-initialized properties will be rendered as {@code null} in JSON to prevent
 * "lazy-loaded" exceptions when the Hibernate session is closed.
 * @author Ross Oreto
 */
public class JpaSerializer extends BeanSerializerFactory {

    private final PersistenceUnitUtil util;

    public JpaSerializer(EntityManager entityManager) {
        super(new SerializerFactoryConfig());
        util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
    }

    @Override
    public JsonSerializer<Object> createSerializer(SerializerProvider prov, JavaType type) throws JsonMappingException {
        Class<?> clazz = type.getRawClass();

        Package pack = clazz.getPackage();
        boolean persistentObject = Objects.nonNull(pack) && pack.getName().startsWith("org.hibernate");
        return persistentObject
                ? new PersistentSerializer(super.createSerializer(prov, type))
                : super.createSerializer(prov, type);
    }

    private class PersistentSerializer extends JsonSerializer<Object> {

        JsonSerializer<Object> defaultSerializer;

        public PersistentSerializer(JsonSerializer<Object> defaultSerializer) {
            this.defaultSerializer = defaultSerializer;
        }

        @Override
        public void serialize(Object value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (!util.isLoaded(value)) {
                generator.writeString("NOT_LOADED");
                return;
            }
            defaultSerializer.serialize(value, generator, provider);
        }
    }
}
