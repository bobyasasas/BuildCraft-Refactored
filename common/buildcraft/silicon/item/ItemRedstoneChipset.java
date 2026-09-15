package buildcraft.silicon.item;

import buildcraft.api.enums.EnumRedstoneChipset;
import buildcraft.api.items.IChipset;
import buildcraft.lib.item.ItemBC_Neptune;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemRedstoneChipset extends ItemBC_Neptune implements IChipset {
    public final EnumRedstoneChipset type;

    public ItemRedstoneChipset(String idBC, Item.Properties props, EnumRedstoneChipset type) {
        super(idBC, props);
        this.type = type;
    }

//    @Override

    // 1.18.2: different item obj
//    @Override


    @Override
    public String getDescriptionId(ItemStack stack) {
        return "item.redstone_" + type.name().toLowerCase() + "_chipset.name";
    }

    // IChipset

    @Override
    public EnumRedstoneChipset getType() {
        return type;
    }
}
