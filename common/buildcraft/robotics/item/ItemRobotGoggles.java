package buildcraft.robotics.item;

import buildcraft.lib.item.IItemBuildCraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class ItemRobotGoggles extends ArmorItem implements IItemBuildCraft {
    private final String idBC;

    public ItemRobotGoggles(String idBC, Properties properties) {
        super(ArmorMaterials.CHAIN, Type.HELMET, properties);
        this.idBC = idBC;
        init();
    }

//    @Override

//    @Override

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        // Never damaged
        return 0;
    }

//    @Override

    @Override
    public String getIdBC() {
        return idBC;
    }

    private String unlocalizedName;

    @Override
    public void setUnlocalizedName(String unlocalizedName) {
        this.unlocalizedName = unlocalizedName;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return this.unlocalizedName;
    }
}
