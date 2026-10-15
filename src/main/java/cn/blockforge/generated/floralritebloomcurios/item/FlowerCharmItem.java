package cn.blockforge.generated.floralritebloomcurios.item;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmAttributes;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
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
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

/**
 * 花灵饰品基类：固定佩戴于[群花]槽，死亡不掉落，佩戴时飘落对应颜色花瓣。
 * 名字整段上对应属性色（§9/§c/§1/§a/§6/§5/§b），描述按“标题 + 逐行效果”分段展示。
 */
public class FlowerCharmItem extends Item implements ICurioItem {

    public final CharmType charm;

    public FlowerCharmItem(CharmType charm, Item.Properties props) {
        super(props);
        this.charm = charm;
    }

    /** 名字用对应属性色（1.20.1 语言文件不解析 § 码，这里在代码里上色）。 */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withStyle(charm.textColor);
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!entity.level().isClientSide) {
            if (entity instanceof Player p) {
                FRBState.markCollected(p, charm);
            }
            burstPetals(entity);
        }
    }

    public void burstPetals(LivingEntity entity) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        level.sendParticles(FRBParticles.petal(charm),
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.7D, entity.getZ(),
                24, 0.5D, 0.7D, 0.5D, 0.06D);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // 注意：这里绝不能再往 stack 的 NBT 里写每 tick 变化的数据（曾经的 FRBTick 计数器）。
        // curios 槽位同时是玩家容器的槽位，物品 NBT 每 tick 变会让容器
        // 与服务端拷贝永远比对不上，容器每 tick 重发槽位同步，游戏会直接卡死。
        if (slotContext.entity().level().isClientSide) return;
        // 无限增益由 CharmTickEvents 每玩家统一维护，避免多槽位重复扫描。
    }

    /** 使用原版无限时长，来源消失时由统一维护器收回。 */
    public static void holdLivingBuffs(Player p) {
        cn.blockforge.generated.floralritebloomcurios.effect.CharmBuffs.tick(p);
    }

    /** 卸下后若不再有任何森语藤来源，收回再生 I / 挖掘速度 I。 */
    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player p && !p.level().isClientSide) {
            cn.blockforge.generated.floralritebloomcurios.effect.CharmBuffs.tick(p);
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        // r96：蹲下时右键用于切换功能（森语藤催熟 / 寒风铃回溯 / 黑灵草光环），
        // 不触发 Curios 装备逻辑——避免"饰品从手中消失被塞进槽里"的误操作。
        // 玩家可松开蹲右键正常装备，或直接拖入饰品栏。
        return !slotContext.entity().isCrouching();
    }

    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                       int lootingLevel, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }

    @Override
    public boolean canEquip(String identifier, LivingEntity livingEntity, ItemStack stack) {
        // 只进[群花]槽；获得终极饰品后花仪槽仍可佩戴（保留收集展示），但不与终极冲突。
        return FRBState.SLOT_CHARM.equals(identifier) && livingEntity instanceof Player;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext,
                                                                        UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        CharmAttributes.apply(charm, "charm_" + charm.id, map);
        return map;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.UNCOMMON;
    }

    /**
     * 分段描述：标题一行（属性色），效果逐行（§5 紫色），装备后的属性由 Curios 自动列出。
     * 1.20.1 不解析语言文件里的 § 码，必须先取出文本再用 LegacyText 解析，
     * 否则 zh_cn 中写的颜色会原样显示成“§d”字样。未写颜色的部分回落到本行基准色。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        tips.add(LegacyText.parse(Component.translatable("frb.charm." + charm.id + ".title").getString())
                .withStyle(charm.textColor));
        for (int i = 1; i <= charm.descLines; i++) {
            Component line;
            if (charm == CharmType.FROST_CHIME && i == 2) {
                line = Component.translatable("frb.charm.frost_chime.line2");
            } else if (charm == CharmType.SKY_TEAR && i == 2) {
                // 耐久回复量 = 1 + [群花]槽内饰品数，tooltip 按当前查看者实时显示
                int amount = FRBState.repairAmount(
                        cn.blockforge.generated.floralritebloomcurios.util.FRBEnv.clientPlayer());
                line = Component.translatable("frb.charm.sky_tear.line2", amount);
            } else {
                line = Component.translatable("frb.charm." + charm.id + ".line" + i);
            }
            tips.add(LegacyText.parse(line.getString()).withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (stack.hasTag() && stack.getTag().getBoolean(FRBState.TAG_SEALED)) {
            tips.add(Component.translatable("frb.charm.sealed").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
