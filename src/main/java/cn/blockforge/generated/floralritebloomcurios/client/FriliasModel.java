package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * 芙莉艾丝：上半身为玩家比例的花妖人形（分层颅骨、立体眉眼与瞳孔、下巴和小嘴、
 * 五缕刘海、双层侧发、鬓发垂发、叠瓣花冠、露肩、白瓣抹胸、绿叶束腰与 V 形腰尖）。
 * 下半身是一朵巨型花：中央金黄花蕊、八片内层白瓣、八片三段外翻大花瓣、
 * 八片鳞片状萼片与四层收尖花托，周身环绕 4 枚菱形花朵符文。
 *
 * 几何来自 design/frilias/frilias.bbmodel，由 import_model.py 转换为 ModelPart；
 * 编辑模型与游戏共用 128×128 的 frilias.png。含花冠总高约 2.4 格，最大外展约 24.5 单位。
 */
public class FriliasModel extends EntityModel<FriliasEntity> {
    private final ModelPart root;
    final ModelPart head;
    private final ModelPart crown;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart flower;
    private final ModelPart runes;
    private final ModelPart hairTailL;
    private final ModelPart hairTailR;

    public FriliasModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.crown = head.getChild("crown");
        this.leftArm = body.getChild("arm_l");
        this.rightArm = body.getChild("arm_r");
        this.flower = body.getChild("flower");
        this.runes = root.getChild("runes");
        this.hairTailL = head.getChild("hair_tail_l");
        this.hairTailR = head.getChild("hair_tail_r");
    }

    public static ModelPart createRoot() {
        MeshDefinition mesh = new MeshDefinition();
        // ---- FriliasModel.createRoot() 几何（由 import_model.py 从用户 bbmodel 生成） ----
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(18, 35).addBox(-1F, -12F, -1F, 2F, 3F, 2F)
                        .texOffs(27, 35).addBox(-3F, -11F, -2F, 6F, 4F, 4F)
                        .texOffs(48, 35).addBox(-4F, -11F, -1F, 2F, 4F, 2F)
                        .texOffs(48, 35).addBox(2F, -11F, -1F, 2F, 4F, 2F)
                        .texOffs(57, 35).addBox(-2F, -9F, -3F, 4F, 5F, 1F)
                        .texOffs(75, 35).addBox(-3F, -9F, 1F, 6F, 4F, 1F)
                        .texOffs(97, 35).addBox(-3F, -7F, -2F, 6F, 5F, 4F)
                        .texOffs(0, 45).addBox(-2F, -3F, -1F, 4F, 3F, 2F)
                        .texOffs(13, 45).addBox(-3F, -2F, -2F, 6F, 1F, 4F), PartPose.offset(0F, 8F, 0F));
        body.addOrReplaceChild("bodice_r_0", CubeListBuilder.create()
                        .texOffs(68, 35).addBox(-1F, 0F, -1F, 2F, 5F, 1F), PartPose.offsetAndRotation(-3F, -9F, -2F, 0F, 0F, -0.29671F));
        body.addOrReplaceChild("bodice_l_1", CubeListBuilder.create()
                        .texOffs(68, 35).addBox(-1F, 0F, -1F, 2F, 5F, 1F), PartPose.offsetAndRotation(3F, -9F, -2F, 0F, 0F, 0.29671F));
        body.addOrReplaceChild("heart_gem_2", CubeListBuilder.create()
                        .texOffs(90, 35).addBox(-1F, -1F, -1F, 2F, 2F, 1F), PartPose.offsetAndRotation(0F, -7F, -3F, 0F, 0F, -0.7854F));
        body.addOrReplaceChild("vleaf_c_3", CubeListBuilder.create()
                        .texOffs(34, 45).addBox(-1F, 0F, -1F, 2F, 3F, 2F), PartPose.offsetAndRotation(0F, -5F, -1F, -0.17453F, 0F, 0F));
        body.addOrReplaceChild("vleaf_r_4", CubeListBuilder.create()
                        .texOffs(43, 45).addBox(-1F, 0F, -1F, 2F, 3F, 2F), PartPose.offsetAndRotation(-2F, -5F, -1F, -0.13963F, 0F, -0.34907F));
        body.addOrReplaceChild("vleaf_l_5", CubeListBuilder.create()
                        .texOffs(43, 45).addBox(-1F, 0F, -1F, 2F, 3F, 2F), PartPose.offsetAndRotation(2F, -5F, -1F, -0.13963F, 0F, 0.34907F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, -5F, -3F, 8F, 5F, 6F)
                        .texOffs(29, 0).addBox(-4F, -7F, -4F, 8F, 2F, 8F)
                        .texOffs(62, 0).addBox(-3F, -9F, -3F, 6F, 2F, 6F)
                        .texOffs(87, 0).addBox(-2F, -10F, -2F, 4F, 1F, 4F)
                        .texOffs(117, 106).addBox(1.375F, -3.85F, -3.7F, 2.3F, 1F, 1F)
                        .texOffs(123, 100).addBox(3.35F, -3.575F, -3.95F, 1.1F, 1F, 1F)
                        .texOffs(122, 107).addBox(-3.675F, -3.85F, -3.7F, 2.3F, 1F, 1F)
                        .texOffs(122, 94).addBox(-4.45F, -3.575F, -3.875F, 1.1F, 1F, 1F)
                        .texOffs(50, 112).addBox(1.5F, -3.05F, -3.525F, 2F, 2F, 1F)
                        .texOffs(109, 123).addBox(1.625F, -3.05F, -3.575F, 1.4F, 2F, 1F)
                        .texOffs(50, 112).addBox(1.55F, -2.7F, -3.6F, 0.4F, 0.3F, 1F)
                        .texOffs(113, 124).addBox(2.025F, -2.55F, -3.625F, 0.6F, 1F, 1F)
                        .texOffs(50, 112).addBox(-3.5F, -3.05F, -3.525F, 2F, 2F, 1F)
                        .texOffs(51, 112).addBox(-3.075F, -2.7F, -3.6F, 0.4F, 0.3F, 1F)
                        .texOffs(112, 120).addBox(-3.025F, -3.05F, -3.575F, 1.4F, 2F, 1F)
                        .texOffs(113, 124).addBox(-2.625F, -2.55F, -3.625F, 0.6F, 1F, 1F)
                        .texOffs(104, 71).addBox(-0.5F, -0.29F, -3.5F, 1F, 1F, 7F)
                        .texOffs(106, 117).addBox(-0.05828F, -0.40533F, -3.52179F, 0.11034F, 0.11034F, 0.77236F)
                        .texOffs(1, 87).addBox(-3.5F, -7.15F, -3.5F, 7F, 7F, 7F)
                        .texOffs(47, 12).addBox(-6F, 0.6F, -5F, 1F, 3F, 1F)
                        .texOffs(52, 12).addBox(-6F, 3.6F, -5F, 1F, 2F, 1F)
                        .texOffs(47, 12).addBox(5F, 0.6F, -5F, 1F, 3F, 1F)
                        .texOffs(52, 12).addBox(5F, 3.6F, -5F, 1F, 2F, 1F)
                        .texOffs(57, 12).addBox(-4F, -8F, 4F, 8F, 8F, 1F)
                        .texOffs(76, 12).addBox(-3F, 0F, 4F, 6F, 2F, 1F), PartPose.offset(0F, -11F, 0F));
        head.addOrReplaceChild("xiaba_0", CubeListBuilder.create()
                        .texOffs(103, 71).addBox(-4F, -0.5F, -3.5F, 3.1F, 1F, 7F), PartPose.offsetAndRotation(0.5F, 0.45F, 0F, 0F, 0F, 0.24784F));
        head.addOrReplaceChild("xiaba_1", CubeListBuilder.create()
                        .texOffs(104, 71).mirror().addBox(0.9F, -0.5F, -3.5F, 3.1F, 1F, 7F), PartPose.offsetAndRotation(-0.5F, 0.45F, 0F, 0F, 0F, -0.24784F));
        head.addOrReplaceChild("zuiba_2", CubeListBuilder.create()
                        .texOffs(105, 118).addBox(-0.44135F, -0.02759F, -0.39722F, 0.34205F, 0.11034F, 0.77236F), PartPose.offsetAndRotation(0.05206F, -0.35567F, -3.12457F, 0F, 0F, 0.24784F));
        head.addOrReplaceChild("zuiba_3", CubeListBuilder.create()
                        .texOffs(106, 118).addBox(0.0993F, -0.02759F, -0.39722F, 0.34205F, 0.11034F, 0.77236F), PartPose.offsetAndRotation(-0.05828F, -0.35567F, -3.12457F, 0F, 0F, -0.24784F));
        head.addOrReplaceChild("fringe_top_4", CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-4F, 0F, -1F, 8F, 1F, 1F), PartPose.offsetAndRotation(0F, -8F, -4F, 0.13963F, 0F, 0F));
        head.addOrReplaceChild("fringe_0_5", CubeListBuilder.create()
                        .texOffs(19, 12).addBox(-0.5F, 0F, -1F, 1F, 5F, 1F), PartPose.offsetAndRotation(-3.5F, -8F, -4F, 0.20944F, 0F, 0.15708F));
        head.addOrReplaceChild("fringe_1_6", CubeListBuilder.create()
                        .texOffs(24, 12).addBox(-1F, 0F, -1F, 2F, 4F, 1F), PartPose.offsetAndRotation(-2F, -8F, -4F, 0.12217F, 0F, 0.06981F));
        head.addOrReplaceChild("fringe_2_7", CubeListBuilder.create()
                        .texOffs(31, 12).addBox(-1F, 0F, -1F, 2F, 3F, 1F), PartPose.offsetAndRotation(0F, -8F, -4F, 0.20944F, 0F, 0F));
        head.addOrReplaceChild("fringe_3_8", CubeListBuilder.create()
                        .texOffs(24, 12).addBox(-1F, 0F, -1F, 2F, 4F, 1F), PartPose.offsetAndRotation(2F, -8F, -4F, 0.12217F, 0F, -0.06981F));
        head.addOrReplaceChild("fringe_4_9", CubeListBuilder.create()
                        .texOffs(19, 12).addBox(-0.5F, 0F, -1F, 1F, 5F, 1F), PartPose.offsetAndRotation(3.5F, -8F, -4F, 0.20944F, 0F, -0.15708F));
        head.addOrReplaceChild("face_lock_r_10", CubeListBuilder.create()
                        .texOffs(38, 12).addBox(-6F, -31.2F, -5F, 2F, 5.2F, 2F), PartPose.offsetAndRotation(-1.9F, 27F, 0F, 0F, 0F, 0.06981F));
        head.addOrReplaceChild("face_lock_l_11", CubeListBuilder.create()
                        .texOffs(38, 12).addBox(4F, -31.2F, -5F, 2F, 5.2F, 2F), PartPose.offsetAndRotation(1.9F, 27F, 0F, 0F, 0F, -0.06981F));
        head.addOrReplaceChild("face_lock_r2_12", CubeListBuilder.create()
                        .texOffs(38, 12).addBox(-5.4F, -29F, -4.5F, 1.4F, 3F, 1.4F), PartPose.offsetAndRotation(-1.7F, 22.5F, 0F, 0F, 0F, 0.06981F));
        head.addOrReplaceChild("face_lock_l2_13", CubeListBuilder.create()
                        .texOffs(38, 12).addBox(4F, -29F, -4.5F, 1.3F, 3F, 1.4F), PartPose.offsetAndRotation(2.2F, 22.2F, 0F, 0F, 0F, -0.08727F));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                        .texOffs(0, 26).addBox(-4F, 0F, -4F, 8F, 1F, 7F)
                        .texOffs(31, 26).addBox(-3F, -1F, -1F, 6F, 1F, 2F)
                        .texOffs(65, 26).addBox(-2F, -2F, -2F, 4F, 1F, 4F)
                        .texOffs(82, 26).addBox(-1F, -3F, -1F, 2F, 1F, 2F)
                        .texOffs(91, 26).addBox(-5F, -1F, -1F, 2F, 1F, 2F)
                        .texOffs(100, 26).addBox(-5F, -2F, -1F, 2F, 1F, 2F)
                        .texOffs(0, 35).addBox(-3F, -1F, 2F, 2F, 1F, 2F)
                        .texOffs(91, 26).addBox(3F, -1F, -1F, 2F, 1F, 2F)
                        .texOffs(100, 26).addBox(3F, -2F, -1F, 2F, 1F, 2F)
                        .texOffs(0, 35).addBox(1F, -1F, 2F, 2F, 1F, 2F), PartPose.offset(0F, -8F, 0F));
        crown.addOrReplaceChild("cf_lo45_0", CubeListBuilder.create()
                        .texOffs(48, 26).addBox(-2F, -1F, -2F, 4F, 1F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0.7854F, 0F));
        crown.addOrReplaceChild("leaf_out_r_1", CubeListBuilder.create()
                        .texOffs(109, 26).addBox(-3F, -1F, -1F, 3F, 1F, 2F), PartPose.offsetAndRotation(-4F, 1F, 0F, 0F, 0F, 0.41888F));
        crown.addOrReplaceChild("leaf_up_r_2", CubeListBuilder.create()
                        .texOffs(120, 26).addBox(-1F, 0F, -2F, 1F, 1F, 2F), PartPose.offsetAndRotation(-4F, 0F, -2F, 0.24435F, 0F, 0.5236F));
        crown.addOrReplaceChild("leaf_out_l_3", CubeListBuilder.create()
                        .texOffs(109, 26).addBox(0F, -1F, -1F, 3F, 1F, 2F), PartPose.offsetAndRotation(4F, 1F, 0F, 0F, 0F, -0.41888F));
        crown.addOrReplaceChild("leaf_up_l_4", CubeListBuilder.create()
                        .texOffs(120, 26).addBox(-1F, 0F, -2F, 1F, 1F, 2F), PartPose.offsetAndRotation(4F, 0F, -2F, 0.24435F, 0F, -0.5236F));
        crown.addOrReplaceChild("leaf_back_-1_5", CubeListBuilder.create()
                        .texOffs(9, 35).addBox(-1F, -1F, 0F, 2F, 1F, 2F), PartPose.offsetAndRotation(-1F, 1F, 3F, -0.41888F, 0F, 0.24435F));
        crown.addOrReplaceChild("leaf_back_1_6", CubeListBuilder.create()
                        .texOffs(9, 35).addBox(-1F, -1F, 0F, 2F, 1F, 2F), PartPose.offsetAndRotation(1F, 1F, 3F, -0.41888F, 0F, -0.24435F));
        PartDefinition hair_tail_l = head.addOrReplaceChild("hair_tail_l", CubeListBuilder.create()
                        .texOffs(91, 12).addBox(-1F, -1F, 0F, 2F, 11F, 2F)
                        .texOffs(100, 12).addBox(-1F, 10F, 0F, 1F, 4F, 1F), PartPose.offsetAndRotation(3F, -4F, 4F, 0.13963F, 0F, -0.10472F));
        PartDefinition hair_tail_r = head.addOrReplaceChild("hair_tail_r", CubeListBuilder.create()
                        .texOffs(91, 12).addBox(-1F, -1F, 0F, 2F, 11F, 2F)
                        .texOffs(100, 12).addBox(-1F, 10F, 0F, 1F, 4F, 1F), PartPose.offsetAndRotation(-3F, -4F, 4F, 0.13963F, 0F, 0.10472F));
        PartDefinition leftArm = body.addOrReplaceChild("arm_l", CubeListBuilder.create()
                        .texOffs(52, 45).addBox(0F, 0F, -1F, 2F, 5F, 2F)
                        .texOffs(61, 45).addBox(0F, 4F, -2F, 2F, 2F, 4F)
                        .texOffs(74, 45).addBox(0F, 5F, -1F, 2F, 4F, 2F)
                        .texOffs(83, 45).addBox(0F, 8F, -1F, 2F, 3F, 2F), PartPose.offsetAndRotation(3F, -10F, 0F, 0F, 0F, -0.24435F));
        PartDefinition rightArm = body.addOrReplaceChild("arm_r", CubeListBuilder.create()
                        .texOffs(52, 45).addBox(-2F, 0F, -1F, 2F, 5F, 2F)
                        .texOffs(61, 45).addBox(-2F, 4F, -2F, 2F, 2F, 4F)
                        .texOffs(74, 45).addBox(-2F, 5F, -1F, 2F, 4F, 2F)
                        .texOffs(83, 45).addBox(-2F, 8F, -1F, 2F, 3F, 2F), PartPose.offsetAndRotation(-3F, -10F, 0F, 0F, 0F, 0.24435F));
        PartDefinition flower = body.addOrReplaceChild("flower", CubeListBuilder.create()
                        .texOffs(92, 45).addBox(-3F, -8F, -3F, 6F, 4F, 6F)
                        .texOffs(117, 45).addBox(3F, -9F, 0F, 1F, 1F, 1F)
                        .texOffs(117, 45).addBox(2F, -9F, 2F, 1F, 1F, 1F)
                        .texOffs(117, 45).addBox(0F, -9F, 3F, 1F, 1F, 1F)
                        .texOffs(117, 45).addBox(-2F, -9F, 2F, 1F, 1F, 1F)
                        .texOffs(117, 45).addBox(-3F, -9F, 0F, 1F, 1F, 1F)
                        .texOffs(117, 45).addBox(-2F, -9F, -2F, 1F, 1F, 1F)
                        .texOffs(117, 45).addBox(0F, -9F, -3F, 1F, 1F, 1F)
                        .texOffs(117, 45).addBox(2F, -9F, -2F, 1F, 1F, 1F)
                        .texOffs(0, 56).addBox(-3F, -1F, -3F, 6F, 5F, 6F)
                        .texOffs(25, 56).addBox(-1F, 3F, -1F, 2F, 5F, 2F)
                        .texOffs(34, 56).addBox(-1F, 7F, -1F, 2F, 4F, 2F)
                        .texOffs(43, 56).addBox(-1F, 10F, -1F, 2F, 3F, 2F), PartPose.offset(0F, 3F, 0F));
        PartDefinition petal_0 = flower.addOrReplaceChild("petal_0", CubeListBuilder.create()
                        .texOffs(52, 56).addBox(-2F, -3F, 1F, 4F, 2F, 4F)
                        .texOffs(69, 56).addBox(-5F, -3F, 5F, 10F, 3F, 4F)
                        .texOffs(98, 56).addBox(-1F, -4F, 5F, 2F, 1F, 4F), PartPose.offsetAndRotation(0F, -4F, 0F, -0.66323F, 0F, 0F));
        petal_0.addOrReplaceChild("petal_0_tipL_0", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(-2F, -2F, 9F, -0.2618F, 0F, 0F));
        petal_0.addOrReplaceChild("petal_0_tipR_1", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(2F, -2F, 9F, -0.2618F, 0F, 0F));
        PartDefinition petal_1 = flower.addOrReplaceChild("petal_1", CubeListBuilder.create()
                        .texOffs(52, 56).addBox(-2F, -2F, 1F, 4F, 2F, 4F)
                        .texOffs(69, 56).addBox(-5F, -2F, 5F, 10F, 3F, 4F)
                        .texOffs(98, 56).addBox(-1F, -3F, 5F, 2F, 1F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.62832F, 0.7854F, 0F));
        petal_1.addOrReplaceChild("petal_1_tipL_0", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(-2F, -1F, 9F, -0.2618F, 0F, 0F));
        petal_1.addOrReplaceChild("petal_1_tipR_1", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(2F, -1F, 9F, -0.2618F, 0F, 0F));
        PartDefinition petal_2 = flower.addOrReplaceChild("petal_2", CubeListBuilder.create()
                        .texOffs(52, 56).addBox(-2F, -3F, 1F, 4F, 2F, 4F)
                        .texOffs(69, 56).addBox(-5F, -3F, 5F, 10F, 3F, 4F)
                        .texOffs(98, 56).addBox(-1F, -4F, 5F, 2F, 1F, 4F), PartPose.offsetAndRotation(0F, -4F, 0F, -0.66323F, 1.5708F, 0F));
        petal_2.addOrReplaceChild("petal_2_tipL_0", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(-2F, -2F, 9F, -0.2618F, 0F, 0F));
        petal_2.addOrReplaceChild("petal_2_tipR_1", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(2F, -2F, 9F, -0.2618F, 0F, 0F));
        PartDefinition petal_3 = flower.addOrReplaceChild("petal_3", CubeListBuilder.create()
                        .texOffs(52, 56).addBox(-2F, -2F, 1F, 4F, 2F, 4F)
                        .texOffs(69, 56).addBox(-5F, -2F, 5F, 10F, 3F, 4F)
                        .texOffs(98, 56).addBox(-1F, -3F, 5F, 2F, 1F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.62832F, 2.35619F, 0F));
        petal_3.addOrReplaceChild("petal_3_tipL_0", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(-2F, -1F, 9F, -0.2618F, 0F, 0F));
        petal_3.addOrReplaceChild("petal_3_tipR_1", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(2F, -1F, 9F, -0.2618F, 0F, 0F));
        PartDefinition petal_4 = flower.addOrReplaceChild("petal_4", CubeListBuilder.create()
                        .texOffs(52, 56).addBox(-2F, -3F, 1F, 4F, 2F, 4F)
                        .texOffs(69, 56).addBox(-5F, -3F, 5F, 10F, 3F, 4F)
                        .texOffs(98, 56).addBox(-1F, -4F, 5F, 2F, 1F, 4F), PartPose.offsetAndRotation(0F, -4F, 0F, -0.66323F, 3.14159F, 0F));
        petal_4.addOrReplaceChild("petal_4_tipL_0", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(-2F, -2F, 9F, -0.2618F, 0F, 0F));
        petal_4.addOrReplaceChild("petal_4_tipR_1", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(2F, -2F, 9F, -0.2618F, 0F, 0F));
        PartDefinition petal_5 = flower.addOrReplaceChild("petal_5", CubeListBuilder.create()
                        .texOffs(52, 56).addBox(-2F, -2F, 1F, 4F, 2F, 4F)
                        .texOffs(69, 56).addBox(-5F, -2F, 5F, 10F, 3F, 4F)
                        .texOffs(98, 56).addBox(-1F, -3F, 5F, 2F, 1F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.62832F, 3.92699F, 0F));
        petal_5.addOrReplaceChild("petal_5_tipL_0", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(-2F, -1F, 9F, -0.2618F, 0F, 0F));
        petal_5.addOrReplaceChild("petal_5_tipR_1", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(2F, -1F, 9F, -0.2618F, 0F, 0F));
        PartDefinition petal_6 = flower.addOrReplaceChild("petal_6", CubeListBuilder.create()
                        .texOffs(52, 56).addBox(-2F, -3F, 1F, 4F, 2F, 4F)
                        .texOffs(69, 56).addBox(-5F, -3F, 5F, 10F, 3F, 4F)
                        .texOffs(98, 56).addBox(-1F, -4F, 5F, 2F, 1F, 4F), PartPose.offsetAndRotation(0F, -4F, 0F, -0.66323F, 4.71239F, 0F));
        petal_6.addOrReplaceChild("petal_6_tipL_0", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(-2F, -2F, 9F, -0.2618F, 0F, 0F));
        petal_6.addOrReplaceChild("petal_6_tipR_1", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(2F, -2F, 9F, -0.2618F, 0F, 0F));
        PartDefinition petal_7 = flower.addOrReplaceChild("petal_7", CubeListBuilder.create()
                        .texOffs(52, 56).addBox(-2F, -2F, 1F, 4F, 2F, 4F)
                        .texOffs(69, 56).addBox(-5F, -2F, 5F, 10F, 3F, 4F)
                        .texOffs(98, 56).addBox(-1F, -3F, 5F, 2F, 1F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.62832F, 5.49779F, 0F));
        petal_7.addOrReplaceChild("petal_7_tipL_0", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(-2F, -1F, 9F, -0.2618F, 0F, 0F));
        petal_7.addOrReplaceChild("petal_7_tipR_1", CubeListBuilder.create()
                        .texOffs(111, 56).addBox(-1F, 0F, 0F, 2F, 3F, 4F), PartPose.offsetAndRotation(2F, -1F, 9F, -0.2618F, 0F, 0F));
        PartDefinition inner_0 = flower.addOrReplaceChild("inner_0", CubeListBuilder.create()
                        .texOffs(0, 68).addBox(-2F, -2F, 4F, 4F, 3F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.34907F, 0.38397F, 0F));
        inner_0.addOrReplaceChild("inner_0_tip_0", CubeListBuilder.create()
                        .texOffs(17, 68).addBox(-1F, -1F, 0F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 8F, -0.27925F, 0F, 0F));
        PartDefinition inner_1 = flower.addOrReplaceChild("inner_1", CubeListBuilder.create()
                        .texOffs(0, 68).addBox(-2F, -2F, 4F, 4F, 3F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.34907F, 1.16937F, 0F));
        inner_1.addOrReplaceChild("inner_1_tip_0", CubeListBuilder.create()
                        .texOffs(17, 68).addBox(-1F, -1F, 0F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 8F, -0.27925F, 0F, 0F));
        PartDefinition inner_2 = flower.addOrReplaceChild("inner_2", CubeListBuilder.create()
                        .texOffs(0, 68).addBox(-2F, -2F, 4F, 4F, 3F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.34907F, 1.95477F, 0F));
        inner_2.addOrReplaceChild("inner_2_tip_0", CubeListBuilder.create()
                        .texOffs(17, 68).addBox(-1F, -1F, 0F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 8F, -0.27925F, 0F, 0F));
        PartDefinition inner_3 = flower.addOrReplaceChild("inner_3", CubeListBuilder.create()
                        .texOffs(0, 68).addBox(-2F, -2F, 4F, 4F, 3F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.34907F, 2.74017F, 0F));
        inner_3.addOrReplaceChild("inner_3_tip_0", CubeListBuilder.create()
                        .texOffs(17, 68).addBox(-1F, -1F, 0F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 8F, -0.27925F, 0F, 0F));
        PartDefinition inner_4 = flower.addOrReplaceChild("inner_4", CubeListBuilder.create()
                        .texOffs(0, 68).addBox(-2F, -2F, 4F, 4F, 3F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.34907F, 3.52557F, 0F));
        inner_4.addOrReplaceChild("inner_4_tip_0", CubeListBuilder.create()
                        .texOffs(17, 68).addBox(-1F, -1F, 0F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 8F, -0.27925F, 0F, 0F));
        PartDefinition inner_5 = flower.addOrReplaceChild("inner_5", CubeListBuilder.create()
                        .texOffs(0, 68).addBox(-2F, -2F, 4F, 4F, 3F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.34907F, 4.31096F, 0F));
        inner_5.addOrReplaceChild("inner_5_tip_0", CubeListBuilder.create()
                        .texOffs(17, 68).addBox(-1F, -1F, 0F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 8F, -0.27925F, 0F, 0F));
        PartDefinition inner_6 = flower.addOrReplaceChild("inner_6", CubeListBuilder.create()
                        .texOffs(0, 68).addBox(-2F, -2F, 4F, 4F, 3F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.34907F, 5.09636F, 0F));
        inner_6.addOrReplaceChild("inner_6_tip_0", CubeListBuilder.create()
                        .texOffs(17, 68).addBox(-1F, -1F, 0F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 8F, -0.27925F, 0F, 0F));
        PartDefinition inner_7 = flower.addOrReplaceChild("inner_7", CubeListBuilder.create()
                        .texOffs(0, 68).addBox(-2F, -2F, 4F, 4F, 3F, 4F), PartPose.offsetAndRotation(0F, -5F, 0F, -0.34907F, 5.88176F, 0F));
        inner_7.addOrReplaceChild("inner_7_tip_0", CubeListBuilder.create()
                        .texOffs(17, 68).addBox(-1F, -1F, 0F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 8F, -0.27925F, 0F, 0F));
        PartDefinition sepal_0 = flower.addOrReplaceChild("sepal_0", CubeListBuilder.create()
                        .texOffs(26, 68).addBox(-1F, -1F, 2F, 2F, 4F, 2F)
                        .texOffs(35, 68).addBox(-1F, 2F, 1F, 2F, 5F, 2F), PartPose.offset(0F, 2F, 0F));
        PartDefinition sepal_1 = flower.addOrReplaceChild("sepal_1", CubeListBuilder.create()
                        .texOffs(26, 68).addBox(-1F, -1F, 2F, 2F, 4F, 2F)
                        .texOffs(35, 68).addBox(-1F, 2F, 1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 0.7854F, 0F));
        PartDefinition sepal_2 = flower.addOrReplaceChild("sepal_2", CubeListBuilder.create()
                        .texOffs(26, 68).addBox(-1F, -1F, 2F, 2F, 4F, 2F)
                        .texOffs(35, 68).addBox(-1F, 2F, 1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 1.5708F, 0F));
        PartDefinition sepal_3 = flower.addOrReplaceChild("sepal_3", CubeListBuilder.create()
                        .texOffs(26, 68).addBox(-1F, -1F, 2F, 2F, 4F, 2F)
                        .texOffs(35, 68).addBox(-1F, 2F, 1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 2.35619F, 0F));
        PartDefinition sepal_4 = flower.addOrReplaceChild("sepal_4", CubeListBuilder.create()
                        .texOffs(26, 68).addBox(-1F, -1F, 2F, 2F, 4F, 2F)
                        .texOffs(35, 68).addBox(-1F, 2F, 1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 3.14159F, 0F));
        PartDefinition sepal_5 = flower.addOrReplaceChild("sepal_5", CubeListBuilder.create()
                        .texOffs(26, 68).addBox(-1F, -1F, 2F, 2F, 4F, 2F)
                        .texOffs(35, 68).addBox(-1F, 2F, 1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 3.92699F, 0F));
        PartDefinition sepal_6 = flower.addOrReplaceChild("sepal_6", CubeListBuilder.create()
                        .texOffs(26, 68).addBox(-1F, -1F, 2F, 2F, 4F, 2F)
                        .texOffs(35, 68).addBox(-1F, 2F, 1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 4.71239F, 0F));
        PartDefinition sepal_7 = flower.addOrReplaceChild("sepal_7", CubeListBuilder.create()
                        .texOffs(26, 68).addBox(-1F, -1F, 2F, 2F, 4F, 2F)
                        .texOffs(35, 68).addBox(-1F, 2F, 1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 5.49779F, 0F));
        PartDefinition runes = mesh.getRoot().addOrReplaceChild("runes", CubeListBuilder.create(), PartPose.offset(0F, 4F, 0F));
        PartDefinition rune_0 = runes.addOrReplaceChild("rune_0", CubeListBuilder.create()
                        .texOffs(44, 68).addBox(15F, -1F, -1F, 1F, 6F, 2F)
                        .texOffs(51, 68).addBox(15F, 1F, -3F, 1F, 2F, 6F)
                        .texOffs(66, 68).addBox(14.5F, 1F, -1F, 2F, 2F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition rune_1 = runes.addOrReplaceChild("rune_1", CubeListBuilder.create()
                        .texOffs(44, 68).addBox(15F, -5F, -1F, 1F, 6F, 2F)
                        .texOffs(51, 68).addBox(15F, -3F, -3F, 1F, 2F, 6F)
                        .texOffs(66, 68).addBox(14.5F, -3F, -1F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 1.5708F, 0F));
        PartDefinition rune_2 = runes.addOrReplaceChild("rune_2", CubeListBuilder.create()
                        .texOffs(44, 68).addBox(15F, -1F, -1F, 1F, 6F, 2F)
                        .texOffs(51, 68).addBox(15F, 1F, -3F, 1F, 2F, 6F)
                        .texOffs(66, 68).addBox(14.6F, 1F, -1F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 3.14159F, 0F));
        PartDefinition rune_3 = runes.addOrReplaceChild("rune_3", CubeListBuilder.create()
                        .texOffs(44, 68).addBox(15F, -5F, -1F, 1F, 6F, 2F)
                        .texOffs(51, 68).addBox(15F, -3F, -3F, 1F, 2F, 6F)
                        .texOffs(66, 68).addBox(14.4F, -3F, -1F, 2F, 2F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 4.71239F, 0F));
        return mesh.getRoot().bake(128, 128);
    }

    @Override
    public void setupAnim(FriliasEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float idle = ageInTicks * 0.045F;
        if (entity.inIntro() || entity.getTransitionTicks() > 0) {
            // 开场/转阶段演出：双臂向两侧上举展开，头部微仰迎接汇聚的星尘
            this.head.yRot = 0.0F;
            this.head.xRot = -0.25F;
            this.leftArm.xRot = -2.45F;
            this.rightArm.xRot = -2.45F;
            this.leftArm.zRot = -0.55F;
            this.rightArm.zRot = 0.55F;
        } else {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
            this.head.xRot = headPitch * ((float) Math.PI / 180F);
            // 悬浮飞行：手臂自然下垂（xRot 每帧重置为小幅摆动，避免演出姿势残留）
            this.leftArm.xRot = (float) Math.sin(idle) * 0.08F;
            this.rightArm.xRot = (float) Math.sin(idle + 0.9D) * 0.08F;
            this.leftArm.zRot = -0.24435F + (float) Math.sin(idle * 1.1D) * 0.05F;
            this.rightArm.zRot = 0.24435F - (float) Math.sin(idle * 1.1D + 0.8D) * 0.05F;
        }
        // 双侧垂发随风飘动
        this.hairTailL.xRot = 0.13963F + (float) Math.sin(idle * 1.3D) * 0.12F;
        this.hairTailR.xRot = 0.13963F + (float) Math.sin(idle * 1.3D + 1.1D) * 0.12F;
        this.hairTailL.zRot = -0.10472F + (float) Math.sin(idle * 0.9D) * 0.06F;
        this.hairTailR.zRot = 0.10472F - (float) Math.sin(idle * 0.9D + 0.7D) * 0.06F;
        // 花身摇曳 + 花冠微摆
        this.flower.zRot = (float) Math.sin(ageInTicks * 0.05D) * 0.05F;
        this.flower.xRot = (float) Math.sin(ageInTicks * 0.07D) * 0.03F;
        this.crown.zRot = (float) Math.sin(ageInTicks * 0.08D) * 0.07F;
        // 符文环匀速环绕 + 整体起伏
        this.runes.yRot = ageInTicks * 0.035F;
        this.runes.y = 4.0F + (float) Math.sin(ageInTicks * 0.06D) * 0.8F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
