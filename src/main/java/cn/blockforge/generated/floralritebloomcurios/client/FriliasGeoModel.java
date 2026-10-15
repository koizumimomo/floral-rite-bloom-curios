package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * L1 · boss 模型接入 GeckoLib：用户提供的 frilias.geo.json + frilias.animation.json。
 * <p>
 * 骨骼结构（geo 内）保留：<code>head / arm_l / arm_r / crown / flower / longdress* / zhao_qun / Teng_wan / he_xin / xia_ban_shen / dreass / shirt / du_zi / Ribbon</code>
 * 以及 <code>runes / fire / ice / void / nature / sky / death / rain / time</code>（八个属性主魔法阵，
 * 由 {@code *_open} 动画展开后 hold_on_last_frame）与 <code>fire2 / ice2 / void2 / nature2 / sky2 / death2 / rain2 / time2</code>
 * （第二套魔法阵，由 {@code *_du_tiao} 动画在 0.375~0.625s 内 scale 0→1→0 走完一次出现动画）。
 * <p>
 * <b>抬手 / 双手交叉</b>等几何关键姿态不在 animation 里写死，
 * 由 {@link FriliasRenderer#setupRotations} 硬覆盖 <code>arm_l / arm_r</code> 的 rotation 字段，
 * 保证转阶段 / 开场 / 读条期间三个状态都能稳定套上。
 *
 * @author r73 L1
 */
public class FriliasGeoModel extends GeoModel<FriliasEntity> {

    @Override
    public ResourceLocation getModelResource(FriliasEntity animatable) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "geo/entity/frilias.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FriliasEntity animatable) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "textures/entity/frilias.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FriliasEntity animatable) {
        return new ResourceLocation(FloralRiteBloom.MOD_ID, "animations/frilias.animation.json");
    }
}