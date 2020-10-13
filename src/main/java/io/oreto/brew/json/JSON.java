package io.oreto.brew.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import io.oreto.brew.io;

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
        mapper.registerModule(new AfterburnerModule());

        return mapper;
    }

    public static ObjectMapper mapper = mapper();

    public static String asString(Object o, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String asString(Object o) {
        return asString(o, mapper);
    }
    public static JsonNode asJson(Object o) { return mapper.valueToTree(o); }
    public static JsonNode asJson(String s) {
        try {
            return mapper.readTree(s);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T from(Map<String, ?> o, Class<T> tClass) {
        return mapper.convertValue(o, tClass);
    }
    public static <T> T from(CharSequence s , Class<T> tClass) {
        try {
            return mapper().readValue(s.toString(), tClass);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<Map<String, ?>> fromCsv(MappingIterator<Map<String, ?>> mappingIterator) {
        try {
            return mappingIterator.readAll();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Map<String, ?>> fromCsv(String csv) {
        MappingIterator<Map<String, ?>> mappingIterator = null;
        try {
            //s = s.replaceAll("[ \t]", "");
            mappingIterator = new CsvMapper()
                    .enable(CsvParser.Feature.ALLOW_COMMENTS)
                    .enable(CsvParser.Feature.TRIM_SPACES).reader()
                    .forType(Map.class)
                    .with(CsvSchema.emptySchema().withHeader())
                    .readValues(csv);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mappingIterator == null ? null : fromCsv(mappingIterator);
    }

    public static List<Map<String, ?>> fromCsv(File csv) {
        return fromCsv(io.fileText(csv).orElse(""));
    }

    public static String toCsv(List<Map<String, ?>> o, File file, String... columns) {
        CsvMapper csvMapper = new CsvMapper();
        try {
            CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
            if (columns.length > 0) {
                for (String column : columns)
                    csvSchemaBuilder.addColumn(column);
            } else if(o.size() > 0) {
                o.get(0).forEach((k, v) -> csvSchemaBuilder.addColumn(k));
            }

            if (file == null) {
                return csvMapper.writerFor(JsonNode.class)
                        .with(csvSchemaBuilder.build().withHeader())
                        .writeValueAsString(asJson(o));
            } else {
                csvMapper.writerFor(JsonNode.class)
                        .with(csvSchemaBuilder.build().withHeader())
                        .writeValue(file, asJson(o));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toCsv(List<Map<String, ?>> o, String... columns) {
        return toCsv(o, null, columns);
    }

    public static String asCsv(Iterable<?> o, File file, String... columns) {
        try {
            CsvMapper csvMapper = new CsvMapper();
            JsonNode jsonNode = asJson(o).elements().next();
            CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();

            if (columns.length > 0) {
                for (String column : columns)
                    csvSchemaBuilder.addColumn(column);
            } else {
                jsonNode.fieldNames().forEachRemaining(csvSchemaBuilder::addColumn);
            }

            if (file == null) {
                return csvMapper.writerFor(JsonNode.class)
                        .with(csvSchemaBuilder.build().withHeader())
                        .writeValueAsString(asJson(o));
            } else {
                csvMapper.writerFor(JsonNode.class)
                        .with(csvSchemaBuilder.build().withHeader())
                        .writeValue(file, asJson(o));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String asCsv(Iterable<?> o, String... columns) {
        return asCsv(o, null, columns);
    }
}
