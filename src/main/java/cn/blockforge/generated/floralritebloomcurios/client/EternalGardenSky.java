package cn.blockforge.generated.floralritebloomcurios.client;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 永恒花园的异色天空：无太阳/月亮/星星/云，天空即雾色——
 * 亮时粉紫的"永昼花海天光"，暗处沉为深堇色。
 * 云层高度设为 NaN（不渲染云），环境花瓣由 AmbientGardenPetals 负责。
 */
public final class EternalGardenSky extends DimensionSpecialEffects {

    public static final ResourceLocation EFFECTS_ID =
            new ResourceLocation(FloralRiteBloom.MOD_ID, "eternal_garden");

    public EternalGardenSky() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        double b = Mth.clamp(brightness, 0.0D, 1.0D);
        // 亮 -> (0.86, 0.56, 0.82) 粉紫；暗 -> (0.36, 0.22, 0.46) 深堇
        return new Vec3(0.36D + 0.50D * b, 0.22D + 0.34D * b, 0.46D + 0.36D * b);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }
}
