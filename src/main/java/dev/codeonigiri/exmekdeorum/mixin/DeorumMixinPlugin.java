package dev.codeonigiri.exmekdeorum.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the Mek Energistics mixin only when that mod is installed.
 *
 * <p>Mixin plugins are constructed before the mod list is available, so this uses a plain
 * class-presence check on the SPI interface instead of {@code ModList}. When the class is
 * missing the mixin (and its {@code implements IMePatternAutomationHost} clause) is never
 * loaded, so the mod keeps working without the optional dependency.</p>
 */
public final class DeorumMixinPlugin implements IMixinConfigPlugin {

    private static final boolean MEKENERGISTICS_PRESENT = isPresent("com.beipuo.mekenergistics.api.upgrade.IMePatternAutomationHost");

    private static boolean isPresent(String name) {
        try {
            Class.forName(name, false, DeorumMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return MEKENERGISTICS_PRESENT;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
