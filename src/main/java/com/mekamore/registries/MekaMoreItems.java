package com.mekamore.registries;

import com.mekamore.MekaMore;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MekaMoreItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MekaMore.MODID);

    public static final DeferredItem<Item> MODULE_LOOTING_UNIT = ITEMS.register("module_looting_unit", () -> IModuleHelper.INSTANCE.createModuleItem(
          () -> MekaMoreModules.LOOTING_UNIT, new Item.Properties().rarity(Rarity.RARE).stacksTo(4)));

    private MekaMoreItems() {
    }
}