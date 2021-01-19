package io.oreto.brew.data.jpa;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.oreto.brew.collections.Lists;
import io.oreto.brew.data.Model;
import io.oreto.brew.serialize.json.JSON;
import io.oreto.brew.str.Str;

import java.util.List;

public class GenerateEntity {

    public static void main(String[] args) throws JsonProcessingException {
        Options options = JSON.from(args[0], Options.class);

        List<String> imports = Lists.of(Model.class.getName());

        Str str = Str.of("package").space().add(options.pack == null ? options.name.toLowerCase() : options.pack)
                .add(';').br(2)
                .add("public class").space().add(Str.toPascal(options.name)).space().add("implements").space()
                .add(Model.class.getSimpleName()).add('<')
                .add(Str.capitalize(options.idType == null ? "Long" : options.idType)).add("> {").br(2);

        str.add('}').br();
        System.out.println(str.toString());
    }

    static class Options {
        String name;
        String tableName;
        String idType;
        String idName;
        String idStrategy;

        // "Long,id,column"
        List<String> fields;

        // include all: *
        // default: idName
        // include: foo,bar
        // exclude: -foo
        String toString;

        boolean timeStamp;
       
        // default: idName
        String hashAndEquals;

        String pack;

        @JsonCreator
        public Options(@JsonProperty("name") String name) {
            this.name = name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public void setIdType(String idType) {
            this.idType = idType;
        }

        public void setIdName(String idName) {
            this.idName = idName;
        }

        public void setIdStrategy(String idStrategy) {
            this.idStrategy = idStrategy;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }

        public void setToString(String toString) {
            this.toString = toString;
        }

        public void setTimeStamp(boolean timeStamp) {
            this.timeStamp = timeStamp;
        }

        public void setHashAndEquals(String hashAndEquals) {
            this.hashAndEquals = hashAndEquals;
        }

        public void setPack(String pack) {
            this.pack = pack;
        }
    }
}
