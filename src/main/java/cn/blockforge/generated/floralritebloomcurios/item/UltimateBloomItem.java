package cn.blockforge.generated.floralritebloomcurios.item;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmAttributes;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import cn.blockforge.generated.floralritebloomcurios.util.LegacyText;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

/**
 * 终极饰品【万花万草之灵·祝福与永恒相随】：占用永恒花槽，聚合七花属性，
 * 免疫环境伤害、永久夜视 / 水下呼吸 / 创造飞行，并解锁终末花环。
 * 名字按用户指定的逐字配色渲染（§9万§c花§1万§b草§5之§8灵§6·§3祝…）。
 */
public class UltimateBloomItem extends Item implements ICurioItem {

    public UltimateBloomItem(Item.Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        // 名字文案与逐字配色统一由 zh_cn.json 的 item.frb.eternal_spirit 驱动（§ 码经 LegacyText 解析）
        return LegacyText.parse(Component.translatable("item.frb.eternal_spirit").getString());
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (!slotContext.entity().level().isClientSide && slotContext.entity() instanceof Player p) {
            for (CharmType type : CharmType.ALL) {
                FRBState.markCollected(p, type);
            }
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!entity.level().isClientSide && entity instanceof Player p) {
            // 只在数值真的变化时才写 NBT：已装备饰品的堆叠每 tick 被改动，
            // 容器同步会比对不上并持续重发槽位包，导致游戏卡死（见 FlowerCharmItem 同款注释）。
            int n = FRBState.collectedCount(p);
            int cur = stack.hasTag() ? stack.getTag().getInt("FRBProgress") : -1;
            if (n != cur) {
                stack.getOrCreateTag().putInt("FRBProgress", n);
            }
            // 七灵归一的无限增益由 CharmTickEvents 统一维护。
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player p && !p.level().isClientSide) {
            cn.blockforge.generated.floralritebloomcurios.effect.CharmBuffs.tick(p);
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext,
                                                                        UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        for (CharmType type : CharmType.ALL) {
            CharmAttributes.apply(type, "eternal_" + type.id, map);
        }
        // 终极饰品本身占满七个群花槽位，卸下后 Curios 自动恢复原槽位数量。
        map.put(SlotAttribute.getOrCreate(FRBState.SLOT_CHARM),
                new AttributeModifier(UUID.nameUUIDFromBytes("frb:eternal_flower_slots".getBytes()),
                        "frb_eternal_flower_slots", -7.0D, AttributeModifier.Operation.ADDITION));
        return map;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                       int lootingLevel, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }

    @Override
    public boolean canEquip(String identifier, LivingEntity livingEntity, ItemStack stack) {
        return FRBState.SLOT_ETERNAL.equals(identifier)
                && livingEntity instanceof Player;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }

    /**
     * 描述逐行渲染：1.20.1 不解析语言文件里的 § 码，必须先取出文本再用 LegacyText 解析，
     * 未写颜色的部分回落到该行基准色。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        tips.add(line("frb.eternal.title", ChatFormatting.LIGHT_PURPLE));
        tips.add(line("frb.eternal.line1", ChatFormatting.DARK_PURPLE));
        tips.add(line("frb.eternal.line2", ChatFormatting.DARK_PURPLE));
        tips.add(line("frb.eternal.line3", ChatFormatting.DARK_PURPLE));
        tips.add(line("frb.eternal.line4", ChatFormatting.LIGHT_PURPLE));
        tips.add(line("frb.eternal.acquire", ChatFormatting.DARK_GRAY));
        int n = stack.hasTag() ? stack.getTag().getInt("FRBProgress") : 0;
        tips.add(Component.translatable("frb.progress", n, 7)
                .withStyle(n >= 7 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
    }

    private static Component line(String key, ChatFormatting base) {
        return LegacyText.parse(Component.translatable(key).getString()).withStyle(base);
    }
}
