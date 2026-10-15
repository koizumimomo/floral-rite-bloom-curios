package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * r73 · L1：frilias 渲染改走 GeckoLib 的 GeoEntityRenderer（frilias.geo.json + frilias.animation.json）。
 * <p>
 * <b>骨骼姿态分层</b>：动画（idle / phase_open / cast）走 channel 控制；手腕骨骼 arm_l / arm_r 由本类
 * 在 {@link GeoRenderEvent.Entity.Pre} 里硬覆盖，覆盖到策略：
 * <ul>
 *   <li>开场上举 / 转阶段抬手：双手向两侧上举 + 头微仰（沿用 r68 原 FriliasModel.java:328-331 的姿态）</li>
 *   <li>读条（beamChargeTicks > 0） / 万花凋零咏唱（milleFlorumCastTick > 0）：双手在胸前交叉蓄力</li>
 * </ul>
 * <b>几何关键演出</b>：boss 脚下浮现的花之摇篮法阵（{@code flower_cradle_sigil.png}，emissive 30%）属于激光
 * 读条专属，与 phase_open 的 8 个属性魔法阵正交，互不影响。
 * <p>
 * <b>读条 HUD</b>：保留 r69 把读条从屏幕底部搬到 boss 头顶 billboard；
 * r73 把宽度再放大 1.6×（barW = 1.6×1.6 = 2.56），按阶段选贴图（霜华/时间/其他）。
 * <p>
 * r73 L1（临时）：FriliasBackRingLayer 是基于老 vanilla FriliasModel 的 RenderLayer，GeoEntityRenderer
 * 不兼容；新 geo 模型已经自包含向后环绕（待用户做骨架造型后重新加）。本类暂不挂任何 layer。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FriliasRenderer extends GeoEntityRenderer<FriliasEntity> {
    /** r96：boss 读条时脚下法阵——按阶段切换 8 张属性法阵（boss_phase_sigils/）。
     *  索引 = phase（0自然 1焚尽 2霜华 3云雨 4天空 5虚空 6枯萎 7时间）。 */
    private static final ResourceLocation[] SIGIL_TEXS = new ResourceLocation[] {
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/boss_phase_sigils/nature.png"),
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/boss_phase_sigils/ember.png"),
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/boss_phase_sigils/frost.png"),
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/boss_phase_sigils/rain.png"),
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/boss_phase_sigils/sky.png"),
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/boss_phase_sigils/void.png"),
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/boss_phase_sigils/wither.png"),
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/boss_phase_sigils/time.png"),
    };
    /** 按阶段取法阵 RenderType（emissive 发光）。 */
    private static net.minecraft.client.renderer.RenderType sigilType(int phase) {
        int idx = Math.max(0, Math.min(SIGIL_TEXS.length - 1, phase));
        return FRBRenderTypes.emissiveGlow(SIGIL_TEXS[idx]);
    }
    /** r69：读条 HUD 贴图（外框 328×38 + 内填充 256×16，从 FriliasBossOverlay 移入）。 */
    private static final ResourceLocation HUD_FRAME = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/floral_progress_frame.png");
    private static final ResourceLocation HUD_FILL = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/floral_progress_fill.png");
    private static final net.minecraft.client.renderer.RenderType FRAME_TYPE =
            FRBRenderTypes.emissiveGlow(HUD_FRAME);
    private static final net.minecraft.client.renderer.RenderType FILL_TYPE =
            FRBRenderTypes.emissiveGlow(HUD_FILL);
    /** r70：霜华阶段专用读条贴图（frost_progress_frame/fill.png，用户提供）。 */
    private static final ResourceLocation FROST_FRAME = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/frost_progress_frame.png");
    private static final ResourceLocation FROST_FILL = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/frost_progress_fill.png");
    private static final net.minecraft.client.renderer.RenderType FROST_FRAME_TYPE =
            FRBRenderTypes.emissiveGlow(FROST_FRAME);
    private static final net.minecraft.client.renderer.RenderType FROST_FILL_TYPE =
            FRBRenderTypes.emissiveGlow(FROST_FILL);
    /** r96：焚尽阶段专用读条贴图（flame_progress_frame/fill.png，用户提供）。 */
    private static final ResourceLocation BURN_FRAME = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/flame_progress_frame.png");
    private static final ResourceLocation BURN_FILL = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/flame_progress_fill.png");
    private static final net.minecraft.client.renderer.RenderType BURN_FRAME_TYPE =
            FRBRenderTypes.emissiveGlow(BURN_FRAME);
    private static final net.minecraft.client.renderer.RenderType BURN_FILL_TYPE =
            FRBRenderTypes.emissiveGlow(BURN_FILL);
    /** r96：云雨阶段专用读条贴图（rain_progress_frame/fill.png，待用户提供）。 */
    private static final ResourceLocation RAIN_FRAME = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/rain_progress_frame.png");
    private static final ResourceLocation RAIN_FILL = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/rain_progress_fill.png");
    private static final net.minecraft.client.renderer.RenderType RAIN_FRAME_TYPE =
            FRBRenderTypes.emissiveGlow(RAIN_FRAME);
    private static final net.minecraft.client.renderer.RenderType RAIN_FILL_TYPE =
            FRBRenderTypes.emissiveGlow(RAIN_FILL);
    /** r96：天空阶段专用读条贴图（sky_progress_frame/fill.png，用户提供）。 */
    private static final ResourceLocation ASTRAL_FRAME = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/sky_progress_frame.png");
    private static final ResourceLocation ASTRAL_FILL = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/sky_progress_fill.png");
    private static final net.minecraft.client.renderer.RenderType ASTRAL_FRAME_TYPE =
            FRBRenderTypes.emissiveGlow(ASTRAL_FRAME);
    private static final net.minecraft.client.renderer.RenderType ASTRAL_FILL_TYPE =
            FRBRenderTypes.emissiveGlow(ASTRAL_FILL);
    /** r96：虚空阶段专用读条贴图——用户指定复用 astral_progress_frame/fill.png。 */
    private static final ResourceLocation VOID_FRAME = ASTRAL_FRAME;
    private static final ResourceLocation VOID_FILL = ASTRAL_FILL;
    private static final net.minecraft.client.renderer.RenderType VOID_FRAME_TYPE = ASTRAL_FRAME_TYPE;
    private static final net.minecraft.client.renderer.RenderType VOID_FILL_TYPE = ASTRAL_FILL_TYPE;
    /** r90：时间阶段专用读条贴图（time_progress_frame/fill.png，用户提供）。 */
    private static final ResourceLocation TIME_FRAME = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/time_progress_frame.png");
    private static final ResourceLocation TIME_FILL = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/time_progress_fill.png");
    private static final net.minecraft.client.renderer.RenderType TIME_FRAME_TYPE =
            FRBRenderTypes.emissiveGlow(TIME_FRAME);
    private static final net.minecraft.client.renderer.RenderType TIME_FILL_TYPE =
            FRBRenderTypes.emissiveGlow(TIME_FILL);
    /** r95：枯萎阶段大范围 AOE 读条贴图（decay_progress_frame/fill.png，用户提供）。 */
    private static final ResourceLocation DECAY_FRAME = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/decay_progress_frame.png");
    private static final ResourceLocation DECAY_FILL = new ResourceLocation(
            FloralRiteBloom.MOD_ID, "textures/gui/hud/decay_progress_fill.png");
    private static final net.minecraft.client.renderer.RenderType DECAY_FRAME_TYPE =
            FRBRenderTypes.emissiveGlow(DECAY_FRAME);
    private static final net.minecraft.client.renderer.RenderType DECAY_FILL_TYPE =
            FRBRenderTypes.emissiveGlow(DECAY_FILL);
    /** 读条条宽度（格）。 */
    private static final float BAR_W = 1.6F * 1.6F; // 1.6× 放大（用户调整：boss 头顶读条太小 → 1.6 倍）
    /** r73 L1：开场上举 / 转阶段抬手的臂部姿态（沿用 r68 老 FriliasModel）。 */
    private static final float RAISE_X = -2.45F;
    private static final float RAISE_Z_L = -0.55F;
    private static final float RAISE_Z_R = 0.55F;
    private static final float HEAD_X_RAISE = -0.25F;
    /** r73 L1：双手交叉蓄力姿态（读条激光 / 万花凋零咏唱期间）。 */
    private static final float CROSS_X = -1.30F;
    private static final float CROSS_Y_L = -0.55F;
    private static final float CROSS_Y_R = 0.55F;

    public FriliasRenderer(EntityRendererProvider.Context context) {
        super(context, new FriliasGeoModel());
        this.scaleHeight = 1.0F; // 用户新模型已是 BlockBench 1:1 比例，不再二次放大
        this.scaleWidth = 1.0F;
        // r73 L1：FriliasBackRingLayer 是 vanilla 模型的 RenderLayer，Geo 不接受；待骨架到位再恢复
    }

    /**
     * r73 L1：在 GeckoLib 渲染前，手动覆盖 arm_l / arm_r / head 的 rotation。
     * <p>
     * GeoBone.setRotX/Y/Z 直接写到模型姿势，优先级高于 AnimationController 的 channel 值，
     * 因此转阶段 / 开场 / 读条期间的硬姿势可以稳定生效。
     */
    @SubscribeEvent
    public static void onRenderEntityPre(GeoRenderEvent.Entity.Pre event) {
        Object entityObj = event.getEntity();
        if (!(entityObj instanceof FriliasEntity)) return;
        FriliasEntity entity = (FriliasEntity) entityObj;
        BakedGeoModel model = event.getModel();

        boolean raising = entity.inIntro() || entity.getTransitionTicks() > 0;
        boolean charging = entity.getBeamChargeTicks() > 0 || entity.getMilleFlorumCastTick() > 0
                || entity.getDecayAoeCharge() > 0;

        if (raising) {
            applyRaiseArms(model);
        } else if (charging) {
            applyCrossArms(model);
        } else if (entity.getFrbPhase() == 4) {
            // r90：天空阶段 boss 高悬玩家上方，需要低头看玩家
            GeoBone head = model.getBone("head").orElse(null);
            if (head != null) {
                head.setRotX(-entity.getXRot() * Mth.DEG_TO_RAD);
            }
        }
    }

    private static void applyRaiseArms(BakedGeoModel model) {
        GeoBone armL = model.getBone("arm_l").orElse(null);
        GeoBone armR = model.getBone("arm_r").orElse(null);
        GeoBone head = model.getBone("head").orElse(null);
        if (armL != null) {
            armL.setRotX(RAISE_X);
            armL.setRotY(0);
            armL.setRotZ(RAISE_Z_L);
        }
        if (armR != null) {
            armR.setRotX(RAISE_X);
            armR.setRotY(0);
            armR.setRotZ(RAISE_Z_R);
        }
        if (head != null) {
            head.setRotX(HEAD_X_RAISE);
            head.setRotY(0);
        }
    }

    private static void applyCrossArms(BakedGeoModel model) {
        GeoBone armL = model.getBone("arm_l").orElse(null);
        GeoBone armR = model.getBone("arm_r").orElse(null);
        if (armL != null) {
            armL.setRotX(CROSS_X);
            armL.setRotY(CROSS_Y_L); // 左臂向内旋到右肩前方
            armL.setRotZ(0);
        }
        if (armR != null) {
            armR.setRotX(CROSS_X);
            armR.setRotY(CROSS_Y_R); // 右臂向内旋到左肩前方
            armR.setRotZ(0);
        }
    }

    @Override
    public void render(FriliasEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
        int charge = entity.getBeamChargeTicks();
        int skillCast = entity.getSkillCastTicks();
        int decayAoe = entity.getDecayAoeCharge();
        // r90：技能读条或激光蓄力时画法阵+进度条；r95：枯萎 AOE 读条同一路径
        // r69（bug2）：死亡/濒死不再绘制读条与法阵（击败 boss 后立即消失）
        if ((charge <= 0 && skillCast <= 0 && decayAoe <= 0) || entity.isDeadOrDying()) return;
        // 0 → 1 展开进度（smoothstep 缓入缓出），满蓄力时法阵刚好完全展开
        float maxTicks;
        int curTicks;
        if (skillCast > 0) {
            maxTicks = Math.max(1, entity.getSkillCastMax());
            curTicks = skillCast;
        } else if (decayAoe > 0) {
            maxTicks = FriliasEntity.DECAY_AOE_CHARGE_TICKS;
            curTicks = decayAoe;
        } else {
            maxTicks = FriliasEntity.BEAM_CHARGE_TICKS;
            curTicks = charge;
        }
        float prog = 1.0F - curTicks / maxTicks;
        float smooth = prog * prog * (3.0F - 2.0F * prog);
        float scale = 3.5F * smooth;
        if (scale > 0.05F) {
            // r96：按当前阶段选法阵贴图（不再固定花之摇篮）
            VertexConsumer buf = bufferSource.getBuffer(sigilType(entity.getFrbPhase()));
            poseStack.pushPose();
            // 脚下贴地水平面，随时间缓慢旋转；r69 传 FULL_BRIGHT 让法阵黑暗中也发亮
            poseStack.translate(0.0D, 0.10D, 0.0D);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                    (entity.tickCount + partialTick) * 2.0F));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
            QuadBills.quad(poseStack, buf, scale, scale, -scale / 2.0F,
                    1.0F, 1.0F, 1.0F, 0.75F, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        drawChargeBar(entity, prog, poseStack, bufferSource);
    }

    /** r69：boss 头顶读条条（面向相机的 billboard，进度从左往右铺满内填充）。
     *  r70：按阶段选贴图——霜华（phase 2）用 frost_progress_*、时间（phase 7）用 time_progress_*，
     *  其余用 floral_progress_*。 */
    private void drawChargeBar(FriliasEntity entity, float prog,
                               PoseStack poseStack, MultiBufferSource buffers) {
        int phase = entity.getFrbPhase();
        net.minecraft.client.renderer.RenderType frameType;
        net.minecraft.client.renderer.RenderType fillType;
        switch (phase) {
            case 1 -> { frameType = BURN_FRAME_TYPE; fillType = BURN_FILL_TYPE; }
            case 2 -> { frameType = FROST_FRAME_TYPE; fillType = FROST_FILL_TYPE; }
            case 3 -> { frameType = RAIN_FRAME_TYPE; fillType = RAIN_FILL_TYPE; }
            case 4 -> { frameType = ASTRAL_FRAME_TYPE; fillType = ASTRAL_FILL_TYPE; }
            case 5 -> { frameType = VOID_FRAME_TYPE; fillType = VOID_FILL_TYPE; }
            case 6 -> { frameType = DECAY_FRAME_TYPE; fillType = DECAY_FILL_TYPE; }
            case 7 -> { frameType = TIME_FRAME_TYPE; fillType = TIME_FILL_TYPE; }
            default -> { frameType = FRAME_TYPE; fillType = FILL_TYPE; }
        }
        float barH = BAR_W * 38.0F / 328.0F; // 保持贴图纵横比
        float fill = Mth.clamp(prog, 0.0F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.55D, 0.0D);
        // billboard：面向相机；再镜像 X 使贴图左缘出现在观察者左侧（原版名牌同款约定）
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-1.0F, 1.0F, 1.0F);
        VertexConsumer frame = buffers.getBuffer(frameType);
        QuadBills.quadUv(poseStack, frame, BAR_W, barH, 0.0F, 0.0F, 1.0F, 1.0F,
                1.0F, 1.0F, 1.0F, 0.95F, LightTexture.FULL_BRIGHT);
        if (fill > 0.02F) {
            // 内填充区（贴图内 36..292 / 328 x 11..27 / 38）
            float inX0 = 36.0F / 328.0F, inX1 = 292.0F / 328.0F;
            float inY0 = 11.0F / 38.0F, inY1 = 27.0F / 38.0F;
            float innerW = (inX1 - inX0) * BAR_W * fill;
            float innerH = (inY1 - inY0) * barH;
            poseStack.pushPose();
            // 左缘对齐条内区左侧，向右生长；z 微移靠相机防 z-fighting
            poseStack.translate(-BAR_W / 2.0F + (inX0 * BAR_W) + innerW / 2.0F, inY0 * barH, 0.01F);
            VertexConsumer fillBuf = buffers.getBuffer(fillType);
            QuadBills.quadUv(poseStack, fillBuf, innerW, innerH,
                    inX0, inY0, inX0 + (inX1 - inX0) * fill, inY1,
                    1.0F, 1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}