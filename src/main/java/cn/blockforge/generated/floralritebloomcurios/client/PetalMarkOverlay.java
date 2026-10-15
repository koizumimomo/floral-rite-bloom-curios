package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 焚生藤【花瓣印记】：画在被标记生物建模的正中心。
 *
 * 做法参考 Battlefield 标记模组：不在世界空间里靠“实体坐标”定位（那样会受到相机
 * 平移与包围盒高度的双重误差），而是挂到 RenderLivingEvent.Post —— 这一刻的
 * PoseStack 正是该生物自己的模型空间（原点=渲染帧插值后的脚底，坐标已相对相机），
 * 于是标记天然跟着生物的移动、缩放与出生/骑乘位移，永远落在建模中心。
 * 四瓣花按层数点亮，关闭深度测试让它浮在模型中心可见。
 */
@Mod.EventBusSubscriber(modid = FloralRiteBloom.MOD_ID, value = Dist.CLIENT)
public final class PetalMarkOverlay {

    private static final ResourceLocation PETAL_MARK =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/petal_mark.png");
    private static final int FULL_LIGHT = 0xF000F0;
    /** 象限 UV：左上 / 右上 / 左下 / 右下，逐瓣明暗。 */
    private static final float[][] UVS = {
            {0.0F, 0.0F, 0.5F, 0.5F},
            {0.5F, 0.0F, 1.0F, 0.5F},
            {0.0F, 0.5F, 0.5F, 1.0F},
            {0.5F, 0.5F, 1.0F, 1.0F}
    };

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post event) {
        LivingEntity entity = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || entity == mc.getCameraEntity()) return;
        MobEffectInstance mark = entity.getEffect(FRBEffects.PETAL_MARK.get());
        if (mark == null) return;

        int stacks = Mth.clamp(mark.getAmplifier() + 1, 1, 4);
        float pt = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        VertexConsumer buffer = event.getMultiBufferSource()
                .getBuffer(RenderType.entityTranslucent(PETAL_MARK));

        // 建模正中心：包围盒垂直中点（脚底为原点）
        float centerY = entity.getBbHeight() * 0.5F;
        // 随体型缩放：小鸡不会顶着一朵比身体还大的花，Boss 的印记也不会小到看不见
        float size = Mth.clamp(entity.getBbWidth() * 1.35F, 0.34F, 1.45F)
                * (0.92F + 0.08F * stacks);
        // 呼吸感：满层时微微搏动
        float breath = 1.0F + 0.06F * Mth.sin((entity.tickCount + pt) * 0.16F);

        Camera camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) return;

        pose.pushPose();
        pose.translate(0.0D, centerY, 0.0D);
        pose.mulPose(camera.rotation());
        pose.scale(size * breath, size * breath, 1.0F);
        // 印记在模型内部，关掉深度测试它才浮得出来（关闭前后成对恢复）
        RenderSystem.disableDepthTest();
        drawFlower(pose, buffer, stacks);
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    /** 四片花瓣：前 stacks 片点亮成橙白发光，其余灰暗半透明。整朵花的几何中心就在原点。 */
    private static void drawFlower(PoseStack pose, VertexConsumer buffer, int stacks) {
        float petal = 0.3F;
        float off = petal / 2.0F;
        // 四个象限的花瓣中心：左上 / 右上 / 左下 / 右下
        float[][] offs = {{-off, off}, {off, off}, {-off, -off}, {off, -off}};
        for (int i = 0; i < 4; i++) {
            boolean lit = i < stacks;
            float[] uv = UVS[i];
            pose.pushPose();
            // quadUv 从局部 y=0 向上画，再下移半个花瓣让象限中心真正对齐原点
            pose.translate(offs[i][0], offs[i][1] - off, 0.0F);
            QuadBills.quadUv(pose, buffer, petal, petal, uv[0], uv[1], uv[2], uv[3],
                    lit ? 1.0F : 0.22F, lit ? 1.0F : 0.22F, lit ? 1.0F : 0.26F,
                    lit ? 0.95F : 0.5F, FULL_LIGHT);
            pose.popPose();
        }
    }

    private PetalMarkOverlay() {}
}
