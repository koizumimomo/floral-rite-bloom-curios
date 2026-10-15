package top.theillusivec4.curios.api;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** 编译期桩：与 Curios 5.14.1+1.20.1 的 SlotAttribute 签名一致。 */
public class SlotAttribute extends RangedAttribute {
    private final String identifier;

    protected SlotAttribute(String identifier) {
        super("curios.slot." + identifier, 0.0D, -1024.0D, 1024.0D);
        this.identifier = identifier;
        this.setSyncable(true);
    }

    public static SlotAttribute getOrCreate(String identifier) {
        return new SlotAttribute(identifier);
    }

    public String getIdentifier() {
        return identifier;
    }
}
