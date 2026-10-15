package cn.blockforge.generated.floralritebloomcurios.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * r86：羽剑实体模型（Java ModelPart 重建）。
 *
 * 参考 koizumi_momo 导出的 feather_sword.json 几何：
 *  - 主刃：12 格长薄片，沿 +Z 方向（yaw=0 时朝前方）
 *  - 剑尖装饰：在主刃 +Z 端
 *  - 剑格（上下两片 ±45° 倾斜）：靠近剑柄
 *  - 剑柄：主刃 -Z 端短段
 *
 * 模型以几何中心为原点（长度方向居中），贴图 16×16，
 * 各方块 texOffs 取 JSON 中对应面的像素区域（按面取色，不追求像素级还原）。
 * 渲染器会按冲锋范围整体放大（剑偏小→放大 2~3 倍）。
 */
public final class FeatherSwordModel {
    /** 贴图尺寸（feather_sword.png 为 16×16）。 */
    public static final int TEX_W = 16;
    public static final int TEX_H = 16;

    public static ModelPart create() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        // 主刃：12 格长，居中后从 z=-6 到 z=6；薄片宽 0.3 厚 1.0
        root.addOrReplaceChild("blade",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.15F, -0.5F, -6.0F, 0.3F, 1.0F, 12.0F),
                PartPose.ZERO);
        // 剑尖装饰：主刃 +Z 端（z=6 → 9），略宽
        root.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(2, 2)
                        .addBox(-0.2F, -0.4F, 6.0F, 0.4F, 0.8F, 3.0F),
                PartPose.ZERO);
        // 剑格上片：z=4~6 处，绕 X 轴 +45°
        root.addOrReplaceChild("guard_up",
                CubeListBuilder.create().texOffs(6, 0)
                        .addBox(-0.25F, 0.0F, 4.0F, 0.5F, 0.7F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.35F, 5.0F,
                        (float) Math.toRadians(45.0F), 0.0F, 0.0F));
        // 剑格下片：z=4~6 处，绕 X 轴 -45°
        root.addOrReplaceChild("guard_down",
                CubeListBuilder.create().texOffs(6, 0)
                        .addBox(-0.25F, 0.0F, 4.0F, 0.5F, 0.7F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -0.35F, 5.0F,
                        (float) Math.toRadians(-45.0F), 0.0F, 0.0F));
        // 剑柄：主刃 -Z 端（z=-7 → -6）
        root.addOrReplaceChild("hilt",
                CubeListBuilder.create().texOffs(12, 0)
                        .addBox(-0.15F, -0.3F, -7.0F, 0.3F, 0.6F, 1.0F),
                PartPose.ZERO);
        return mesh.getRoot().bake(TEX_W, TEX_H);
    }

    private FeatherSwordModel() {}
}
