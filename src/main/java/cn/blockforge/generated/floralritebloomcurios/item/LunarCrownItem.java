package cn.blockforge.generated.floralritebloomcurios.item;

import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.util.LegacyText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * r83：「月之庇护」金色头冠——改为原版头盔（ArmorItem / EquipmentSlot.HEAD）。
 * （r74~r82 曾走 Curios head 槽，r83 起不再依赖 Curios。）
 *
 * - +6 护甲 / +5 盔甲韧性（由 ArmorMaterial 提供，原版属性栏自动显示）
 * - 最终伤害 × 0.80（LivingHurtEvent 拦截，见 LunarCrownEvents；判定=头盔槽是否为本物品）
 *
 * 名字「月之庇护」以金色（§6）渲染，由 lang 驱动 + LegacyText 解析。
 * 渲染：物品栏/手持/地面走 BEWLR（GeoItemRenderer）；
 *       戴在头上走 IClientItemExtensions#getGenericArmorModel（GeoArmorRenderer，
 *       geo 骨骼名必须为 "head" 才会跟随头部）。
 */
public class LunarCrownItem extends ArmorItem implements GeoItem {

    /** 佩戴月之庇护的玩家受到的最终伤害 × 0.80。EventBus 由 LunarCrownEvents 挂接。 */
    public static final float FINAL_DAMAGE_MULT = 0.80F;

    /** GeckoLib 实例缓存（本物品无动画，纯静态）。 */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 自定义盔甲材料：6 护甲 / 5 韧性（仅头盔使用；其余部位数值无意义）。 */
    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            // 原版公式：部位基准耐久 × 材料倍率（头盔基准 11，下界合金倍率 37）
            return 11 * 37;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return type == ArmorItem.Type.HELMET ? 6 : 2;
        }

        @Override
        public int getEnchantmentValue() {
            return 15;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "floral_rite_bloom_curios:lunar_crown";
        }

        @Override
        public float getToughness() {
            return 5.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    public LunarCrownItem(Properties props) {
        super(MATERIAL, Type.HELMET, props);
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 静态姿态模型，跳过
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private software.bernie.geckolib.renderer.GeoItemRenderer<LunarCrownItem> itemRenderer;
            private GeoArmorRenderer<LunarCrownItem> armorRenderer;

            /** 物品栏 / 手持 / 地面：BEWLR 3D 模型（r88：子类化以支持 gui display 抬高缩放）。 */
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new cn.blockforge.generated.floralritebloomcurios.client.LunarCrownItemRenderer();
                }
                return this.itemRenderer;
            }

            /** 戴在头上：GeckoLib 盔甲渲染（geo 骨骼名 "head" 绑定头部）。 */
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                    net.minecraft.client.model.HumanoidModel<?> original) {
                if (this.armorRenderer == null) {
                    this.armorRenderer = new GeoArmorRenderer<>(
                            new cn.blockforge.generated.floralritebloomcurios.client.LunarCrownGeoModel());
                }
                // r88 崩溃修复：GeoArmorRenderer 内部 currentEntity/currentStack/currentSlot
                // 必须在每次渲染前由 prepForRender 注入，否则为 null。平时世界渲染因
                // LevelRenderer#shouldShowEntityOutlines()=false 侥幸绕过 NPE；创造物品栏用
                // RenderSystem.runAsFancy 渲染玩家模型时轮廓检测为 true →
                // Minecraft.shouldEntityAppearGlowing(null) NPE 闪退。
                this.armorRenderer.prepForRender(entity, stack, slot, original);
                return this.armorRenderer;
            }
        });
    }

    // ---- 名字 / Tooltip ----

    @Override
    public Component getName(ItemStack stack) {
        // lang 键 item.frb.lunar_crown（zh_cn: "月之庇护" + §6 上色；en_us 英文）
        return LegacyText.parse(Component.translatable("item.frb.lunar_crown").getString())
                .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tips, TooltipFlag flag) {
        // 标题（金色）+ 属性区（原版自动渲染 +6 护甲 / +5 韧性）+ 减伤行。
        // r83：删掉"佩戴在头饰槽位"与"死亡不掉落"描述。
        tips.add(LegacyText.parse(Component.translatable("frb.lunar_crown.title").getString())
                .withStyle(ChatFormatting.GOLD));
        tips.add(LegacyText.parse("§a+20% " + Component.translatable(
                "attribute.modifier.floral_rite_bloom_curios.lunar_crown_reduction").getString()));
        // 实际最终减伤由 LunarCrownEvents.onLivingHurt 拦截，amount *= 0.80
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    /** 检测玩家头盔槽是否装着月之庇护（原版盔甲栏，客户端服务端通用）。 */
    public static boolean hasReduction(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(FRBItems.LUNAR_CROWN.get());
    }
}
