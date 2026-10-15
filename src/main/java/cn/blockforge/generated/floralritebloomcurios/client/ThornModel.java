package cn.blockforge.generated.floralritebloomcurios.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** 由 design/frilias/thorn_model.py 生成，与预览共用几何和贴图。 */
public final class ThornModel {
    public static ModelPart create() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("stem_base", CubeListBuilder.create().texOffs(0, 0).addBox(-2.000000F, -11.000000F, -2.000000F, 4.000000F, 11.000000F, 4.000000F), PartPose.offsetAndRotation(0.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        mesh.getRoot().addOrReplaceChild("stem_middle", CubeListBuilder.create().texOffs(0, 0).addBox(-1.500000F, -22.000000F, -1.500000F, 3.000000F, 12.000000F, 3.000000F), PartPose.offsetAndRotation(0.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        mesh.getRoot().addOrReplaceChild("stem_upper", CubeListBuilder.create().texOffs(0, 0).addBox(-1.000000F, -30.000000F, -1.000000F, 2.000000F, 9.000000F, 2.000000F), PartPose.offsetAndRotation(0.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        mesh.getRoot().addOrReplaceChild("stem_tip", CubeListBuilder.create().texOffs(0, 0).addBox(-0.500000F, -7.000000F, -0.500000F, 1.000000F, 7.000000F, 1.000000F), PartPose.offsetAndRotation(0.000000F, -29.000000F, 0.000000F, 0.000000F, 0.000000F, 0.174533F));
        mesh.getRoot().addOrReplaceChild("spur_0", CubeListBuilder.create().texOffs(0, 0).addBox(-1.000000F, -5.000000F, -1.000000F, 2.000000F, 5.000000F, 2.000000F), PartPose.offsetAndRotation(0.000000F, -7.000000F, 0.000000F, -0.000000F, 0.000000F, 1.012291F));
        mesh.getRoot().addOrReplaceChild("spur_tip_0", CubeListBuilder.create().texOffs(0, 0).addBox(-0.500000F, -9.000000F, -0.500000F, 1.000000F, 4.000000F, 1.000000F), PartPose.offsetAndRotation(0.000000F, -7.000000F, 0.000000F, -0.000000F, 0.000000F, 1.012291F));
        mesh.getRoot().addOrReplaceChild("spur_1", CubeListBuilder.create().texOffs(0, 0).addBox(-1.000000F, -5.000000F, -1.000000F, 2.000000F, 5.000000F, 2.000000F), PartPose.offsetAndRotation(0.000000F, -12.000000F, 0.000000F, -0.580626F, 0.000000F, -0.829220F));
        mesh.getRoot().addOrReplaceChild("spur_tip_1", CubeListBuilder.create().texOffs(0, 0).addBox(-0.500000F, -9.000000F, -0.500000F, 1.000000F, 4.000000F, 1.000000F), PartPose.offsetAndRotation(0.000000F, -12.000000F, 0.000000F, -0.580626F, 0.000000F, -0.829220F));
        mesh.getRoot().addOrReplaceChild("spur_2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.000000F, -5.000000F, -1.000000F, 2.000000F, 5.000000F, 2.000000F), PartPose.offsetAndRotation(0.000000F, -18.000000F, 0.000000F, 0.977798F, 0.000000F, -0.262000F));
        mesh.getRoot().addOrReplaceChild("spur_tip_2", CubeListBuilder.create().texOffs(0, 0).addBox(-0.500000F, -9.000000F, -0.500000F, 1.000000F, 4.000000F, 1.000000F), PartPose.offsetAndRotation(0.000000F, -18.000000F, 0.000000F, 0.977798F, 0.000000F, -0.262000F));
        mesh.getRoot().addOrReplaceChild("spur_3", CubeListBuilder.create().texOffs(0, 0).addBox(-1.000000F, -5.000000F, -1.000000F, 2.000000F, 5.000000F, 2.000000F), PartPose.offsetAndRotation(0.000000F, -24.000000F, 0.000000F, -0.917447F, 0.000000F, 0.427813F));
        mesh.getRoot().addOrReplaceChild("spur_tip_3", CubeListBuilder.create().texOffs(0, 0).addBox(-0.500000F, -9.000000F, -0.500000F, 1.000000F, 4.000000F, 1.000000F), PartPose.offsetAndRotation(0.000000F, -24.000000F, 0.000000F, -0.917447F, 0.000000F, 0.427813F));
        return mesh.getRoot().bake(32, 32);
    }
    private ThornModel() {}
}
