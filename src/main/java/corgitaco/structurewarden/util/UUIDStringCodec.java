package corgitaco.structurewarden.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.UUID;

public class UUIDStringCodec {

    public static final Codec<UUID> CODEC = Codec.STRING.comapFlatMap(
            s ->
            {
                try {
                    return DataResult.success(UUID.fromString(s));
                } catch (Exception e) // fromString throws if it can't convert the string to a UUID
                {
                    return DataResult.error(e.getMessage());
                }
            },
            UUID::toString);
}
