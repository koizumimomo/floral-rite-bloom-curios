package cn.blockforge.generated.floralritebloomcurios.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 花仪台座：台面上放一份仪式供品（单个堆叠），供品随方块实体同步给客户端用于渲染。
 */
public class FloralAltarBlockEntity extends BlockEntity {

    private ItemStack offering = ItemStack.EMPTY;

    public FloralAltarBlockEntity(BlockPos pos, BlockState state) {
        super(cn.blockforge.generated.floralritebloomcurios.registry.FRBBlockEntities.FLORAL_ALTAR.get(), pos, state);
    }

    public ItemStack getOffering() {
        return offering;
    }

    public boolean isEmpty() {
        return offering.isEmpty();
    }

    public void setOffering(ItemStack stack) {
        this.offering = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        setChanged();
    }

    /** ISS Pedestal 模式：setChanged 自动发 BlockEntityData 同步包。 */
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!offering.isEmpty()) {
            tag.put("Offering", offering.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 空同步包也必须覆盖旧状态，否则客户端会继续渲染已经取走的供品。
        offering = tag.contains("Offering") ? ItemStack.of(tag.getCompound("Offering")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
        // ISS Pedestal：客户端收到更新后额外触发一次重绘，避免旧物品残影
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
