package com.anantaya.smartcgolem;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class GolemProfessionTest {
    @Test
    public void collectorPrefixOrSimpleTagWorks() {
        assertEquals(GolemProfession.COLETOR, GolemProfession.fromName("Coletor"));
        assertEquals(GolemProfession.COLETOR, GolemProfession.fromName("Coletor: João"));
        assertEquals(GolemProfession.COLETOR, GolemProfession.fromName("  COLETOR :  Ana  "));
        assertEquals(GolemProfession.COLETOR, GolemProfession.fromName("Collector: Alex"));
    }

    @Test
    public void groundPickupIsSharedOnlyByDesignatedProfessions() {
        assertTrue(GolemProfession.COLETOR.collectsGroundDrops());
        assertTrue(GolemProfession.AGRICULTOR.collectsGroundDrops());
        assertFalse(GolemProfession.ORGANIZADOR.collectsGroundDrops());
    }

    @Test
    public void compostingBelongsToTheSameFarmerNameTag() {
        assertEquals(GolemProfession.AGRICULTOR,
                GolemProfession.fromName("Agricultor: João"));
        assertEquals(GolemProfession.AGRICULTOR,
                GolemProfession.fromName("agricultor"));
        // No separate Composteiro job or name tag exists.
        assertEquals(GolemProfession.ORGANIZADOR,
                GolemProfession.fromName("Composteiro"));
    }

    @Test
    public void unconfiguredGolemsKeepLegacySorting() {
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName(null));
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName(""));
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName("João"));
        assertEquals(GolemProfession.AGRICULTOR, GolemProfession.fromName("Agricultor: Pedro"));
        assertEquals(GolemProfession.AGRICULTOR, GolemProfession.fromName("Farmer: Ana"));
        assertEquals(GolemProfession.AGRICULTOR, GolemProfession.fromName("Agricultor"));
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName("Fornalheiro"));
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName("Supercoletor: João"));
    }
}
