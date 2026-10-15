package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 落花：从花之摇篮法阵上空 8 格处飘落，撞地结算。
 *
 * 落地判定：
 * - if 法阵范围（半径 1.5）内有玩家 → 全员获得【花灵之助】（+20% 攻击）持续 60s
 * - else 法阵范围内没有玩家 → 对领域内非 boss 玩家造成 8 点魔法伤害
 */
public class FlowerCradleSigilFlowerEntity extends MechanicEntity {

    private final FlowerCradleSigilEntity sigil;
    private double targetY;

    public FlowerCradleSigilFlowerEntity(EntityType<? extends FlowerCradleSigilFlowerEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
        this.noPhysics = true; // 飞行过程中不与方块碰撞
        this.sigil = null;
    }

    private FlowerCradleSigilFlowerEntity(EntityType<? extends FlowerCradleSigilFlowerEntity> type,
                                          Level level, FlowerCradleSigilEntity sigil) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.sigil = sigil;
    }

    public static void spawn(ServerLevel level, double x, double y, double z, FlowerCradleSigilEntity sigil) {
        FlowerCradleSigilFlowerEntity e = new FlowerCradleSigilFlowerEntity(
                FRBEntities.FLOWER_CRADLE_SIGIL_FLOWER.get(), level, sigil);
        e.setPos(x, y, z);
        e.targetY = sigil.getY() + 0.1D;
        level.addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        // 缓慢飘下：每 tick -0.18 格
        double y = this.getY();
        if (y > this.targetY) {
            this.setPos(this.getX(), y - 0.18D, this.getZ());
            return;
        }
        // 落地：结算
        settle((ServerLevel) this.level());
        this.discard();
    }

    private void settle(ServerLevel level) {
        if (this.sigil == null) return;
        // 找 sigil 范围内的玩家
        boolean anyPlayerOnSigil = !level.getEntitiesOfClass(Player.class,
                this.sigil.getBoundingBox().inflate(this.sigil.radius()), Player::isAlive).isEmpty();
        if (anyPlayerOnSigil) {
            // 给予花灵之助
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                    this.sigil.getBoundingBox().inflate(this.sigil.radius()))) {
                p.addEffect(new MobEffectInstance(FRBEffects.FLORAL_SPIRIT_AID.get(), 1200, 0));
            }
            level.playSound(null, this.sigil.getX(), this.sigil.getY(), this.sigil.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 1.2F, 1.4F);
        } else {
            // 惩罚：8 点魔法伤害（领域内非 boss 玩家）
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                    this.sigil.getBoundingBox().inflate(64.0D), Player::isAlive)) {
                p.hurt(level.damageSources().magic(), 8.0F);
            }
            level.playSound(null, this.sigil.getX(), this.sigil.getY(), this.sigil.getZ(),
                    SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 1.0F, 1.6F);
        }
        // r69：结算完成后回收法阵（时序：法阵保持到落花落地才消失）
        this.sigil.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}