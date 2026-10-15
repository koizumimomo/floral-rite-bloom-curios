package cn.blockforge.generated.floralritebloomcurios.entity;

import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.network.BossFxPacket;
import cn.blockforge.generated.floralritebloomcurios.config.FRBConfig;
import cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBBlocks;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import cn.blockforge.generated.floralritebloomcurios.util.MechanicEntity;
import cn.blockforge.generated.floralritebloomcurios.util.TimeRewindTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * 世界之花 Boss【万千花草之灵·芙莉艾丝·于死亡中绽放的花】。
 * 七阶段：自然 → 焚尽 → 霜华 → 云雨 → 天空 → 虚空 → 枯萎。
 * 每阶段结束时封印玩家对应属性的花灵饰品；击败 Boss 后全部复原，
 * 并由 Boss 亲手交出【万花万草之灵·祝福与永恒相随】。
 */
public class FriliasEntity extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    // ---- r73 L1 · GeckoLib 动画控制器缓存 ----
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache frb$animCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.frb$animCache;
    }

    /**
     * r73 L1：四个独立通道 —— idle 待机 / phase_open 阶段入场（hold_on_last_frame） /
     * cast 技能放出（一次性，0.75s 提前）/ intro_raise 开场抬手兜底（intensity=1 表示抬臂裁切）。
     * <p>
     * 三个状态机可以 <b>并行播放</b>，phase_open 的 hold_on_last_frame + cast 的一次性 + 硬覆盖手臂旋转
     * 互不干扰。
     */
    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        // 1) idle —— 永远循环
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "idle", 0,
                state -> state.setAndContinue(
                        software.bernie.geckolib.core.animation.RawAnimation.begin()
                                .thenLoop("idle"))));

        // 2) phase_open —— 切阶段时播 *_open 一次性展开并保留
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "phase_open", 0,
                state -> state.setAndContinue(
                        software.bernie.geckolib.core.animation.RawAnimation.begin()
                                .thenPlayAndHold(this.frb$currentPhaseOpenAnim()))));

        // 3) cast —— 释放技能时播 *_du_tiao 一次性播放
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "cast", 0,
                state -> state.setAndContinue(
                        software.bernie.geckolib.core.animation.RawAnimation.begin()
                                .thenPlay(this.frb$currentCastAnim()))));

        // 4) ring_idle —— boss 背后环绕（flower_ring / petal_ring / glyph_ring / out 四层反向自转），
        // 与 phase_open 的属性魔法阵并行常驻，loop=true 8 秒一圈。
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "ring_idle", 0,
                state -> state.setAndContinue(
                        software.bernie.geckolib.core.animation.RawAnimation.begin()
                                .thenLoop("ring_idle"))));
    }

    /** 当前阶段对应的 *_open 动画名（fire_open/ice_open/...）。 */
    private String frb$currentPhaseOpenAnim() {
        int idx = (this.phase >= 0 && this.phase < PHASE_ANIM_PREFIX.length) ? this.phase : 0;
        return PHASE_ANIM_PREFIX[idx] + "_open";
    }

    /** 当前阶段对应的 *_du_tiao 动画名（fire_du_tiao/ice_du_tiao/...）；万花凋零期间走 time_du_tiao。 */
    private String frb$currentCastAnim() {
        if (this.frb$endAnimTriggered) return "end";
        if (this.milleFlorumCastTick > 0) return "time_du_tiao";
        int idx = (this.phase >= 0 && this.phase < PHASE_ANIM_PREFIX.length) ? this.phase : 0;
        return PHASE_ANIM_PREFIX[idx] + "_du_tiao";
    }

    /** L1：触发对应阶段的 *_open 一次性入场（setAnimation + forceAnimationReset 会强制顶掉上一阶段的
     *  *_open —— GeckoLib 同一 controller 同时只播一个动画，旧法阵自动停止）。 */
    public void frb$triggerPhaseOpen(int phaseIndex) {
        this.phase = phaseIndex;
        if (this.level() != null && this.level().isClientSide) {
            var mgr = this.frb$animCache.getManagerForId(0);
            if (mgr != null) {
                software.bernie.geckolib.core.animation.AnimationController<?> ctrl =
                        mgr.getAnimationControllers().get("phase_open");
                if (ctrl != null) {
                    // r78：必须 setAnimation 一次 — 仅 forceAnimationReset 不能让 controller 切到新 phase 的
                    //      *_open 序列，因为 controller lambda 内部 setAndContinue 每次返回的 RawAnimation
                    //      相同（GeckoLib 视为"未变化"），旧动画 hold 不动。直接 setAnimation 一次强制切。
                    ctrl.setAnimation(software.bernie.geckolib.core.animation.RawAnimation.begin()
                            .thenPlayAndHold(this.frb$currentPhaseOpenAnim()));
                    ctrl.forceAnimationReset();
                }
            }
        }
    }

    /** L1：触发对应阶段的 *_du_tiao（一次性）。
     *  r85：双端可用——客户端直接重启 cast 通道；服务端通过 DATA_CAST_SYNC 计数器同步到客户端。 */
    public void frb$triggerCastAnim() {
        if (this.level() == null) return;
        if (this.level().isClientSide) {
            frb$playCastAnimClient();
        } else {
            this.entityData.set(DATA_CAST_SYNC, this.entityData.get(DATA_CAST_SYNC) + 1);
        }
    }

    /** 客户端实际播放 *_du_tiao：重启 cast 通道。 */
    private void frb$playCastAnimClient() {
        var mgr = this.frb$animCache.getManagerForId(0);
        if (mgr == null) return;
        software.bernie.geckolib.core.animation.AnimationController<?> ctrl =
                mgr.getAnimationControllers().get("cast");
        if (ctrl != null) {
            // 与 phase_open 同理：动画名随阶段变化时必须 setAnimation 强制切换
            ctrl.setAnimation(software.bernie.geckolib.core.animation.RawAnimation.begin()
                    .thenPlay(this.frb$currentCastAnim()));
            ctrl.forceAnimationReset();
        }
    }

    /**
     * r101：触发终幕 "end" 退场动画。服务端通过 DATA_CAST_SYNC 同步到客户端，
     * 客户端 cast 通道会因 frb$endAnimTriggered=true 而播放 "end"。
     */
    public void frb$triggerEndAnim() {
        this.frb$endAnimTriggered = true;
        this.frb$triggerCastAnim();
    }

    /** r85：客户端数据同步钩子——阶段号变化时切 phase_open（顶掉旧法阵）；施法计数器变化时播 *_du_tiao。 */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!this.level().isClientSide) return;
        if (DATA_PHASE.equals(key)) {
            this.frb$triggerPhaseOpen(this.entityData.get(DATA_PHASE));
        } else if (DATA_CAST_SYNC.equals(key)) {
            this.frb$playCastAnimClient();
        }
    }

    /** 转阶段演出时长：100 tick（5 秒无敌，足够花瓣汇聚动画播完）。 */
    public static final int TRANSITION_TICKS = 100;
    /** 开场动画时长：200 tick（登场 40 + 星系汇聚 120 + 爆发落地 40）。 */
    public static final int INTRO_TICKS = 200;
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRANSITION =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.INT);
    /** 开场动画状态同步：客户端据此摆苏醒姿势（introTicks 本身只在服务端递减）。 */
    private static final EntityDataAccessor<Boolean> DATA_INTRO =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.BOOLEAN);
    /** 朝向同步：悬浮 Boss 几乎无位移，客户端 yBodyRot 靠位移推算会不跟手，直接同步 yaw 让身体立刻转向。 */
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.FLOAT);
    public static final String[] PHASE_KEYS =
            {"nature", "burn", "frost", "rain", "sky", "void", "decay", "time"};
    /** r73 L1：阶段索引 → geo 动画名前缀（new 模型改名为 fire/ice/death，对应 _frost/_burn/_decay 旧名）。 */
    public static final String[] PHASE_ANIM_PREFIX =
            {"nature", "fire", "ice", "rain", "sky", "void", "death", "time"};
    /** 每阶段结束时封印的花灵：自然→森语藤、焚尽→焚生藤、霜华→寒风铃、云雨→天泪花、天空→天穹花、虚空→星界花。 */
    public static final CharmType[] PHASE_SEALS = {
            CharmType.FOREST_WHISPER, CharmType.EMBER_VINE, CharmType.FROST_CHIME,
            CharmType.SKY_TEAR, CharmType.SKY_PILLAR, CharmType.ASTRAL_BLOOM};
    public static final BossEvent.BossBarColor[] BAR_COLORS = {
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarColor.RED, BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarColor.PINK, BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarColor.WHITE};
    /** 自然绿、焚尽橙红、霜华冰蓝、云雨深蓝、天空白青、虚空紫黑、枯萎灰黑、时间冷白。 */
    public static final int[] PHASE_COLORS =
            {0x7ce68b, 0xff5a2a, 0x9fdcff, 0x3a59d8, 0xdff7ff, 0x6b2a8f, 0x3c3c3c, 0xE0E0E0};
    public static final ChatFormatting[] PHASE_FORMAT = {
            ChatFormatting.GREEN, ChatFormatting.GOLD, ChatFormatting.AQUA,
            ChatFormatting.BLUE, ChatFormatting.WHITE, ChatFormatting.DARK_PURPLE, ChatFormatting.GRAY,
            ChatFormatting.WHITE};
    public static final int PHASE_COUNT = 7;

    private static final EntityDataAccessor<Integer> DATA_RUNES =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.INT);
    /** A6 激光读条剩余 tick（客户端 HUD 占位条读取；0 = 未在蓄力）。 */
    private static final EntityDataAccessor<Integer> DATA_BEAM_CHARGE =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.INT);
    /** D1 霜凝蓄力剩余 tick（客户端据此在 boss 头顶绘制 gaze 印记）。 */
    private static final EntityDataAccessor<Integer> DATA_FROST_GAZE =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.INT);
    /** r85：施法动画同步计数器——服务端每次 +1，客户端 onSyncedDataUpdated 检测到变化就播 *_du_tiao。 */
    private static final EntityDataAccessor<Integer> DATA_CAST_SYNC =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.INT);
    /** r90：技能读条剩余 tick（>0 时 renderer 画 drawChargeBar + boss 无敌 + 升空）。 */
    private static final EntityDataAccessor<Integer> DATA_SKILL_CAST =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.INT);
    /** r90：技能读条总时长（用于计算进度条 0→1）。 */
    private static final EntityDataAccessor<Integer> DATA_SKILL_CAST_MAX =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.INT);
    /** r95：枯萎阶段大范围 AOE 读条剩余 tick（客户端 HUD 读取；0 = 未在读条）。 */
    private static final EntityDataAccessor<Integer> DATA_DECAY_AOE_CHARGE =
            SynchedEntityData.defineId(FriliasEntity.class, EntityDataSerializers.INT);

    private int phase = 0;
    private int transition = 0;
    private int timer = 0;
    private int rainTeleports = 0;
    private int regenCounter = 0;
    /** 悬浮近战的攻击间隔计时（tick）。 */
    private int meleeCooldown = 0;
    /** r86：风眼月环锁定计时——正在预警/刚结算时不再放新的月环，避免叠加。 */
    private int windEyeLockTicks = 0;
    /** r96：天空阶段固定悬停锚点（X/Z），boss 不再跟随玩家跑动，只在原地悬浮并看向玩家。 */
    private double skyAnchorX = 0.0D;
    private double skyAnchorZ = 0.0D;
    private boolean skyAnchorSet = false;
    /** r96：天空阶段已释放的机制计数——释放 2~4 个后随机传送一次。 */
    private int skyMechanicReleased = 0;
    private int skyTeleportThreshold = 3;
    /** r90：技能读条剩余 tick（服务端递减，同步到客户端画进度条）。 */
    private int skillCastTicks = 0;
    /** r90：技能读条总时长。 */
    private int skillCastMax = 0;
    /** r90：技能释放前升空高度（格）。 */
    private static final double SKILL_CAST_HOVER = 3.0D;
    /** r90：技能读条时长（与法阵动画 15 tick 一致）。 */
    public static final int SKILL_CAST_TICKS = 15;
    /** r90：云雨阶段传送后无敌时长：60 tick（3 秒）。 */
    public static final int RAIN_TELEPORT_INVULN_TICKS = 60;
    /** r96：技能队列——正在释放某个技能时，把 cd 到点的其他机制暂存，待当前技能释放完毕后依次执行。 */
    private final java.util.List<Runnable> mechanicQueue = new java.util.ArrayList<>();
    /** r96：已调度但尚未执行的机制计数（15 tick 法阵动画窗口内算"释放中"）。 */
    private int activeMechanicCount = 0;
    /** r90：云雨传送无敌剩余 tick（>0 时 hurt() 直接拒绝）。 */
    private int rainTeleportInvulnTicks = 0;
    /** r96：万变水波当前是否为斜向排列（false=正点 N/S/E/W，true=斜向）。 */
    private boolean waterRippleDiagonal = false;
    /** r96：万变水波实体引用列表（用于全屏 AOE 后重置位置时清理）。 */
    private final java.util.List<java.util.UUID> waterRippleIds = new java.util.ArrayList<>();
    /** 开场动画剩余 tick；>0 时无敌、悬浮、不索敌。 */
    private int introTicks = INTRO_TICKS;
    /** 客户端访问器：开场动画播放中（镜头扭曲等演出效果用）。 */
    public boolean isIntro() {
        return this.entityData.get(DATA_INTRO);
    }
    private double introGroundY = Double.NaN;
    private final java.util.List<UUID> flowerIds = new java.util.ArrayList<>();

    /**
     * A4 解锁等级：0 = 阶段初始可用机制 / 1 = 血量跌破 70% 后 / 2 = 跌破 40% 后。
     * 机制类自身决定它在该阶段哪个等级生效。
     */
    private int mechanicTier = 0;
    private int lastDamagePhase = -1; // -1 = 还没进入战斗

    /** 新机制调度器：构造时一次性注册所有 PhaseMechanic。 */
    private final java.util.List<cn.blockforge.generated.floralritebloomcurios.util.PhaseMechanic> mechanics =
            new java.util.ArrayList<>(cn.blockforge.generated.floralritebloomcurios.util.Mechanics.all());

    /** H1 死亡之花复活：记录"最近一次死亡的花"时间和 slot，由机制每 80 tick 检查。 */
    private int lastKilledFlowerSlot = -1;
    private long lastKilledFlowerAt = -1;

    /** D1 霜凝蓄力 · C2 烈焰击退蓄力：剩余 tick；>0 时锁定当前阶段能力调用。 */
    private int flameChargeTicks = 0;
    private int frostGazeTicks = 0;
    /** r74：霜凝蓄力起 - 一次性扫领域内参战者缓存，读条结束统一用这份名单判定（不再每 tick getEntitiesOfClass 扫）。 */
    private final java.util.List<ServerPlayer> frostGazeCachedTargets = new java.util.ArrayList<>();
    /** r75：霜凝蓄力期间挂在 boss 头顶的大眼睛实体；蓄力归零时 discard。包内可见（FriliasGazeEyeEntity 同包内访问）。 */
    FriliasGazeEyeEntity gazeEye;

    /** A6 激光读条（r66）：剩余 tick；>0 时周身 6 点汇聚粒子，归零后发射花瓣光束。 */
    public static final int BEAM_CHARGE_TICKS = 24;
    private int beamChargeTicks = 0;
    private LivingEntity beamTarget;
    private float beamDamage;
    private int beamColor;
    private MobEffect beamPotion;

    // ---- r95：枯萎阶段大范围 AOE 机制 ----
    /** AOE 读条时长：2 秒 = 40 tick。 */
    public static final int DECAY_AOE_CHARGE_TICKS = 40;
    /** AOE 释放后冷却：5 秒 = 100 tick（加上读条 40 = 间隔 140 tick = 7 秒）。 */
    public static final int DECAY_AOE_CD_TICKS = 100;
    /** AOE 伤害范围（格）。 */
    public static final double DECAY_AOE_RANGE = 24.0D;
    /** AOE 伤害值（花灵伤害）。r98：×1.5 → 12 */
    public static final float DECAY_AOE_DAMAGE = 12.0F;
    private int decayAoeChargeTicks = 0;
    private int decayAoeCdTicks = 0;

    /** 客户端读取 AOE 读条剩余 tick（0 = 未在读条）。 */
    public int getDecayAoeCharge() { return this.entityData.get(DATA_DECAY_AOE_CHARGE); }

    // ---- 额外·时间阶段 / 万花凋零终结技（H2） ----

    /** 额外·时间阶段的 phase 索引（7）。阶段颜色单独 TIME_PHASE_COLOR，不复用 PHASE_COLORS[0..6]。 */
    public static final int PHASE_TIME = 7;
    /** 时间阶段主题色（冷白）。 */
    public static final int TIME_PHASE_COLOR = 0xE0E0E0;
    /** 时间阶段主题 formatting。 */
    public static final ChatFormatting TIME_PHASE_FORMAT = ChatFormatting.WHITE;

    /** 万花凋零咏唱总时长：120 tick（6 秒）；读满后触发终结一击（24 格秒杀 + 魔法阵）。 */
    public static final int MILLE_FLORUM_CAST_TICKS = 120;

    /** 当前是否处于时间阶段（phase 7）。 */
    private boolean timePhaseActive = false;
    /** r64：开局检测一次多人状态，之后整场战斗都按开场时是否多人来走机制——避免每次 cd 扫描领域玩家导致偶发卡顿。 */
    private boolean introMultiplayerScanned = false;
    private boolean introIsMultiplayer = false;
    // ---- r93：说话系统（仿梦幻终焉 UomWither：LongLookAtPlayerGoal 盯人闲聊 + ChanBegGoal 对创造玩家害羞）----
    /** 战斗闲聊冷却（tick），归零后按低概率随机说一句。 */
    private int frbTalkCd = 240;
    /** 机制施法喊话冷却，避免每次施法都刷屏。 */
    private int frbCastTalkCd = 0;
    /** 创造模式盯视害羞：当前连续盯视者 UUID 与已盯 tick 数。 */
    private UUID frbShyWatcher = null;
    private int frbShyStareTicks = 0;
    /** 害羞台词冷却。 */
    private int frbShyCd = 0;
    /** 时间阶段技能轮换的下一个挑选 tick。 */
    private int timePhaseRotateTick = 0;
    // ---- I2 时间阶段专属机制（r67）：预支生命 / 时间停止 / 时间倒流 ----
    // r70：三个内置 cd 改由 config/frb_boss.toml 控制（FRBConfig#lifeDrainCd 等）

    private int lifeDrainCd = 0;
    private int timeStopCd = 0;
    private int rewindCd = 0;
    /** 时间停止持续：80 tick（4 秒）。 */
    public static final int TIME_STOP_TICKS = 80;
    /** 时停内连招触发 tick（第 30 tick 接一个三选一技能）。 */
    public static final int TIME_STOP_COMBO_AT = 30;
    /** 时停剩余 tick；>0 时每 tick 把冻结体拽回快照点。 */
    private int timeStopTicks = 0;
    /** 时停快照：施法瞬间（或中途闯入时）各实体的位置。 */
    private final java.util.Map<UUID, Vec3> timeStopSnapshot = new java.util.HashMap<>();
    /** 时停连招不重复轮换：0=自然光束 1=焚尽灼热之种 2=天空风刃；全用过后重置回随机三选一。 */
    private final boolean[] timeStopComboUsed = new boolean[3];
    /** 时间倒流：玩家位置环形缓冲（每 5 tick 采样，保留 100 tick 历史）。 */
    private final TimeRewindTracker rewindTracker = new TimeRewindTracker();
    /** 万花凋零咏唱剩余 tick；>0 表示正在咏唱；cast 时 boss 锁 1 血 + 无敌。 */
    private int milleFlorumCastTick = 0;
    /** 万花凋零咏唱期间是否进入"提前承认"路径（boss 在剩余 1 秒时停止咏唱 → 显示真正认可文本）。 */
    private boolean milleFlorumRecognized = false;
    /** 万花凋零咏唱期间是否在最后 2 秒（hp <= 0）被打断走 KILLED。 */
    private boolean milleFlorumKilledAtLast = false;
    /** r101：终幕退场动画标志——为 true 时 cast 通道播放 "end" 动画。 */
    private boolean frb$endAnimTriggered = false;

    private final ServerBossEvent bossEvent;

    public FriliasEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.bossEvent = (ServerBossEvent) new ServerBossEvent(
                Component.translatable("entity.frb.frilias.name"),
                BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_20)
                .setDarkenScreen(true);
        this.setPersistenceRequired();
        this.entityData.set(DATA_INTRO, this.introTicks > 0);
        // r78：boss 默认离地悬浮 1.25 格——开场演出从地面升起前，boss 不会埋进地里。
        this.setOnGround(false);
        if (this.getBbHeight() > 0) {
            this.setPos(this.getX(), this.getY() + 1.25D, this.getZ());
        }
    }

    /** r78：战斗终结真正死亡时掉落：原始花瓣 ×6、月之水晶碎片 ×1、世界之花意志 × 2。 */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (this.level().isClientSide) return;
        if (!this.isDeadOrDying()) return;
        dropWinLoot();
        // r90：死亡音效（之前只在转阶段播过一次，这里补终焉死亡音）
        this.level().playSound(null, this,
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.FRILIAS_DEATH.get(),
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 1.0F);
    }

    /** r90：战利品掉落公共方法，供 die() 和 milleFlorumExit 共用。
     *  基础掉落：原始花瓣 ×6、世界之花意志 ×1。
     *  进入过时间阶段（phase >= PHASE_TIME）时追加额外战利品（待用户后续添加新物品）。 */
    private void dropWinLoot() {
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        drops.add(new ItemStack(cn.blockforge.generated.floralritebloomcurios.registry.FRBItems.PRIMORDIAL_PETAL.get(), 6));
        drops.add(new ItemStack(cn.blockforge.generated.floralritebloomcurios.registry.FRBItems.WORLD_FLOWER_WILL.get(), 1));
        // r90：进入过时间阶段（击败时间阶段 boss）才掉落的额外战利品
        if (this.phase >= PHASE_TIME) {
            // TODO: 用户后续添加的专属战利品放在这里
        }
        for (ItemStack s : drops) {
            this.spawnAtLocation(s, 1.0F);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RUNES, 4);
        this.entityData.define(DATA_PHASE, 0);
        this.entityData.define(DATA_TRANSITION, 0);
        this.entityData.define(DATA_INTRO, Boolean.FALSE);
        this.entityData.define(DATA_YAW, 0.0F);
        this.entityData.define(DATA_BEAM_CHARGE, 0);
        this.entityData.define(DATA_FROST_GAZE, 0);
        this.entityData.define(DATA_CAST_SYNC, 0);
        this.entityData.define(DATA_SKILL_CAST, 0);
        this.entityData.define(DATA_SKILL_CAST_MAX, 0);
        this.entityData.define(DATA_DECAY_AOE_CHARGE, 0);
    }

    /** 芙莉艾丝免疫黑死病，避免黑灵草的范围效果影响 Boss。 */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == FRBEffects.BLACK_DEATH.get()) return false;
        return super.canBeAffected(effect);
    }

    /** 云雨阶段剩余的传送次数（客户端据此绘制环绕符文）。 */
    public int getRunesLeft() {
        return this.entityData.get(DATA_RUNES);
    }

    /** A6 激光读条剩余 tick（客户端 HUD 占位条用；0 = 未在蓄力）。 */
    public int getBeamChargeTicks() {
        return this.entityData.get(DATA_BEAM_CHARGE);
    }

    /** r90：技能读条剩余 tick（renderer 据此画 drawChargeBar；0 = 未在释放技能）。 */
    public int getSkillCastTicks() {
        return this.entityData.get(DATA_SKILL_CAST);
    }

    /** r90：技能读条总时长（renderer 据此计算进度条 0→1）。 */
    public int getSkillCastMax() {
        return this.entityData.get(DATA_SKILL_CAST_MAX);
    }

    /** r90：是否正在释放技能（读条中）。 */
    public boolean isSkillCasting() {
        return this.skillCastTicks > 0;
    }

    /**
     * r90：技能释放前统一处理——升空3格 + 无敌 + 设读条计时器。
     * 仅霜凝、积雨云、风眼三个技能接入；法阵动画由各调用点自行 frb$triggerCastAnim()。
     * 天空阶段（phase 4）不升空（已悬浮 3 格）；虚空阶段（phase 5）不调用本方法。
     *
     * @param durationTicks 无敌+升空持续 tick（霜凝 30 / 积雨云 25 / 风眼 65）
     */
    public void beginSkillCast(ServerLevel level, int durationTicks) {
        if (this.skillCastTicks > 0) return;
        this.skillCastTicks = durationTicks;
        this.skillCastMax = durationTicks;
        this.entityData.set(DATA_SKILL_CAST, durationTicks);
        this.entityData.set(DATA_SKILL_CAST_MAX, durationTicks);
        // r96：天空阶段 boss 已悬浮 3 格高，风眼/冲击波预警期不再全程无敌——
        //      否则每 80t 冲击波 + 75t 风眼无敌导致覆盖率 ~94%，玩家几乎无法输出。
        if (this.phase != 4) {
            this.setInvulnerable(true);
        }
        // 天空阶段已悬浮 3 格，不再升空；其它阶段升空并关重力悬停
        if (this.phase != 4) {
            this.setNoGravity(true);
            int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(this.getX()), Mth.floor(this.getZ()));
            double targetY = Math.max(this.getY(), gy) + SKILL_CAST_HOVER;
            this.setDeltaMovement(0, (targetY - this.getY()) * 0.2D, 0);
            this.hasImpulse = true;
        }
    }

    /** r90：每 tick 递减技能读条计时器，归零时恢复可被攻击并落回地面。 */
    private void tickSkillCast(ServerLevel level) {
        if (this.skillCastTicks <= 0) return;
        this.skillCastTicks--;
        this.entityData.set(DATA_SKILL_CAST, this.skillCastTicks);
        // 天空阶段不升空，保持悬浮
        if (this.phase != 4) {
            int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(this.getX()), Mth.floor(this.getZ()));
            double targetY = Math.max(this.getY(), gy) + SKILL_CAST_HOVER;
            if (this.getY() < targetY - 0.5D) {
                this.setDeltaMovement(0, 0.3D, 0);
                this.hasImpulse = true;
            } else if (this.skillCastTicks > 0) {
                // 到达高度后悬浮
                this.setDeltaMovement(Vec3.ZERO);
            }
        }
        // 归零时恢复：解除无敌；非天空/虚空阶段恢复重力落地
        if (this.skillCastTicks <= 0) {
            this.setInvulnerable(false);
            this.entityData.set(DATA_SKILL_CAST, 0);
            if (this.phase != 4 && this.phase != 5) {
                this.setNoGravity(false);
            }
            // r96：长读条技能释放完毕，尝试把暂存队列里的下一个机制排上
            tryDrainMechanicQueue();
        }
    }

    /** r73 L1：万花凋零咏唱剩余 tick（renderer 用此判断是否进入双手交叉蓄力姿势）。 */
    public int getMilleFlorumCastTick() {
        return this.milleFlorumCastTick;
    }

    /** D1 霜凝蓄力剩余 tick（客户端据此在 boss 头顶绘制 gaze 印记）。 */
    public int getFrostGazeTicks() {
        return this.entityData.get(DATA_FROST_GAZE);
    }

    /** 包内访问：死亡之花均分排布需要读取存活花朵 UUID 列表（r66）。 */
    java.util.List<UUID> frbFlowerIds() {
        return this.flowerIds;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 悬浮追踪 + hoverMove 内的近战判定取代原 MeleeAttackGoal：
        // 原 Goal 依赖寻路，而悬浮移动每 tick stop() 导航，导致 Boss 攻击欲望极低。
        // 不挂 RandomStrollGoal：漫步目标点内部的高度图查询会触达未加载区块（同步生成卡死），
        // 且按设计 Boss 只追踪玩家。
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                if (FriliasEntity.this.introTicks > 0 || !super.canUse()) return false;
                // r93：显式拒绝创造/旁观目标——原版排除依赖 invulnerable，这里兜底
                return !(this.target instanceof Player p) || (!p.isCreative() && !p.isSpectator());
            }

            @Override
            public boolean canContinueToUse() {
                // r93：战斗中途切成创造/旁观立即脱战
                if (this.target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
                return super.canContinueToUse();
            }
        });
    }

    // ---- Boss 血条 ----

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
        this.bossEvent.setColor(BAR_COLORS[phase]);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.isAlive()) {
            this.bossEvent.setName(barName());
            this.bossEvent.setProgress(this.transition > 0 || this.introTicks > 0 ? 0.0F
                    : Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
        }
        // 客户端按 DATA_YAW 硬对齐三向朝向（悬浮无位移，客户端 yBodyRot 自己推不动）
        if (this.level().isClientSide && this.isAlive()
                && !this.entityData.get(DATA_INTRO) && this.getTransitionTicks() <= 0) {
            float yaw = this.entityData.get(DATA_YAW);
            if (Float.isFinite(yaw)) {
                // rotLerp 签名是 rotLerp(delta, start, end)：delta（0~1 插值比例）在第一位。
                // r43 曾误把当前角度当 delta、t 当 end 传入，每 tick 放大约 180 倍发散成
                // Infinity（疯转/部件消失/隐身根因，与旧版误传 40 等效且更快）。
                float t = 0.25F;
                float yRot = Float.isFinite(this.getYRot()) ? Mth.rotLerp(t, this.getYRot(), yaw) : yaw;
                float body = Float.isFinite(this.yBodyRot) ? Mth.rotLerp(t, this.yBodyRot, yaw) : yaw;
                float head = Float.isFinite(this.yHeadRot) ? Mth.rotLerp(t, this.yHeadRot, yaw) : yaw;
                this.setYRot(yRot);
                this.yBodyRot = body;
                this.yHeadRot = head;
            }
        }
    }

    // ---- 开场无敌 / 阶段锁血 / 枯萎无敌 / 动态减伤 ----
    // r70：锁血比例 / 连击保护窗口与折算系数改由 config/frb_boss.toml 控制

    private int lastPainTick = -100;
    /** r72 动态减伤：60 tick 滑窗内累计伤害 tick 数；超过 40 后开始按 0.6 进一步折算（"短时间内受击越多伤害越低"）。 */
    private int recentPainWindow = 0;
    private int recentPainTick0 = -100;

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.introTicks > 0 || this.transition > 0) return false;
        // r90：云雨阶段传送后 3 秒无敌
        if (this.rainTeleportInvulnTicks > 0
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (this.phase == PHASE_COUNT - 1 && anyFlowerAlive()) return false;
        // r72 回到过去：时间阶段受击时 10% 概率无视本次伤害（灵感来自梦幻终焉·终焉秩序维系者）。
        // 不对 BYPASSES_INVULNERABILITY 类伤害生效（虚空伤害照样走流程）。
        if (this.timePhaseActive && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && this.random.nextFloat() < 0.10F) {
            // 视觉回滚：玩家身上扩散一圈时间粒子 + 半 tick 短无敌
            if (!this.level().isClientSide) {
                ServerLevel sl = (ServerLevel) this.level();
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                        source.getEntity() == null ? this.getX() : source.getEntity().getX(),
                        source.getEntity() == null ? this.getY() + 1.0D : source.getEntity().getY() + 1.0D,
                        source.getEntity() == null ? this.getZ() : source.getEntity().getZ(),
                        18, 0.5D, 0.7D, 0.5D, 0.05D);
            }
            return false;
        }
        // r99：时间阶段额外 50% 常态减伤（600 HP → 等效 1200 HP），
        //      与单次 8% 封顶 / 连击衰减 / 60tick 滑窗叠加，防止被高频输出秒掉。
        if (this.timePhaseActive && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= 0.5F;
        }
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // 动态减伤：单次封顶（比例可配，1.0 = 不限）；非弹幕的高频连击额外衰减。
            // 丰饶花瓣的花灵伤害（floral DamageType）已挂 minecraft:bypasses_cooldown，
            // 原版无敌帧吞伤由 tag 全局豁免（对所有目标生效，无需在这里特判）。
            // 召唤物海（外 mod）等仍受连击衰减压制。
            boolean petal = source.getDirectEntity() instanceof PetalVolleyEntity;
            // r90：时间阶段单次伤害上限更严（8% 最大生命，其它阶段 15%）
            float capFraction = this.timePhaseActive
                    ? FRBConfig.timeMaxHitFraction()
                    : FRBConfig.maxHitFraction();
            amount = Math.min(amount, this.getMaxHealth() * capFraction);
            if (FRBConfig.comboProtectEnabled() && !petal
                    && this.tickCount - this.lastPainTick < FRBConfig.comboWindowTicks()) {
                amount *= FRBConfig.comboFactor();
            }
            // r72 动态减伤：60 tick 滑窗内连续受击超过 40 tick → 再打 0.6 折（防止外 mod 高频多源伤害压死 boss）
            if (!petal) {
                if (this.tickCount - this.recentPainTick0 >= 60) {
                    this.recentPainWindow = 0;
                    this.recentPainTick0 = this.tickCount;
                }
                this.recentPainWindow++;
                if (this.recentPainWindow > 40) {
                    amount *= 0.6F;
                }
            }
            this.lastPainTick = this.tickCount;
        }
        boolean result = super.hurt(source, amount);
        // A4 血量分段解锁（只在成功受击后判断；r65：提示文本已按用户要求移除，静默解锁）
        if (result && !this.level().isClientSide) {
            float ratio = this.getHealth() / this.getMaxHealth();
            if (this.mechanicTier < 1 && ratio <= 0.70F) {
                this.mechanicTier = 1;
                this.lastDamagePhase = this.phase;
            } else if (this.mechanicTier < 2 && ratio <= 0.40F) {
                this.mechanicTier = 2;
                this.lastDamagePhase = this.phase;
            }
        }
        return result;
    }

    public int getMechanicTier() {
        return this.mechanicTier;
    }

    /**
     * r87 调试用：创造模式「相位之杖」右键 boss → 立即强制进入下一阶段。
     * 复用正常 beginTransition 流程（transition 演出结束后 enterPhaseAbilities 入场），
     * 因此属性/召唤物/重力等入场逻辑与正常打空血转阶段完全一致。
     * 开场演出中、转阶段演出中、最后阶段、万花咏唱中不允许跳。
     */
    public boolean frb$debugForceNextPhase(ServerLevel level) {
        if (this.introTicks > 0 || this.transition > 0) return false;
        if (this.phase >= PHASE_COUNT - 1) return false;
        if (this.milleFlorumCastTick > 0) return false;
        beginTransition(level, this.phase);
        return true;
    }

    /** 在原版实际扣血时截住致命一击，避免高伤害跳过阶段。r72：同时防住玩家用 /kill、setHealth、虚空伤害绕过锁血。 */
    @Override
    public void setHealth(float health) {
        // r72：难度 peaceful 时允许玩家逃脱战斗（boss 直接离场 + 解封玩家饰品）。
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl
                && sl.getServer().getWorldData().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL
                && health <= 0.0F && this.isAlive()) {
            super.setHealth(0.0F);
            this.die(sl.damageSources().generic());
            return;
        }
        // r77：时间阶段（phase=7）受致死伤害 → 启动万花凋零咏唱（锁 1 HP + 120 血咏唱池）。
        //      r107：咏唱期间不再有减伤与击杀窗口限制——任何时刻打死 boss 即中断咏唱取胜。
        if (!this.level().isClientSide && health <= 0.0F
                && !this.isRemoved() && this.isAlive() && this.phase == PHASE_TIME) {
            // 玩家还没在咏唱里 — 启动咏唱流程并锁血
            if (this.milleFlorumCastTick <= 0 && !this.milleFlorumKilledAtLast) {
                super.setHealth(1.0F);
                if (this.level() instanceof ServerLevel ss) {
                    this.startMilleFlorumFinaleCast(ss, null);
                    // 广播一段"时间开始倒流"的入场文字（参考 enterTimePhase 的提示）
                    for (ServerPlayer p : ss.getEntitiesOfClass(ServerPlayer.class,
                            this.getBoundingBox().inflate(96.0D))) {
                        p.displayClientMessage(Component.translatable("frb.boss.time_phase.enter")
                                .withStyle(TIME_PHASE_FORMAT), true);
                    }
                    ss.playSound(null, this.getX(), this.getY(), this.getZ(),
                            net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                            net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.5F);
                    FRBNetwork.sendBossFx(ss, this, BossFxPacket.FX_TINT, 0.85F, 80, TIME_PHASE_COLOR);
                }
                return;
            }
            // r107：咏唱中被打死 → 允许真实死亡（玩家打断咏唱即胜利）
            this.milleFlorumKilledAtLast = true;
            if (this.level() instanceof ServerLevel sl) {
                FRBNetwork.sendFinaleEnd(sl, this);
            }
            super.setHealth(health);
            return;
        }
        // r72：拒绝任何 setHealth(<=0) 绕过锁血——只有走 `super.hurt()` 的合法路径才能扣到 1 HP 触发转阶段。
        if (!this.level().isClientSide && health <= 0.0F
                && !this.isRemoved() && this.isAlive()) {
            // 进入正常锁血分支（转阶段）— 拒绝对非合法路径的瞬杀
            if (this.phase < PHASE_COUNT - 1 && this.transition == 0) {
                super.setHealth(1.0F);
                beginTransition((ServerLevel) this.level(), this.phase);
                return;
            }
            // 枯萎期间已被 hurt() 拦截，此处到达的都是 /kill / setHealth / 虚空伤害等异常路径 → 拒绝。
            return;
        }
        if (!this.level().isClientSide && this.bossEvent != null && this.transition > 0
                && health < this.getHealth()) return;
        super.setHealth(health);
    }

    /**
     * r72：拒绝 /kill 直接调用 Entity#kill()。
     * 玩家调 /kill 时实际走 super.kill() → setHealth(0) → 已在上方被 setHealth 拒绝，
     * 这里再补一道防御以防有人绕过 setHealth 直接改 DATA_HEALTH。
     * 真正的死亡路径只走 `die(DamageSource)` + `remove(KILLED)`。
     */
    @Override
    public void kill() {
        // 锁血状态下静默忽略 /kill
    }

    /**
     * r72：拦截末影龙咆哮（DamageSource.OUT_OF_WORLD）类"虚空伤害"绕过锁血。
     * 玩家若切换到末地/虚空仍想杀 boss，必须先回到主世界。
     */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)
                && this.phase < PHASE_COUNT - 1) {
            // BOSSES_INVULNERABILITY 类伤害（虚空、kill 等）直到转入 phase 6 之前的转阶段才允许
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public void aiStep() {
        // 移动阶段统一采用悬浮飞行；虚空与枯萎阶段在各自能力中恢复重力。
        this.setNoGravity(this.transition > 0 || this.phase <= 4 || this.introTicks > 0);
        super.aiStep();
        if (this.level().isClientSide) return;
        ServerLevel level = (ServerLevel) this.level();
        if (this.introTicks > 0) {
            introTick(level);
            return;
        }
        if (this.transition > 0) {
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            this.transition--;
            this.entityData.set(DATA_TRANSITION, this.transition);
            if (this.transition == 0) {
                this.setHealth(this.getMaxHealth());
                // r91：无敌结束、boss 重返战场时给一次与开场落地同规格的演出
                // （镜头震动 + FOV 脉冲 + 白闪 + 径向冲击波）
                FRBNetwork.sendBossFx(level, this,
                        BossFxPacket.FX_SHAKE | BossFxPacket.FX_FLASH | BossFxPacket.FX_SHOCKWAVE,
                        1.0F, 30);
                this.enterPhaseAbilities(level);
            }
            return;
        }
        this.timer++;
        // 自然生命恢复：r98 自然阶段再 ×2（5 tick 回 2 点），其他阶段保持 20 tick 回 1 点
        if (this.phase < PHASE_COUNT - 1) {
            this.regenCounter++;
            if (this.regenCounter % (this.phase == 0 ? 5 : 20) == 0) {
                this.heal(this.phase == 0 ? 2.0F : 1.0F);
            }
        }
        // 封印领域：每秒按当前阶段刷新领域内 / 外玩家的花灵封印
        if (this.tickCount % 20 == 0) {
            updateSealDomain(level);
        }
        ambientFlowers(level);
        // r93：战斗闲聊 + 创造模式盯视害羞
        frbTalkTick(level);
        // r64：开场检测一次多人状态后，整场战斗都按此判定多人机制。
        // 旧版每次 cd 都调用 fightersInRange，会扫描半径 40 格内所有玩家，
        // 在远区块未加载时会偶发卡顿。
        if (!this.introMultiplayerScanned) {
            // r70：multiplayerMechanics=false 时跳过人数检测（荆棘共生等多人机制单人也会释放）
            this.introIsMultiplayer = !FRBConfig.multiplayerMechanics()
                    || cn.blockforge.generated.floralritebloomcurios.util.Mechanics
                    .fightersInRange(level, this) >= 2;
            this.introMultiplayerScanned = true;
        }
        // A1 调度器：每阶段注册的所有 PhaseMechanic 按 cooldown + tier 自动运行
        // r96：技能队列——释放中（skillCastTicks>0 或已有机制在 15t 法阵窗口内）时，cd 到点的机制入队暂存。
        // r100：喝醉了 / 无有效目标（如玩家切创造）时，不释放任何技能，也不排队。
        boolean skillDisabled = this.hasEffect(FRBEffects.DRUNK.get()) || this.getTarget() == null;
        boolean casting = this.skillCastTicks > 0 || this.activeMechanicCount > 0;
        if (!skillDisabled) {
        for (var mechanic : this.mechanics) {
            if (mechanic.phase() != this.phase) continue;
            if (this.mechanicTier < mechanic.unlockedAt()) continue;
            if (!mechanic.unlockedInMultiplayer() && !this.introIsMultiplayer) continue;
            if (this.timer % mechanic.cooldown() != 0) continue;
            if (this.timer == 0) continue; // 开场第 0 tick 不触发（转阶段 timer 重置后避免连发）
            final int triggerTimer = this.timer;
            Runnable fire = () -> {
                // r85：机制释放统一延后 15 tick——先播 *_du_tiao 法阵动画，法阵展开到位时机制刚好生效。
                this.frb$triggerCastAnim();
                // r93：施法喊话（30% 概率 + 10s 冷却，避免刷屏）
                if (this.frbCastTalkCd <= 0 && level.random.nextFloat() < 0.3F) {
                    this.frbCastTalkCd = 200;
                    frbSay(level, "frb.boss.say.cast_" + level.random.nextInt(4));
                }
                this.activeMechanicCount++;
                this.scheduleDelayedTask(15, () -> {
                    mechanic.tick(this, level, triggerTimer);
                    this.activeMechanicCount--;
                    // r96：天空阶段每释放 2~4 个机制后随机传送一次，更新悬停锚点
                    if (this.phase == 4) {
                        this.skyMechanicReleased++;
                        if (this.skyMechanicReleased >= this.skyTeleportThreshold) {
                            teleportNearPlayer(level);
                            this.skyAnchorX = this.getX();
                            this.skyAnchorZ = this.getZ();
                            this.skyMechanicReleased = 0;
                            this.skyTeleportThreshold = 2 + level.getRandom().nextInt(3); // 2~4
                        }
                    }
                    // 释放完毕后尝试把队列里的下一个机制排上
                    tryDrainMechanicQueue();
                });
            };
            if (casting) {
                // 正在释放技能，暂存
                this.mechanicQueue.add(fire);
            } else {
                // 空闲，直接释放
                fire.run();
                casting = true; // 本 tick 后续其他机制也入队，避免同一 tick 多个机制同时启动
            }
        }
        } // end skillDisabled guard
        // C2 / D1 蓄力倒计时
        if (this.flameChargeTicks > 0) {
            tickFlameCharge(level);
        }
        if (this.frostGazeTicks > 0) {
            tickFrostGaze(level);
        }
        // A6 激光读条（r66）
        if (this.beamChargeTicks > 0) {
            tickPetalBeamCharge(level);
        }
        // r90：技能读条递减 + 升空悬浮 + 归零恢复
        if (this.skillCastTicks > 0) {
            tickSkillCast(level);
        }
        // r90：云雨传送无敌递减
        if (this.rainTeleportInvulnTicks > 0) {
            this.rainTeleportInvulnTicks--;
        }
        // I2 时间停止：冻结维持 + 连招触发（r67）
        if (this.timeStopTicks > 0) {
            tickTimeStop(level);
        }
        // I2 时间倒流：位置历史采样（每 5 tick，r67）
        if (this.tickCount % TimeRewindTracker.SAMPLE_INTERVAL == 0) {
            this.rewindTracker.sample(level, this);
        }
        // 万花凋零咏唱驱动（H2 终结技 / 时间阶段被 "授" 后的兜底）
        if (this.milleFlorumCastTick > 0) {
            tickMilleFlorum(level);
        }
        // r64：咏唱退出后 boss 可能已被 discard（milleFlorumExit → discard），
        // 但 aiStep 还在本 tick 内继续跑，switch/case 6/decayPhase 会再次走到
        // onAllFlowersDefeated 并重新启动一次终结技，引发崩溃循环。
        if (this.isRemoved()) return;
        switch (this.phase) {
            case 0 -> naturePhase(level);
            case 1 -> burnPhase(level);
            case 2 -> frostPhase(level);
            case 3 -> rainPhase(level);
            case 4 -> skyPhase(level);
            case 5 -> voidPhase(level);
            case 6 -> decayPhase(level);
            case PHASE_TIME -> timePhase(level);
            default -> decayPhase(level);
        }
        if (this.phase <= 3) {
            hoverMove(level);
        }
        // 自管延迟任务推进
        tickPendingTasks();
    }

    // ============================================================
    // r93：boss 说话系统（参考梦幻终焉 UomWither：LongLookAtPlayerGoal 盯人说话
    //      + ChanBegGoal 只对创造玩家"害羞盯回去"。本 boss 渲染为手绘路径不动，
    //      害羞表现 = 聊天颜文字 + 爱心粒子 + 音效 + 服务端 lookControl 盯回去）。
    // ============================================================

    /** 以「<芙莉艾丝> 台词」格式广播给 96 格内所有玩家（聊天栏）。 */
    public void frbSay(ServerLevel level, String key) {
        net.minecraft.network.chat.MutableComponent msg = Component.literal("<")
                .append(Component.translatable("frb.boss.say.name"))
                .append("> ")
                .append(Component.translatable(key));
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(96.0D))) {
            p.sendSystemMessage(msg);
        }
    }

    /** 只发给单个玩家（仁慈救援台词等定向台词）。 */
    public void frbSayTo(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.literal("<")
                .append(Component.translatable("frb.boss.say.name"))
                .append("> ")
                .append(Component.translatable(key)));
    }

    /** 主 tick 钩子：低频战斗闲聊 + 创造玩家盯视害羞。 */
    private void frbTalkTick(ServerLevel level) {
        if (this.frbCastTalkCd > 0) this.frbCastTalkCd--;
        // 战斗闲聊：有攻击目标时 cd 归零后每 tick 1/200 概率说一句（期望 ~10s），说完进 24s 冷却
        if (this.frbTalkCd > 0) {
            this.frbTalkCd--;
        } else if (this.getTarget() != null && this.random.nextInt(200) == 0) {
            this.frbTalkCd = 480;
            frbSay(level, "frb.boss.say.idle_" + this.random.nextInt(4));
        }
        // 创造模式盯视害羞：同一创造玩家准星连续对准 boss 满 3 秒触发
        if (this.frbShyCd > 0) {
            this.frbShyCd--;
        } else {
            ServerPlayer watcher = frbFindShyWatcher(level);
            if (watcher != null && watcher.getUUID().equals(this.frbShyWatcher)) {
                this.frbShyStareTicks++;
            } else {
                this.frbShyWatcher = watcher == null ? null : watcher.getUUID();
                this.frbShyStareTicks = watcher == null ? 0 : 1;
            }
            if (watcher != null && this.frbShyStareTicks >= 60) {
                this.frbShyStareTicks = 0;
                this.frbShyCd = 600; // 30s 内不再害羞，避免刷屏
                this.getLookControl().setLookAt(watcher, 10.0F, (float) this.getMaxHeadXRot());
                frbSay(level, "frb.boss.say.shy_" + this.random.nextInt(3));
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                        this.getX(), this.getY() + 2.2D, this.getZ(), 6, 0.5D, 0.4D, 0.5D, 0.0D);
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F, 1.6F);
            }
        }
    }

    /** 24 格内准星正对 boss（视角点积 > 0.985 ≈ 10° 内）且有视线的创造玩家。 */
    private ServerPlayer frbFindShyWatcher(ServerLevel level) {
        Vec3 eyes = this.getEyePosition();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(24.0D), Player::isCreative)) {
            if (!p.hasLineOfSight(this)) continue;
            Vec3 toBoss = eyes.subtract(p.getEyePosition());
            if (toBoss.lengthSqr() < 1.0E-4D) continue;
            if (p.getViewVector(1.0F).normalize().dot(toBoss.normalize()) > 0.985D) return p;
        }
        return null;
    }

    /** r93：radius 内最近的生存/冒险玩家（代替会点到创造玩家的 getNearestPlayer/getRandomPlayer）。 */
    private ServerPlayer frbNearestFighter(ServerLevel level, double radius) {
        ServerPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (ServerPlayer p : cn.blockforge.generated.floralritebloomcurios.util.Mechanics
                .playersInDomain(level, this, radius)) {
            double d = p.distanceToSqr(this);
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    /** 开场动画三幕：登场悬浮（40t）→ 星系双螺旋汇聚（120t）→ 爆发落地（40t），全程无敌。 */
    private void introTick(ServerLevel level) {
        if (Double.isNaN(this.introGroundY)) {
            this.introGroundY = this.getY();
        }
        this.introTicks--;
        this.entityData.set(DATA_INTRO, this.introTicks > 0);
        int t = this.introTicks; // 199..0
        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = false;
        this.fallDistance = 0.0F;

        // 悬浮高度：前 40 tick 缓升 2.8 格，中段保持，最后 40 tick 缓降
        double rise;
        if (t > 160) {
            float p = (200 - t) / 40.0F;
            rise = 2.8D * (1.0D - (1.0D - p) * (1.0D - p));
        } else if (t > 40) {
            rise = 2.8D;
        } else {
            float p = t / 40.0F;
            rise = 2.8D * p * p;
        }
        this.setPos(this.getX(), this.introGroundY + rise, this.getZ());

        if (t == 199) { // 第一幕开启：阶段标题 + 召唤音
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(96.0D))) {
                FRBNetwork.sendPhaseTitle(p, this.getId(), -1);
            }
            // 目睹苏醒的成就（仅首次开场演出触发；老存档 intro=0 不进此分支）
            cn.blockforge.generated.floralritebloomcurios.util.FRBAdvancements
                    .awardAround(level, this, 96.0D, "world_flower_awaken");
            level.playSound(null, this,
                    cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.FRILIAS_AWAKEN.get(),
                    SoundSource.HOSTILE, 3.0F, 0.6F);
        }
        if (t > 160) { // 第一幕：星云雾气环绕升起
            if (t % 3 == 0) {
                double a = this.tickCount * 0.35D;
                level.sendParticles(FRBParticles.INTRO_WISP.get(),
                        this.getX() + Math.cos(a) * 3.2D,
                        this.getY() - 0.5D + Math.sin(a * 0.7D) * 0.8D,
                        this.getZ() + Math.sin(a) * 3.2D,
                        1, 0.0D, 0.01D, 0.0D, 0.0D);
            }
        } else if (t > 40) { // 第二幕：双螺旋星系汇聚 + 中心光球渐亮
            int p = 160 - t; // 0..119
            double frac = p / 120.0D;
            double angle = p * 0.28D;
            double radius = 6.5D * (1.0D - frac) + 0.4D;
            double h = -1.5D + frac * 4.0D;
            for (int s = 0; s < 2; s++) {
                double a = angle + s * Math.PI;
                level.sendParticles(FRBParticles.INTRO_SPIRAL.get(),
                        this.getX() + Math.cos(a) * radius, this.getY() + h,
                        this.getZ() + Math.sin(a) * radius, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                if (p % 3 == 0) {
                    level.sendParticles(FRBParticles.INTRO_STAR.get(),
                            this.getX() + Math.cos(a + 0.5D) * radius * 1.15D,
                            this.getY() + h + 0.6D,
                            this.getZ() + Math.sin(a + 0.5D) * radius * 1.15D,
                            1, 0.0D, 0.0D, 0.0D, 0.01D);
                }
            }
            if (p % 4 == 0) {
                level.sendParticles(FRBParticles.INTRO_BLOOM.get(),
                        this.getX(), this.getY() + 1.2D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            if (t % 25 == 0) {
                level.playSound(null, this, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 2.0F, 1.2F);
            }
        } else { // 第三幕：爆发落地
            if (t == 40) {
                FRBNetwork.sendBossFx(level, this,
                        BossFxPacket.FX_SHAKE | BossFxPacket.FX_FLASH | BossFxPacket.FX_SHOCKWAVE,
                        1.0F, 30);
                for (int i = 0; i < 40; i++) {
                    double a = i / 40.0D * Math.PI * 2.0D;
                    level.sendParticles(FRBParticles.ECLIPSE_RING.get(),
                            this.getX() + Math.cos(a) * 2.0D, this.getY() + 1.2D,
                            this.getZ() + Math.sin(a) * 2.0D,
                            1, Math.cos(a) * 0.35D, 0.05D, Math.sin(a) * 0.35D, 0.0D);
                }
                ringBurst(level, PHASE_COLORS[0]);
                level.playSound(null, this, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5F, 1.0F);
                level.playSound(null, this, SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 2.0F, 0.8F);
            }
            if (t % 2 == 0 && t < 40) {
                level.sendParticles(FRBParticles.PULSE_RING.get(),
                        this.getX(), this.getY() + 0.8D, this.getZ(), 2, 0.8D, 0.2D, 0.8D, 0.0D);
            }
            if (t == 0) {
                level.playSound(null, this,
                        cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.FRILIAS_PHASE.get(),
                        SoundSource.HOSTILE, 3.0F, 0.7F);
                level.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + 1.0D, this.getZ(), 24, 1.2D, 0.8D, 1.2D, 0.06D);
            }
        }
    }

    /**
     * 自然至云雨阶段：定点悬浮（不追踪玩家，追人欲望是"封印领域像全局效果"的根源），
     * 仅保持悬浮高度与面向目标；近战惩罚保留——玩家自己贴脸才吃攻击，
     * 走位压力完全交给各阶段机制（荆棘/生之焰/冰刺等，已加密）。
     */
    private void hoverMove(ServerLevel level) {
        this.setNoGravity(true);
        this.getNavigation().stop();

        int x = Mth.floor(this.getX());
        int z = Mth.floor(this.getZ());
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        double hoverY = groundY + 3.0D; // r98：所有阶段统一离地 3 格
        double dy = Mth.clamp((hoverY - this.getY()) * 0.22D, -0.3D, 0.3D);

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double horizontal = Math.hypot(dx, dz);
            // 面向目标：服务端三值同步 + DATA_YAW 广播。悬浮 Boss 几乎无位移，客户端
            // yBodyRot 靠位移推算不跟手（只转头不转身的根因），客户端 tick 里按 DATA_YAW 硬对齐
            if (horizontal > 0.01D) {
                float yaw = (float) Mth.atan2(dz, dx) * (180.0F / (float) Math.PI) - 90.0F;
                if (Float.isFinite(yaw)) { // 防坐标异常产生的 NaN 传染朝向与同步值
                    this.setRot(yaw, this.getXRot());
                    this.yHeadRot = yaw;
                    this.yBodyRot = yaw;
                    this.entityData.set(DATA_YAW, yaw);
                }
            }
            // 贴身近战惩罚：玩家主动靠近到触及范围才会挨打
            if (this.meleeCooldown > 0) {
                this.meleeCooldown--;
            } else if (horizontal <= 2.4D && this.doHurtTarget(target)) {
                this.meleeCooldown = 16;
            }
        }
        this.setDeltaMovement(0.0D, dy, 0.0D);
        this.hasImpulse = true;
    }

    /** oldPhase 阶段结束：锁血、转阶段、清除残留增益（花灵封印由封印领域每秒刷新）。 */
    private void beginTransition(ServerLevel level, int oldPhase) {
        this.phase = oldPhase + 1;
        this.transition = TRANSITION_TICKS;
        this.timer = 0;
        this.entityData.set(DATA_PHASE, this.phase);
        this.entityData.set(DATA_TRANSITION, this.transition);
        // r73 L1：转阶段触发 phase_open 一次性播放新阶段的属性魔法阵展开动画
        this.frb$triggerPhaseOpen(this.phase);
        this.rainTeleports = 0;
        // r96：转阶段清空技能暂存队列，避免上一阶段 cd 到点的机制在新阶段误触发
        this.mechanicQueue.clear();
        this.activeMechanicCount = 0;
        this.bossEvent.setColor(BAR_COLORS[this.phase]);
        this.bossEvent.setName(barName());
        this.bossEvent.setProgress(0.0F);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.clearFire();
        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(96.0D));
        for (ServerPlayer p : nearby) {
            FRBNetwork.sendPhaseTitle(p, this.getId(), this.phase);
        }
        // r93：转阶段台词（阶段 1~6 各一句）
        frbSay(level, "frb.boss.say.phase_" + this.phase);
        // 阶段结束后清除残留增益
        if (oldPhase == 2 || oldPhase == 3) { // 霜华→清霜蚀/霜之华；云雨→清云之华
            for (ServerPlayer p : nearby) {
                p.removeEffect(FRBEffects.FROSTBITE.get());
                p.removeEffect(FRBEffects.FROST_BLOOM.get());
                p.removeEffect(FRBEffects.CLOUD_GRACE.get());
                p.displayClientMessage(Component.translatable("frb.boss.buffclear")
                        .withStyle(ChatFormatting.AQUA), true);
            }
        }
        if (oldPhase == 1) { // 焚尽结束 → 清除周围 32 格生之焰
            sweepLifeFire(level);
        }
        if (oldPhase == 4) { // 天空 → 落地
            this.setNoGravity(false);
            landAtGround(level);
        }
        if (oldPhase == 3) { // r64：云雨 → 积雨云全清，避免积雨云跨阶段残留
            for (RainCloudEntity cloud : level.getEntitiesOfClass(RainCloudEntity.class,
                    this.getBoundingBox().inflate(64.0D))) {
                cloud.discard();
            }
            // r96：云雨结束 → 清除万变水波
            clearWaterRipples(level);
        }
        if (oldPhase == 5) { // 虚空 → 收走全部裂隙
            for (VoidRiftEntity rift : level.getEntitiesOfClass(VoidRiftEntity.class,
                    this.getBoundingBox().inflate(64.0D))) {
                rift.discard();
            }
        }
        // r96：天空阶段结束 → 重置固定悬停锚点，下次进天空阶段重新选取
        if (oldPhase == 4) {
            this.skyAnchorSet = false;
        }
        level.playSound(null, this, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 0.8F);
        ringBurst(level, PHASE_COLORS[this.phase]);
        // 转阶段演出：镜头震动 + FOV 脉冲 + 白闪渐变 + 主题色脉冲环与微粒
        FRBNetwork.sendBossFx(level, this,
                BossFxPacket.FX_SHAKE | BossFxPacket.FX_FLASH, 0.7F, 25);
        // 全屏后处理：屏幕浸染为新阶段主题色并缓慢消退（各阶段转场一致，与枯萎的灰度后处理同级）
        FRBNetwork.sendBossFx(level, this, BossFxPacket.FX_TINT,
                0.65F, 50, PHASE_COLORS[this.phase]);
        level.sendParticles(FRBParticles.themeMote(this.phase),
                this.getX(), this.getY() + 1.2D, this.getZ(), 48, 2.6D, 1.2D, 2.6D, 0.02D);
        for (int i = 0; i < 32; i++) {
            double a = i / 32.0D * Math.PI * 2.0D;
            level.sendParticles(FRBParticles.PULSE_RING.get(),
                    this.getX() + Math.cos(a) * 2.4D, this.getY() + 1.2D,
                    this.getZ() + Math.sin(a) * 2.4D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

    }

    private void enterPhaseAbilities(ServerLevel level) {
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            switch (this.phase) {
                case 4 -> speed.setBaseValue(0.45D);
                case 6 -> speed.setBaseValue(0.24D);
                default -> speed.setBaseValue(0.33D);
            }
        }
        switch (this.phase) {
            case 3 -> { // 云雨：4 枚环绕符文 = 4 次传送
                this.entityData.set(DATA_RUNES, 4);
                // r96：进入云雨阶段立即生成 4 个万变水波（正点或斜点随机）
                clearWaterRipples(level);
                this.waterRippleDiagonal = level.getRandom().nextBoolean();
                spawnWaterRipples(level);
            }
            case 5 -> { // 虚空：离地 2.5 格悬浮、静止、不攻击
                this.setNoGravity(true);
                this.setTarget(null);
                this.getNavigation().stop();
                this.setDeltaMovement(Vec3.ZERO);
                // r77：进入虚空阶段立刻喷 6 条（用 cd = 40 tick 跑一次）：
                // boss.spawnSingleVoidRift(...) 内部已经做了"已有 ≤ 12"上限，
                // 落地位置按玩家附近 3~8 格散布，避免堆叠。
                for (int i = 0; i < 6; i++) {
                    this.spawnSingleVoidRift(level);
                }
            }
            case 6 -> { // 枯萎：只剩 10 点血量 + 三朵环绕的死亡之花护命
                this.setNoGravity(true); // r98：统一离地 3 格
                this.setTarget(null);
                var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(10.0D);
                }
                this.setHealth(10.0F);
                this.flowerIds.clear();
                for (int i = 0; i < 3; i++) {
                    DeathFlowerEntity flower =
                            new DeathFlowerEntity(FRBEntities.DEATH_FLOWER.get(), level);
                    flower.setPos(this.getX(), this.getY() + 2.0D, this.getZ());
                    flower.bind(this.getUUID(), i);
                    level.addFreshEntity(flower);
                    this.flowerIds.add(flower.getUUID());
                }
                level.playSound(null, this,
                        cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.FRILIAS_DEATH.get(),
                        SoundSource.HOSTILE, 2.0F, 1.4F);
            }
            case PHASE_TIME -> { // r77：时间阶段 → 600 HP + 8 护甲（玩家应该能打死终结）
                this.timePhaseActive = true;
                var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(600.0D);
                }
                var armor = this.getAttribute(Attributes.ARMOR);
                if (armor != null) {
                    armor.setBaseValue(8.0D);
                }
                // 进入时回满血（前面 6 个阶段的血量残余在这里清零）。
                this.setHealth(this.getMaxHealth());
                // r100：清除前阶段残留的减伤 debuff，统一改为「时之华」
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                        this.getBoundingBox().inflate(64.0D))) {
                    p.removeEffect(FRBEffects.FROST_BLOOM.get());
                    p.removeEffect(FRBEffects.CLOUD_GRACE.get());
                }
            }
            default -> { }
        }
    }

    // ---- 各阶段能力 ----

    /** 自然：boss 左右两侧生成法阵 → 锁定玩家 → 沿方向发射荆棘（endr dragon 风格），配合高速再生形成消耗战。 */
    private void naturePhase(ServerLevel level) {
        // r64 endr-dragon 风格荆棘：boss 左右两侧后方各 0.5 格处发射 1 颗荆棘飞弹（SpikeVolleyEntity），
        // 锁定玩家位置快照作为瞄准方向，不追踪。cd 200 tick (10 秒)。
        if (this.timer % FRBConfig.thornVolley() == 0 && !this.hasEffect(FRBEffects.DRUNK.get()) && this.getTarget() instanceof ServerPlayer tp && tp.isAlive()) {
            // r85：先播 *_du_tiao 法阵动画，荆棘发射延后 15 tick（法阵展开到位时荆棘刚好出手）
            this.frb$triggerCastAnim();
            // r96：在调度前锁定玩家位置快照——预警圈和弹道都指向同一固定点，
            //      玩家离开预警圈即可闪避（此前 aimPoint 在 lambda 内取值导致追踪）。
            final BlockPos warnPos = tp.blockPosition();
            final Vec3 aimPoint = tp.position().add(0.0D, 0.5D, 0.0D);
            final Vec3 bossPosSnap = this.position();
            this.scheduleDelayedTask(15, () -> {
                if (!tp.isAlive()) return;
                // r92：荆棘射击预警改用独立 AOE 法阵（aoe_circle.png + 绿色调），
                //      不再复用花之摇篮贴图——避免与 B1 机制的法阵在视觉上混淆成"双份"。
                AoeWarnSigilEntity.spawn(level, warnPos,
                        FlowerCradleSigilEntity.LIFETIME, 0.49F, 0.90F, 0.55F);
                Vec3 toTarget = aimPoint.subtract(bossPosSnap).normalize();
                Vec3 side = new Vec3(-toTarget.z, 0.0D, toTarget.x).normalize();
                for (int sideIdx = -1; sideIdx <= 1; sideIdx += 2) {
                    // boss 侧后方 0.5 格：side * 1.8（左右）+ -look * 0.5（后方）
                    double sx = bossPosSnap.x + side.x * 1.8D * sideIdx - toTarget.x * 0.5D;
                    double sy = bossPosSnap.y + 1.0D;
                    double sz = bossPosSnap.z + side.z * 1.8D * sideIdx - toTarget.z * 0.5D;
                    Vec3 from = new Vec3(sx, sy, sz);
                    Vec3 dir = aimPoint.subtract(from).normalize();
                    SpikeVolleyEntity.shoot(level, from, dir, SpikeVolleyEntity.KIND_THORN, this);
                }
                // 远程锁定提示：玩家头顶绿色 marker（4 秒）+ 玩家脚下 1 颗 mycelium 粒子
                FRBNetwork.sendMarker(tp, 60, 0x7ce68b);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.MYCELIUM,
                        tp.getX(), tp.getY() + 0.1D, tp.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            });
        }
        // r69（bug2）：激光内置 cd 40→160 tick（8 秒），此前 2 秒一发压制感过强；r70 读 config
        if (this.timer % FRBConfig.natureBeam() == 0 && !this.hasEffect(FRBEffects.DRUNK.get()) && this.getTarget() instanceof ServerPlayer btp && btp.isAlive()) {
            // r72：伤害 = base × 全局倍率 × 自然阶段倍率
            startPetalBeamCharge(level, btp, FRBConfig.dmg(3.0F) * FRBConfig.dmgNatureBeam(), PHASE_COLORS[0], MobEffects.POISON);
        }
    }

    /** 焚尽：Boss 附近的花草植物全部点燃为生之焰（树木在树冠下生火），并在玩家周围地面催生生之焰形成走位压迫。 */
    private void burnPhase(ServerLevel level) {
        // r78：焚尽阶段持续在 boss 周围撒一层焚尽主题微粒（解决用户截图 #6 反馈的"粒子缺失"）。
        //      themeMote(1) 对应 EMBER_VINE 主题色（橙红）；4 tick 一轮，每轮 8 颗，均匀散布 boss 身上。
        if (this.tickCount % 4 == 0) {
            for (int i = 0; i < 8; i++) {
                double a = (this.tickCount * 0.07D) + i * (Math.PI * 2.0D / 8.0D);
                level.sendParticles(FRBParticles.themeMote(this.phase),
                        this.getX() + Math.cos(a) * 1.6D, this.getY() + 1.6D + Math.sin(this.tickCount * 0.1D) * 0.2D,
                        this.getZ() + Math.sin(a) * 1.6D,
                        1, 0.0D, 0.05D, 0.0D, 0.0D);
            }
        }
        BlockState lifeFire = FRBBlocks.LIFE_FIRE.get().defaultBlockState();
        if (this.timer % FRBConfig.burnLifeFire() == 0 && !this.hasEffect(FRBEffects.DRUNK.get()) && this.getTarget() != null) {
            BlockPos center = this.blockPosition();
            BlockPos min = center.offset(-12, -3, -12);
            BlockPos max = center.offset(12, 3, 12);
            // 区块未全部加载时跳过本轮：对未加载区块的 getBlockState 会同步触发生成，
            // boss 战斗中高速绕圈时会把区块依赖链引爆（服务线程卡死数十秒，见 frb_watchdog 转储）
            if (areaLoaded(level, min, max)) {
                for (BlockPos p : BlockPos.betweenClosed(min, max)) {
                    BlockState state = level.getBlockState(p);
                    // r65 修复：花/草/紫水晶簇——原地替换为生之焰附层（不再在其上方浮空生成）；
                    // 原木/树叶维持旧行为：在其顶面生成一片（模拟树冠下生火）。
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                        BlockPos above = p.above();
                        if (level.isEmptyBlock(above)) {
                            level.setBlockAndUpdate(above, lifeFire);
                        }
                    } else if (cn.blockforge.generated.floralritebloomcurios.block.LifeFireBlock
                            .isReplaceableFlora(state)) {
                        level.setBlockAndUpdate(p, lifeFire);
                    }
                }
            }
        }
        // 无树环境（永恒花园）也有效：附近玩家脚下四周随机冒出生之焰
        if (this.timer % FRBConfig.burnFireSpout() == 0 && !this.hasEffect(FRBEffects.DRUNK.get()) && this.getTarget() != null) {
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(32.0D))) {
                RandomSource r = level.getRandom();
                for (int i = 0; i < 3; i++) {
                    BlockPos base = p.blockPosition().offset(r.nextIntBetweenInclusive(-4, 4), 0,
                            r.nextIntBetweenInclusive(-4, 4));
                    // getHeightmapPos 对未加载区块会同步触发生成——必须先检查目标区块
                    if (!level.hasChunk(SectionPos.blockToSectionCoord(base.getX()),
                            SectionPos.blockToSectionCoord(base.getZ()))) {
                        continue;
                    }
                    BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
                    // r103：getHeightmapPos 已返回地面上方的空气位置，直接放置；花/草/紫水晶簇原地替换
                    BlockState fs = level.getBlockState(ground);
                    if (fs.isAir() || cn.blockforge.generated.floralritebloomcurios.block.LifeFireBlock
                            .isReplaceableFlora(fs)) {
                        level.setBlockAndUpdate(ground, lifeFire);
                        level.sendParticles(ParticleTypes.FLAME,
                                ground.getX() + 0.5D, ground.getY() + 0.6D, ground.getZ() + 0.5D,
                                6, 0.2D, 0.2D, 0.2D, 0.01D);
                    }
                }
            }
        }
    }

    /** 清除 Boss 周围 32 格（竖直 ±16 格）内的所有生之焰，在焚尽阶段结束时调用。 */
    private void sweepLifeFire(ServerLevel level) {
        BlockPos center = this.blockPosition();
        BlockPos min = center.offset(-32, -16, -32);
        BlockPos max = center.offset(32, 16, 32);
        if (areaLoaded(level, min, max)) {
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                if (level.getBlockState(pos).is(FRBBlocks.LIFE_FIRE.get())) {
                    level.removeBlock(pos, false);
                }
            }
        }
        level.playSound(null, this, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.6F);
    }

    /** 矩形范围覆盖的所有区块都已加载（范围扫描前检查，防止同步触发区块生成）。 */
    private static boolean areaLoaded(ServerLevel level, BlockPos min, BlockPos max) {
        int minCX = SectionPos.blockToSectionCoord(min.getX());
        int maxCX = SectionPos.blockToSectionCoord(max.getX());
        int minCZ = SectionPos.blockToSectionCoord(min.getZ());
        int maxCZ = SectionPos.blockToSectionCoord(max.getZ());
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                if (!level.hasChunk(cx, cz)) return false;
            }
        }
        return true;
    }

    /** 霜华：范围 36 格；玩家持续获得【霜之华】（攻击伤害 -50%）；法阵预告后冰刺爆起。 */
    private void frostPhase(ServerLevel level) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(36.0D))) {
            // r101：无限时长，仅在未持有时施加；阶段结束统一清除
            if (!p.hasEffect(FRBEffects.FROST_BLOOM.get())) {
                p.addEffect(new MobEffectInstance(FRBEffects.FROST_BLOOM.get(), -1, 0, true, true));
            }
        }
        if (this.timer % FRBConfig.frostIceSpike() == 0 && !this.hasEffect(FRBEffects.DRUNK.get()) && this.getTarget() != null) {
            // r64 endr-dragon 风格冰刺：boss 左右两侧后方各 0.5 格发射 1 颗冰刺飞弹，
            // 锁定玩家位置快照作为瞄准方向，不追踪。
            if (this.getTarget() instanceof ServerPlayer tp && tp.isAlive()) {
                // r85：先播 *_du_tiao 法阵动画，冰刺发射延后 15 tick
                this.frb$triggerCastAnim();
                // r96：同自然阶段——调度前锁定位置快照，预警圈与弹道指向同一点。
                final BlockPos warnPos = tp.blockPosition();
                final Vec3 aimPoint = tp.position().add(0.0D, 0.5D, 0.0D);
                final Vec3 bossPosSnap = this.position();
                this.scheduleDelayedTask(15, () -> {
                    if (!tp.isAlive()) return;
                    // r92：冰刺双弹预警——aoe_circle.png + 冰蓝色调（与荆棘绿色区分）。
                    //      与荆棘共用同一独立预警实体，不再依赖花之摇篮贴图。
                    AoeWarnSigilEntity.spawn(level, warnPos,
                            FlowerCradleSigilEntity.LIFETIME, 0.62F, 0.86F, 1.0F);
                    Vec3 toTarget = aimPoint.subtract(bossPosSnap).normalize();
                    Vec3 side = new Vec3(-toTarget.z, 0.0D, toTarget.x).normalize();
                    for (int sideIdx = -1; sideIdx <= 1; sideIdx += 2) {
                        double sx = bossPosSnap.x + side.x * 1.8D * sideIdx - toTarget.x * 0.5D;
                        double sy = bossPosSnap.y + 1.0D;
                        double sz = bossPosSnap.z + side.z * 1.8D * sideIdx - toTarget.z * 0.5D;
                        Vec3 from = new Vec3(sx, sy, sz);
                        Vec3 dir = aimPoint.subtract(from).normalize();
                        SpikeVolleyEntity.shoot(level, from, dir, SpikeVolleyEntity.KIND_ICE, this);
                    }
                    FRBNetwork.sendMarker(tp, 30, 0x9fdcff);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                            tp.getX(), tp.getY() + 0.1D, tp.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                });
            }
        }
        // r69（bug2）：激光内置 cd 50→160 tick（8 秒）；r70 读 config
        if (this.timer % FRBConfig.frostBeam() == 0 && !this.hasEffect(FRBEffects.DRUNK.get()) && this.getTarget() != null) {
            // r72：伤害 = base × 全局倍率 × 霜华阶段倍率
            startPetalBeamCharge(level, this.getTarget(), FRBConfig.dmg(4.0F) * FRBConfig.dmgFrostBeam(), PHASE_COLORS[2], MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    /** 云雨：天之泪降雨、玩家攻击大减且恢复与流失反转；传送由 4 枚环绕符文计数，用尽后回血。 */
    private void rainPhase(ServerLevel level) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(40.0D))) {
            // r101：无限时长，仅在未持有时施加；阶段结束统一清除
            if (!p.hasEffect(FRBEffects.CLOUD_GRACE.get())) {
                p.addEffect(new MobEffectInstance(FRBEffects.CLOUD_GRACE.get(), -1, 0, true, true));
            }
        }
        double ratio = this.getHealth() / this.getMaxHealth();
        // r78：每 200 tick 在最近的玩家头顶召唤一朵积雨云（之前由 RainCloudMechanic 接管，
        //      但 tick 0 才初始化、新进入阶段不立即刷，导致玩家看不到积雨云）。
        if (this.timer % 200 == 0 && this.timer > 0) {
            Player nearestRain = frbNearestFighter(level, 32.0D); // r93：积雨云只追生存/冒险玩家
            if (nearestRain != null) {
                // r90：积雨云释放时升空 3 格 + 无敌约 1.25 秒（25 tick）+ 法阵读条动画
                this.beginSkillCast(level, 25);
                this.frb$triggerCastAnim();
                // r96：生成高度改为玩家头顶 +3，与 RainCloudEntity 的新悬停高度一致
                RainCloudEntity.spawn(level, nearestRain.getX(), nearestRain.getY() + 3.0D, nearestRain.getZ());
            }
        }
        // r96：云雨全屏 AOE（cd 480t = 24s），6 花灵伤害 + 2 格击退，释放后重置万变水波位置
        if (this.timer % FRBConfig.rainAoe() == 0 && this.timer > 0 && !this.hasEffect(FRBEffects.DRUNK.get()) && this.getTarget() != null) {
            castRainAoe(level);
        }
        if (ratio <= 1.0D - 0.2D * (this.rainTeleports + 1) && this.rainTeleports < 4) {
            this.rainTeleports++;
            this.entityData.set(DATA_RUNES, 4 - this.rainTeleports);
            teleportNearPlayer(level);
            // r90：传送后 3 秒无敌
            this.rainTeleportInvulnTicks = RAIN_TELEPORT_INVULN_TICKS;
            // r96：boss 因血量过低传送时，同步刷新一次万变水波位置（正点↔斜点互换）
            clearWaterRipples(level);
            this.waterRippleDiagonal = !this.waterRippleDiagonal;
            spawnWaterRipples(level);
            // r75：传送时显示"云雨的力量回溯了时间，剩余次数：x/4"（用户截图 #8 需求）
            int left = 4 - this.rainTeleports;
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(64.0D))) {
                p.displayClientMessage(Component.translatable("frb.mechanic.rain.teleport", left)
                        .withStyle(ChatFormatting.BLUE), true);
            }
            if (this.rainTeleports >= 4) {
                this.heal(this.getMaxHealth() * 0.15F);
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                        this.getBoundingBox().inflate(48.0D))) {
                    p.displayClientMessage(Component.translatable("frb.boss.rain.heal")
                            .withStyle(ChatFormatting.BLUE), true);
                }
                level.playSound(null, this, SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 2.0F, 0.8F);
                ringBurst(level, PHASE_COLORS[3]);
            }
        }
    }

    /**
     * r96 在 boss 周围 8 格生成 4 个万变水波。
     * 根据 waterRippleDiagonal 决定是正点（N/S/E/W）还是斜向（4 个对角线）。
     */
    private void spawnWaterRipples(ServerLevel level) {
        double bx = this.getX();
        double bz = this.getZ();
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(bx), Mth.floor(bz));
        double baseY = gy + 0.05D;
        double dist = 8.0D;
        double[][] offsets;
        if (this.waterRippleDiagonal) {
            // 斜向：4 个对角线，距离 8 格
            double d = dist / Math.sqrt(2.0D);
            offsets = new double[][]{{d, d}, {-d, d}, {d, -d}, {-d, -d}};
        } else {
            // 正点：N/S/E/W
            offsets = new double[][]{{dist, 0}, {-dist, 0}, {0, dist}, {0, -dist}};
        }
        // 云雨阶段很长（直到血量见底），给 12000 tick（10 分钟）存活期，阶段结束时手动清理
        int lifetime = 12000;
        for (double[] off : offsets) {
            WaterRippleEntity e = WaterRippleEntity.spawn(level, bx + off[0], baseY, bz + off[1], lifetime);
            this.waterRippleIds.add(e.getUUID());
        }
    }

    /** r96 清除所有万变水波实体。 */
    private void clearWaterRipples(ServerLevel level) {
        for (java.util.UUID id : this.waterRippleIds) {
            if (level.getEntity(id) instanceof WaterRippleEntity w) {
                w.discard();
            }
        }
        this.waterRippleIds.clear();
    }

    /**
     * r96 云雨全屏 AOE：对领域内所有玩家造成 6 点花灵伤害 + 2 格击退，
     * 释放后重置万变水波位置（正点↔斜点互换）。
     */
    private void castRainAoe(ServerLevel level) {
        Holder<net.minecraft.world.damagesource.DamageType> floralHolder = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(cn.blockforge.generated.floralritebloomcurios.registry.FRBDamageTypes.FLORAL);
        DamageSource src = new DamageSource(floralHolder, this, this);
        float dmg = FRBConfig.dmg(9.0F) * FRBConfig.dmgRainAoe();
        for (ServerPlayer p : cn.blockforge.generated.floralritebloomcurios.util.Mechanics
                .playersInDomain(level, this, 48.0D)) {
            if (p.hurt(src, dmg)) {
                // 2 格击退：从 boss 向外推开（方向 = 玩家→boss 的反方向）
                double dx = p.getX() - this.getX();
                double dz = p.getZ() - this.getZ();
                double len = Math.max(0.001D, Math.hypot(dx, dz));
                p.knockback(1.0F, dx / len, dz / len);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        p.getX(), p.getY() + 1.0D, p.getZ(), 8, 0.4D, 0.5D, 0.4D, 0.1D);
            }
        }
        // 全屏雨滴粒子提示
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(48.0D))) {
            level.sendParticles(ParticleTypes.RAIN,
                    p.getX(), p.getY() + 3.0D, p.getZ(), 60, 1.0D, 1.0D, 1.0D, 0.1D);
        }
        level.playSound(null, this, SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.HOSTILE, 2.0F, 0.7F);
        // r96：从 boss 中心向外扩散的蓝色水花冲击波（可视化全屏 AOE 范围）
        expandingWave(level, ParticleTypes.SPLASH, 32.0D, 80, 0.1D);
        expandingWave(level, ParticleTypes.FALLING_WATER, 24.0D, 64, 0.5D);
        // 释放后重置万变水波位置：正点↔斜点互换
        clearWaterRipples(level);
        this.waterRippleDiagonal = !this.waterRippleDiagonal;
        spawnWaterRipples(level);
    }

    private void teleportNearPlayer(ServerLevel level) {
        // r93：传送落点只取生存/冒险玩家（getRandomPlayer 会点到创造玩家）
        LivingEntity t = this.getTarget() != null ? this.getTarget() : frbNearestFighter(level, 64.0D);
        if (t == null) return;
        RandomSource r = level.getRandom();
        double a = r.nextDouble() * Math.PI * 2.0D;
        // 距离 6→10：用户在 r63 补丁里要求云雨阶段传送距离增加 4 格或以上，
        // 避免贴身传送后 PhaseMechanic.LightningMechanic/RainCloudMechanic 难以触发。
        double d = 10.0D;
        double x = t.getX() + Math.cos(a) * d;
        double z = t.getZ() + Math.sin(a) * d;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                this.getX(), this.getY() + 1.0D, this.getZ(), 40, 0.5D, 1.0D, 0.5D, 0.2D);
        this.teleportTo(x, t.getY() + 0.2D, z);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                x, this.getY() + 1.0D, z, 40, 0.5D, 1.0D, 0.5D, 0.2D);
        level.playSound(null, this, SoundEvents.SHULKER_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.9F);
    }

    /** 天空：r90 固定在玩家上方 3 格悬浮，面朝玩家，释放技能时不再升空（beginSkillCast 跳过 phase 4）。 */
    private void skyPhase(ServerLevel level) {
        this.setNoGravity(true);
        this.getNavigation().stop();
        LivingEntity t = this.getTarget();
        if (t == null) {
            t = frbNearestFighter(level, 64.0D); // r93：只取生存/冒险玩家
        }
        // r96：天空阶段 boss 固定在一个悬停点，不再跟随玩家头顶跑动；
        //      进阶段首 tick 以当前位置为锚点，之后只调整 Y 高度并原地转向看向玩家。
        if (!this.skyAnchorSet) {
            this.skyAnchorX = this.getX();
            this.skyAnchorZ = this.getZ();
            this.skyAnchorSet = true;
            // r96：进天空阶段时初始化传送阈值（2~4 个机制后传送）
            this.skyMechanicReleased = 0;
            this.skyTeleportThreshold = 2 + level.getRandom().nextInt(3);
        }
        double targetX = this.skyAnchorX;
        double targetZ = this.skyAnchorZ;
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(targetX), Mth.floor(targetZ));
        double targetY = gy + 3.0D;
        double dx = Mth.clamp((targetX - this.getX()) * 0.15D, -0.3D, 0.3D);
        double dy = Mth.clamp((targetY - this.getY()) * 0.15D, -0.3D, 0.3D);
        double dz = Mth.clamp((targetZ - this.getZ()) * 0.15D, -0.3D, 0.3D);
        this.setDeltaMovement(dx, dy, dz);
        this.hasImpulse = true;
        // 始终面朝玩家（即使位置固定，也转头看向目标）
        if (t != null) {
            Vec3 faceDir = t.getEyePosition().subtract(this.getEyePosition());
            if (faceDir.lengthSqr() > 0.0001D) {
                // 注意：Minecraft setYRot/setXRot 接受角度制，需将 atan2 弧度转为角度
                float yaw = (float) Mth.atan2(faceDir.z, faceDir.x) * (180.0F / (float) Math.PI) - 90.0F;
                float pitch = (float) -Math.toDegrees(Math.atan2(faceDir.y, Math.hypot(faceDir.x, faceDir.z)));
                if (Float.isFinite(yaw)) {
                    this.setYRot(yaw);
                    this.setXRot(pitch);
                    this.setRot(yaw, pitch);
                    this.yHeadRot = yaw;
                    this.yBodyRot = yaw;
                    this.entityData.set(DATA_YAW, yaw);
                    this.setYHeadRot(yaw);
                }
            }
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
        // r86：风眼月环锁定计时递减（每 tick）
        if (this.windEyeLockTicks > 0) this.windEyeLockTicks--;
        if (this.timer % FRBConfig.skyShockwave() == 0 && !this.hasEffect(FRBEffects.DRUNK.get()) && this.getTarget() != null) {
            // r85：先播 *_du_tiao 法阵动画，冲击波结算延后 15 tick
            this.frb$triggerCastAnim();
            this.scheduleDelayedTask(15, () -> {
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                        this.getBoundingBox().inflate(16.0D))) {
                    Vec3 dir = p.position().subtract(this.position());
                    double len = Math.max(0.001D, dir.length());
                    p.setDeltaMovement(dir.x / len * 0.9D, -0.4D, dir.z / len * 0.9D);
                    p.hurtMarked = true;
                    // r72：天空冲击波伤害 = base × 全局倍率 × 天空阶段倍率
                    p.hurt(level.damageSources().mobAttack(this), FRBConfig.dmg(3.0F) * FRBConfig.dmgSkyShockwave());
                    level.sendParticles(ParticleTypes.CLOUD,
                            p.getX(), p.getY() + 1.0D, p.getZ(), 10, 0.3D, 0.5D, 0.3D, 0.0D);
                }
                level.playSound(null, this, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.0F, 1.6F);
                // r86：击退后必接一个月环——玩家被击退后须立刻回头靠近 boss 躲入风眼
                this.castWindEye(level, false);
            });
        }
        // r75：天空阶段额外风刃——独立触发（之前由 PhaseMechanic.WindBladeMechanic 在 timer % 200 == 0
        // 时喷一次，与 shockwave 经常错过；现在按 windBlade() cd 主动喷，让天空阶段机制更密集。
        if (this.timer % Math.max(20, FRBConfig.windBlade()) == 0) {
            // r85：先播 *_du_tiao 法阵动画，风刃发射延后 15 tick
            this.frb$triggerCastAnim();
            final double windAngle = this.tickCount * 0.13D;
            this.scheduleDelayedTask(15, () -> {
                cn.blockforge.generated.floralritebloomcurios.entity.WindBladeEntity.shoot(
                        level, this.position().add(0, 0.5D, 0),
                        new Vec3(Math.cos(windAngle), -0.15D, Math.sin(windAngle)).normalize(),
                        this);
                for (ServerPlayer p : cn.blockforge.generated.floralritebloomcurios.util.Mechanics
                        .playersInDomain(level, this, 32.0D)) {
                    p.displayClientMessage(Component.translatable("frb.mechanic.sky.wind_blade")
                            .withStyle(ChatFormatting.WHITE), true);
                }
            });
        }
        // r86：偶尔独立释放风眼月环（击退链式的那次不算，由 windEye() cd 控制）
        if (this.windEyeLockTicks <= 0
                && this.timer % Math.max(40, FRBConfig.windEye()) == 0
                && this.timer > 0) {
            this.castWindEye(level, true);
        }
        // r86：羽剑冲锋——按 featherSword() cd 朝当前目标方向释放
        // r90：进阶段后 100 tick（5 秒）必定首发一次，避免天空阶段过短或与开场特效重叠导致玩家从没见过
        int swordCd = Math.max(40, FRBConfig.featherSword());
        if (t != null && (this.timer == 100
                || (this.timer > 100 && this.timer % swordCd == 0))) {
            final LivingEntity swordTarget = t;
            this.frb$triggerCastAnim();
            this.scheduleDelayedTask(15, () -> this.castFeatherSword(level, swordTarget));
        }
    }

    /**
     * r86 月环（风眼）：在 boss 位置放风眼预警贴图，60 tick 后结算环带伤害。
     * triggerAnim=true 时先播 *_du_tiao 法阵动画再预警（独立释放用）；
     * triggerAnim=false 时立即预警（击退链式接续，避免重复播法阵）。
     */
    private void castWindEye(ServerLevel level, boolean triggerAnim) {
        if (this.windEyeLockTicks > 0) return; // 已有月环在预警/结算中
        this.windEyeLockTicks = 75; // 60 预警 + 15 缓冲
        // r90：风眼预警全程无敌（天空阶段已悬浮 3 格，beginSkillCast 不再升空）
        this.beginSkillCast(level, 75);
        Runnable warning = () -> {
            double cx = this.getX();
            double cz = this.getZ();
            int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(cx), Mth.floor(cz));
            double groundY = Math.max(this.getY() - 5.0D, gy);
            // 发送风眼贴图预警（60 tick）
            cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork.sendWindEye(
                    level, cx, groundY + 0.05D, cz, 18.0F, 60);
            // r101：龙卷风特效——粒子从环带底部生成，带切向速度 + 上升速度，
            //      自然螺旋上升形成真正的龙卷风漏斗（而非固定高度的堆叠圆环）。
            double tornInner = 4.0D;
            double tornOuter = 18.0D;
            for (int t = 0; t < 60; t += 1) {
                final int ft = t;
                this.scheduleDelayedTask(t, () -> {
                    RandomSource rnd = level.getRandom();
                    // 每 tick 在环带内随机半径生成 8 颗螺旋上升粒子
                    for (int k = 0; k < 8; k++) {
                        double a = rnd.nextDouble() * Math.PI * 2.0D;
                        double r = tornInner + rnd.nextDouble() * (tornOuter - tornInner);
                        double px = cx + Math.cos(a) * r;
                        double pz = cz + Math.sin(a) * r;
                        double baseY = groundY + 0.3D + rnd.nextDouble() * 0.5D;
                        // 切向速度（逆时针旋转）+ 上升速度 + 微向内收（漏斗感）
                        double tanSpeed = 0.25D + rnd.nextDouble() * 0.15D;
                        double upSpeed = 0.18D + rnd.nextDouble() * 0.12D;
                        double inward = 0.02D;
                        double vx = -Math.sin(a) * tanSpeed - Math.cos(a) * inward;
                        double vz = Math.cos(a) * tanSpeed - Math.sin(a) * inward;
                        level.sendParticles(ParticleTypes.CLOUD,
                                px, baseY, pz, 1,
                                vx, upSpeed, vz, 0.0D);
                    }
                    // 偶尔撒一颗发光粒子点缀
                    if (ft % 3 == 0) {
                        double a = rnd.nextDouble() * Math.PI * 2.0D;
                        double r = tornInner + rnd.nextDouble() * (tornOuter - tornInner);
                        level.sendParticles(ParticleTypes.END_ROD,
                                cx + Math.cos(a) * r, groundY + 1.0D + rnd.nextDouble() * 3.0D,
                                cz + Math.sin(a) * r, 1,
                                -Math.sin(a) * 0.1D, 0.08D, Math.cos(a) * 0.1D, 0.0D);
                    }
                });
            }
            for (ServerPlayer p : cn.blockforge.generated.floralritebloomcurios.util.Mechanics
                    .playersInDomain(level, this, 32.0D)) {
                p.displayClientMessage(Component.translatable("frb.mechanic.sky.wind_eye")
                        .withStyle(ChatFormatting.WHITE), true);
            }
            // 60 tick 后结算伤害 + 环带撒粒子
            this.scheduleDelayedTask(60, () -> {
                double innerR = 4.0D;
                double outerR = 18.0D;
                float dmg = FRBConfig.dmg(8.0F) * FRBConfig.dmgWindEye();
                DamageSource src = level.damageSources().mobAttack(this);
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                        this.getBoundingBox().inflate(outerR))) {
                    if (!cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .inAnnulus(p.getX(), p.getZ(), cx, cz, innerR, outerR)) continue;
                    if (!cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .overlapsHeight(p.getBoundingBox().minY, p.getBoundingBox().maxY,
                                    groundY, 4.0D)) continue;
                    p.hurt(src, dmg);
                    // r107：风眼环形 AOE 附带击飞——命中玩家向上抛起约 4 格
                    //      （v0 ≈ 0.82，重力 0.08/tick → 最大高度 ≈ v0²/(2g) ≈ 4.0 格）
                    p.setDeltaMovement(p.getDeltaMovement().x, 0.82D, p.getDeltaMovement().z);
                    p.hasImpulse = true;
                    p.hurtMarked = true;
                }
                // 在环带（4~18）内撒粒子表示 boss 攻击了这些地方（r90 翻倍 80→160）
                var rand = level.getRandom();
                for (int i = 0; i < 160; i++) {
                    double a = rand.nextDouble() * Math.PI * 2.0D;
                    double r = innerR + rand.nextDouble() * (outerR - innerR);
                    double px = cx + Math.cos(a) * r;
                    double pz = cz + Math.sin(a) * r;
                    level.sendParticles(ParticleTypes.CLOUD,
                            px, groundY + 0.5D, pz, 2, 0.1D, 0.2D, 0.1D, 0.02D);
                    level.sendParticles(ParticleTypes.END_ROD,
                            px, groundY + 0.3D, pz, 1, 0.0D, 0.05D, 0.0D, 0.0D);
                }
                level.playSound(null, this, SoundEvents.WITHER_SHOOT,
                        SoundSource.HOSTILE, 1.2F, 0.7F);
                // r90：天空阶段每次风眼结算后随机传送一次（同云雨阶段传送）
                if (this.phase == 4) {
                    this.teleportNearPlayer(level);
                }
            });
        };
        if (triggerAnim) {
            this.frb$triggerCastAnim();
            this.scheduleDelayedTask(15, warning);
        } else {
            warning.run();
        }
    }

    /**
     * r90 虚空潮汐：先放环形 aoe，再放圆形 aoe（顺序随机）。
     * 环形：外半径 18（同风眼），内半径 8（贴图内外比 8:18）。
     * 圆形：半径 9（环形的一半，贴图 256 是 512 的一半）。
     * 预警 60 tick 后结算伤害 + 撒粒子。两次 aoe 间隔 20 tick。
     * r91：结算粒子换为例子库蓝紫星芒 star_streak（tide_void_star）。
     */
    public void castVoidTide(ServerLevel level) {
        // 随机顺序：50% 先环后圆，50% 先圆后环
        boolean ringFirst = level.getRandom().nextBoolean();
        double cx = this.getX();
        double cz = this.getZ();
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(cx), Mth.floor(cz));
        double groundY = Math.max(this.getY() - 5.0D, gy);

        // 第一击
        int kind1 = ringFirst ? cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_RING
                              : cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_CIRCLE;
        float r1 = ringFirst ? 18.0F : 9.0F;
        FRBNetwork.sendVoidTide(level, cx, groundY + 0.05D, cz, r1, 60, kind1);
        this.scheduleDelayedTask(60, () -> {
            DamageSource src = level.damageSources().indirectMagic(this, this);
            float dmg = FRBConfig.dmg(12.0F) * FRBConfig.dmgVoidTide();
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(20.0D))) {
                boolean hit;
                if (kind1 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_RING) {
                    hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .inAnnulus(p.getX(), p.getZ(), cx, cz, 8.0D, 18.0D);
                } else {
                    hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .inCircle(p.getX(), p.getZ(), cx, cz, 9.0D);
                }
                if (!hit) continue;
                if (!cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                        .overlapsHeight(p.getBoundingBox().minY, p.getBoundingBox().maxY,
                                groundY, 4.0D)) continue;
                p.hurt(src, dmg);
                p.hurtMarked = true;
            }
            // 粒子（翻倍=160 颗）
            var rand = level.getRandom();
            for (int i = 0; i < 160; i++) {
                double a, r;
                if (kind1 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_RING) {
                    a = rand.nextDouble() * Math.PI * 2.0D;
                    r = 8.0D + rand.nextDouble() * 10.0D;
                } else {
                    a = rand.nextDouble() * Math.PI * 2.0D;
                    r = rand.nextDouble() * 9.0D;
                }
                double px = cx + Math.cos(a) * r;
                double pz = cz + Math.sin(a) * r;
                // r91：原 PORTAL/REVERSE_PORTAL 粒子在地面暗光下几乎看不见，
                //      换例子库蓝紫星芒 star_streak，高度随机撒在 0.5~1.4 的受击带内
                double starY = 0.5D + rand.nextDouble() * 0.9D;
                level.sendParticles(FRBParticles.TIDE_VOID_STAR.get(),
                        px, groundY + starY, pz, 1, 0.18D, 0.10D, 0.18D, 0.02D);
            }
            level.playSound(null, this, SoundEvents.WITHER_SHOOT,
                    SoundSource.HOSTILE, 1.2F, 0.5F);
        });

        // 第二击（间隔 20 tick 后预警）
        int kind2 = ringFirst ? cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_CIRCLE
                              : cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_RING;
        float r2 = ringFirst ? 9.0F : 18.0F;
        this.scheduleDelayedTask(80, () -> {
            FRBNetwork.sendVoidTide(level, cx, groundY + 0.05D, cz, r2, 60, kind2);
            this.scheduleDelayedTask(60, () -> {
                DamageSource src2 = level.damageSources().indirectMagic(this, this);
                float dmg2 = FRBConfig.dmg(12.0F) * FRBConfig.dmgVoidTide();
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                        this.getBoundingBox().inflate(20.0D))) {
                    boolean hit;
                    if (kind2 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_RING) {
                        hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                                .inAnnulus(p.getX(), p.getZ(), cx, cz, 8.0D, 18.0D);
                    } else {
                        hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                                .inCircle(p.getX(), p.getZ(), cx, cz, 9.0D);
                    }
                    if (!hit) continue;
                    if (!cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .overlapsHeight(p.getBoundingBox().minY, p.getBoundingBox().maxY,
                                    groundY, 4.0D)) continue;
                    p.hurt(src2, dmg2);
                    p.hurtMarked = true;
                }
                // 粒子（翻倍=160 颗）
                var rand = level.getRandom();
                for (int i = 0; i < 160; i++) {
                    double a, r;
                    if (kind2 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_RING) {
                        a = rand.nextDouble() * Math.PI * 2.0D;
                        r = 8.0D + rand.nextDouble() * 10.0D;
                    } else {
                        a = rand.nextDouble() * Math.PI * 2.0D;
                        r = rand.nextDouble() * 9.0D;
                    }
                    double px = cx + Math.cos(a) * r;
                    double pz = cz + Math.sin(a) * r;
                    // r91：例子库蓝紫星芒，高度随机 0.5~1.4
                    double starY = 0.5D + rand.nextDouble() * 0.9D;
                    level.sendParticles(FRBParticles.TIDE_VOID_STAR.get(),
                            px, groundY + starY, pz, 1, 0.18D, 0.10D, 0.18D, 0.02D);
                }
                level.playSound(null, this, SoundEvents.WITHER_SHOOT,
                        SoundSource.HOSTILE, 1.2F, 0.5F);
            });
        });
    }

    /**
     * r90 时间潮汐：同虚空潮汐机制，贴图换为 time_aoe_ring/circle。
     * r91：结算粒子换为例子库金色圣星 holy_star（tide_time_star）。
     * 随机顺序先环后圆或先圆后环，60t 预警 → 8 点魔法 + 160 粒子，间隔 20t。
     */
    public void castTimeTide(ServerLevel level) {
        boolean ringFirst = level.getRandom().nextBoolean();
        double cx = this.getX();
        double cz = this.getZ();
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(cx), Mth.floor(cz));
        double groundY = Math.max(this.getY() - 5.0D, gy);

        int kind1 = ringFirst ? cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_TIME_RING
                              : cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_TIME_CIRCLE;
        float r1 = ringFirst ? 18.0F : 9.0F;
        FRBNetwork.sendVoidTide(level, cx, groundY + 0.05D, cz, r1, 60, kind1);
        this.scheduleDelayedTask(60, () -> {
            DamageSource src = level.damageSources().indirectMagic(this, this);
            float dmg = FRBConfig.dmg(8.0F) * FRBConfig.dmgTimeTide();
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(20.0D))) {
                boolean hit;
                if (kind1 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_TIME_RING) {
                    hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .inAnnulus(p.getX(), p.getZ(), cx, cz, 8.0D, 18.0D);
                } else {
                    hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .inCircle(p.getX(), p.getZ(), cx, cz, 9.0D);
                }
                if (!hit) continue;
                if (!cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                        .overlapsHeight(p.getBoundingBox().minY, p.getBoundingBox().maxY,
                                groundY, 4.0D)) continue;
                p.hurt(src, dmg);
                p.hurtMarked = true;
            }
            // 粒子（160 颗金色圣星）
            var rand = level.getRandom();
            for (int i = 0; i < 160; i++) {
                double a, r;
                if (kind1 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_TIME_RING) {
                    a = rand.nextDouble() * Math.PI * 2.0D;
                    r = 8.0D + rand.nextDouble() * 10.0D;
                } else {
                    a = rand.nextDouble() * Math.PI * 2.0D;
                    r = rand.nextDouble() * 9.0D;
                }
                double px = cx + Math.cos(a) * r;
                double pz = cz + Math.sin(a) * r;
                // r91：换例子库金色八芒圣星 holy_star，高度随机撒在 0.5~1.4 的受击带内
                double starY = 0.5D + rand.nextDouble() * 0.9D;
                level.sendParticles(FRBParticles.TIDE_TIME_STAR.get(),
                        px, groundY + starY, pz, 1, 0.18D, 0.10D, 0.18D, 0.02D);
            }
            level.playSound(null, this, SoundEvents.WITHER_SHOOT,
                    SoundSource.HOSTILE, 1.2F, 0.5F);
        });

        int kind2 = ringFirst ? cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_TIME_CIRCLE
                              : cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_TIME_RING;
        float r2 = ringFirst ? 9.0F : 18.0F;
        this.scheduleDelayedTask(80, () -> {
            FRBNetwork.sendVoidTide(level, cx, groundY + 0.05D, cz, r2, 60, kind2);
            this.scheduleDelayedTask(60, () -> {
                DamageSource src2 = level.damageSources().indirectMagic(this, this);
                float dmg2 = FRBConfig.dmg(8.0F) * FRBConfig.dmgTimeTide();
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                        this.getBoundingBox().inflate(20.0D))) {
                    boolean hit;
                    if (kind2 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_TIME_RING) {
                        hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                                .inAnnulus(p.getX(), p.getZ(), cx, cz, 8.0D, 18.0D);
                    } else {
                        hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                                .inCircle(p.getX(), p.getZ(), cx, cz, 9.0D);
                    }
                    if (!hit) continue;
                    if (!cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .overlapsHeight(p.getBoundingBox().minY, p.getBoundingBox().maxY,
                                    groundY, 4.0D)) continue;
                    p.hurt(src2, dmg2);
                    p.hurtMarked = true;
                }
                var rand = level.getRandom();
                for (int i = 0; i < 160; i++) {
                    double a, r;
                    if (kind2 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_TIME_RING) {
                        a = rand.nextDouble() * Math.PI * 2.0D;
                        r = 8.0D + rand.nextDouble() * 10.0D;
                    } else {
                        a = rand.nextDouble() * Math.PI * 2.0D;
                        r = rand.nextDouble() * 9.0D;
                    }
                    double px = cx + Math.cos(a) * r;
                    double pz = cz + Math.sin(a) * r;
                    // r91：例子库金色八芒圣星，高度随机 0.5~1.4
                    double starY = 0.5D + rand.nextDouble() * 0.9D;
                    level.sendParticles(FRBParticles.TIDE_TIME_STAR.get(),
                            px, groundY + starY, pz, 1, 0.18D, 0.10D, 0.18D, 0.02D);
                }
                level.playSound(null, this, SoundEvents.WITHER_SHOOT,
                        SoundSource.HOSTILE, 1.2F, 0.5F);
            });
        });
    }

    /**
     * r96 钢铁月环（霜华阶段圆形/环形 AOE）：
     * 读条 3 秒（60 tick），随机先环后圆或先圆后环，与虚空潮汐同结构，
     * 贴图换为 frost_moon_ring/circle，粒子用雪花。
     * 基础伤害 8 点魔法（受 dmgFrostMoonRing 倍率影响）。
     */
    public void castFrostMoonRing(ServerLevel level) {
        boolean ringFirst = level.getRandom().nextBoolean();
        double cx = this.getX();
        double cz = this.getZ();
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(cx), Mth.floor(cz));
        double groundY = Math.max(this.getY() - 5.0D, gy);

        int kind1 = ringFirst ? cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_FROST_MOON_RING
                              : cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_FROST_MOON_CIRCLE;
        float r1 = ringFirst ? 18.0F : 9.0F;
        FRBNetwork.sendVoidTide(level, cx, groundY + 0.05D, cz, r1, 60, kind1);
        this.scheduleDelayedTask(60, () -> {
            DamageSource src = level.damageSources().indirectMagic(this, this);
            float dmg = FRBConfig.dmg(12.0F) * FRBConfig.dmgFrostMoonRing();
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(20.0D))) {
                boolean hit;
                if (kind1 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_FROST_MOON_RING) {
                    hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .inAnnulus(p.getX(), p.getZ(), cx, cz, 8.0D, 18.0D);
                } else {
                    hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .inCircle(p.getX(), p.getZ(), cx, cz, 9.0D);
                }
                if (!hit) continue;
                if (!cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                        .overlapsHeight(p.getBoundingBox().minY, p.getBoundingBox().maxY,
                                groundY, 4.0D)) continue;
                p.hurt(src, dmg);
                p.hurtMarked = true;
            }
            // 雪花粒子
            var rand = level.getRandom();
            for (int i = 0; i < 160; i++) {
                double a, r;
                if (kind1 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_FROST_MOON_RING) {
                    a = rand.nextDouble() * Math.PI * 2.0D;
                    r = 8.0D + rand.nextDouble() * 10.0D;
                } else {
                    a = rand.nextDouble() * Math.PI * 2.0D;
                    r = rand.nextDouble() * 9.0D;
                }
                double px = cx + Math.cos(a) * r;
                double pz = cz + Math.sin(a) * r;
                double starY = 0.5D + rand.nextDouble() * 0.9D;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                        px, groundY + starY, pz, 1, 0.18D, 0.10D, 0.18D, 0.02D);
            }
            level.playSound(null, this, SoundEvents.PLAYER_HURT_FREEZE,
                    SoundSource.HOSTILE, 1.2F, 0.6F);
        });

        int kind2 = ringFirst ? cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_FROST_MOON_CIRCLE
                              : cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_FROST_MOON_RING;
        float r2 = ringFirst ? 9.0F : 18.0F;
        this.scheduleDelayedTask(80, () -> {
            FRBNetwork.sendVoidTide(level, cx, groundY + 0.05D, cz, r2, 60, kind2);
            this.scheduleDelayedTask(60, () -> {
                DamageSource src2 = level.damageSources().indirectMagic(this, this);
                float dmg2 = FRBConfig.dmg(12.0F) * FRBConfig.dmgFrostMoonRing();
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                        this.getBoundingBox().inflate(20.0D))) {
                    boolean hit;
                    if (kind2 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_FROST_MOON_RING) {
                        hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                                .inAnnulus(p.getX(), p.getZ(), cx, cz, 8.0D, 18.0D);
                    } else {
                        hit = cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                                .inCircle(p.getX(), p.getZ(), cx, cz, 9.0D);
                    }
                    if (!hit) continue;
                    if (!cn.blockforge.generated.floralritebloomcurios.util.SkyAttackGeometry
                            .overlapsHeight(p.getBoundingBox().minY, p.getBoundingBox().maxY,
                                    groundY, 4.0D)) continue;
                    p.hurt(src2, dmg2);
                    p.hurtMarked = true;
                }
                var rand = level.getRandom();
                for (int i = 0; i < 160; i++) {
                    double a, r;
                    if (kind2 == cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_FROST_MOON_RING) {
                        a = rand.nextDouble() * Math.PI * 2.0D;
                        r = 8.0D + rand.nextDouble() * 10.0D;
                    } else {
                        a = rand.nextDouble() * Math.PI * 2.0D;
                        r = rand.nextDouble() * 9.0D;
                    }
                    double px = cx + Math.cos(a) * r;
                    double pz = cz + Math.sin(a) * r;
                    double starY = 0.5D + rand.nextDouble() * 0.9D;
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                            px, groundY + starY, pz, 1, 0.18D, 0.10D, 0.18D, 0.02D);
                }
                level.playSound(null, this, SoundEvents.PLAYER_HURT_FREEZE,
                        SoundSource.HOSTILE, 1.2F, 0.6F);
            });
        });
    }

    /**
     * r96 光辉炎柱（焚尽阶段随机落点炎柱 AOE）：
     * 在 boss 周围 16~24 格随机落 8 个半径 3 的圆形预警，2.5 秒（50 tick）后爆发，
     * 造成 12 点花灵伤害 + 6 点爆炸伤害。
     *
     * 炎柱视觉：地面预警圆（flame_pillar_circle.png）+ 爆发时大量向上喷射的 FLAME 粒子，
     * 配合 EXPLOSION 核心粒子营造"冲天炎柱"效果。
     */
    public void castFlamePillar(ServerLevel level) {
        RandomSource r = level.getRandom();
        int count = 8;
        double bx = this.getX();
        double bz = this.getZ();

        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2.0D;
            double d = r.nextDouble() * 24.0D; // r98：0..24 格整圆（避免贴脸免伤）
            double x = bx + Math.cos(a) * d;
            double z = bz + Math.sin(a) * d;
            int cx = net.minecraft.core.SectionPos.blockToSectionCoord(
                    net.minecraft.util.Mth.floor(x));
            int cz = net.minecraft.core.SectionPos.blockToSectionCoord(
                    net.minecraft.util.Mth.floor(z));
            if (!level.hasChunk(cx, cz)) continue;
            net.minecraft.core.BlockPos baseXZ = new net.minecraft.core.BlockPos(
                    net.minecraft.util.Mth.floor(x), 0, net.minecraft.util.Mth.floor(z));
            net.minecraft.core.BlockPos groundTop = level.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    baseXZ);
            net.minecraft.core.BlockPos surface = groundTop;
            for (int dy = 0; dy <= 6; dy++) {
                net.minecraft.core.BlockPos up = groundTop.above(dy);
                if (level.getBlockState(up).isAir()) {
                    surface = up;
                    break;
                }
            }
            double px = surface.getX() + 0.5D;
            double py = surface.getY();
            double pz = surface.getZ() + 0.5D;
            // 50 tick 预警圆
            FRBNetwork.sendVoidTide(level, px, py + 0.05D, pz, 3.0F, 50,
                    cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_FLAME_PILLAR_CIRCLE);
            this.scheduleDelayedTask(50, () -> {
                // r98：光柱贴图（用户自绘 flame_pillar.png，十字交叉 billboard）
                FRBNetwork.sendFlameBeam(level, px, py, pz, 1.5F, 12.0F, 30);
                // 伤害结算：12 花灵 + 6 爆炸
                Holder<net.minecraft.world.damagesource.DamageType> floralHolder = level.registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(cn.blockforge.generated.floralritebloomcurios.registry.FRBDamageTypes.FLORAL);
                DamageSource floralSrc = new DamageSource(floralHolder, this, this);
                DamageSource explodeSrc = level.damageSources().explosion(this, this);
                float floralDmg = FRBConfig.dmg(18.0F) * FRBConfig.dmgFlamePillar();
                float explodeDmg = FRBConfig.dmg(9.0F) * FRBConfig.dmgFlamePillar();
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                        new net.minecraft.world.phys.AABB(px - 3.0D, py - 1.0D, pz - 3.0D,
                                px + 3.0D, py + 4.0D, pz + 3.0D))) {
                    double dx2 = p.getX() - px;
                    double dz2 = p.getZ() - pz;
                    if (dx2 * dx2 + dz2 * dz2 <= 9.0D) { // 半径 3
                        p.hurt(floralSrc, floralDmg);
                        p.hurt(explodeSrc, explodeDmg);
                        p.hurtMarked = true;
                    }
                }
                // r96：冲天炎柱 = 纯粒子效果（大量向上喷射的 FLAME 粒子柱 + 爆炸核心）
                var rand = level.getRandom();
                for (int j = 0; j < 50; j++) {
                    double ox = (rand.nextDouble() - 0.5D) * 2.5D;
                    double oz = (rand.nextDouble() - 0.5D) * 2.5D;
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                            px + ox, py + 0.3D, pz + oz, 1,
                            0.0D, 0.4D + rand.nextDouble() * 0.3D, 0.0D, 0.05D);
                }
                for (int j = 0; j < 20; j++) {
                    double ox = (rand.nextDouble() - 0.5D) * 1.5D;
                    double oz = (rand.nextDouble() - 0.5D) * 1.5D;
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            px + ox, py + 0.5D, pz + oz, 1,
                            0.0D, 0.5D + rand.nextDouble() * 0.3D, 0.0D, 0.04D);
                }
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                        px, py + 0.5D, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
                        px, py + 0.5D, pz, 14, 0.8D, 0.5D, 0.8D, 0.04D);
                level.playSound(null, px, py, pz,
                        net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.6F, 0.9F);
            });
        }
    }

    /**
     * r86 羽剑冲锋：60 tick 红色直线预警 + 16 tick 贴地贯穿冲锋。
     * r90：天空阶段 boss 固定悬在玩家正上方，朝玩家方向的水平分量几乎为零，
     *      旧实现会静默失败（玩家因此从没见过该技能）。改为参考天界水母——
     *      羽剑在玩家侧后方 7 格地面出现，沿水平方向贯穿玩家位置（前后共 14 格），
     *      玩家必须侧移闪避。方向优先取 boss→玩家水平朝向，重合时取随机方向。
     */
    private void castFeatherSword(ServerLevel level, LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        Vec3 norm;
        if (Math.hypot(dx, dz) < 2.0D) {
            // boss 几乎悬在目标正上方：随机选一个水平方向
            double a = level.getRandom().nextDouble() * Math.PI * 2.0D;
            norm = new Vec3(Math.cos(a), 0.0D, Math.sin(a));
        } else {
            norm = new Vec3(dx, 0.0D, dz).normalize();
        }
        // 以目标为中点：起点在侧后方 7 格，终点在身前 7 格（全长 14 格）
        final double BACK = 7.0D;
        final float RANGE = 14.0F;
        double sx = target.getX() - norm.x * BACK;
        double sz = target.getZ() - norm.z * BACK;
        int startGy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(sx), Mth.floor(sz));
        Vec3 from = new Vec3(sx, startGy + 0.5D, sz);
        float scale = 2.5F; // 剑身原长约 1 格，放大到 2.5 格与 3 格宽预警线协调
        cn.blockforge.generated.floralritebloomcurios.entity.FeatherSwordEntity.launch(
                level, from, norm, this, RANGE, scale, startGy);
        for (ServerPlayer p : cn.blockforge.generated.floralritebloomcurios.util.Mechanics
                .playersInDomain(level, this, 32.0D)) {
            p.displayClientMessage(Component.translatable("frb.mechanic.sky.feather_sword")
                    .withStyle(ChatFormatting.RED), true);
        }
        level.playSound(null, this, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.5F, 0.8F);
    }

    /** 将天空阶段结束时的 Boss 放回当前坐标下方最近的地面。 */
    private void landAtGround(ServerLevel level) {
        int x = Mth.floor(this.getX());
        int z = Mth.floor(this.getZ());
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (groundY > level.getMinBuildHeight()) {
            this.setPos(this.getX(), groundY, this.getZ());
            this.setDeltaMovement(Vec3.ZERO);
            this.hasImpulse = true;
        }
    }

    /** 虚空：离地 2.5 格悬浮不动不攻击；裂隙生成由 Mechanics.VoidRiftReducedMechanic 接管。 */
    private void voidPhase(ServerLevel level) {
        this.setNoGravity(true);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
        // r98：离地 3 格悬浮（与天空阶段统一）
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(this.getX()), Mth.floor(this.getZ()));
        double targetY = gy + 3.0D;
        if (this.getY() < targetY) {
            this.setDeltaMovement(0, 0.2D, 0);
        } else {
            this.setPos(this.getX(), targetY, this.getZ());
            this.setDeltaMovement(Vec3.ZERO);
        }
        this.hasImpulse = true;
        // 保留天空→虚空过渡时的镜头演出
        if (this.timer == 1) {
            FRBNetwork.sendBossFx(level, this, BossFxPacket.FX_TINT, 0.65F, 50, PHASE_COLORS[this.phase]);
        }
    }

    /** 由 PhaseMechanic 调用——在 G1 减密度机制里生成单条虚空裂隙。 */
    public void spawnSingleVoidRift(ServerLevel level) {
        if (level.getEntitiesOfClass(VoidRiftEntity.class, this.getBoundingBox().inflate(48.0D)).size() >= 12) return;
        RandomSource r = level.getRandom();
        Player nearby = level.getNearestPlayer(this, 32.0D);
        double a = r.nextDouble() * Math.PI * 2.0D;
        double d = 3.0D + r.nextDouble() * 5.0D;
        double x = (nearby == null ? this.getX() : nearby.getX()) + Math.cos(a) * d;
        double z = (nearby == null ? this.getZ() : nearby.getZ()) + Math.sin(a) * d;
        int top = (int) Math.floor(this.getY() + 6);
        int bottom = Math.max((int) Math.floor(this.getY() - 12), level.getMinBuildHeight() + 1);
        if (!level.hasChunk(SectionPos.blockToSectionCoord(Mth.floor(x)),
                SectionPos.blockToSectionCoord(Mth.floor(z)))) {
            return;
        }
        for (int yy = top; yy >= bottom; yy--) {
            BlockPos feet = BlockPos.containing(x, yy, z);
            if (!level.getBlockState(feet.below()).isAir()
                    && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                VoidRiftEntity.spawn(level, x, yy, z);
                FRBNetwork.sendBossFx(level, this,
                        BossFxPacket.FX_SHAKE | BossFxPacket.FX_SHOCKWAVE, 0.3F, 20);
                // 额外附 ender_slash 紫色飞溅
                level.sendParticles(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles.VOID_SLASH.get(),
                        x, yy + 1.0D, z, 6, 0.3D, 0.5D, 0.3D, 0.0D);
                break;
            }
        }
    }

    /** 枯萎：不攻击不移动（无敌判定在 hurt()），环绕三朵死亡之花。 */
    private void decayPhase(ServerLevel level) {
        this.getNavigation().stop();
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
        this.setNoGravity(true);
        // r98：所有阶段统一离地 3 格
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(this.getX()), Mth.floor(this.getZ()));
        double targetY = gy + 3.0D;
        double dy = Mth.clamp((targetY - this.getY()) * 0.2D, -0.3D, 0.3D);
        this.setDeltaMovement(0.0D, dy, 0.0D);
        this.hasImpulse = true;
        // 枯萎领域：全屏褪为黑白（持续刷新直到战斗结束 / 玩家远离）
        if (this.tickCount % 20 == 0) {
            FRBNetwork.sendBossFx(level, this, BossFxPacket.FX_GRAY, 0.85F, 30);
        }
        if (this.tickCount % 40 == 0 && anyFlowerAlive()) {
            level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 2.2D, this.getZ(),
                    2, 3.0D, 1.0D, 3.0D, 0.0D);
        }
        // r95：枯萎阶段限制——每秒剥夺领域内所有玩家的正面 buff + 禁止自然回血
        if (this.tickCount % 20 == 0) {
            for (ServerPlayer p : cn.blockforge.generated.floralritebloomcurios.util.Mechanics
                    .playersInDomain(level, this, DECAY_AOE_RANGE)) {
                // r96：先收集正面效果再移除——直接在 entrySet().removeIf 内调用 removeEffect
                //      会因双重修改 map 触发 ConcurrentModificationException
                java.util.List<MobEffectInstance> toRemove = new java.util.ArrayList<>();
                for (MobEffectInstance inst : p.getActiveEffects()) {
                    if (inst.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
                        toRemove.add(inst);
                    }
                }
                for (MobEffectInstance inst : toRemove) {
                    p.removeEffect(inst.getEffect());
                }
            }
        }
        // r95：枯萎阶段大范围 AOE——2s 读条 → 释放 24 格 8 点花灵伤害 → 5s cd，循环
        tickDecayAoe(level);
        // 三朵死亡之花全部清除 → 触发 H2 终结技 / 时间阶段判定（r63）
        if (!anyFlowerAlive()) {
            onAllFlowersDefeated(level);
        }
    }

    // ---- r95：枯萎阶段大范围 AOE 驱动 ----

    /**
     * 枯萎 AOE 循环：cd 归零 → 启动 2s 读条（DATA_DECAY_AOE_CHARGE 同步给客户端 HUD）
     * → 读条归零 → 释放 24 格 8 点花灵伤害 → 进入 5s cd → 循环。
     * 总周期 = 40 + 100 = 140 tick = 7 秒。
     */
    private void tickDecayAoe(ServerLevel level) {
        if (this.decayAoeCdTicks > 0) {
            this.decayAoeCdTicks--;
            return;
        }
        if (this.decayAoeChargeTicks <= 0 && this.decayAoeCdTicks <= 0) {
            // 启动新一轮读条
            this.decayAoeChargeTicks = DECAY_AOE_CHARGE_TICKS;
            this.entityData.set(DATA_DECAY_AOE_CHARGE, DECAY_AOE_CHARGE_TICKS);
            frbSay(level, "frb.boss.say.cast_" + level.random.nextInt(4));
            level.playSound(null, this.getX(), this.getY() + 1.5D, this.getZ(),
                    SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.5F, 0.5F);
        }
        if (this.decayAoeChargeTicks > 0) {
            this.decayAoeChargeTicks--;
            // r95：读条期间采用激光（花瓣光束）同一路径——6 锚点（头顶/身侧/脚下各两）
            //      绕竖直轴旋转并向 boss 身体中心收拢；枯萎灰黑色 dust + 汇聚花瓣。
            int color = PHASE_COLORS[6];
            Vector3f col = new Vector3f(((color >> 16) & 255) / 255.0F,
                    ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F);
            double prog = 1.0D - (double) this.decayAoeChargeTicks / DECAY_AOE_CHARGE_TICKS;
            double r = 1.5D - 1.25D * prog; // 旋转半径逐渐收拢
            // AOE 以自身为中心，汇聚终点取 boss 身体中心（非指向性光束）
            double cx = this.getX();
            double cy = this.getY() + this.getBbHeight() * 0.5D;
            double cz = this.getZ();
            double[] heights = {2.6D, 1.4D, 0.2D}; // 头顶 / 身侧 / 脚下
            for (int i = 0; i < 6; i++) {
                double h = heights[i / 2];
                double y = this.getY() + h;
                double ang = this.tickCount * 0.55D + i * Math.PI / 3.0D;
                double px = this.getX() + Math.cos(ang) * r;
                double pz = this.getZ() + Math.sin(ang) * r;
                level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(col, 1.1F),
                        px, y, pz, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                if (this.decayAoeChargeTicks % 4 == 0) {
                    level.sendParticles(FRBParticles.CONVERGING_PETAL.get(),
                            px, y, pz, 1, cx - px, cy - y, cz - pz, 1.0D);
                }
            }
            if (this.decayAoeChargeTicks <= 0) {
                this.entityData.set(DATA_DECAY_AOE_CHARGE, 0);
                releaseDecayAoe(level);
                this.decayAoeCdTicks = DECAY_AOE_CD_TICKS;
            } else {
                this.entityData.set(DATA_DECAY_AOE_CHARGE, this.decayAoeChargeTicks);
            }
        }
    }

    /** 释放枯萎 AOE：对 24 格内所有生存/冒险玩家造成 8 点花灵伤害 + 视觉。 */
    private void releaseDecayAoe(ServerLevel level) {
        Holder<net.minecraft.world.damagesource.DamageType> floral = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(cn.blockforge.generated.floralritebloomcurios.registry.FRBDamageTypes.FLORAL);
        DamageSource src = new DamageSource(floral, this, this);
        for (ServerPlayer p : cn.blockforge.generated.floralritebloomcurios.util.Mechanics
                .playersInDomain(level, this, DECAY_AOE_RANGE)) {
            if (p.hurt(src, FRBConfig.dmg(DECAY_AOE_DAMAGE))) {
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        p.getX(), p.getY() + 1.0D, p.getZ(), 8, 0.4D, 0.5D, 0.4D, 0.1D);
            }
        }
        // 视觉：boss 位置爆发粒子 + 音效
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + 1.0D, this.getZ(), 40, 4.0D, 2.0D, 4.0D, 0.2D);
        level.sendParticles(ParticleTypes.WITCH,
                this.getX(), this.getY() + 1.0D, this.getZ(), 30, 6.0D, 3.0D, 6.0D, 0.1D);
        // r96：从 boss 中心向外扩散的紫黑色枯萎冲击波（可视化 AOE 范围）
        expandingWave(level, ParticleTypes.SOUL_FIRE_FLAME, DECAY_AOE_RANGE, 72, 0.1D);
        expandingWave(level, ParticleTypes.WITCH, DECAY_AOE_RANGE * 0.7, 56, 0.3D);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 2.0F, 0.4F);
        FRBNetwork.sendBossFx(level, this, BossFxPacket.FX_TINT, 0.6F, 30, 0x2a1a2e);
    }

    private boolean anyFlowerAlive() {
        if (!(this.level() instanceof ServerLevel level)) return false;
        this.flowerIds.removeIf(id -> !(level.getEntity(id) instanceof DeathFlowerEntity f) || !f.isAlive());
        return !this.flowerIds.isEmpty();
    }

    /** 死亡之花被击败时回调：记录死亡时间和 slot（H1 复活机制用）。 */
    public void onDeathFlowerKilled() {
        if (this.level().isClientSide) return;
        ServerLevel level = (ServerLevel) this.level();
        // 记录最近一次被击毙的花（用于 H1 复活判定）
        // 注：此处无法拿 slot，从 Die 事件外侧调用 onDeathFlowerKilled(slot) 才能传入。
        if (this.anyFlowerAlive()) return;
        ringBurst(level, 0xffe27a);
        level.playSound(null, this, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 0.6F);
    }

    /** H1 复活记录版本：DeathFlowerEntity.die() 调用此版本。 */
    public void onDeathFlowerKilled(int slot, long gameTime) {
        if (this.level().isClientSide) return;
        this.lastKilledFlowerSlot = slot;
        this.lastKilledFlowerAt = gameTime;
        onDeathFlowerKilled();
    }

    /**
     * H1 死亡之花复活：最近 60 tick（3 秒）内若没有任何花被击败过，
     * 而此时仅有 1~2 朵花存活（其余全部已被消灭），把最近一次被消灭的花以满血重新生成。
     */
    public void maybeReviveDeathFlowers(ServerLevel level) {
        if (!(level instanceof ServerLevel)) return;
        long gameTime = level.getGameTime();
        if (this.lastKilledFlowerSlot < 0) return;
        if (gameTime - this.lastKilledFlowerAt < 60L) return;
        if (!anyFlowerAlive()) return;
        // 找已经死亡的花（不在 flowerIds 里的 slot）
        java.util.Set<Integer> alive = new java.util.HashSet<>();
        for (UUID id : this.flowerIds) {
            if (level.getEntity(id) instanceof DeathFlowerEntity f && f.isAlive()) {
                alive.add(f.getOrbitSlot());
            }
        }
        if (alive.size() >= 3) return;
        int deadSlot = -1;
        for (int i = 0; i < 3; i++) {
            if (!alive.contains(i)) { deadSlot = i; break; }
        }
        if (deadSlot < 0) return;
        // 复活
        DeathFlowerEntity reborn = new DeathFlowerEntity(
                cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities.DEATH_FLOWER.get(), level);
        reborn.setPos(this.getX(), this.getY() + 2.0D, this.getZ());
        reborn.bind(this.getUUID(), deadSlot);
        level.addFreshEntity(reborn);
        this.flowerIds.add(reborn.getUUID());
        // 复活粒子 + 文字提示
        level.sendParticles(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles.REVIVE_WISP.get(),
                reborn.getX(), reborn.getY() + 1.0D, reborn.getZ(),
                30, 0.5D, 1.0D, 0.5D, 0.05D);
        level.playSound(null, reborn.getX(), reborn.getY(), reborn.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8F, 1.4F);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(48.0D))) {
            p.displayClientMessage(Component.translatable("frb.boss.flower.revive")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        // 清掉标记，下一轮修复要等下一次击杀
        this.lastKilledFlowerSlot = -1;
    }

    // ---- 额外·时间阶段 + 万花凋零终结技（H2） ----

    /** 是否正在万花凋零咏唱（用于 hurt() / setHealth() 锁血 / 无敌判定）。 */
    public boolean isMilleFlorumCasting() {
        return this.milleFlorumCastTick > 0;
    }

    /** 当前是否处于时间阶段（phase 7）。 */
    public boolean isTimePhase() {
        return this.timePhaseActive;
    }

    /** r95：当前是否处于枯萎阶段（phase 6）——供事件处理器判断是否禁止回血。 */
    public boolean isDecayPhase() {
        return this.phase == 6;
    }

    /**
     * 枯萎阶段三朵死亡之花全部清除后的总闸：先检测领域内是否有玩家
     * 持有「解封过的丰饶」或「万花万草之灵」，有则跳过万花凋零直接转阶段到 PHASE_TIME；
     * 无则启动万花凋零终结技咏唱。两者互斥。
     */
    public void onAllFlowersDefeated(ServerLevel level) {
        if (this.timePhaseActive) return;
        if (this.milleFlorumCastTick > 0) return;
        // r64：boss 已被丢弃或已死亡则不再触发终结技（避免 cast → die → 同一 aiStep 内反复启动）
        if (this.dead || this.isRemoved()) return;
        boolean anyUnsealed = false;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(48.0D))) {
            if (FRBState.holdingUnsealedBountiful(p) || FRBState.hasUltimate(p)) {
                anyUnsealed = true;
                break;
            }
        }
        if (anyUnsealed) {
            enterTimePhase(level);
        } else {
            startMilleFlorumFinaleCast(level, null);
        }
    }

    /**
     * 启动万花凋零终结技咏唱。
     *
     * @param recognizeImmediate 若非空（即被玩家在时间阶段触发），则跳过咏唱立即进入"承认"路径。
     */
    public void startMilleFlorumFinaleCast(ServerLevel level, @org.jetbrains.annotations.Nullable Runnable recognizeImmediate) {
        if (this.milleFlorumCastTick > 0) return;
        this.milleFlorumCastTick = MILLE_FLORUM_CAST_TICKS;
        this.milleFlorumRecognized = false;
        this.milleFlorumKilledAtLast = false;
        // r95：终结技启动时清除枯萎 AOE 状态
        this.decayAoeChargeTicks = 0;
        this.decayAoeCdTicks = 0;
        this.entityData.set(DATA_DECAY_AOE_CHARGE, 0);
        // 抬手姿势：复用 transition 演出（同其它转阶段），不动现有姿势代码
        this.setNoGravity(true);
        this.getNavigation().stop();
        this.setTarget(null);
        // 黑洞实体悬浮在头顶
        MilleFlorumBlackHoleEntity.spawn(level, this);
        // 屏幕文本（6 秒倒计时）+ 镜头万花凋零后处理
        FRBNetwork.sendFinaleStart(level, this, MILLE_FLORUM_CAST_TICKS,
                "frb.boss.mille_florum.cast", "frb.boss.mille_florum.countdown",
                MILLE_FLORUM_CAST_TICKS / 20);
        // r102/r106：世界空间波纹冲击波（球壳透镜扩散），从 boss 中心向外扩散
        FRBNetwork.sendShaderEffect(level, this, "Shockwave", MILLE_FLORUM_CAST_TICKS, 1.0F);
        // r106：虫洞镜头变形（Bosses-Unleashed 移植）——咏唱全程 boss 中心处的球壳透镜+体积星光隧道
        FRBNetwork.sendShaderEffect(level, this, "Wormhole", MILLE_FLORUM_CAST_TICKS, 1.0F);
        // r107：咏唱期间恢复到 120 血、无额外减伤；任何时刻打死 boss 即可中断咏唱取胜
        this.setHealth(120.0F);
        level.playSound(null, this.getX(), this.getY() + 1.5D, this.getZ(),
                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.7F);
        if (recognizeImmediate != null) {
            recognizeImmediate.run();
        }
    }

    /**
     * 万花凋零咏唱驱动：每 tick 推进 + 倒计时文本广播。
     * 咏唱完整读满 120t 后触发终结一击；咏唱期间玩家可随时打死 boss 打断。
     */
    private void tickMilleFlorum(ServerLevel level) {
        int remaining = this.milleFlorumCastTick - 1;
        this.milleFlorumCastTick = remaining;
        // 每 5 tick 更新一次倒计时文本（减少网络风暴）
        if (remaining > 0 && remaining % 5 == 0) {
            int seconds = Math.max(0, (remaining + 19) / 20);
            FRBNetwork.sendFinaleTextUpdate(level, this,
                    "frb.boss.mille_florum.cast", "frb.boss.mille_florum.countdown", seconds);
        }
        // r73 L1：咏唱剩余 15 tick（=0.75s）时触发 time_du_tiao，让 du_tiao 在 0.75s 标记时 boss 收场动画播出
        if (remaining == 15 && !this.milleFlorumKilledAtLast) {
            this.frb$triggerCastAnim();
        }
        // r107：咏唱读满 → 触发终结一击（24 格内所有玩家秒杀 + mille_florum.png 魔法阵）
        if (remaining <= 0 && !this.milleFlorumKilledAtLast) {
            this.milleFlorumRecognized = true;
            milleFlorumExit(level, true);
        }
    }

    /** 万花凋零终结技退出：true=真正认可；false=被击杀（不死流程）。 */
    public void milleFlorumExit(ServerLevel level, boolean recognized) {
        this.milleFlorumCastTick = 0;
        if (recognized) {
            // r107：读条结束（未被玩家击杀）→ 万花凋零真正的终结一击：
            //      地面浮现 mille_florum.png 魔法阵（半径 24 格），并秒杀 24 格内所有玩家。
            final double gx = this.getX();
            final double gz = this.getZ();
            final int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(gx), Mth.floor(gz));
            final double groundY = Math.max(this.getY() - 5.0D, gy);
            cn.blockforge.generated.floralritebloomcurios.network.FRBNetwork.sendVoidTide(
                    level, gx, groundY + 0.05D, gz, 24.0F, 60,
                    cn.blockforge.generated.floralritebloomcurios.network.VoidTidePacket.KIND_MILLE_FLORUM);
            level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                    net.minecraft.sounds.SoundEvents.WITHER_DEATH,
                    net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.5F);
            // 秒杀：超大魔法伤害（远超血量，护甲/抗性也吃不满减免）
            DamageSource killSrc = level.damageSources().indirectMagic(this, this);
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(24.0D))) {
                if (p.distanceToSqr(gx, this.getY(), gz) > 24.0D * 24.0D) continue;
                p.hurt(killSrc, 1.0E9F);
                p.hurtMarked = true;
            }
            // r101：终幕退场序列——end 动画 → 花瓣汇聚 → 花雨扩散 → 退场
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(96.0D))) {
                p.displayClientMessage(Component.translatable("frb.boss.mille_florum.recognize")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            // 1) 播放 end 退场动画（用户后续制作 geo 动画文件中名为 "end" 的动画）
            this.frb$triggerEndAnim();
            // 2) 花瓣从四周汇聚到 boss 身体中心（持续 ~2 秒，40 tick 汇聚）
            final double cx = this.getX();
            final double cy = this.getY() + 1.2D;
            final double cz = this.getZ();
            for (int i = 0; i < 40; i++) {
                double a = i / 40.0D * Math.PI * 2.0D;
                double r = 4.0D + (i % 3) * 1.5D;
                double sx = cx + Math.cos(a) * r;
                double sz = cz + Math.sin(a) * r;
                double sy = cy + (i % 5 - 2) * 0.8D;
                // CONVERGING_PETAL 的速度参数 = 目标坐标（tx,ty,tz）
                level.sendParticles(FRBParticles.CONVERGING_PETAL.get(),
                        sx, sy, sz, 0, cx, cy, cz, 1.0D);
            }
            // 3) 2 秒后花瓣一次性扩散成花雨，随后 boss 退场
            this.scheduleDelayedTask(45, () -> {
                // 花雨扩散：从 boss 中心向四周喷发大量花瓣
                for (int i = 0; i < 80; i++) {
                    double a = i / 80.0D * Math.PI * 2.0D;
                    double elev = (i % 7 - 3) * 0.3D;
                    double vx = Math.cos(a) * (0.3D + (i % 5) * 0.08D);
                    double vz = Math.sin(a) * (0.3D + (i % 5) * 0.08D);
                    level.sendParticles(FRBParticles.GARDEN_PETAL.get(),
                            cx, cy, cz, 0, vx, 0.15D + elev * 0.05D, vz, 1.0D);
                }
                ringBurst(level, TIME_PHASE_COLOR);
                // r102：花雨爆发时再触发一次世界空间冲击波
                FRBNetwork.sendShaderEffect(level, this, "Shockwave", 120, 1.0F);
                dropWinLoot();
                level.playSound(null, this,
                        cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.FRILIAS_DEATH.get(),
                        net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 1.0F);
                releaseFight(false);
                this.setNoGravity(false);
                this.discard();
            });
        }
    }

    /**
     * 进入额外·时间阶段（phase 7）：保留 boss 战斗；每 200 tick 随机选一个之前阶段的技能释放一次。
     */
    private void enterTimePhase(ServerLevel level) {
        this.phase = PHASE_TIME;
        this.timer = 0;
        this.timePhaseActive = true;
        // r98：时间阶段走与其它阶段一致的转场演出（100 tick 无敌 + 震屏/白闪/主题色脉冲），
        // transition 归零时 enterPhaseAbilities(PHASE_TIME) 再正式套用 600 HP / 8 护甲。
        this.transition = TRANSITION_TICKS;
        this.entityData.set(DATA_PHASE, this.phase);
        this.entityData.set(DATA_TRANSITION, this.transition);
        this.bossEvent.setColor(BossEvent.BossBarColor.WHITE);
        this.bossEvent.setName(barName());
        this.bossEvent.setProgress(0.0F);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.decayAoeChargeTicks = 0;
        this.decayAoeCdTicks = 0;
        this.entityData.set(DATA_DECAY_AOE_CHARGE, 0);
        this.timePhaseRotateTick = 200; // 首轮立即挑选
        this.lifeDrainCd = 100;
        this.rewindCd = 220;
        this.timeStopCd = 320;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(96.0D))) {
            FRBNetwork.sendPhaseTitle(p, this.getId(), this.phase);
            p.displayClientMessage(Component.translatable("frb.boss.time_phase.enter")
                    .withStyle(TIME_PHASE_FORMAT), true);
        }
        frbSay(level, "frb.boss.say.phase_7");
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.5F);
        // 转场演出：与 beginTransition 对齐——震屏+白闪、主题色浸染、环形脉冲、时间主题微粒
        FRBNetwork.sendBossFx(level, this,
                BossFxPacket.FX_SHAKE | BossFxPacket.FX_FLASH, 0.8F, 28);
        FRBNetwork.sendBossFx(level, this, BossFxPacket.FX_TINT, 0.85F, 80, TIME_PHASE_COLOR);
        ringBurst(level, TIME_PHASE_COLOR);
        // 时间粒子：选用 TIME_PHASE_06（虚空色，最贴近"时间倒流"的氛围）
        level.sendParticles(FRBParticles.TIME_PHASE_06.get(),
                this.getX(), this.getY() + 1.2D, this.getZ(), 60, 3.0D, 1.5D, 3.0D, 0.02D);
        for (int i = 0; i < 40; i++) {
            double a = i / 40.0D * Math.PI * 2.0D;
            level.sendParticles(FRBParticles.PULSE_RING.get(),
                    this.getX() + Math.cos(a) * 2.6D, this.getY() + 1.2D,
                    this.getZ() + Math.sin(a) * 2.6D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 额外·时间阶段驱动：每 200 tick 随机选一个前阶段技能。 */
    private void timePhase(ServerLevel level) {
        if (this.milleFlorumCastTick > 0) return; // cast 时让位
        // r100：时之华——时间阶段持续附加，造成伤害 -70%（与云雨/霜华阶段的减伤 debuff 同机制）
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(40.0D))) {
            // r101：无限时长，仅在未持有时施加；阶段结束统一清除
            if (!p.hasEffect(FRBEffects.TIME_FLOWER.get())) {
                p.addEffect(new MobEffectInstance(FRBEffects.TIME_FLOWER.get(), -1, 0, true, true));
            }
        }
        this.getNavigation().stop();
        this.setTarget(frbNearestFighter(level, 32.0D)); // r93：只锁生存/冒险玩家
        // r98：所有阶段统一离地 3 格悬浮
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(this.getX()), Mth.floor(this.getZ()));
        double targetY = gy + 3.0D;
        double dy = Mth.clamp((targetY - this.getY()) * 0.2D, -0.3D, 0.3D);
        this.setDeltaMovement(0.0D, dy, 0.0D);
        this.hasImpulse = true;
        // r100：喝醉了 / 无有效目标时，时间阶段也不释放任何技能
        boolean tpSkillDisabled = this.hasEffect(FRBEffects.DRUNK.get()) || this.getTarget() == null;
        if (!tpSkillDisabled) {
        // I2 四机制常态独立运转（r67）：即使不在时停中也各自按内置 cd 使用（r70：cd 读 config）
        if (--this.lifeDrainCd <= 0) {
            this.lifeDrainCd = FRBConfig.lifeDrainCd();
            // r85：机制释放统一延后 15 tick——先播 time_du_tiao 法阵动画，法阵展开到位时机制刚好生效
            this.frb$triggerCastAnim();
            this.scheduleDelayedTask(15, () -> castLifeDrain(level));
        }
        if (--this.rewindCd <= 0) {
            this.rewindCd = FRBConfig.rewindCd();
            this.frb$triggerCastAnim();
            this.scheduleDelayedTask(15, () -> castTimeRewind(level));
        }
        if (--this.timeStopCd <= 0) {
            this.timeStopCd = FRBConfig.timeStopCd();
            this.frb$triggerCastAnim();
            this.scheduleDelayedTask(15, () -> startTimeStop(level));
        }
        if (this.timePhaseRotateTick <= 0) {
            this.timePhaseRotateTick = FRBConfig.timeRotate();
            // r99：机制池扩展至 11 种——
            //   0=自然光束 1=灼热之种 2=霜凝 3=积雨云 4=双风刃
            //   5=花之摇篮 6=光辉炎柱 7=烈焰击退 8=钢铁月环 9=云雨全屏AOE 10=召唤虚空裂隙
            int pick = level.random.nextInt(11);
            switch (pick) {
                case 0 -> {
                    // 自然光束（激光）
                    if (this.getTarget() != null) {
                        startPetalBeamCharge(level, this.getTarget(), FRBConfig.dmg(3.0F) * FRBConfig.dmgNatureBeam(), PHASE_COLORS[0], MobEffects.POISON);
                    }
                }
                case 1 -> {
                    // 灼热之种（80 tick 引信）
                    this.frb$triggerCastAnim();
                    this.scheduleDelayedTask(15, () -> {
                        ServerPlayer tp = level.getEntitiesOfClass(ServerPlayer.class,
                                this.getBoundingBox().inflate(32.0D)).stream().findAny().orElse(null);
                        if (tp != null) {
                            final ServerPlayer target = tp;
                            final BlockPos feet = target.blockPosition();
                            FRBNetwork.sendMarker(target, 80, 0xff5050);
                            this.scheduleDelayedTask(80, () -> {
                                ServerLevel sl = target.serverLevel();
                                if (sl == null) return;
                                fireBurstExplosion(sl, feet);
                            });
                        }
                    });
                }
                case 2 -> {
                    // 霜凝
                    this.frb$triggerCastAnim();
                    this.beginSkillCast(level, 45);
                    this.scheduleDelayedTask(15, () -> startFrostGaze(level, 30));
                }
                case 3 -> {
                    // 积雨云
                    this.frb$triggerCastAnim();
                    this.beginSkillCast(level, 25);
                    this.scheduleDelayedTask(15, () -> {
                        Player nearest = frbNearestFighter(level, 24.0D);
                        if (nearest != null) {
                            RainCloudEntity.spawn(level, nearest.getX(), nearest.getY() + 6.0D, nearest.getZ());
                        }
                    });
                }
                case 4 -> {
                    // 双风刃
                    this.frb$triggerCastAnim();
                    this.scheduleDelayedTask(15, () -> {
                        RandomSource r = level.random;
                        double baseAngle = r.nextDouble() * Math.PI * 2.0D;
                        for (int i = 0; i < 2; i++) {
                            double a = baseAngle + i * Math.PI;
                            Vec3 dir = new Vec3(Math.cos(a), -0.15D, Math.sin(a)).normalize();
                            cn.blockforge.generated.floralritebloomcurios.entity.WindBladeEntity.shoot(
                                    level, this.position().add(0, 0.5D, 0), dir, this);
                        }
                    });
                }
                case 5 -> {
                    // 花之摇篮：点名一名玩家脚下生成法阵
                    this.frb$triggerCastAnim();
                    this.scheduleDelayedTask(15, () -> {
                        var targets = cn.blockforge.generated.floralritebloomcurios.util.Mechanics
                                .playersInDomain(level, this, 24.0D);
                        if (targets.isEmpty()) return;
                        ServerPlayer p = targets.get(level.random.nextInt(targets.size()));
                        BlockPos ground = p.blockPosition().below();
                        FlowerCradleSigilEntity.spawn(level, ground, p);
                        level.playSound(null, ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D,
                                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 1.2F);
                    });
                }
                case 6 -> {
                    // 光辉炎柱
                    this.frb$triggerCastAnim();
                    this.scheduleDelayedTask(15, () -> castFlamePillar(level));
                }
                case 7 -> {
                    // 烈焰击退：1.5 秒蓄力 → 范围爆炸+击退
                    this.frb$triggerCastAnim();
                    this.beginSkillCast(level, 30);
                    this.scheduleDelayedTask(15, () -> startFlameCharge(level, 30));
                }
                case 8 -> {
                    // 钢铁月环
                    this.frb$triggerCastAnim();
                    this.scheduleDelayedTask(15, () -> castFrostMoonRing(level));
                }
                case 9 -> {
                    // 云雨全屏 AOE
                    this.frb$triggerCastAnim();
                    this.scheduleDelayedTask(15, () -> castRainAoe(level));
                }
                case 10 -> {
                    // 召唤虚空裂隙：一次刷 4 个
                    this.frb$triggerCastAnim();
                    this.scheduleDelayedTask(15, () -> {
                        for (int i = 0; i < 4; i++) {
                            spawnSingleVoidRift(level);
                        }
                    });
                }
                default -> {}
            }
        } else {
            this.timePhaseRotateTick--;
        }
        } // end tpSkillDisabled guard
        // 时间阶段主题粒子
        if (this.tickCount % 3 == 0) {
            int pickMote = (this.tickCount / 3) % 6;
            SimpleParticleType mote = switch (pickMote) {
                case 0 -> FRBParticles.TIME_PHASE_01.get();
                case 1 -> FRBParticles.TIME_PHASE_02.get();
                case 2 -> FRBParticles.TIME_PHASE_03.get();
                case 3 -> FRBParticles.TIME_PHASE_04.get();
                case 4 -> FRBParticles.TIME_PHASE_05.get();
                default -> FRBParticles.TIME_PHASE_06.get();
            };
            double angle = this.tickCount * 0.18D;
            level.sendParticles(mote,
                    this.getX() + Math.cos(angle) * 2.4D, this.getY() + 1.6D + Math.sin(angle * 0.7D) * 0.6D,
                    this.getZ() + Math.sin(angle) * 2.4D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (this.tickCount % 40 == 0) {
            level.sendParticles(FRBParticles.TIME_TICK_RING.get(),
                    this.getX(), this.getY() + 0.6D, this.getZ(),
                    2, 2.2D, 0.1D, 2.2D, 0.0D);
        }
    }

    // ---- I2 时间阶段专属机制（r67）：预支生命 / 时间停止 / 时间倒流 ----

    /** 时间阶段机制提示：领域内（96 格）全员 actionbar 文本。 */
    private void broadcastTimeText(ServerLevel level, String key) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(96.0D))) {
            p.displayClientMessage(Component.translatable(key).withStyle(TIME_PHASE_FORMAT), true);
        }
    }

    /**
     * I2 预支生命：吸取领域内所有玩家当前生命值的 25%，计算合计后全部作为回复量给予自身（吸血）。
     */
    public void castLifeDrain(ServerLevel level) {
        var victims = level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(48.0D),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
        if (victims.isEmpty()) return;
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 2.0F, 0.6F);
        broadcastTimeText(level, "frb.boss.time.drain");
        float total = 0.0F;
        for (ServerPlayer p : victims) {
            // r72：生命汲取吸取比例 = 25% × 全局倍率 × 时间阶段倍率
            float take = Math.max(p.getHealth() * 0.25F * FRBConfig.dmg(1.0F) * FRBConfig.dmgLifeDrain(), 1.0F);
            p.hurt(level.damageSources().indirectMagic(this, this), take);
            total += take;
            // 生命汲取：受害者 → boss 的拖尾粒子（速度 = 目标差值 / 20，约 20 tick 走完）
            level.sendParticles(FRBParticles.TIME_HOURGLASS.get(),
                    p.getX(), p.getY() + 1.0D, p.getZ(), 6,
                    (this.getX() - p.getX()) / 20.0D,
                    (this.getY() + 1.6D - p.getY() - 1.0D) / 20.0D,
                    (this.getZ() - p.getZ()) / 20.0D, 1.0D);
        }
        if (total > 0.0F) {
            this.heal(total);
            level.sendParticles(ParticleTypes.HEART,
                    this.getX(), this.getY() + this.getBbHeight() + 0.5D, this.getZ(),
                    8, 0.8D, 0.4D, 0.8D, 0.02D);
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 1.5F, 0.6F);
        }
    }

    /**
     * I2 时间停止：快照领域内全部可冻实体的位置，随后每 tick 强制拽回快照点 + 清零速度；
     * 第 {@link #TIME_STOP_COMBO_AT} tick 接一个不重复的连招（自然光束 / 灼热之种 / 风刃三选一）。
     */
    public void startTimeStop(ServerLevel level) {
        if (this.timeStopTicks > 0) return;
        this.timeStopTicks = TIME_STOP_TICKS;
        this.timeStopSnapshot.clear();
        this.snapshotTimeStop(level);
        // r73 · L5 时间静止 Mixin 入口：登记维度 + 灌快照到全局状态，让 ParticleEngine.render /
        // Entity.baseTick 拦截器在时停期间统一冻结非豁免实体与粒子
        cn.blockforge.generated.floralritebloomcurios.util.TimeStopState.setActive(level, level.getGameTime());
        cn.blockforge.generated.floralritebloomcurios.util.TimeStopState.putFrozenPositions(this.timeStopSnapshot);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 3.0F, 0.5F);
        broadcastTimeText(level, "frb.boss.time.stop");
        // 时停全屏黑白滤镜（FX_GRAY），不再用 FX_TINT 白色染色
        FRBNetwork.sendBossFx(level, this, BossFxPacket.FX_GRAY, 1.0F, TIME_STOP_TICKS, TIME_PHASE_COLOR);
    }

    /** 时停豁免：boss 自身 / 机制实体（含死亡之花）/ boss 自己的弹射物 / 创造与旁观玩家。 */
    private boolean timeStopFreezable(Entity e) {
        if (e == this || e instanceof MechanicEntity) return false;
        if (e instanceof DeathFlowerEntity) return false;
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (e instanceof Projectile proj && proj.getOwner() == this) return false;
        return e instanceof LivingEntity || e instanceof Projectile || e instanceof ItemEntity;
    }

    /** 收集需要冻结的实体并记录快照。 */
    private void snapshotTimeStop(ServerLevel level) {
        for (Entity e : level.getEntities(this, this.getBoundingBox().inflate(48.0D))) {
            if (timeStopFreezable(e)) {
                this.timeStopSnapshot.put(e.getUUID(), e.position());
            }
        }
    }

    /** 时停维持：每 tick 把冻结体拽回快照点，第 30 tick 接连招，结束时清快照 + 恢复音效。 */
    private void tickTimeStop(ServerLevel level) {
        int t = this.timeStopTicks--;
        if (t == TIME_STOP_COMBO_AT) {
            castTimeStopCombo(level);
        }
        for (Entity e : level.getEntities(this, this.getBoundingBox().inflate(48.0D))) {
            if (!timeStopFreezable(e)) continue;
            Vec3 snap = this.timeStopSnapshot.get(e.getUUID());
            if (snap == null) {
                // 时停中途闯入者：就地冻结
                this.timeStopSnapshot.put(e.getUUID(), e.position());
                // r73 · L5：同步写入全局 TimeStopState，让 EntityMixin 在 baseTick 拦截时也能拉到
                cn.blockforge.generated.floralritebloomcurios.util.TimeStopState.putFrozenPositions(java.util.Map.of(e.getUUID(), e.position()));
                continue;
            }
            if (e.distanceToSqr(snap) > 1.0E-4D) {
                if (e instanceof ServerPlayer sp) {
                    sp.connection.teleport(snap.x, snap.y, snap.z, sp.getYRot(), sp.getXRot());
                } else {
                    e.teleportTo(snap.x, snap.y, snap.z);
                }
            }
            e.setDeltaMovement(Vec3.ZERO);
            if (e instanceof LivingEntity le) {
                le.fallDistance = 0.0F;
            }
        }
        // r101：时停期间强制停止所有玩家的持续物品使用（如丰饶连发），
        //      避免 onUseTick 持续生成射弹、时停结束后集中飞出造成暴毙。
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(48.0D))) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (p.isUsingItem()) p.stopUsingItem();
        }
        // 时停视觉：boss 周身表盘 + 刻度环
        if (this.timeStopTicks % 10 == 0) {
            level.sendParticles(FRBParticles.TIME_CLOCK_FACE.get(),
                    this.getX(), this.getY() + 1.8D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(FRBParticles.TIME_TICK_RING.get(),
                    this.getX(), this.getY() + 0.6D, this.getZ(), 2, 3.0D, 0.1D, 3.0D, 0.0D);
        }
        if (this.timeStopTicks <= 0) {
            this.timeStopSnapshot.clear();
            // r73 · L5：时停结束，解除全局标记 → ParticleEngine.render + Entity.baseTick 恢复正常
            cn.blockforge.generated.floralritebloomcurios.util.TimeStopState.clearActive(level);
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.5F, 1.4F);
        }
    }

    /**
     * 时停连招：从「自然光束 / 焚尽灼热之种 / 天空风刃」中随机选一个未用过的释放；
     * 下一次从剩下两个中随机，第三次必定是最后一个未用机制；三种全用后重置回随机三选一。
     */
    private void castTimeStopCombo(ServerLevel level) {
        java.util.List<Integer> unused = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (!this.timeStopComboUsed[i]) unused.add(i);
        }
        if (unused.isEmpty()) {
            java.util.Arrays.fill(this.timeStopComboUsed, false);
            for (int i = 0; i < 3; i++) unused.add(i);
        }
        int pick = unused.get(level.random.nextInt(unused.size()));
        this.timeStopComboUsed[pick] = true;
        switch (pick) {
            case 0 -> {
                // 自然·花瓣光束（时停版：伤害更高 + 中毒）
                LivingEntity target = this.getTarget();
                if (target != null) {
                    // r72：时停自然光束（伤害 = base × 全局 × 自然倍率，4.0F 比常规 3.0F 更疼）
                    startPetalBeamCharge(level, target, FRBConfig.dmg(4.0F) * FRBConfig.dmgNatureBeam(), PHASE_COLORS[0], MobEffects.POISON);
                }
            }
            case 1 -> {
                // 焚尽·灼热之种（80 tick 引信：时停结束后正好要求玩家走位拆弹）
                // r74：同 timePhase 修复——废弃 sl.explode()，改用手写演出
                ServerPlayer tp = level.getEntitiesOfClass(ServerPlayer.class,
                        this.getBoundingBox().inflate(32.0D),
                        p -> p.isAlive() && !p.isCreative() && !p.isSpectator())
                        .stream().findAny().orElse(null);
                if (tp != null) {
                    final ServerPlayer target = tp;
                    final BlockPos feet = target.blockPosition();
                    FRBNetwork.sendMarker(target, 80, 0xff5050);
                    this.scheduleDelayedTask(80, () -> {
                        ServerLevel sl = target.serverLevel();
                        if (sl == null) return;
                        fireBurstExplosion(sl, feet);
                    });
                }
            }
            default -> {
                // 天空·风刃：瞄准时停中无法位移的玩家，正面 + 左右夹击共三道
                LivingEntity target = this.getTarget();
                if (target != null) {
                    Vec3 from = this.position().add(0, 0.5D, 0);
                    Vec3 to = target.position().add(0, target.getBbHeight() * 0.4D, 0);
                    Vec3 dir = to.subtract(from).normalize();
                    cn.blockforge.generated.floralritebloomcurios.entity.WindBladeEntity.shoot(level, from, dir, this);
                    Vec3 cross = new Vec3(-dir.z, 0.0D, dir.x).normalize();
                    cn.blockforge.generated.floralritebloomcurios.entity.WindBladeEntity.shoot(
                            level, from.add(cross.scale(1.5D)), dir, this);
                    cn.blockforge.generated.floralritebloomcurios.entity.WindBladeEntity.shoot(
                            level, from.subtract(cross.scale(1.5D)), dir, this);
                }
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 1.6F);
            }
        }
    }

    /**
     * I2 时间倒流：把领域内玩家传回 3 秒（60 tick）前的位置——只回溯位置与视角，不回溯任何状态。
     */
    public void castTimeRewind(ServerLevel level) {
        var players = level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(48.0D));
        boolean any = false;
        for (ServerPlayer p : players) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            TimeRewindTracker.Sample s = this.rewindTracker.lookup(p.getUUID(), TimeRewindTracker.REWIND_TICKS);
            if (s == null) continue;
            any = true;
            // 原地与落点双向演出
            level.sendParticles(ParticleTypes.PORTAL,
                    p.getX(), p.getY() + 1.0D, p.getZ(), 24, 0.3D, 0.8D, 0.3D, 0.5D);
            p.connection.teleport(s.x(), s.y(), s.z(), s.yaw(), s.pitch());
            p.fallDistance = 0.0F;
            p.setDeltaMovement(Vec3.ZERO);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    s.x(), s.y() + 1.0D, s.z(), 24, 0.3D, 0.8D, 0.3D, 0.5D);
            level.playSound(null, s.x(), s.y(), s.z(),
                    SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
        if (any) {
            broadcastTimeText(level, "frb.boss.time.rewind");
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
    }

    // ---- C2 烈焰击退：1.5 秒蓄力 → 6 爆炸 + 击退 6 格 ----

    public void startFlameCharge(ServerLevel level, int ticks) {
        if (this.flameChargeTicks > 0) return;
        this.flameChargeTicks = ticks;
        // 蓄力粒子：橙红 + ember_spark
        level.sendParticles(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles.FLAME_KNOCKBACK_CHARGE.get(),
                this.getX(), this.getY() + 1.0D, this.getZ(), 24, 1.5D, 0.8D, 1.5D, 0.05D);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void tickFlameCharge(ServerLevel level) {
        // r100：喝醉时取消蓄力，不释放
        if (this.hasEffect(FRBEffects.DRUNK.get())) {
            this.flameChargeTicks = 0;
            return;
        }
        this.flameChargeTicks--;
        if (this.flameChargeTicks > 0) return;
        // 蓄力结束：6 格内全玩家 6 爆炸 + 击退
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(6.0D))) {
            p.hurt(level.damageSources().explosion(this, this), FRBConfig.dmg(6.0F) * FRBConfig.dmgFlameKnockback());
            Vec3 dir = p.position().subtract(this.position()).normalize().scale(1.5D);
            p.push(dir.x, 0.3D, dir.z);
            p.hurtMarked = true;
            level.sendParticles(cn.blockforge.generated.floralritebloomcurios.registry.FRBParticles.FLAME_KNOCKBACK_CHARGE.get(),
                    p.getX(), p.getY() + 0.5D, p.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.04D);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 1.0F);
        FRBNetwork.sendBossFx(level, this, BossFxPacket.FX_FLASH, 0.4F, 15);
        // r98：boss 下方播放爆炸特效，让玩家明确是被 boss 击退的
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 0.5D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                this.getX(), this.getY() + 0.5D, this.getZ(), 6, 0.8D, 0.5D, 0.8D, 0.03D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                this.getX(), this.getY() + 0.8D, this.getZ(), 16, 1.2D, 0.6D, 1.2D, 0.05D);
    }

    // ---- 灼热之种爆炸演出（r74）：从 timePhase / castTimeStopCombo 重复触发，原版 sl.explode()
    //      即便 NONE 也会做方块 ray-trace + 实体扫描，0.75s 触发时明显卡顿；
    //      改用手写粒子 + 音效 + 1.5 格内伤害，与 Mechanics.BurningSeedMechanic 保持一致。
    private void fireBurstExplosion(ServerLevel sl, BlockPos feet) {
        double ex = feet.getX() + 0.5D, ey = feet.getY() + 0.5D, ez = feet.getZ() + 0.5D;
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                ex, ey, ez, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                ex, ey, ez, 4, 0.6D, 0.4D, 0.6D, 0.02D);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                ex, ey + 0.4D, ez, 8, 0.5D, 0.3D, 0.5D, 0.04D);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                ex, ey + 0.2D, ez, 6, 0.5D, 0.3D, 0.5D, 0.03D);
        sl.playSound(null, ex, ey, ez,
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 1.0F);
        for (ServerPlayer victim : sl.getEntitiesOfClass(ServerPlayer.class,
                new net.minecraft.world.phys.AABB(feet).inflate(1.5D))) {
            victim.hurt(sl.damageSources().explosion(this, this),
                    FRBConfig.dmg(8.0F) * FRBConfig.dmgBurningSeed());
        }
    }

    // ---- D1 霜凝：1.5 秒蓄力 → 判定玩家画面是否有 boss → 8 冰冻 + 3 秒失明 ----

    public void startFrostGaze(ServerLevel level, int ticks) {
        if (this.frostGazeTicks > 0) return;
        this.frostGazeTicks = ticks;
        this.entityData.set(DATA_FROST_GAZE, ticks);
        // r90：霜凝蓄力期间升空 3 格 + 无敌（持续整个蓄力 30 tick）
        this.beginSkillCast(level, ticks);
        // r85：抬手动画已由调用方统一触发（调度器 / 时间阶段轮换均先播 *_du_tiao 再延后 15t 调用），此处不再重复
        // r74：一次性扫一次领域内名单缓存，蓄力期粒子 / 读条结束判定都用这份名单。
        //      不再每 tick getEntitiesOfClass——读条期 30 tick 累加能省掉 14 次 AABB 扫描 + 5 次列表分配。
        this.frostGazeCachedTargets.clear();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(48.0D),
                pp -> pp.isAlive() && !pp.isCreative() && !pp.isSpectator())) {
            this.frostGazeCachedTargets.add(p);
            FRBNetwork.sendMarker(p, ticks, 0x9fdcff);
        }
        // r66：提示音换成用户提供的 chou_hen.ogg（蓄力开始时播放一次）
        level.playSound(null, this.getX(), this.getY() + 2.0D, this.getZ(),
                cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.CHOU_HEN.get(),
                SoundSource.HOSTILE, 1.6F, 1.0F);
        // r75：固定头顶的大眼睛实体（1.5x，不旋转，不随玩家移动）
        FriliasGazeEyeEntity.spawnOrUpdate(level, this);
        // FROST_EYE 当前位置爆开（仅作为起手特效，不持续刷在 boss 身上——r75 截图 #6 移除 boss 身上的 gaze 粒子）
        for (int s = 0; s < 6; s++) {
            double a = s * Math.PI / 3.0D;
            level.sendParticles(FRBParticles.FROST_EYE.get(),
                    this.getX() + Math.cos(a) * 0.5D, this.getY() + 2.2D,
                    this.getZ() + Math.sin(a) * 0.5D, 2, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    private void tickFrostGaze(ServerLevel level) {
        // r100：喝醉时取消蓄力，不释放
        if (this.hasEffect(FRBEffects.DRUNK.get())) {
            this.frostGazeTicks = 0;
            this.entityData.set(DATA_FROST_GAZE, 0);
            return;
        }
        this.frostGazeTicks--;
        if (this.frostGazeTicks > 0) {
            this.entityData.set(DATA_FROST_GAZE, this.frostGazeTicks);
            // r70 卡顿减负：所有粒子频率 2t→4t，环绕粒子 10→6 颗且只对参战玩家刷
            if (this.tickCount % 4 == 0) {
                // r75：移除 boss 身上的 FROST_EYE 粒子（截图 #6 要求"boss 身上不要 gaze 粒子"）；
                //      大眼睛已经做成 FriliasGazeEyeEntity 单独挂载，玩家附近仍保留粒子定位。
                // r74：用 startFrostGaze 缓存的名单喷霜眼粒子，不再重扫 AABB
                for (ServerPlayer p : this.frostGazeCachedTargets) {
                    for (int i = 0; i < 6; i++) {
                        double a = this.tickCount * 0.25D + i * Math.PI * 2.0D / 6.0D;
                        level.sendParticles(FRBParticles.FROST_EYE.get(),
                                p.getX() + Math.cos(a) * 0.9D, p.getY() + 1.0D,
                                p.getZ() + Math.sin(a) * 0.9D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
                // boss 周围 8 格随机方向喷冷气（看得到气流席卷玩家）
                double a = level.random.nextDouble() * Math.PI * 2.0D;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                        this.getX() + Math.cos(a) * 1.6D, this.getY() + 0.8D,
                        this.getZ() + Math.sin(a) * 1.6D, 4, 0.4D, 0.2D, 0.4D, 0.0D);
            }
            return;
        }
        this.entityData.set(DATA_FROST_GAZE, 0);
        // r74：蓄力结束按 skill 释放瞬间一次性判定所有玩家是否在看着 boss——
        //      不再每 tick 判定；用 startFrostGaze 缓存的名单（不重扫，原列表）
        for (ServerPlayer p : this.frostGazeCachedTargets) {
            if (!bossInPlayerView(p)) continue;
            // r72：霜凝之视伤害 = base × 全局 × 霜华阶段倍率
            p.hurt(level.damageSources().indirectMagic(this, this), FRBConfig.dmg(8.0F) * FRBConfig.dmgFrostGaze());
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
            // r75：被霜凝命中的玩家 actionbar 提示"神明的力量让你暂时失去了光明"
            p.displayClientMessage(Component.translatable("frb.mechanic.frost.blind")
                    .withStyle(ChatFormatting.AQUA), true);
            // r70 卡顿减负：结算粒子 100+8 → 30+4
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.0D, p.getZ(),
                    30, 0.7D, 1.0D, 0.7D, 0.06D);
            level.sendParticles(FRBParticles.FROST_EYE.get(),
                    p.getX(), p.getY() + 1.2D, p.getZ(), 4, 0.4D, 0.5D, 0.4D, 0.0D);
        }
        this.frostGazeCachedTargets.clear();
        FRBNetwork.sendBossFx(level, this, BossFxPacket.FX_FLASH, 0.85F, 25);
        // r75：移除头顶大眼睛实体（结算完毕 → 让眼睛淡出）
        if (this.gazeEye != null) {
            this.gazeEye.discard();
            this.gazeEye = null;
        }
    }

    private boolean bossInPlayerView(ServerPlayer p) {
        // 简化判定：检查 boss 与玩家视线的点积 > cos(45°)
        Vec3 look = p.getViewVector(1.0F);
        Vec3 toBoss = this.position().subtract(p.getEyePosition()).normalize();
        return look.dot(toBoss) > 0.707D;
    }

    // ---- E2 落雷：延迟 tick 后劈向目标 ----

    public void scheduleLightningStrike(ServerLevel level, ServerPlayer target, int delayTicks) {
        scheduleDelayedTask(delayTicks, () -> {
            ServerLevel sl = (target != null && target.isAlive()) ? target.serverLevel() : null;
            if (sl == null) return;
            if (!target.isAlive()) return;
            // 检查贴太近 → 伤害翻倍
            boolean doubled = false;
            for (ServerPlayer other : sl.getEntitiesOfClass(ServerPlayer.class,
                    target.getBoundingBox().inflate(3.0D))) {
                if (other != target && other.isAlive()) {
                    doubled = true;
                    break;
                }
            }
            // r72：落雷伤害 = base × 全局 × 云雨阶段倍率（翻倍=12，单人=6）
        float damage = doubled
                ? FRBConfig.dmg(12.0F) * FRBConfig.dmgLightning()
                : FRBConfig.dmg(6.0F) * FRBConfig.dmgLightning();
            target.hurt(sl.damageSources().lightningBolt(), damage);
            // r96：用原版纯视觉劈雷（不重复造伤），替换原来只有电火花粒子的方案
            // Forge 1.20.1 的 ServerLevel 没有 strikeLightningEffect，手动生成 LightningBolt 并设为仅视觉
            net.minecraft.core.BlockPos boltPos = net.minecraft.core.BlockPos.containing(target.position());
            net.minecraft.world.entity.LightningBolt bolt = new net.minecraft.world.entity.LightningBolt(
                    net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, sl);
            bolt.moveTo(boltPos.getX() + 0.5D, boltPos.getY(), boltPos.getZ() + 0.5D);
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + 1.0D, target.getZ(), 16, 0.4D, 0.5D, 0.4D, 0.05D);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.5F, 1.2F);
        });
    }

    // ---- 自管延迟任务队列（避免依赖 MinecraftServer.tell(Runnable, int)）----

    /** r96：技能释放完毕后，若队列里有暂存的机制，取出第一个执行（cd 在释放后才真正开始结算）。 */
    private void tryDrainMechanicQueue() {
        if (this.mechanicQueue.isEmpty()) return;
        // 仍在释放中（长读条技能如霜凝/风眼）则不排空，等 skillCastTicks 归零后再排空
        if (this.skillCastTicks > 0 || this.activeMechanicCount > 0) return;
        Runnable next = this.mechanicQueue.remove(0);
        try {
            next.run();
        } catch (Exception e) {
            // 防御：避免单个机制异常卡死队列
            this.activeMechanicCount = Math.max(0, this.activeMechanicCount - 1);
        }
    }

    private final java.util.List<PendingTask> pendingTasks = new java.util.ArrayList<>();

    private record PendingTask(int dueTick, Runnable task) {}

    /** 由机制调用：在 dueTick 后执行 task（以服务端 tickCount 计数）。 */
    public void scheduleDelayedTask(int delayTicks, Runnable task) {
        if (this.level() instanceof ServerLevel sl) {
            this.pendingTasks.add(new PendingTask(sl.getServer().getTickCount() + delayTicks, task));
        }
    }

    /** aiStep 末尾推进：过期任务触发并清理。 */
    private void tickPendingTasks() {
        if (this.pendingTasks.isEmpty()) return;
        if (!(this.level() instanceof ServerLevel sl)) return;
        int now = sl.getServer().getTickCount();
        for (int i = this.pendingTasks.size() - 1; i >= 0; i--) {
            PendingTask p = this.pendingTasks.get(i);
            if (p.dueTick() <= now) {
                try {
                    p.task().run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.pendingTasks.remove(i);
            }
        }
    }

    // ---- 通用演出 ----

    /** 环绕的对应颜色荧光花朵粒子。 */
    private void ambientFlowers(ServerLevel level) {
        if (this.tickCount % 4 != 0) return;
        double a = this.tickCount * 0.09D;
        int c = PHASE_COLORS[this.phase];
        Vector3f col = new Vector3f(((c >> 16) & 255) / 255.0F, ((c >> 8) & 255) / 255.0F, (c & 255) / 255.0F);
        level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(col, 1.4F),
                this.getX() + Math.cos(a) * 1.7D, this.getY() + 1.0D + Math.sin(a * 1.7D) * 0.6D,
                this.getZ() + Math.sin(a) * 1.7D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private void ringBurst(ServerLevel level, int color) {
        Vector3f col = new Vector3f(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F,
                (color & 255) / 255.0F);
        for (int i = 0; i < 48; i++) {
            double a = i / 48.0D * Math.PI * 2.0D;
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(col, 1.6F),
                    this.getX() + Math.cos(a) * 2.2D, this.getY() + 1.1D, this.getZ() + Math.sin(a) * 2.2D,
                    1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
    }

    /**
     * r96：从 boss 中心向外扩散的环形粒子波——分 6 圈、半径递增，每圈粒子带径向初速度，
     * 视觉上形成一波冲击波从 boss 脚下向外扩散。用于云雨全屏 AOE / 枯萎 AOE 的可视化。
     *
     * @param particle 粒子类型
     * @param maxRange 最大扩散半径（格）
     * @param count    每圈粒子数
     * @param yOffset  生成高度相对 boss 脚下的偏移
     */
    private void expandingWave(ServerLevel level, ParticleOptions particle, double maxRange,
                               int count, double yOffset) {
        double bx = this.getX();
        double by = this.getY() + yOffset;
        double bz = this.getZ();
        // 6 圈，半径从 maxRange*0.15 递增到 maxRange
        for (int ring = 0; ring < 6; ring++) {
            double r = maxRange * (0.15D + 0.17D * ring);
            float speed = 0.15F + 0.05F * ring; // 外圈更快，形成连续波
            for (int i = 0; i < count; i++) {
                double a = 2.0D * Math.PI * i / count;
                double dx = Math.cos(a);
                double dz = Math.sin(a);
                // 在半径 r 处生成 1 颗粒子，给径向初速度（通过 xDist/zDist 模拟方向）
                level.sendParticles(particle,
                        bx + dx * r, by, bz + dz * r,
                        1, dx * 0.3D, 0.05D, dz * 0.3D, speed);
            }
        }
    }

    // ---- A6 激光读条（r66）：周身 6 点竖向环绕粒子汇聚到 boss 面前 → 发射花瓣光束 ----

    /**
     * 开始读条：锁定目标并蓄力 1.2 秒（24 tick）。蓄力期间 boss 持续转向目标，
     * 6 个锚点（头顶两侧 / 身体两侧 / 脚下两侧）绕竖直轴旋转并向面前收拢，
     * 期间向面前一点发射汇聚花瓣粒子；归零后调用 petalBeam 结算。
     */
    public void startPetalBeamCharge(ServerLevel level, LivingEntity target, float damage, int color,
                                     MobEffect potion) {
        if (this.beamChargeTicks > 0) return; // 正在蓄力，不叠加
        this.beamTarget = target;
        this.beamDamage = damage;
        this.beamColor = color;
        this.beamPotion = potion;
        this.beamChargeTicks = BEAM_CHARGE_TICKS;
        this.entityData.set(DATA_BEAM_CHARGE, BEAM_CHARGE_TICKS);
    }

    private void tickPetalBeamCharge(ServerLevel level) {
        // r100：喝醉时取消蓄力，不发射光束
        if (this.hasEffect(FRBEffects.DRUNK.get())) {
            this.beamChargeTicks = 0;
            this.entityData.set(DATA_BEAM_CHARGE, 0);
            this.beamTarget = null;
            return;
        }
        this.beamChargeTicks--;
        // r69（bug2）：读条期间死亡立即清零，不再发射光束
        if (this.isDeadOrDying()) {
            this.beamChargeTicks = 0;
            this.entityData.set(DATA_BEAM_CHARGE, 0);
            this.beamTarget = null;
            return;
        }
        // r73 L1：读条剩余 15 tick（=0.75s）时触发 *_du_tiao，让 du_tiao 在 0.75s 标记刚好光束命中
        if (this.beamChargeTicks == 15) {
            this.frb$triggerCastAnim();
        }
        if (this.beamChargeTicks <= 0) {
            this.entityData.set(DATA_BEAM_CHARGE, 0);
            LivingEntity target = this.beamTarget;
            this.beamTarget = null;
            if (target != null && target.isAlive()
                    && target.position().distanceToSqr(this.position()) <= 34.0D * 34.0D) {
                petalBeam(level, target, this.beamDamage, this.beamColor, this.beamPotion);
            }
            return;
        }
        this.entityData.set(DATA_BEAM_CHARGE, this.beamChargeTicks);
        // 蓄力期间持续转向锁定目标
        if (this.beamTarget != null) {
            Vec3 face = this.beamTarget.getEyePosition().subtract(this.getEyePosition());
            float yaw = (float) Mth.atan2(face.z, face.x) - (float) Math.PI / 2.0F;
            this.setRot(yaw, this.getXRot());
            this.yHeadRot = yaw;
        }
        // 汇聚终点：boss 面前一点
        Vec3 look = this.getLookAngle();
        Vec3 facePoint = this.getEyePosition().add(look.scale(1.4D));
        Vector3f col = new Vector3f(((this.beamColor >> 16) & 255) / 255.0F,
                ((this.beamColor >> 8) & 255) / 255.0F, (this.beamColor & 255) / 255.0F);
        double prog = 1.0D - this.beamChargeTicks / (double) BEAM_CHARGE_TICKS; // 0 → 1
        double r = 1.5D - 1.25D * prog; // 旋转半径逐渐收拢
        double[] heights = {2.6D, 1.4D, 0.2D}; // 头顶 / 身侧 / 脚下
        for (int i = 0; i < 6; i++) {
            double h = heights[i / 2];
            double y = this.getY() + h + (1.8D - h) * prog * 0.5D; // 高度缓慢向面前靠拢
            double ang = this.tickCount * 0.55D + i * Math.PI / 3.0D;
            double px = this.getX() + Math.cos(ang) * r;
            double pz = this.getZ() + Math.sin(ang) * r;
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(col, 1.1F),
                    px, y, pz, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            // 汇聚花瓣：velocity 参数承载目标坐标（ConvergingPetalParticle 40 刻弧线飞向面前）
            if (this.beamChargeTicks % 4 == 0) {
                level.sendParticles(FRBParticles.CONVERGING_PETAL.get(),
                        px, y, pz, 1,
                        facePoint.x - px, facePoint.y - y, facePoint.z - pz, 1.0D);
            }
        }
    }

    /** 花瓣光束：粒子路径 + 伤害 + 可选负面效果。 */
    private void petalBeam(ServerLevel level, LivingEntity target, float damage, int color,
                           MobEffect potion) {
        Vec3 from = this.getEyePosition();
        Vec3 to = target.getEyePosition();
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len > 30.0D) return;
        Vec3 step = delta.scale(1.0D / Math.max(1.0D, len / 0.45D));
        Vector3f col = new Vector3f(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F,
                (color & 255) / 255.0F);
        Vec3 p = from;
        for (int i = 0; i < len / 0.45D; i++) {
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(col, 0.9F),
                    p.x, p.y, p.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
            p = p.add(step);
        }
        level.playSound(null, this, SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.4F);
        target.hurt(level.damageSources().mobAttack(this), damage);
        if (potion != null) {
            target.addEffect(new MobEffectInstance(potion, 100, 0));
        }
    }

    // ---- 封印领域 ----

    /** 领域半径：靠近 Boss（24 格）即被封印当前阶段及之前阶段的花灵饰品；边缘有属性 emblem 环提示（客户端渲染）。 */
    private static final double SEAL_RADIUS = 24.0D;

    /**
     * 封印领域：由 aiStep 每秒刷新。领域内玩家封印 PHASE_SEALS[0..当前阶段]；
     * 领域外的全服玩家（逃跑 / 被击败后重生 / 切换维度）解除全部封印，
     * 因此被打死后饰品自然回来，再次靠近 Boss 又会回到被封印状态。
     */
    /** 客户端 BGM 判定：存活且开场演出已结束（战斗中）。 */
    public boolean isBattleMusicOn() {
        return this.isAlive() && !this.entityData.get(DATA_INTRO);
    }

    private void updateSealDomain(ServerLevel level) {
        // r91：领域边界可视化改到客户端 FRBWorldRender（八阶段属性 emblem 环），不再服务端播尘粒。
        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(SEAL_RADIUS));
        int maxPhase = Math.min(this.phase, PHASE_SEALS.length - 1);
        // r77：枯萎阶段额外封印黑灵草（PHASE_SEALS 长度是 6，最后一段 decay 没有挂花灵，
        //      单独追加 BLACK_SPIRIT 让玩家在最后一阶段没法继续靠黑死病光环偷血）。
        java.util.List<CharmType> extraSeal = new java.util.ArrayList<>();
        if (this.phase == 6) extraSeal.add(CharmType.BLACK_SPIRIT);
        for (ServerPlayer p : nearby) {
            for (int i = 0; i <= maxPhase; i++) {
                CharmType sealed = PHASE_SEALS[i];
                if (FRBState.sealCharm(p, sealed) > 0) {
                    p.displayClientMessage(Component.translatable("frb.boss.seal",
                                    Component.translatable("item.frb." + sealed.id))
                            .withStyle(ChatFormatting.DARK_RED), false);
                    p.playSound(cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.SEAL_APPLY.get(),
                            1.0F, 0.5F);
                }
            }
            for (CharmType sealed : extraSeal) {
                if (FRBState.sealCharm(p, sealed) > 0) {
                    p.displayClientMessage(Component.translatable("frb.boss.seal",
                                    Component.translatable("item.frb." + sealed.id))
                            .withStyle(ChatFormatting.DARK_RED), false);
                    p.playSound(cn.blockforge.generated.floralritebloomcurios.registry.FRBSounds.SEAL_APPLY.get(),
                            1.0F, 0.5F);
                }
            }
        }
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (nearby.contains(p)) continue;
            if (FRBState.releaseAllSeals(p) > 0) {
                p.displayClientMessage(Component.translatable("frb.boss.unseal")
                        .withStyle(ChatFormatting.YELLOW), false);
            }
        }
    }

    // ---- 离场复原（击败或消散都解除封印 / 清理残留实体） ----

    public void releaseFight(boolean defeated) {
        if (this.level().isClientSide) return;
        ServerLevel level = (ServerLevel) this.level();
        // Boss 结束后清理全服所有玩家的封印（含已切维度的玩家），避免饰品永久残留。
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (FRBState.releaseAllSeals(p) > 0) {
                p.displayClientMessage(Component.translatable(
                                defeated ? "frb.boss.unseal.win" : "frb.boss.unseal")
                        .withStyle(defeated ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
            }
        }
        for (VoidRiftEntity rift : level.getEntitiesOfClass(VoidRiftEntity.class,
                this.getBoundingBox().inflate(64.0D))) {
            rift.discard();
        }
        for (SpikeEntity spike : level.getEntitiesOfClass(SpikeEntity.class,
                this.getBoundingBox().inflate(64.0D))) {
            spike.discard();
        }
        for (UUID id : this.flowerIds) {
            if (level.getEntity(id) instanceof DeathFlowerEntity f) {
                f.discard();
            }
        }
        // r101：战斗结束清理万变水波（含时间阶段复用云雨技能产生的）
        clearWaterRipples(level);
        // r101：清除所有阶段减伤 debuff（无限时长版必须手动清）
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.removeEffect(FRBEffects.FROST_BLOOM.get());
            p.removeEffect(FRBEffects.CLOUD_GRACE.get());
            p.removeEffect(FRBEffects.TIME_FLOWER.get());
        }
        this.setNoGravity(false);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide) {
            // r69（bug2）：离场清读条，防止客户端读条数据残留
            this.beamChargeTicks = 0;
            this.entityData.set(DATA_BEAM_CHARGE, 0);
            // r75：boss 离场清掉头顶 gaze 大眼睛实体
            if (this.gazeEye != null) {
                this.gazeEye.discard();
                this.gazeEye = null;
            }
            // r73 · L5：boss 死亡/消散时若正在时停，立刻解除全局标记（否则 ParticleEngine.render
            // 和 Entity.baseTick 会一直拦截到玩家重进维度）
            if (this.timeStopTicks > 0) {
                cn.blockforge.generated.floralritebloomcurios.util.TimeStopState.clearActive(this.level() instanceof ServerLevel sl ? sl : null);
                this.timeStopTicks = 0;
                this.timeStopSnapshot.clear();
            }
            // 离场必解封：KILLED = 被击败复原（不依赖事件总线的单一时序），
            // 消散（切和平 / 管理员移除 / 区块卸载清理）同样复原，避免封印永久残留。
            this.releaseFight(reason == Entity.RemovalReason.KILLED);
        }
        super.remove(reason);
    }

    // ---- 序列化 ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FRBPhase", this.phase);
        tag.putInt("FRBTransition", this.transition);
        tag.putInt("FRBPhaseTimer", this.timer);
        tag.putInt("FRBRainTeleports", this.rainTeleports);
        tag.putInt("FRBIntro", this.introTicks);
        tag.putDouble("FRBIntroGroundY", this.introGroundY);
        ListTag flowers = new ListTag();
        for (UUID id : this.flowerIds) {
            CompoundTag one = new CompoundTag();
            one.putUUID("Id", id);
            flowers.add(one);
        }
        tag.put("FRBFlowers", flowers);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.phase = Math.max(0, Math.min(6, tag.getInt("FRBPhase")));
        this.transition = Mth.clamp(tag.getInt("FRBTransition"), 0, TRANSITION_TICKS);
        this.timer = Math.max(0, tag.getInt("FRBPhaseTimer"));
        this.entityData.set(DATA_PHASE, this.phase);
        this.entityData.set(DATA_TRANSITION, this.transition);
        this.bossEvent.setName(barName());
        this.rainTeleports = tag.getInt("FRBRainTeleports");
        this.entityData.set(DATA_RUNES, Math.max(0, 4 - this.rainTeleports));
        this.bossEvent.setColor(BAR_COLORS[this.phase]);
        this.introTicks = Math.max(0, tag.getInt("FRBIntro"));
        this.entityData.set(DATA_INTRO, this.introTicks > 0);
        this.introGroundY = tag.contains("FRBIntroGroundY")
                ? tag.getDouble("FRBIntroGroundY") : Double.NaN;
        this.flowerIds.clear();
        ListTag flowers = tag.getList("FRBFlowers", 10);
        for (int i = 0; i < flowers.size(); i++) {
            CompoundTag one = flowers.getCompound(i);
            if (one.hasUUID("Id")) {
                this.flowerIds.add(one.getUUID("Id"));
            }
        }
    }

    private Component barName() {
        // r75 防御：time 阶段走独立 lang，避免越界。理论上 PHASE_KEYS 已扩到长度 8，
        // 此处额外做钳制作为兜底（防止后续误改）。
        String key = (this.phase >= 0 && this.phase < PHASE_KEYS.length)
                ? PHASE_KEYS[this.phase] : "time";
        return Component.translatable("frb.boss.bar." + key);
    }

    public int getFrbPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public int getTransitionTicks() {
        return this.entityData.get(DATA_TRANSITION);
    }

    /** 是否处于开场动画（客户端据此摆出苏醒姿态；经 DATA_INTRO 同步到客户端）。 */
    public boolean inIntro() {
        return this.entityData.get(DATA_INTRO);
    }
}
