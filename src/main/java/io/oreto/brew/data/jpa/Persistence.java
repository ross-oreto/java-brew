package io.oreto.brew.data.jpa;

import io.oreto.brew.io;
import org.simpleframework.xml.*;
import org.simpleframework.xml.core.Persister;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Root(name = "persistence", strict = false)
public class Persistence {

    public static Optional<Persistence> parseXml(ClassLoader classLoader) {
        Optional<String> xml = io.resourceText(classLoader, "META-INF", "persistence.xml");
        Serializer serializer = new Persister();

        if (xml.isPresent()) {
            Persistence persistence;
            try {
                persistence = serializer.read(Persistence.class, xml.get());
            } catch (Exception e) {
                e.printStackTrace();
                return Optional.empty();
            }
            return Optional.of(persistence);
        }
        return Optional.empty();
    }
    public static Optional<Persistence> parseXml()  {
       return parseXml(Persistence.class.getClassLoader());
    }

    @ElementList(name = "persistence-unit", inline = true)
    private List<Unit> units;

    public List<Unit> getUnits() {
        return units;
    }
    public Unit getDefaultUnit() {
        return units.get(0);
    }
    public List<String> getUnitNames() {
        return units.stream().map(Unit::getName).collect(Collectors.toList());
    }
    public void setUnits(List<Unit> units) {
        this.units = units;
    }

    @Root(name = "persistence-unit", strict = false)
    public static class Unit {
        @Attribute private String name;
        @Attribute(name = "transaction-type", required = false) private String transactionType;
        @Element(required = false) private String description;
        @Element(required = false) private String provider;
        @Element(name = "jta-data-source", required = false) private String jtaDataSource;
        @Element(name = "non-jta-data-source", required = false) private String nonJtaDataSource;

        @ElementList(required = false) private List<Property> properties;

        public List<Property> getProperties() {
            return properties;
        }
        public void setProperties(List<Property> properties) {
            this.properties = properties;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getProvider() {
            return provider;
        }
        public void setProvider(String provider) {
            this.provider = provider;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getTransactionType() {
            return transactionType;
        }
        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }
        public String getJtaDataSource() {
            return jtaDataSource;
        }
        public void setJtaDataSource(String jtaDataSource) {
            this.jtaDataSource = jtaDataSource;
        }
        public String getNonJtaDataSource() {
            return nonJtaDataSource;
        }
        public void setNonJtaDataSource(String nonJtaDataSource) {
            this.nonJtaDataSource = nonJtaDataSource;
        }

        @Root(name = "property")
        public static class Property {
            @Attribute(required = false) private String name;
            @Attribute(required = false) private String value;

            public String getName() {
                return name;
            }
            public void setName(String name) {
                this.name = name;
            }
            public String getValue() {
                return value;
            }
            public void setValue(String value) {
                this.value = value;
            }
        }
    }
}
