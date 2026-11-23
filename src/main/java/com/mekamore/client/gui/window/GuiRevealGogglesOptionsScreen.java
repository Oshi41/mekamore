package com.mekamore.client.gui.window;

import com.mekamore.content.gear.reveal_googles.ModuleRevealingGogglesConfig;
import com.mekamore.registries.MekaMoreConfig;
import mekanism.api.gear.IModule;
import mekanism.api.gear.ModuleData;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.network.PacketUtils;
import mekanism.common.network.to_server.PacketUpdateModuleSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reveal Goggles filter editor as a Mekanism {@link GuiWindow}.
 *
 * <p>Two modes share the same window area: LIST (the filter rows) and PICK (search + results).
 * All widget changes are deferred to {@link #tick()} so we never modify the child list from inside
 * a click/typing callback, and the search field is created once per PICK session so typing does not reset it.
 */
public class GuiRevealGogglesOptionsScreen extends GuiWindow {

    public static final int W = 200;
    public static final int H = 180;

    private static final int CONTENT_X = 7;
    private static final int FIRST_ROW_Y = 24;
    private static final int ROW_PITCH = 20;
    private static final int ROW_H = 16;
    private static final int MAX_ROWS = 5;

    private static final int RESULTS_PER_PAGE = 5;
    private static final int RESULT_PITCH = 18;
    private static final int MAX_RESULTS_PER_KIND = 50;

    private enum Mode { LIST, PICK }

    // ── What we need to save through Mekanism's packet ────────────────
    private final Holder<ModuleData<?>> moduleHolder;
    private final int installedCount;
    private final int slotIndex;
    private final ModuleRevealingGogglesConfig config;

    // ── Working copy ──────────────────────────────────────────────────
    private final int rowCount;
    private final ResourceKey<?>[] slots;
    /** Entries beyond the visible rows (only possible if the max-filters config is larger than MAX_ROWS); kept so Apply never drops them. */
    private final List<ResourceKey<?>> hiddenExtras = new ArrayList<>();

    // ── UI state ──────────────────────────────────────────────────────
    private Mode mode = Mode.LIST;
    private int editingIndex = -1;
    private GuiTextField searchField;
    private final List<ResourceKey<?>> searchResults = new ArrayList<>();
    private int searchScroll = 0;

    private final List<GuiElement> contentElements = new ArrayList<>();
    private final List<GuiElement> resultElements = new ArrayList<>();
    private boolean rebuildContent;
    private boolean rebuildResults;

    public GuiRevealGogglesOptionsScreen(IGuiWrapper gui, int x, int y, IModule<?> module, int slotIndex,
                                   ModuleRevealingGogglesConfig config) {
        super(gui, x, y, W, H, SelectedWindowData.UNSPECIFIED);
        interactionStrategy = InteractionStrategy.NONE;

        // Capture plain values, not the IModule itself (it can go stale while the window is open)
        this.moduleHolder = module.getDataHolder();
        this.installedCount = module.getInstalledCount();
        this.slotIndex = slotIndex;
        this.config = config;

        this.rowCount = Math.max(1, Math.min(MekaMoreConfig.revealGooglesMaxFilters.get(), MAX_ROWS));
        this.slots = new ResourceKey<?>[rowCount];
        List<ResourceKey<?>> current = config.get();
        for (int i = 0; i < current.size(); i++) {
            if (i < rowCount) {
                slots[i] = current.get(i);
            } else {
                hiddenExtras.add(current.get(i));
            }
        }
        rebuildContent();
    }

    // ── Deferred rebuilds ─────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (rebuildContent) {
            rebuildContent();
        } else if (rebuildResults) {
            rebuildResultRows();
        }
    }

    private <E extends GuiElement> E add(List<GuiElement> bucket, E element) {
        addChild(element);
        bucket.add(element);
        return element;
    }

    private void removeAll(List<GuiElement> bucket) {
        children().removeAll(bucket);
        bucket.clear();
    }

    private void rebuildContent() {
        rebuildContent = false;
        rebuildResults = false;
        //Drop focus first so it can't point at an element we are about to remove (clearFocus() is private in Mekanism)
        setFocused(null);
        removeAll(resultElements);
        removeAll(contentElements);
        searchField = null;
        if (mode == Mode.LIST) {
            buildList();
        } else {
            buildPicker();
        }
    }

    // ── LIST mode ─────────────────────────────────────────────────────

    private void buildList() {
        for (int i = 0; i < rowCount; i++) {
            final int index = i;
            int rowY = relativeY + FIRST_ROW_Y + i * ROW_PITCH;
            ResourceKey<?> key = slots[i];
            Component label = key != null ? labelFor(key) : Component.translatable("gui.mekamore.reveal_goggles.pick_block");

            add(contentElements, new MekanismButton(gui(), relativeX + CONTENT_X + 20, rowY, 140, ROW_H, label,
                    (element, mx, my) -> {
                        openPicker(index);
                        return true;
                    }));
            add(contentElements, new MekanismButton(gui(), relativeX + CONTENT_X + 164, rowY, 16, ROW_H, Component.literal("✕"),
                    (element, mx, my) -> {
                        slots[index] = null;
                        rebuildContent = true;
                        return true;
                    }));
        }
        add(contentElements, new MekanismButton(gui(), relativeX + W / 2 - 45, relativeY + H - 22, 90, 16,
                Component.translatable("gui.mekamore.reveal_goggles.apply"),
                (element, mx, my) -> {
                    applyAndClose();
                    return true;
                }));
    }

    private void openPicker(int index) {
        editingIndex = index;
        mode = Mode.PICK;
        searchResults.clear();
        searchScroll = 0;
        rebuildContent = true;
    }

    private void closePicker() {
        editingIndex = -1;
        mode = Mode.LIST;
        rebuildContent = true;
    }

    // ── PICK mode ─────────────────────────────────────────────────────

    private void buildPicker() {
        searchField = add(contentElements, new GuiTextField(gui(), relativeX + CONTENT_X, relativeY + 22, W - 2 * CONTENT_X, 14));
        searchField.setMaxLength(64);
        searchField.setResponder(this::onSearchChanged);
        setFocused(searchField);
        searchField.setFocused(true);

        int arrowX = relativeX + W - CONTENT_X - 14;
        add(contentElements, new MekanismButton(gui(), arrowX, relativeY + 42, 14, 14, Component.literal("▲"),
                (element, mx, my) -> {
                    if (searchScroll > 0) {
                        searchScroll--;
                        rebuildResults = true;
                    }
                    return true;
                }));
        add(contentElements, new MekanismButton(gui(), arrowX, relativeY + 58, 14, 14, Component.literal("▼"),
                (element, mx, my) -> {
                    if (searchScroll + RESULTS_PER_PAGE < searchResults.size()) {
                        searchScroll++;
                        rebuildResults = true;
                    }
                    return true;
                }));
        add(contentElements, new MekanismButton(gui(), relativeX + W / 2 - 45, relativeY + H - 22, 90, 16,
                Component.translatable("gui.mekamore.reveal_goggles.cancel"),
                (element, mx, my) -> {
                    closePicker();
                    return true;
                }));
        rebuildResultRows();
    }

    private void rebuildResultRows() {
        rebuildResults = false;
        removeAll(resultElements);
        int from = Math.min(searchScroll, searchResults.size());
        int to = Math.min(from + RESULTS_PER_PAGE, searchResults.size());
        for (int i = from; i < to; i++) {
            final ResourceKey<?> key = searchResults.get(i);
            add(resultElements, new MekanismButton(gui(), relativeX + CONTENT_X, relativeY + 42 + (i - from) * RESULT_PITCH,
                    W - 2 * CONTENT_X - 18, 16, labelFor(key),
                    (element, mx, my) -> {
                        if (editingIndex >= 0) {
                            slots[editingIndex] = key;
                        }
                        closePicker();
                        return true;
                    }));
        }
    }

    private void onSearchChanged(String query) {
        searchResults.clear();
        searchScroll = 0;
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) {
            int found = 0;
            for (Block block : BuiltInRegistries.BLOCK) {
                if (block.asItem() == Items.AIR) {
                    continue;
                }
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                if (id.toString().contains(q)) {
                    searchResults.add(ResourceKey.create(Registries.BLOCK, id));
                    if (++found >= MAX_RESULTS_PER_KIND) {
                        break;
                    }
                }
            }
            found = 0;
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if (id.toString().contains(q)) {
                    searchResults.add(ResourceKey.create(Registries.ENTITY_TYPE, id));
                    if (++found >= MAX_RESULTS_PER_KIND) {
                        break;
                    }
                }
            }
        }
        rebuildResults = true;
    }

    // ── Labels / icons ────────────────────────────────────────────────

    private static boolean isBlock(ResourceKey<?> key) {
        return key.registry().equals(Registries.BLOCK.location());
    }

    private static boolean isEntity(ResourceKey<?> key) {
        return key.registry().equals(Registries.ENTITY_TYPE.location());
    }

    private static Component labelFor(ResourceKey<?> key) {
        ResourceLocation id = key.location();
        if (isBlock(key)) {
            return BuiltInRegistries.BLOCK.getOptional(id).<Component>map(Block::getName).orElse(Component.literal(id.toString()));
        } else if (isEntity(key)) {
            Component name = BuiltInRegistries.ENTITY_TYPE.getOptional(id).<Component>map(EntityType::getDescription)
                    .orElse(Component.literal(id.toString()));
            return Component.literal("[E] ").append(name);
        }
        return Component.literal(id.toString());
    }

    private static ItemStack iconFor(ResourceKey<?> key) {
        ResourceLocation id = key.location();
        if (isBlock(key)) {
            return BuiltInRegistries.BLOCK.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
        } else if (isEntity(key)) {
            return BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(SpawnEggItem::byId).map(ItemStack::new).orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    // ── Rendering ─────────────────────────────────────────────────────

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        drawTitleText(guiGraphics, Component.translatable("gui.mekamore.reveal_goggles.title"), 6);

        if (mode == Mode.LIST) {
            for (int i = 0; i < rowCount; i++) {
                if (slots[i] != null) {
                    ItemStack icon = iconFor(slots[i]);
                    if (!icon.isEmpty()) {
                        gui().renderItem(guiGraphics, icon, relativeX + CONTENT_X, relativeY + FIRST_ROW_Y + i * ROW_PITCH);
                    }
                }
            }
        } else if (!searchResults.isEmpty()) {
            //drawScrollingString positions are relative to this window
            drawScrollingString(guiGraphics, Component.literal(searchResults.size() + " found"), CONTENT_X, H - 36,
                    TextAlignment.LEFT, subheadingTextColor(), 4, false);
        }
    }

    // ── Keyboard ──────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        //Escape in the picker goes back to the list; a second Escape lets GuiWindow handle it
        if (keyCode == 256 && mode == Mode.PICK) {
            closePicker();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── Commit ────────────────────────────────────────────────────────

    private void applyAndClose() {
        List<ResourceKey<?>> result = new ArrayList<>();
        for (ResourceKey<?> key : slots) {
            if (key != null) {
                result.add(key);
            }
        }
        result.addAll(hiddenExtras);

        //with() de-duplicates, sorts and caps; the server applies the same sanitising when it decodes the packet
        ModuleRevealingGogglesConfig updated = config.with(result);
        if (!updated.equals(config)) {
            PacketUtils.sendToServer(PacketUpdateModuleSettings.create(slotIndex, moduleHolder, installedCount, updated));
        }
        close();
    }
}
 