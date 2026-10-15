package cn.blockforge.generated.floralritebloomcurios.registry;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.block.entity.FloralAltarBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FRBBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FloralRiteBloom.MOD_ID);

    public static final RegistryObject<BlockEntityType<FloralAltarBlockEntity>> FLORAL_ALTAR =
            BLOCK_ENTITIES.register("floral_altar", () -> BlockEntityType.Builder
                    .of(FloralAltarBlockEntity::new, FRBBlocks.FLORAL_ALTAR.get())
                    .build(null));

    private FRBBlockEntities() {}
}
