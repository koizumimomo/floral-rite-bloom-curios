package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FlowerCradleSigilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * 花之摇篮法阵：r71 简化——只渲染一张固定贴地不动的面片（不再旋转、不再脉冲、不再叠描边）。
 * r92：参考 FF14 数据包 memory_torrent_song 的 color.mcfunction（塔身dust包边
 *      + 有人/无人变色），为法阵添加粒子包边——沿半径 2 圆周、离地 1.5 格高
 *      均匀撒 12 颗 dust 粒子。贴图负责"机制存在感"，粒子包边负责"判定范围"。
 */
public class FlowerCradleSigilRenderer extends EntityRenderer<FlowerCradleSigilEntity> {

    private static final ResourceLocation TEX = new ResourceLocation(FloralRiteBloom.MOD_ID,
            "textures/entity/flower_cradle_sigil.png");
    private static final RenderType GLOW_TYPE = FRBRenderTypes.emissiveGlow(TEX);

    /** 包边粒子数（对应 FF14 一座塔 5 行 dust 的视觉密度，但因只有 1 层所以总数更少）。 */
    private static final int RING_PARTICLES = 12;
    /** 包边高度：离地 1.5 格。 */
    private static final double RING_HEIGHT = 1.5D;
    /** 包边半径：与 sigil 判定半径一致（2.0 格）。 */
    private static final double RING_RADIUS = 2.0D;
    /** 自然阶段主题色（绿）——与 FriliasEntity.PHASE_COLORS[0] 一致。 */
    private static final Vector3f RING_COLOR = new Vector3f(0.49F, 0.90F, 0.55F);

    public FlowerCradleSigilRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(FlowerCradleSigilEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        VertexConsumer buf = buffers.getBuffer(GLOW_TYPE);
        pose.pushPose();
        // 固定 0.1 格抬高避免 z-fighting；不旋转；不脉冲；尺寸固定 4 格。
        pose.translate(0.0D, 0.1D, 0.0D);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
        QuadBills.quad(pose, buf, 4.0F, 4.0F, -2.0F, 1.0F, 1.0F, 1.0F, 0.92F, light);
        pose.popPose();

        // r92：粒子包边——参考 FF14 踩塔的 dust 圆环，让玩家清楚看到判定边界。
        // 客户端本地粒子，不占网络带宽；法阵存活期间持续刷新。
        spawnRingParticles(entity);
    }

    /** 沿法阵圆周在离地 1.5 格处均匀生成 RING_PARTICLES 颗 dust 粒子。 */
    private static void spawnRingParticles(FlowerCradleSigilEntity entity) {
        Level level = entity.level();
        if (!level.isClientSide) return;
        DustParticleOptions dust = new DustParticleOptions(RING_COLOR, 1.2F);
        double cx = entity.getX();
        double cy = entity.getY() + RING_HEIGHT;
        double cz = entity.getZ();
        for (int i = 0; i < RING_PARTICLES; i++) {
            double angle = (Math.PI * 2.0D) * i / RING_PARTICLES;
            double x = cx + Math.cos(angle) * RING_RADIUS;
            double z = cz + Math.sin(angle) * RING_RADIUS;
            level.addParticle(dust, x, cy, z, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(FlowerCradleSigilEntity entity) {
        return TEX;
    }
}