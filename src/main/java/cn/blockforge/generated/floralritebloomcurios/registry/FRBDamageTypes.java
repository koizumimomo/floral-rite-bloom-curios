package cn.blockforge.generated.floralritebloomcurios.registry;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * 本模组的伤害类型（DamageType 走 datapack 数据注册：data/&lt;id&gt;/damage_type/*.json）。
 * 花灵伤害（floral）：丰饶弹幕的专属伤害，通过 minecraft:bypasses_cooldown tag
 * 绕过原版受击无敌帧（invulnerableTime>10 且伤害≤上一击时整发吞伤），高频弹幕每发全额结算。
 */
public final class FRBDamageTypes {

    public static final ResourceKey<DamageType> FLORAL =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(FloralRiteBloom.MOD_ID, "floral"));

    private FRBDamageTypes() {}
}
