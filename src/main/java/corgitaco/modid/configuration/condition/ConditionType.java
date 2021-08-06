package corgitaco.modid.configuration.condition;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import corgitaco.modid.Main;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Map;

public enum ConditionType implements IStringSerializable {
    BLOCK_BREAK,
    BLOCK_PLACE,
    CONTAINER_OPEN;

    public static final Codec<ConditionType> CODEC = IStringSerializable.fromEnum(ConditionType::values, ConditionType::getTypeFromId);

    private static final Map<String, ConditionType> BY_ID = Util.make(Maps.newHashMap(), (nameToTypeMap) -> {
        for (ConditionType key : values()) {
            nameToTypeMap.put(key.name(), key);
        }
    });

    private final IFormattableTextComponent actionTranslationComponent = new TranslationTextComponent(Main.MOD_ID + ".conditiontype.action." + name().toLowerCase()).withStyle(TextFormatting.RED);

    @Nullable
    public static ConditionType getTypeFromId(String idIn) {
        return BY_ID.get(idIn);
    }

    @Override
    public String getSerializedName() {
        return this.name();
    }

    public IFormattableTextComponent getActionTranslationComponent() {
        return actionTranslationComponent;
    }
}
