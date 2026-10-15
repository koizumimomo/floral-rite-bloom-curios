package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 花之摇篮（B1 自然阶段）：地面法阵实体。
 *
 * 流程（参见 Mechanics#flowerCradle）：
 * 1. boss 在目标玩家脚下生成 sigil（持续 3 秒）
 * 2. 3 秒后对应花从天上落到 sigil 中心（FlowerCradleSigilFlowerEntity）
 * 3. 落花命中时若 sigil 上有玩家 → 给予【花灵之助】buff（+20% 攻击）
 *    若没有任何玩家在 sigil 范围内 → 对领域内非 boss 玩家造成 8 点魔法伤害
 *
 * A5：法阵属于机制实体，由 MechanicEntity 提供效果免疫。
 */
public class FlowerCradleSigilEntity extends MechanicEntity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(FlowerCradleSigilEntity.class, EntityDataSerializers.INT);

    /** 持续时间：3 秒（60 tick）。 */
    public static final int LIFETIME = 60;

    /** r69：落花飘落约 44 tick，法阵保留到落花落地由落花回收；-100 tick 兜底强制消失。 */
    private static final int GRACE_TICKS = 100;

    public FlowerCradleSigilEntity(EntityType<? extends FlowerCradleSigilEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, BlockPos ground, Player target) {
        FlowerCradleSigilEntity e = new FlowerCradleSigilEntity(
                FRBEntities.FLOWER_CRADLE_SIGIL.get(), level);
        e.setPos(ground.getX() + 0.5D, ground.getY() + 0.05D, ground.getZ() + 0.5D);
        e.entityData.set(DATA_LIFETIME, LIFETIME);
        level.addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        int left = this.entityData.get(DATA_LIFETIME) - 1;
        this.entityData.set(DATA_LIFETIME, left);
        // 寿命到：生成落花。r69 修复时序——法阵先保持展示，等落花落地结算时
        // 由落花回调 discard()（此前同 tick discard 导致"法阵消失后才出现落花"）。
        // r92：spawnWarn 已删除（荆棘/冰刺改用 AoeWarnSigilEntity），
        //      本实体永远承担完整 B1 机制（生成落花 + 结算）。
        if (left == 0) {
            cn.blockforge.generated.floralritebloomcurios.entity.FlowerCradleSigilFlowerEntity.spawn(
                    (ServerLevel) this.level(), this.getX(), this.getY() + 8.0D, this.getZ(), this);
        }
        // 兜底：落花异常丢失时强制消失，防止法阵残留
        if (left <= -GRACE_TICKS) {
            this.discard();
        }
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFETIME);
    }

    /** sigil 半径：r69 扩大到 2.0 格（覆盖约 3x3 格区域的判定）。 */
    public double radius() {
        return 2.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_LIFETIME, tag.getInt("FRBLife"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FRBLife", this.entityData.get(DATA_LIFETIME));
    }
}