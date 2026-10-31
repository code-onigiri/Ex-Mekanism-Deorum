package dev.exmekdeorum.registry;

import mekanism.api.text.ILangEntry;

/**
 * Minimal {@link ILangEntry} that simply returns a translation key.
 */
public record DeorumLangEntry(String key) implements ILangEntry {

    @Override
    public String getTranslationKey() {
        return key;
    }
}
