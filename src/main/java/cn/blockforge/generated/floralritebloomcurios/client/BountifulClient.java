package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.item.BountifulItem;
import cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork;
import cn.blockforge.generated.floralritebloomcurios.network.LockTargetPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 丰饶的左键锁定：左键按下的瞬间做一次准心射线拾取（最前方敌对生物，32 格），
 * 被方块遮挡则不锁定；拾取成功发 C2S 包由服务端确认加锁。点空或按住不改变现有锁定——
 * 锁定无时限，目标死亡或再次点击切换才失效（服务端持有 TAG_LOCK UUID）。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT)
public final class BountifulClient {

    private static final int LOCK_RANGE = 32;
    private static boolean prevMouseDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) {
            prevMouseDown = false;
            return;
        }
        boolean down = mc.options.keyAttack.isDown();
        boolean pressed = down && !prevMouseDown;
        prevMouseDown = down;
        if (!pressed) return;
        if (!(player.getMainHandItem().getItem() instanceof BountifulItem)) return;
        // 点选：仅在左键按下的瞬间重新锁定；点空时保持现有锁定不丢失
        LivingEntity target = pickTarget(player);
        if (target != null) {
            FRBNetwork.CHANNEL.sendToServer(new LockTargetPacket(target.getId()));
        }
    }

    /** 准心射线拾取最前方的敌对生物（含 Boss），考虑方块遮挡。 */
    private static LivingEntity pickTarget(Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(LOCK_RANGE));
        AABB searchBox = player.getBoundingBox()
                .expandTowards(player.getLookAngle().scale(LOCK_RANGE)).inflate(2.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, from, to,
                searchBox, e -> e instanceof LivingEntity living && living instanceof Enemy
                        && living.isAlive() && !e.isSpectator() && e != player, 0.0F);
        if (hit == null) return null;

        // 方块遮挡：射线先碰到方块则视为不可见，不锁定
        BlockHitResult block = player.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS
                && block.getLocation().distanceToSqr(from)
                < hit.getLocation().distanceToSqr(from)) {
            return null;
        }

        Entity entity = hit.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    private BountifulClient() {}
}
