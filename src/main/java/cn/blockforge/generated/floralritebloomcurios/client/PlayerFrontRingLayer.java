package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.config.FRBConfig;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

/**
 * r78：神环前面渲染版（与 PlayerBackRingLayer 互补）。
 * 玩家站在自己化身前面时，第一人称视角看不到自己背后光环，所以再加一层胸前光环。
 * 默认开启（FRBConfig.showFrontRing）。
 * <p>
 * r91：与后环一样改走 GeckoLib geo 模型（{@link PlayerHaloRenderer}）。
 * emissiveGlow 已 NO_CULL，不再需要 r90 的 scale(1,1,-1) 镜像绕序 hack。
 */
public final class PlayerFrontRingLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** geo 根骨骼 pivot≈(0, 23, 4)px，与后环共用同一套抵消值。 */
    private static final double PIVOT_Y = 23.0D / 16.0D;
    private static final double PIVOT_Z = 4.0D / 16.0D;
    /** 相对胸前锚点：上 2px、前 3px（沿用 r90 手绘前环位置）。 */
    private static final double UP = 2.0D / 16.0D;
    private static final double FRONT = 3.0D / 16.0D;

    public PlayerFrontRingLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !FRBState.hasUltimate(player)) return;
        if (!cn.blockforge.generated.floralritebloomcurios.config.FRBConfig.showFrontRing()) return;

        float t = player.tickCount + partialTick;
        float breathe = Mth.sin(t * 0.055F) * 0.018F;

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        // 抵消 LivingEntityRenderer 的 (-1,-1,1) 翻转，回到 GeckoLib 世界系。
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // ModelPart 局部系（含实体 yaw）：+Y 经抵消后朝上、-Z 朝胸前。
        // geo prep 自带 (0,+23,+4)/16 pivot 偏移，预偏移 = 目标(上 2、前 3) - pivot。
        poseStack.translate(0.0D, -PIVOT_Y + UP + breathe, -PIVOT_Z - FRONT);
        PlayerHaloRenderer.INSTANCE.renderHalo(poseStack, buffers, t);
        poseStack.popPose();
    }
}
