package dev.codeonigiri.exmekdeorum.client;

import dev.codeonigiri.exmekdeorum.ExMekanismDeorum;
import dev.codeonigiri.exmekdeorum.compat.jei.DeorumJeiRuntime;
import dev.codeonigiri.exmekdeorum.machine.DeorumMachineFamily;
import dev.codeonigiri.exmekdeorum.registry.DeorumMachineContainer;
import dev.codeonigiri.exmekdeorum.tile.TileEntityDeorumMachine;
import java.util.List;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.window.GuiSideConfigurationTab;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Shared screen for every Ex Deorum machine.
 *
 * <p>The screen is intentionally generic: it reads the tile (tier / family / fluid or not) and
 * draws the matching widgets, so tuning a single class changes all 20 machines.</p>
 *
 * <h2>Layout</h2>
 * <p>The GUI is 200x188 pixels. {@code assets/exmekdeorum/textures/gui/deorum_machine.png} is a
 * copy of Mekanism's {@code gui/base.png} (MIT) so the border is pixel-identical to Mekanism.
 * It is a 256x256 sheet drawn with Mekanism's 4-pixel nine-slice: the four 4x4 corners are copied
 * 1:1 and the edges/center are stretched to the window size. If you edit the sheet, keep the
 * border inside the outer 4 pixels and the middle a flat colour; position-specific art (header
 * bands, separators) must stay out of the texture because stretching would move it.</p>
 *
 * <p>The player inventory starts at y = 104 and the label sits at y = 94, so the machine area
 * (slots 1-20, progress bar, gauges) fills the upper block.</p>
 *
 * <h2>Customising the GUI</h2>
 * <ul>
 *   <li>Edit {@code textures/gui/deorum_machine.png} directly (the asset generator does not
 *       overwrite it); keep the border in the outer 4 pixels.</li>
 *   <li>Move widgets by editing the constants below. They must stay in sync with the slot
 *       coordinates in {@link TileEntityDeorumMachine} (see the slot table in README.md).</li>
 *   <li>Slot frames are drawn by Mekanism ({@code gui/slot/normal.png}); the background should not
 *       contain slot art, otherwise it will not line up if the GUI size is changed.</li>
 * </ul>
 */
public class GuiDeorumMachine extends GuiMekanismTile<TileEntityDeorumMachine, DeorumMachineContainer> {

    /** Full size of the rendered window. */
    public static final int WIDTH = 200;
    public static final int HEIGHT = 188;

    /** Player inventory label; the inventory rows start 10 pixels lower. */
    public static final int INVENTORY_LABEL_Y = 94;
    /** Machine title label. */
    public static final int TITLE_Y = 6;

    /** Energy bar: 8 px wide, 54 px tall, flush with the inner right border. */
    public static final int POWER_X = 184;
    public static final int POWER_Y = 18;
    public static final int POWER_HEIGHT = 54;
    /** Compact machines (hammer / sieve) keep the progress bar under the input column. */
    public static final int PROGRESS_X = 30;
    /** Barrel / crucible centre the progress bar inside the machine area (inventory-aware: 8..192). */
    public static final int PROGRESS_CENTER_X = 88;
    public static final int PROGRESS_Y = 76;
    /** Progress bar is vertically centred in the machine area on barrel / crucible. */
    public static final int PROGRESS_CENTER_Y = 44;
    /** Barrel input gauge (left of the progress bar, 8 px from the item column). */
    public static final int GAUGE_X = 64;
    /** Fluid gauge right of the progress bar (8 px): barrel output tank / crucible output. */
    public static final int GAUGE_RIGHT_X = 121;
    /** Gauges are vertically centred on the same line as the progress bar. */
    public static final int GAUGE_Y = 22;
    public static final int GAUGE_W = 16;
    public static final int GAUGE_H = 54;

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(ExMekanismDeorum.MOD_ID, "textures/gui/deorum_machine.png");

    private GuiSideConfigurationTab<TileEntityDeorumMachine> sideConfigTab;

    public GuiDeorumMachine(DeorumMachineContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = DeorumMachineContainer.INVENTORY_X;
        inventoryLabelY = INVENTORY_LABEL_Y;
        titleLabelY = TITLE_Y;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), POWER_X, POWER_Y, POWER_HEIGHT));
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getActive));
        // Side configuration (input/output/eject per face). GuiConfigurableTile would also add the
        // transporter tab, which does not apply to these machines, so only the side tab is added.
        sideConfigTab = addRenderableWidget(new GuiSideConfigurationTab<>(this, tile, () -> sideConfigTab));
        DeorumMachineFamily family = tile.getFamily();
        boolean compact = family == DeorumMachineFamily.HAMMER || family == DeorumMachineFamily.SIEVE;
        int progressX = compact ? PROGRESS_X : PROGRESS_CENTER_X;
        int progressY = compact ? PROGRESS_Y : PROGRESS_CENTER_Y;
        // Clicking the progress bar opens the matching JEI category (like Mekanism machines do).
        addRenderableWidget(new GuiProgress(() -> tile.getProgress(), ProgressType.BAR, this, progressX, progressY) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0 && isMouseOver(mouseX, mouseY)) {
                    DeorumJeiRuntime.openRecipeCategories(tile.getFamily());
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
        });

        List<BasicFluidTank> tanks = tile.getFluidTanks();
        if (tanks.size() == 1) {
            // Crucible: item input on the left, output fluid on the right, progress bar centred.
            addFluidGauge(tanks.get(0), GAUGE_RIGHT_X, GAUGE_Y, GAUGE_W, GAUGE_H);
        } else if (tanks.size() >= 2) {
            // Barrel: input tank left of the progress bar, output tank right of it.
            addFluidGauge(tanks.get(0), GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H);
            addFluidGauge(tanks.get(1), GAUGE_RIGHT_X, GAUGE_Y, GAUGE_W, GAUGE_H);
        }
    }

    private void addFluidGauge(BasicFluidTank tank, int x, int y, int width, int height) {
        addRenderableWidget(new GuiFluidGauge(() -> tank, () -> List.<IExtendedFluidTank>of(tank),
                GaugeType.STANDARD, this, x, y, width, height));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Mekanism's 4-pixel nine-slice: corners are copied, edges/center are stretched.
        GuiUtils.renderBackgroundTexture(guiGraphics, BACKGROUND, 4, 4, leftPos, topPos,
                imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    private void renderInventoryText(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }
}
