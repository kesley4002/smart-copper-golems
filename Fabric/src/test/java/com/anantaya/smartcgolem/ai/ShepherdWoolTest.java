package com.anantaya.smartcgolem.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShepherdWoolTest {
    @Test
    public void collectOnlyNaturalSheepWoolOfAnyColor() {
        String[] colors = {"white","orange","magenta","light_blue","yellow",
                "lime","pink","gray","light_gray","cyan","purple","blue",
                "brown","green","red","black"};
        for (String color : colors) {
            assertTrue(color, ShepherdWool.acceptsId("minecraft:" + color + "_wool"));
            assertFalse(color + " carpet", ShepherdWool.acceptsId(
                    "minecraft:" + color + "_carpet"));
        }
    }

    @Test
    public void ignoreSheepMeatAndUnrelatedAnimalDrops() {
        for (String id : new String[]{"minecraft:mutton", "minecraft:cooked_mutton",
                "minecraft:rotten_flesh", "minecraft:egg", "minecraft:feather",
                "minecraft:leather", "minecraft:bone", "minecraft:white_dye",
                "minecraft:shears", "minecraft:string"}) {
            assertFalse(id, ShepherdWool.acceptsId(id));
        }
        assertFalse(ShepherdWool.acceptsId(null));
    }
}
