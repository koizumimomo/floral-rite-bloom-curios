package cn.blockforge.generated.floralritebloomcurios.ritual;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.item.BountifulItem;
import cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import cn.blockforge.generated.floralritebloomcurios.util.BountifulRites;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 仪式执行：匹配条件 → 消耗供品 → 凝结出对应的花灵种子；
 * 手持丰饶右键祭坛则走花仪锻造：按七仪顺序铭刻（每种各一次），附带升腾粒子动画。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID)
public final class RitualManager {
    private static final java.util.Map<java.util.UUID, PendingRitual> PENDING = new java.util.HashMap<>();
    private static final int RITUAL_TICKS = 40;

    /** 待结算仪式类型：SEED=凝结花灵种子 / CRAFT=丰饶凝结 / UNSEAL=丰饶解封 / FORGE=花灵印刻
     *  / LUNAR_PRIMROSE=月见草种子 / LUNAR_SHARD=月之水晶 / LUNAR_CROWN=月之庇护。 */
    private enum Kind { SEED, CRAFT, UNSEAL, FORGE, LUNAR_PRIMROSE, LUNAR_SHARD, LUNAR_CROWN }

    private record PendingRitual(net.minecraft.core.BlockPos altarPos, CharmType type, long finishAt,
                                 int ticks, Kind kind) {}

    /** 尝试以手持的无相花种举行仪式（供品优先取周围花仪台座）。返回 true 表示成功。 */
    public static boolean attempt(ServerLevel level, BlockPos altarPos, Player player) {
        Offerings src = Offerings.at(level, altarPos, player);
        // 丰饶合成：材料定义见 RitualRecipes.bountifulCraft()（r84 单一数据源）
        RitualRecipes.BountyDef craft = RitualRecipes.bountifulCraft();
        if (hasAll(src, craft.costs()) && hasAnyCharm(src)) {
            return succeedBountiful(level, altarPos, player, src);
        }
        for (CharmType type : CharmType.ALL) {
            Rituals.Def def = Rituals.DEFS.get(type);
            if (Rituals.check(level, altarPos, def, src) == null) {
                return succeed(level, altarPos, player, def, src);
            }
        }
        return false;
    }

    /** 创造模式祭坛：只要求已有供品，不受环境、高度和群系影响。 */
    public static boolean attemptCreative(ServerLevel level, BlockPos altarPos, Player player) {
        Offerings src = Offerings.of(player);
        RitualRecipes.BountyDef craft = RitualRecipes.bountifulCraft();
        if (hasAll(src, craft.costs()) && hasAnyCharm(src)) {
            return succeedBountiful(level, altarPos, player, src);
        }
        for (CharmType type : CharmType.ALL) {
            Rituals.Def def = Rituals.DEFS.get(type);
            if (Rituals.checkCreative(def, src) == null) {
                return succeed(level, altarPos, player, def, src);
            }
        }
        return false;
    }

    /** r84：供品是否满足全部固定材料（供 RitualRecipes 消费方使用）。 */
    private static boolean hasAll(Offerings src, List<Rituals.Cost> costs) {
        for (Rituals.Cost cost : costs) {
            if (src.count(cost.item()) < cost.count()) return false;
        }
        return true;
    }

    private static boolean hasAnyCharm(Offerings src) {
        for (CharmType type : CharmType.ALL) {
            if (src.count(FRBItems.charmOf(type)) >= 1) return true;
        }
        return false;
    }

