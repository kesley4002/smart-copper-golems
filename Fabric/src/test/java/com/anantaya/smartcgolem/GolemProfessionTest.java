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
    public void portugueseAndEnglishNameTagsSelectTheSameProfessions() {
        assertEquals(GolemProfession.COLETOR, GolemProfession.fromName("Coletor: Pedro"));
        assertEquals(GolemProfession.COLETOR, GolemProfession.fromName("Collector: Peter"));
        assertEquals(GolemProfession.AGRICULTOR, GolemProfession.fromName("Agricultor: João"));
        assertEquals(GolemProfession.AGRICULTOR, GolemProfession.fromName("Farmer: John"));
        assertEquals(GolemProfession.FORNALHEIRO,
                GolemProfession.fromName("Fornalheiro: Carlos"));
        assertEquals(GolemProfession.FORNALHEIRO,
                GolemProfession.fromName("Furnace Tender: Charles"));
        assertEquals(GolemProfession.FORNALHEIRO,
                GolemProfession.fromName("Furnace Keeper: Charles"));
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName("Peter"));
    }

    @Test
    public void shepherdHasItsOwnPortugueseAndEnglishProfession() {
        assertEquals(GolemProfession.PASTOR, GolemProfession.fromName("Pastor: Lúcia"));
        assertEquals(GolemProfession.PASTOR, GolemProfession.fromName("Pastor de Ovelhas: Ana"));
        assertEquals(GolemProfession.PASTOR, GolemProfession.fromName("Tosquiador: Pedro"));
        assertEquals(GolemProfession.PASTOR, GolemProfession.fromName("Shepherd: Alex"));
        assertEquals(GolemProfession.PASTOR, GolemProfession.fromName("Shearer: Kate"));
        assertTrue(GolemProfession.PASTOR.collectsGroundDrops());
        assertFalse(GolemProfession.FORNALHEIRO.collectsGroundDrops());
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName("Lúcia"));
    }

    @Test
    public void unconfiguredGolemsKeepLegacySorting() {
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName(null));
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName(""));
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName("João"));
        assertEquals(GolemProfession.AGRICULTOR, GolemProfession.fromName("Agricultor: Pedro"));
        assertEquals(GolemProfession.AGRICULTOR, GolemProfession.fromName("Farmer: Ana"));
        assertEquals(GolemProfession.AGRICULTOR, GolemProfession.fromName("Agricultor"));
        assertEquals(GolemProfession.FORNALHEIRO, GolemProfession.fromName("Fornalheiro"));
        assertEquals(GolemProfession.FORNALHEIRO, GolemProfession.fromName("Fornalheiro: Carlos"));
        assertFalse(GolemProfession.FORNALHEIRO.collectsGroundDrops());
        assertEquals(GolemProfession.ORGANIZADOR, GolemProfession.fromName("Supercoletor: João"));
    }
}
