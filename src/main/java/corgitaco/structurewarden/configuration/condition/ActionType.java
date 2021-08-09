package corgitaco.structurewarden.configuration.condition;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import corgitaco.structurewarden.StructureWarden;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Map;

public enum ActionType implements IStringSerializable {
    BLOCK_BREAK,
    BLOCK_PLACE,
    CONTAINER_OPEN;

    public static final Codec<ActionType> CODEC = IStringSerializable.fromEnum(ActionType::values, ActionType::getTypeFromId);

    private static final Map<String, ActionType> BY_ID = Util.make(Maps.newHashMap(), (nameToTypeMap) -> {
        for (ActionType key : values()) {
            nameToTypeMap.put(key.name(), key);
        }
    });

    private final IFormattableTextComponent actionTranslationComponent = new TranslationTextComponent(StructureWarden.MOD_ID + ".actiontype.action." + name().toLowerCase()).withStyle(TextFormatting.RED);

    @Nullable
    public static ActionType getTypeFromId(String idIn) {
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
