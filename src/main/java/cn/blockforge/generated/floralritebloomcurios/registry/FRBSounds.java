package cn.blockforge.generated.floralritebloomcurios.registry;

import cn.blockforge.generated.floralritebloomcurios.FloralRiteBloom;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 自定义音效注册（数据在 assets/sounds.json，ogg 文件在 assets/sounds/）。 */
public final class FRBSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FloralRiteBloom.MOD_ID);

    public static final RegistryObject<SoundEvent> BOSS_MUSIC = register("boss_music");
    public static final RegistryObject<SoundEvent> FRILIAS_AWAKEN = register("frilias_awaken");
    public static final RegistryObject<SoundEvent> FRILIAS_SUMMON = register("frilias_summon");
    public static final RegistryObject<SoundEvent> FRILIAS_PHASE = register("frilias_phase");
    public static final RegistryObject<SoundEvent> FRILIAS_DEATH = register("frilias_death");
    public static final RegistryObject<SoundEvent> SEAL_APPLY = register("seal_apply");
    public static final RegistryObject<SoundEvent> BOUNTIFUL_SHOOT = register("bountiful_shoot");
    public static final RegistryObject<SoundEvent> CHIME_RECALL = register("chime_recall");
    public static final RegistryObject<SoundEvent> WREATH_BURST = register("wreath_burst");
    public static final RegistryObject<SoundEvent> GARDEN_GATE = register("garden_gate");
    public static final RegistryObject<SoundEvent> RITUAL_COMPLETE = register("ritual_complete");
    /** 霜凝提示音（r66 用户提供 chou_hen.ogg，boss 开始蓄力时播放一次）。 */
    public static final RegistryObject<SoundEvent> CHOU_HEN = register("chou_hen");
    /** r101：喝醉了果冻回弹时播放的玩偶音乐（源自 friendswine doll music）。 */
    public static final RegistryObject<SoundEvent> DOLL_MUSIC = register("doll_music");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(FloralRiteBloom.MOD_ID, name)));
    }

    private FRBSounds() {}
}
