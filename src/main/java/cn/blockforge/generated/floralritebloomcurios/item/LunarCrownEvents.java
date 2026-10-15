package cn.blockforge.generated.floralritebloomcurios.item;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r74：「月之庇护」佩戴者最终伤害 × 0.80。
 *
 * - LivingHurtEvent 是 vanilla 计算完所有"加法/乘法"伤害系数后的钩子，
 *   在此处拦截 → amount *= FINAL_DAMAGE_MULT 模拟"最终减伤 20%"语义。
 * - r83：改为原版头盔槽（EquipmentSlot.HEAD）实时判定，不再依赖 Curios。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class LunarCrownEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAmount() <= 0.0F) return;
        if (!LunarCrownItem.hasReduction(player)) return;
        event.setAmount(event.getAmount() * LunarCrownItem.FINAL_DAMAGE_MULT);
    }

    private LunarCrownEvents() {}
}