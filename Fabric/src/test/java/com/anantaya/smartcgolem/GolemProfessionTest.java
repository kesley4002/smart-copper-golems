package com.anantaya.smartcgolem;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class GolemProfessionTest {
    @Test
    public void collectorPrefixOrSimpleTagWorks() {
        assertEquals(GolemProfession.COLETOR, GolemProfession.fromName("Coletor"));
        assertEquals(GolemProfession.COLETOR, GolemProfession.fromName("Coletor: João"));
        assertEquals(GolemProfession.COLETOR, GolemProfession.fromName("  COLETOR :  Ana  "));
        assertEquals(GolemProfession.COLETOR, GolemProfession.fromName("Collector: Alex"));
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
