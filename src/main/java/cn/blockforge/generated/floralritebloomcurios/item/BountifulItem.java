package cn.blockforge.generated.floralritebloomcurios.item;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.entity.PetalVolleyEntity;
import cn.blockforge.generated.floralritebloomcurios.util.BountifulRites;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import cn.blockforge.generated.floralritebloomcurios.util.LegacyText;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 占位武器「丰饶」（后续替换为正式 3D 模型「巨型草茎」）：
 * 手持时生命恢复加速 + 护甲/韧性 +4；按住右键从身侧左右交替散射花瓣 / 叶片弹（自动追踪）；
 * 左键准心拾取最前方敌对生物锁定为优先目标（见 BountifulClient / BountifulEvents）。
 */
public class BountifulItem extends Item implements software.bernie.geckolib.animatable.GeoItem {

    /** 锁定目标存放在玩家 persistentData 的 UUID 键。 */
    public static final String TAG_LOCK = "frb_locked_target";

    /** GeckoLib 动画缓存（当前模型无动画，仅静态姿态）。 */
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache cache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        // 纯静态模型：无控制器、无动画
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /** 3D 模型渲染器：GeckoLib GeoItemRenderer（见 BountifulGeoModel），与铁魔法武器同一渲染链。 */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private software.bernie.geckolib.renderer.GeoItemRenderer<BountifulItem> renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new software.bernie.geckolib.renderer.GeoItemRenderer<>(
                            new cn.blockforge.generated.floralritebloomcurios.client.BountifulGeoModel());
                }
                return this.renderer;
            }
        });
    }

    private static final UUID ARMOR_UUID = UUID.nameUUIDFromBytes("frb:bountiful_armor".getBytes());
    private static final UUID TOUGHNESS_UUID = UUID.nameUUIDFromBytes("frb:bountiful_toughness".getBytes());
    /** 每 5 tick 一发（花瓣/叶片交替）。 */
    private static final int VOLLEY_INTERVAL = 5;

    /** 手持属性（主手槽位）：+4 护甲 / +4 韧性，由原版属性系统施加，tooltip 自动显示在属性区。 */
    private final Multimap<Attribute, AttributeModifier> heldAttributes = ImmutableMultimap.of(
            Attributes.ARMOR, new AttributeModifier(ARMOR_UUID, "frb_bountiful_armor", 4.0D,
                    AttributeModifier.Operation.ADDITION),
            Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TOUGHNESS_UUID, "frb_bountiful_toughness", 4.0D,
                    AttributeModifier.Operation.ADDITION));

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return slot == EquipmentSlot.MAINHAND ? heldAttributes : ImmutableMultimap.of();
    }

    public BountifulItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        // 绿色名字：lang 键 + LegacyText（1.20.1 语言文件不解析 § 码）；锻造等级以金色 +N 追加
        Component name = LegacyText.parse(Component.translatable("item.frb.bountiful").getString())
                .withStyle(ChatFormatting.GREEN);
        int lv = BountifulRites.level(stack);
        if (lv > 0) {
            return name.copy().append(Component.literal(" +" + lv).withStyle(ChatFormatting.GOLD));
        }
        return name;
    }

    // ---- 手持再生（服务端 BountifulEvents 每 tick 调用；护甲/韧性走 getAttributeModifiers） ----

    public static void tickHeld(ServerPlayer player) {
        boolean held = player.getMainHandItem().getItem() instanceof BountifulItem
                || player.getOffhandItem().getItem() instanceof BountifulItem;
        if (held && player.isAlive() && player.tickCount % 40 == 0) {
            // 70 tick 的再生 I，每 40 tick 刷新 → 常驻不断档
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 70, 0, false, true, false));
        }
    }

    // ---- 右键按住：持续散射 ----

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int elapsed = getUseDuration(stack) - remaining;
        if (elapsed <= 0 || level.isClientSide || !(user instanceof Player player)) return;
        // 花灵联动：佩戴（未封印）的饰品越多，射速越快（单发快射）
        int count = activeCharmCount(player);
        int interval = Math.max(2, VOLLEY_INTERVAL - count / 2);
        if (elapsed % interval != 0) return;
        int shot = elapsed / interval;
        // 单发快射：每次只发一枚（花瓣/叶子交替），饰品越多射速越快
        boolean leaf = shot % 2 == 1;
        // 发射点：玩家周身上半球随机（仰角 15°~75°、半径 0.5~0.9），花瓣从身周任意方向射向目标
        RandomSource r = level.getRandom();
        double theta = r.nextDouble() * Math.PI * 2.0D;
        double elev = Math.toRadians(15.0D + r.nextDouble() * 60.0D);
        double radius = 0.5D + r.nextDouble() * 0.4D;
        Vec3 spawn = player.getEyePosition().add(new Vec3(
                Math.cos(theta) * Math.cos(elev),
                Math.sin(elev),
                Math.sin(theta) * Math.cos(elev)).scale(radius));
        // 基准方向：锁定目标优先，否则朝视线方向
        Vec3 look = player.getLookAngle();
        Vec3 base = look;
        if (player.getPersistentData().hasUUID(TAG_LOCK)) {
            Entity locked = ((ServerLevel) level)
                    .getEntity(player.getPersistentData().getUUID(TAG_LOCK));
            if (locked != null && locked.isAlive()) {
                base = locked.getBoundingBox().getCenter().subtract(spawn);
            }
        }
        Vec3 dir = new Vec3(
                base.x + level.random.triangle(0.0D, 0.04D),
                base.y + level.random.triangle(0.0D, 0.04D),
                base.z + level.random.triangle(0.0D, 0.04D)).normalize();
        // 伤害 =（基础 4 + 玩家攻击力）× 花仪铭记倍率（每仪 +10%）
        float attack = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (PetalVolleyEntity.BASE_DAMAGE + attack) * BountifulRites.damageMultiplier(stack);
        PetalVolleyEntity.shoot((ServerLevel) level, player, spawn, dir, leaf, damage);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.BOUNTIFUL_SHOOT.get(),
                SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    /** 未被封印、正在生效的花灵饰品数量（终极饰品视为全部生效，上限 7）。 */
    public static int activeCharmCount(Player player) {
        int n = 0;
        for (CharmType type : CharmType.ALL) {
            if (FRBState.active(player, type)) n++;
        }
        return n;
    }

    // ---- Tooltip ----

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("frb.bountiful.line1").withStyle(ChatFormatting.GREEN));
        tips.add(Component.translatable("frb.bountiful.line2").withStyle(ChatFormatting.GRAY));
        tips.add(Component.translatable("frb.bountiful.line3").withStyle(ChatFormatting.GRAY));
        // 封印态：新凝成的丰饶需用「世界之花的意志」解封后才能印刻
        if (!BountifulRites.isUnsealed(stack)) {
            tips.add(Component.translatable("frb.bountiful.sealed").withStyle(ChatFormatting.GOLD));
        }
        // 花仪铭记：七仪灰色待铭，完成的转为对应花灵颜色
        tips.add(Component.translatable("frb.bountiful.rites.title",
                String.valueOf(BountifulRites.level(stack))).withStyle(ChatFormatting.DARK_GRAY));
        for (CharmType type : CharmType.ALL) {
            if (BountifulRites.has(stack, type)) {
                tips.add(Component.literal("❀ ").withStyle(type.textColor)
                        .append(Component.translatable("frb.rite." + type.id).withStyle(type.textColor))
                        .append(Component.literal(" ✦").withStyle(type.textColor)));
            } else {
                tips.add(Component.literal("◇ ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.translatable("frb.rite." + type.id)
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            }
        }
    }
}
