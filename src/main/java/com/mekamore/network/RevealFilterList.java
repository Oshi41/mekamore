package com.mekamore.network4;

import com.mekamore.content.gear.RevealFilter;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

public record RevealFilterList(RevealFilter[] filters) {

    public static final RevealFilterList EMPTY = new RevealFilterList(new RevealFilter[0]);
    public static final int MAX_FILTERS = 3;

    public static final Codec<RevealFilterList> CODEC = RevealFilter.CODEC
            .listOf(0, MAX_FILTERS)
            .xmap(
                    list -> new RevealFilterList(list.toArray(RevealFilter[]::new)),
                    rfl -> Arrays.asList(rfl.filters())
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, RevealFilterList> STREAM_CODEC =
            StreamCodec.of(
                    (buf, list) -> {
                        buf.writeByte(list.filters().length);
                        for (RevealFilter f : list.filters()) {
                            buf.writeBoolean(f.blockType() != null);
                            if (f.blockType() != null) buf.writeResourceLocation(f.blockType());
                            buf.writeBoolean(f.entityType() != null);
                            if (f.entityType() != null) buf.writeResourceLocation(f.entityType());
                        }
                    },
                    buf -> {
                        int count = buf.readByte();
                        RevealFilter[] filters = new RevealFilter[count];
                        for (int i = 0; i < count; i++) {
                            ResourceLocation block = buf.readBoolean() ? buf.readResourceLocation() : null;
                            ResourceLocation entity = buf.readBoolean() ? buf.readResourceLocation() : null;
                            filters[i] = new RevealFilter(block, entity);
                        }
                        return new RevealFilterList(filters);
                    }
            );
}