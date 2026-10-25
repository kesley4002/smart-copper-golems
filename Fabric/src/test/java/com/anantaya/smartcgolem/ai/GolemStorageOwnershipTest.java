package com.anantaya.smartcgolem.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class GolemStorageOwnershipTest {
    @Test
    public void unnamedStorageRemainsSharedByAllGolems() {
        assertTrue(GolemStorageOwnership.accepts(null, "Coletor: Roger"));
        assertTrue(GolemStorageOwnership.accepts("", "Organizador: Pedro"));
        assertTrue(GolemStorageOwnership.accepts("   ", null));
    }

    @Test
    public void namedStorageOnlyAcceptsItsOwner() {
        assertTrue(GolemStorageOwnership.accepts("Roger", "Roger"));
        assertTrue(GolemStorageOwnership.accepts("Roger", "Coletor: Roger"));
        assertTrue(GolemStorageOwnership.accepts("Roger", "Collector: Roger"));
        assertTrue(GolemStorageOwnership.accepts("Coletor: Roger", "Coletor: Roger"));
        assertTrue(GolemStorageOwnership.accepts("Roger", "Fornalheiro: Roger"));
        assertFalse(GolemStorageOwnership.accepts("Roger", "Coletor: Pedro"));
        assertFalse(GolemStorageOwnership.accepts("Roger", "Rogerzinho"));
        assertFalse(GolemStorageOwnership.accepts("Roger", "Rog"));
        assertFalse(GolemStorageOwnership.accepts("Roger", null));
        assertFalse(GolemStorageOwnership.accepts("Coletor: Roger", "Roger"));
        assertFalse(GolemStorageOwnership.accepts("Pedro", "Agricultor: Roger"));
    }

    @Test
    public void accentsAndUppercaseDoNotBreakOwnerNames() {
        assertTrue(GolemStorageOwnership.accepts("João", "Agricultor: JOÃO"));
        assertTrue(GolemStorageOwnership.accepts("JOAO", "Coletor: João"));
        assertTrue(GolemStorageOwnership.accepts("  Roger  ", "  Coletor: Roger "));
    }

    @Test
    public void doubleChestCannotBeSplitAcrossOwners() {
        assertTrue(GolemStorageOwnership.namesCompatible(null, null));
        assertTrue(GolemStorageOwnership.namesCompatible("Roger", null));
        assertTrue(GolemStorageOwnership.namesCompatible(null, "Roger"));
        assertTrue(GolemStorageOwnership.namesCompatible("Roger", "roger"));
        assertTrue(GolemStorageOwnership.namesCompatible("João", "JOAO"));
        assertFalse(GolemStorageOwnership.namesCompatible("Roger", "Pedro"));
        assertFalse(GolemStorageOwnership.namesCompatible("Coletor: Roger", "Roger"));
    }
}
