package cn.blockforge.generated.floralritebloomcurios.abilities;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 星界花：短距传送（按键 G）；与天穹花协同 —— 传送后触发风爆并获得短暂无敌。
 */
public final class AstralBlink {
    public static final long BLINK_COOLDOWN = 30L * 20L;

    /** r96：返回是否真正传送成功（供服务端决定是否下发冷却 HUD）。 */
    public static boolean blink(ServerPlayer player) {
        if (!FRBState.active(player, CharmType.ASTRAL_BLOOM)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (!FRBState.useCooldown(player, "BlinkCd", level.getGameTime(), BLINK_COOLDOWN)) {
            player.displayClientMessage(Component.translatable("frb.blink.cd",
                            FRBState.cooldownLeft(player, "BlinkCd", level.getGameTime()))
                    .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(18.0D));
        Vec3 dest = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation();
        double tx = dest.x - look.x * 0.8D;
        double ty = dest.y - look.y * 0.8D;
        double tz = dest.z - look.z * 0.8D;
        BlockPos feet = findSafeFloor(level, tx, ty, tz);
        if (feet == null) {
            player.displayClientMessage(Component.translatable("frb.blink.fail")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        doTeleport(player, feet.getX() + 0.5D, feet.getY() + 0.05D, feet.getZ() + 0.5D);
        return true;
    }

    private static void doTeleport(ServerPlayer player, double x, double y, double z) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(),
                30, 0.4D, 0.9D, 0.4D, 0.12D);
        player.teleportTo(x, y, z);
        level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0D, z, 30, 0.4D, 0.9D, 0.4D, 0.12D);
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.3F);
        // 虚空 + 天空协同：传送后风爆与短暂无敌
        if (FRBState.active(player, CharmType.ASTRAL_BLOOM)
                && FRBState.active(player, CharmType.SKY_PILLAR)) {
            windBurst(player, x, y, z);
            player.invulnerableTime = Math.max(player.invulnerableTime, 40);
        }
    }

    private static void windBurst(ServerPlayer player, double x, double y, double z) {
        ServerLevel level = player.serverLevel();
        AABB box = new AABB(x - 3.5D, y - 1.0D, z - 3.5D, x + 3.5D, y + 3.0D, z + 3.5D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && e instanceof Enemy)) {
            double dx = target.getX() - x;
            double dz = target.getZ() - z;
            double len = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
            target.setDeltaMovement(dx / len * 1.4D, 0.8D, dz / len * 1.4D);
            target.hurtMarked = true;
            target.hurt(level.damageSources().playerAttack(player), 2.0F);
        }
        level.sendParticles(ParticleTypes.POOF, x, y + 1.0D, z, 12, 0.8D, 0.6D, 0.8D, 0.0D);
        level.sendParticles(FRBParticles.petal(CharmType.SKY_PILLAR), x, y + 1.2D, z,
                16, 1.0D, 0.8D, 1.0D, 0.05D);
    }

    /** 向上寻找一个可站立的落点（3 格以内）。 */
    private static BlockPos findSafeFloor(ServerLevel level, double x, double y, double z) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos feet = BlockPos.containing(x, Math.floor(y) + dy, z);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                    && !level.getBlockState(feet.below()).isAir()) {
                return feet;
            }
        }
        return null;
    }

    private AstralBlink() {}
}