    /** 丰饶凝结：消耗 RitualRecipes.bountifulCraft() 定义的供品，40 tick 后产出封印的丰饶。 */
    private static boolean succeedBountiful(ServerLevel level, BlockPos altarPos, Player player, Offerings src) {
        RitualRecipes.BountyDef craft = RitualRecipes.bountifulCraft();
        for (Rituals.Cost cost : craft.costs()) {
            src.consume(cost.item(), cost.count());
        }
        for (CharmType type : CharmType.ALL) {
            Item charm = FRBItems.charmOf(type);
            if (src.count(charm) >= 1) {
                src.consume(charm, 1);
                break;
            }
        }
        PENDING.put(player.getUUID(), new PendingRitual(altarPos.immutable(), null,
                level.getGameTime() + RITUAL_TICKS, RITUAL_TICKS, Kind.CRAFT));
        level.playSound(null, altarPos, SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 1.0F, 0.6F);
        FRBNetwork.sendFormationToTracking(level, altarPos, 0);
        player.displayClientMessage(Component.translatable("frb.bountiful_craft.start")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    /** 按指定花灵类型尝试（供 /frb ritual 或提示用），返回失败原因键或 null。 */
    public static String checkFor(ServerLevel level, BlockPos altarPos, Player player, CharmType type) {
        return Rituals.check(level, altarPos, Rituals.DEFS.get(type), Offerings.at(level, altarPos, player));
    }

    // ---- 花仪锻造：手持丰饶右键祭坛，七仪各印刻一次 ----

    /**
     * 手持丰饶右键花仪祭坛：封印态时优先解封（消耗世界之花意志，创造祭坛免消耗）；
     * 已解封则按七仪顺序匹配第一个满足的仪式（环境 + 祭品 + 对应花灵×1），
     * 消耗后 40 tick 印刻进丰饶（每种花灵仪式各一次，防重复强化）。
     *
     * @param creative 创造祭坛：跳过环境检查与解封消耗
     */
    public static boolean attemptForge(ServerLevel level, BlockPos altarPos, Player player, boolean creative) {
        ItemStack bountiful = player.getMainHandItem();
        Offerings src = Offerings.at(level, altarPos, player);
        // 封印态 → 解封分支：需要「世界之花的意志」×1
        if (!BountifulRites.isUnsealed(bountiful)) {
            if (!creative && src.count(FRBItems.WORLD_FLOWER_WILL.get()) < 1) {
                player.displayClientMessage(Component.translatable("frb.unseal.need")
                        .withStyle(ChatFormatting.RED), true);
                return false;
            }
            if (!creative) {
                src.consume(FRBItems.WORLD_FLOWER_WILL.get(), 1);
            }
            PENDING.put(player.getUUID(), new PendingRitual(altarPos.immutable(), null,
                    level.getGameTime() + RITUAL_TICKS, RITUAL_TICKS, Kind.UNSEAL));
            level.playSound(null, altarPos, SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 1.0F, 0.6F);
            FRBNetwork.sendFormationToTracking(level, altarPos, 0);
            player.displayClientMessage(Component.translatable("frb.unseal.start")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return true;
        }
        String firstFail = null;
        for (CharmType type : CharmType.ALL) {
            if (BountifulRites.has(bountiful, type)) continue; // 该仪已印刻 → 防逃课核心
            Rituals.Def def = Rituals.DEFS.get(type);
            // 印刻消耗：该仪式的祭品 + 对应的花灵饰品（种出来收获的那件：霜华吃寒风铃、枯萎吃黑灵草……）
            Item catalyst = FRBItems.charmOf(type);
            String fail = creative ? null : Rituals.checkEnvironment(level, altarPos, def);
            if (fail == null) {
                // 创造祭坛：跳过环境与祭品，只消耗对应花灵饰品，方便测试伤害
                boolean ok = creative || src.count(catalyst) >= 1;
                if (ok && !creative) {
                    for (Rituals.Cost cost : def.costs()) {
                        if (src.count(cost.item()) < cost.count()) {
                            ok = false;
                            break;
                        }
                    }
                }
                if (ok) {
                    src.consume(catalyst, 1);
                    if (!creative) {
                        for (Rituals.Cost cost : def.costs()) {
                            src.consume(cost.item(), cost.count());
                            if (cost.returnBucket()) {
                                ItemStack bucket = new ItemStack(net.minecraft.world.item.Items.BUCKET);
                                if (!player.getInventory().add(bucket)) player.drop(bucket, false);
                            }
                        }
                    }
                    long finishAt = level.getGameTime() + RITUAL_TICKS;
                    PENDING.put(player.getUUID(), new PendingRitual(altarPos.immutable(), type, finishAt, RITUAL_TICKS, Kind.FORGE));
                    level.playSound(null, altarPos, SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 1.0F, 0.6F);
                    // 地面法阵 + 客户端氛围动画（与花灵仪式共用）
                    FRBNetwork.sendFormationToTracking(level, altarPos, 0);
                    player.displayClientMessage(Component.translatable("frb.forge.start",
                            Component.translatable("frb.rite." + type.id)).withStyle(type.textColor), true);
                    return true;
                }
                fail = "frb.ritual.fail.cost";
            }
            if (firstFail == null) firstFail = fail;
        }
        player.displayClientMessage(Component.translatable(firstFail != null ? firstFail : "frb.forge.full")
                .withStyle(ChatFormatting.RED), true);
        return false;
    }

    /** 锻造过程动画：前 2/3 双旋臂从祭坛四周升起收拢，后 1/3 化作流光飞向玩家。 */
    private static void forgeTickParticles(ServerLevel level, PendingRitual pending, ServerPlayer player, long now) {
        double t = 1.0D - (pending.finishAt() - now) / (double) RITUAL_TICKS;
        // 解封（type=null）用星点，印刻用对应花灵的花色微粒
        net.minecraft.core.particles.ParticleOptions mote = pending.type() == null
                ? net.minecraft.core.particles.ParticleTypes.END_ROD
                : FRBParticles.moteOf(pending.type());
        double cx = pending.altarPos().getX() + 0.5D;
        double cy = pending.altarPos().getY() + 0.8D;
        double cz = pending.altarPos().getZ() + 0.5D;
        if (t < 0.66D || player == null) {
            double prog = Math.min(1.0D, t / 0.66D);
            for (int arm = 0; arm < 2; arm++) {
                double ang = now * 0.45D + arm * Math.PI;
                double r = 1.7D * (1.0D - prog) + 0.3D;
                level.sendParticles(mote,
                        cx + Math.cos(ang) * r, cy + prog * 1.8D, cz + Math.sin(ang) * r,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        } else {
            double p = (t - 0.66D) / 0.34D;
            double sy = cy + 1.8D;
            double px = player.getX();
            double py = player.getY() + 1.0D;
            double pz = player.getZ();
            double x = cx + (px - cx) * p;
            double y = sy + (py - sy) * p + Math.sin(p * Math.PI) * 0.9D;
            double z = cz + (pz - cz) * p;
            level.sendParticles(mote, x, y, z, 2, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    /** 解封结算：解开玩家身上第一件封印的丰饶（主手优先），同时消耗 1 个「世界之花的意志」，粒子爆发 + 双音效 + 提示。 */
    private static void finishUnseal(ServerLevel level, PendingRitual pending, ServerPlayer player) {
        ItemStack target = ItemStack.EMPTY;
        if (player.getMainHandItem().getItem() instanceof BountifulItem
                && !BountifulRites.isUnsealed(player.getMainHandItem())) {
            target = player.getMainHandItem();
        } else {
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize() && target.isEmpty(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.getItem() instanceof BountifulItem && !BountifulRites.isUnsealed(s)) {
                    target = s;
                }
            }
        }
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        if (target.isEmpty()) {
            player.displayClientMessage(Component.translatable("frb.unseal.missing")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        // r72：消耗「世界之花的意志」（主手优先；副手次之；背包最后）。
        // 服务器端原子性扣除，避免多人场景双扣。
        ItemStack willStack = ItemStack.EMPTY;
        if (player.getMainHandItem().is(cn.blockforge.generated.floralritebloomcurios.registry.FRBItems.WORLD_FLOWER_WILL.get())) {
            willStack = player.getMainHandItem();
        } else if (player.getOffhandItem().is(cn.blockforge.generated.floralritebloomcurios.registry.FRBItems.WORLD_FLOWER_WILL.get())) {
            willStack = player.getOffhandItem();
        } else {
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize() && willStack.isEmpty(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(cn.blockforge.generated.floralritebloomcurios.registry.FRBItems.WORLD_FLOWER_WILL.get())) {
                    willStack = s;
                }
            }
        }
        if (willStack.isEmpty()) {
            player.displayClientMessage(Component.translatable("frb.unseal.need_will")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        willStack.shrink(1);
        BountifulRites.unseal(target);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                px, py + 1.0D, pz, 30, 0.4D, 0.7D, 0.4D, 0.05D);
        level.sendParticles(FRBParticles.INTRO_STAR.get(), px, py + 1.0D, pz,
                14, 0.3D, 0.5D, 0.3D, 0.02D);
        level.playSound(null, px, py, pz,
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.RITUAL_COMPLETE.get(),
                SoundSource.MASTER, 0.9F, 1.2F);
        level.playSound(null, px, py, pz, SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.7F, 1.4F);
        player.displayClientMessage(Component.translatable("frb.unseal.done")
                .withStyle(ChatFormatting.GOLD), false);
    }

    /** 锻造结算：把印刻写进玩家身上的丰饶（主手优先），粒子爆发 + 双音效 + 提示。 */
    private static void finishForge(ServerLevel level, PendingRitual pending, ServerPlayer player) {
        ItemStack target = ItemStack.EMPTY;
        if (player.getMainHandItem().getItem() instanceof BountifulItem
                && !BountifulRites.has(player.getMainHandItem(), pending.type())) {
            target = player.getMainHandItem();
        } else {
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize() && target.isEmpty(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.getItem() instanceof BountifulItem && !BountifulRites.has(s, pending.type())) {
                    target = s;
                }
            }
        }
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        if (target.isEmpty()) {
            player.displayClientMessage(Component.translatable("frb.forge.missing")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        BountifulRites.mark(target, pending.type());
        int lv = BountifulRites.level(target);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                px, py + 1.0D, pz, 24, 0.4D, 0.6D, 0.4D, 0.05D);
        level.sendParticles(FRBParticles.moteOf(pending.type()), px, py + 1.0D, pz, 12, 0.3D, 0.5D, 0.3D, 0.02D);
        level.playSound(null, px, py, pz,
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.RITUAL_COMPLETE.get(),
                SoundSource.MASTER, 0.9F, 1.4F);
        level.playSound(null, px, py, pz, SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.7F, 1.2F);
        player.displayClientMessage(Component.translatable("frb.forge.done",
                        Component.translatable("frb.rite." + pending.type().id),
                        String.valueOf(lv), String.valueOf(lv * 10))
                .withStyle(pending.type().textColor), false);
    }

    private static boolean succeed(ServerLevel level, BlockPos altarPos, Player player,
                                   Rituals.Def def, Offerings src) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        if (PENDING.containsKey(serverPlayer.getUUID())) return false;
        if (!src.consume(FRBItems.FORMLESS_SEED.get(), 1)) return false;
        if (!src.consume(FRBItems.PRIMORDIAL_PETAL.get(), 2)) return false;
        for (Rituals.Cost cost : def.costs()) {
            if (!src.consume(cost.item(), cost.count())) return false;
            if (cost.returnBucket()) {
                ItemStack bucket = new ItemStack(net.minecraft.world.item.Items.BUCKET);
                if (!player.getInventory().add(bucket)) player.drop(bucket, false);
            }
        }

        long finishAt = level.getGameTime() + RITUAL_TICKS;
        PENDING.put(serverPlayer.getUUID(), new PendingRitual(altarPos.immutable(), def.type(), finishAt, RITUAL_TICKS, Kind.SEED));
        level.playSound(null, altarPos, SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 1.2F, 1.4F);
        level.playSound(null, altarPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.MASTER, 1.0F, 0.9F);
        // 客户端法阵动画负责花瓣旋转与向中心汇聚，服务端只发送一次动画起点。
        FRBNetwork.sendFormationToTracking(level, altarPos, 0);
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("frb.ritual.success",
                            Component.translatable("item.frb." + def.type().id))
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide
                || !(event.level instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        var it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            PendingRitual pending = entry.getValue();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (now < pending.finishAt()) {
                // 锻造/解封/月之仪式过程动画：升腾旋臂 → 流光飞向玩家
                Kind k = pending.kind();
                if (k == Kind.FORGE || k == Kind.UNSEAL
                        || k == Kind.LUNAR_SHARD || k == Kind.LUNAR_CROWN || k == Kind.LUNAR_PRIMROSE) {
                    forgeTickParticles(level, pending, player, now);
                }
                continue;
            }
            if (player != null) {
                switch (pending.kind()) {
                    case FORGE -> finishForge(level, pending, player);
                    case UNSEAL -> finishUnseal(level, pending, player);
                    case CRAFT -> {
                        // 丰饶凝结完成（封印态，产出未带解封标记的丰饶）
                        ItemStack result = new ItemStack(FRBItems.BOUNTIFUL.get());
                        if (!player.getInventory().add(result)) player.drop(result, false);
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                                pending.altarPos().getX() + 0.5D, pending.altarPos().getY() + 1.2D,
                                pending.altarPos().getZ() + 0.5D, 40, 0.5D, 1.0D, 0.5D, 0.03D);
                        level.playSound(null, pending.altarPos(),
                                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.RITUAL_COMPLETE.get(),
                                SoundSource.MASTER, 1.0F, 1.2F);
                        player.displayClientMessage(Component.translatable("frb.bountiful_craft.done")
                                .withStyle(ChatFormatting.GREEN), false);
                    }
                    case SEED -> {
                        ItemStack result = new ItemStack(FRBItems.seedOf(pending.type()));
                        if (!player.getInventory().add(result)) player.drop(result, false);
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                                pending.altarPos().getX() + 0.5D, pending.altarPos().getY() + 1.2D,
                                pending.altarPos().getZ() + 0.5D, 32, 0.4D, 0.8D, 0.4D, 0.03D);
                        level.playSound(null, pending.altarPos(),
                                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.RITUAL_COMPLETE.get(),
                                SoundSource.MASTER, 0.8F, 1.5F);
                    }
                    case LUNAR_PRIMROSE -> finishLunarPrimrose(level, pending, player);
                    case LUNAR_SHARD -> finishLunarShard(level, pending, player);
                    case LUNAR_CROWN -> finishLunarCrown(level, pending, player);
                }
            }
            it.remove();
        }
    }

    /** 从玩家背包（含快捷栏）移除指定数量的物品。 */
    public static boolean consume(Player player, Item item, int count) {
        if (count <= 0) return true;
        if (player.getInventory().countItem(item) < count) return false;
        int left = count;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
                if (stack.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
            }
        }
        return left == 0;
    }

    // ============================================================
    // r74：月见草 / 月之水晶 / 月之庇护 仪式链
    //   入口：RitualAltarBlock.use() 根据手持物品分支
    //     - 手持 AMETHYST_SHARD → attemptLunarShard()    （夜 + 月相）
    //     - 手持 LUNAR_SHARD    → attemptLunarCrown()    （夜 + 紫水晶簇×2 + 水×1 + 8 月见草 + 8 紫水晶碎片）
    //     - 手持 FORMLESS_SEED  → 现有 7 花仪式；失败时回落到 attemptEveningPrimroseSeed()
    // ============================================================

    /**
     * r74 月相仪式通用流程（r84 起材料/环境/时长全部读 RitualRecipes.lunar(kind) 单一数据源）。
     * 引子（catalyst）：非空时必须手持且消耗 1 个；供品不足/非夜晚/环境不足时失败。
     */
    private static boolean attemptLunar(ServerLevel level, BlockPos altarPos, Player player,
                                        RitualRecipes.LunarKind kind) {
        RitualRecipes.LunarDef def = RitualRecipes.lunar(kind);
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        if (PENDING.containsKey(serverPlayer.getUUID())) return false;
        ItemStack hand = player.getMainHandItem();
        if (def.catalyst() != null && !hand.is(def.catalyst())) return false;
        if (!level.isNight()) {
            player.displayClientMessage(Component.translatable("frb.ritual.fail.night").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (def.amethystEnv() && !primroseEnvironmentOk(level, altarPos)) {
            player.displayClientMessage(Component.translatable("frb.ritual.fail.blocks").withStyle(ChatFormatting.RED), true);
            return false;
        }
        Offerings src = Offerings.at(level, altarPos, player);
        if (!hasAll(src, def.costs())) {
            player.displayClientMessage(Component.translatable("frb.ritual.fail.cost").withStyle(ChatFormatting.RED), true);
            return false;
        }
        // 消耗：手持引子 ×1 + 供品
        if (def.catalyst() != null) hand.shrink(1);
        for (Rituals.Cost cost : def.costs()) {
            src.consume(cost.item(), cost.count());
        }
        long finishAt = level.getGameTime() + def.ritualTicks();
        Kind managerKind = switch (kind) {
            case PRIMROSE_SEED -> Kind.LUNAR_PRIMROSE;
            case LUNAR_SHARD -> Kind.LUNAR_SHARD;
            case LUNAR_CROWN -> Kind.LUNAR_CROWN;
        };
        // 月之水晶仪式把月相概率带进 pending（kind 不可携带数据，用 persistentData）
        if (kind == RitualRecipes.LunarKind.LUNAR_SHARD) {
            serverPlayer.getPersistentData().putFloat("FRBLunarShardChance",
                    cn.blockforge.generated.floralritebloomcurios.util.MoonPhase.successChance(level.getDayTime()));
        }
        PENDING.put(serverPlayer.getUUID(), new PendingRitual(altarPos.immutable(), null,
                finishAt, def.ritualTicks(), managerKind));
        level.playSound(null, altarPos, SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 1.2F, 1.4F);
        FRBNetwork.sendFormationToTracking(level, altarPos, 0);
        return true;
    }

    /** r74：月见草种子仪式（定义见 RitualRecipes.lunar）。 */
    public static boolean attemptEveningPrimroseSeed(ServerLevel level, BlockPos altarPos, Player player) {
        if (!attemptLunar(level, altarPos, player, RitualRecipes.LunarKind.PRIMROSE_SEED)) return false;
        player.displayClientMessage(Component.translatable("frb.lunar.primrose.start").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    /** r74：月之水晶仪式（手持紫水晶碎片，按月相概率产出；定义见 RitualRecipes.lunar）。 */
    public static boolean attemptLunarShard(ServerLevel level, BlockPos altarPos, Player player) {
        if (!attemptLunar(level, altarPos, player, RitualRecipes.LunarKind.LUNAR_SHARD)) return false;
        level.playSound(null, altarPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.MASTER, 1.0F, 0.9F);
        player.displayClientMessage(Component.translatable("frb.lunar.shard.start").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    /** r74：月之庇护仪式（定义见 RitualRecipes.lunar）。 */
    public static boolean attemptLunarCrown(ServerLevel level, BlockPos altarPos, Player player) {
        if (!attemptLunar(level, altarPos, player, RitualRecipes.LunarKind.LUNAR_CROWN)) return false;
        level.playSound(null, altarPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.MASTER, 1.2F, 0.7F);
        player.displayClientMessage(Component.translatable("frb.lunar.crown.start").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** 月见草 / 月之庇护 共用的环境检查：紫水晶簇×2 + 水×1 源（流体验证）。 */
    private static boolean primroseEnvironmentOk(ServerLevel level, BlockPos center) {
        int clusters = 0;
        int waterSources = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 3, 3))) {
            BlockState state = level.getBlockState(p);
            if (state.is(net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER)) {
                clusters++;
            } else {
                var fl = state.getFluidState();
                if (fl.is(net.minecraft.tags.FluidTags.WATER) && fl.isSource()) {
                    waterSources++;
                }
            }
        }
        return clusters >= 2 && waterSources >= 1;
    }

    private static void finishLunarPrimrose(ServerLevel level, PendingRitual pending, ServerPlayer player) {
        ItemStack result = new ItemStack(FRBItems.EVENING_PRIMROSE_SEED.get());
        if (!player.getInventory().add(result)) player.drop(result, false);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                pending.altarPos().getX() + 0.5D, pending.altarPos().getY() + 1.2D,
                pending.altarPos().getZ() + 0.5D, 32, 0.4D, 0.8D, 0.4D, 0.03D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                pending.altarPos().getX() + 0.5D, pending.altarPos().getY() + 1.0D,
                pending.altarPos().getZ() + 0.5D, 14, 0.4D, 0.5D, 0.4D, 0.02D);
        level.playSound(null, pending.altarPos(),
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.RITUAL_COMPLETE.get(),
                SoundSource.MASTER, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("frb.lunar.primrose.done").withStyle(ChatFormatting.GREEN), false);
    }

    private static void finishLunarShard(ServerLevel level, PendingRitual pending, ServerPlayer player) {
        float chance = player.getPersistentData().getFloat("FRBLunarShardChance");
        // 满月 = 50%, 新月 = 5%；用同一个 RandomSource 比一次
        if (level.random.nextFloat() < chance) {
            ItemStack result = new ItemStack(FRBItems.LUNAR_SHARD.get());
            if (!player.getInventory().add(result)) player.drop(result, false);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pending.altarPos().getX() + 0.5D, pending.altarPos().getY() + 1.2D,
                    pending.altarPos().getZ() + 0.5D, 40, 0.5D, 1.0D, 0.5D, 0.03D);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                    pending.altarPos().getX() + 0.5D, pending.altarPos().getY() + 1.0D,
                    pending.altarPos().getZ() + 0.5D, 18, 0.4D, 0.6D, 0.4D, 0.02D);
            level.playSound(null, pending.altarPos(),
                    cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.RITUAL_COMPLETE.get(),
                    SoundSource.MASTER, 1.0F, 1.5F);
            player.displayClientMessage(Component.translatable("frb.lunar.shard.done")
                    .withStyle(ChatFormatting.GOLD), false);
        } else {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    pending.altarPos().getX() + 0.5D, pending.altarPos().getY() + 1.2D,
                    pending.altarPos().getZ() + 0.5D, 18, 0.3D, 0.4D, 0.3D, 0.02D);
            level.playSound(null, pending.altarPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.MASTER, 0.8F, 0.8F);
            player.displayClientMessage(Component.translatable("frb.lunar.shard.fail")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        player.getPersistentData().remove("FRBLunarShardChance");
    }

    private static void finishLunarCrown(ServerLevel level, PendingRitual pending, ServerPlayer player) {
        ItemStack result = new ItemStack(FRBItems.LUNAR_CROWN.get());
        if (!player.getInventory().add(result)) player.drop(result, false);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                pending.altarPos().getX() + 0.5D, pending.altarPos().getY() + 1.2D,
                pending.altarPos().getZ() + 0.5D, 60, 0.6D, 1.2D, 0.6D, 0.04D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                pending.altarPos().getX() + 0.5D, pending.altarPos().getY() + 1.0D,
                pending.altarPos().getZ() + 0.5D, 30, 0.5D, 0.8D, 0.5D, 0.02D);
        level.playSound(null, pending.altarPos(),
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.RITUAL_COMPLETE.get(),
                SoundSource.MASTER, 1.0F, 1.0F);
        level.playSound(null, pending.altarPos(), SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.8F, 1.4F);
        player.displayClientMessage(Component.translatable("frb.lunar.crown.done")
                .withStyle(ChatFormatting.GOLD), false);
    }

    private RitualManager() {}
}
