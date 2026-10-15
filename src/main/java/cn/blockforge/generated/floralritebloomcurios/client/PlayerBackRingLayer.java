package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

/** 在佩戴万花万草之灵时，于玩家背后显示双层花环（r91 起改走 GeckoLib geo 模型）。
 *  像素/高清两套模型+贴图由 {@link PlayerHaloRenderer} 按 FRBConfig.haloStyle() 实时切换。
 *  boss 自身光环仍走 FriliasBackRingLayer 的独立路径，不受配置影响。 */
public final class PlayerBackRingLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** geo 根骨骼 pivot≈(0, 23, 4)px，与前环共用同一套抵消值。 */
    private static final double PIVOT_Y = 23.0D / 16.0D;
    private static final double PIVOT_Z = 4.0D / 16.0D;
    /** 相对后背锚点：上 6px（肩背高度，环心落在肩胸之间）、后 3px（贴着后背）。 */
    private static final double UP = 6.0D / 16.0D;
    private static final double BACK = 3.0D / 16.0D;

    public PlayerBackRingLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !FRBState.hasUltimate(player)) return;

        float time = player.tickCount + partialTick;
        float breathe = Mth.sin(time * 0.055F) * 0.018F;

        poseStack.pushPose();
        // r96：后环跟随身体（不跟随头部视角），用 body 骨骼定位
        getParentModel().body.translateAndRotate(poseStack);
        // 抵消 LivingEntityRenderer 的 (-1,-1,1) 翻转，回到 GeckoLib 世界系
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // 抵消 geo 模型 pivot (0,+23,+4)/16，将环中心放到后背肩背高度
        poseStack.translate(0.0D, -PIVOT_Y + UP + breathe, -PIVOT_Z + BACK);
        PlayerHaloRenderer.INSTANCE.renderHalo(poseStack, buffers, time);
        poseStack.popPose();
    }
}
