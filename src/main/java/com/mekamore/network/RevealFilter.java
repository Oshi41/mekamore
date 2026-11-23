package com.mekamore.content.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * One filter slot: match a block type, an entity type, or both.
 * Null fields mean "not used".
 */
public record RevealFilter(
        ResourceLocation blockType,   // nullable
        ResourceLocation entityType   // nullable
) {
    public static final RevealFilter EMPTY = new RevealFilter(null, null);

    public static final Codec<RevealFilter> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.optionalFieldOf("block").forGetter(f -> Optional.ofNullable(f.blockType)),
            ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(f -> Optional.ofNullable(f.entityType))
    ).apply(inst, (block, entity) -> new RevealFilter(
            block.orElse(null),
            entity.orElse(null)
    )));

    public boolean isEmpty() {
        return blockType == null && entityType == null;
    }
}