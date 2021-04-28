package io.oreto.brew.data.jpa;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersistenceXmlTest {

    @Test
    public void readXml() {
        Optional<Persistence> persistence = Persistence.parseXml();
        assertTrue(persistence.isPresent());
        assertEquals("my-pu", persistence.get().getDefaultUnit().getName());
        assertEquals(3, persistence.get().getDefaultUnit().getProperties().size());
        assertEquals("second", persistence.get().getUnits().get(1).getName());
    }
}
