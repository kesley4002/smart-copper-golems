package com.anantaya.smartcgolem;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Name tags define a golem's optional profession. No custom entity and no
 * change to the vanilla copper golem save format is necessary.
 *
 *   "Coletor"       -> COLETOR
 *   "Coletor: João" -> COLETOR, keeping the complete custom name
 *   "João"          -> ORGANIZADOR (all original behavior)
 *
 * Only implemented professions are registered here. Unknown/future names
 * keep the original organizer behavior until those jobs are actually built.
 */
public enum GolemProfession {
    ORGANIZADOR,
    COLETOR,
    AGRICULTOR,
    FORNALHEIRO,
    PASTOR;

    /** Both professions collect loose items. Ordinary organizers do not. */
    public boolean collectsGroundDrops() {
        return this == COLETOR || this == AGRICULTOR || this == PASTOR;
    }

    public static GolemProfession fromName(String customName) {
        if (customName == null || customName.isBlank()) return ORGANIZADOR;
        String normalized = Normalizer.normalize(customName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        int colon = normalized.indexOf(':');
        String label = (colon >= 0 ? normalized.substring(0, colon) : normalized).trim();
        return switch (label) {
            case "coletor", "collector" -> COLETOR;
            case "agricultor", "farmer" -> AGRICULTOR;
            case "fornalheiro", "furnace", "furnace tender", "furnace keeper", "furnace operator" -> FORNALHEIRO;
            case "pastor", "pastor de ovelhas", "tosquiador", "shepherd", "shearer" -> PASTOR;
            default -> ORGANIZADOR;
        };
    }
}
