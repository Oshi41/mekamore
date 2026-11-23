package com.mekamore.network.packages;

import com.mekamore.MekaMore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.BrainDebugPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ScanResult(Set<Integer> entityIds, List<BlockPos> blocks) implements CustomPacketPayload {
    public static final Type<ScanResult> TYPE = new Type<>(MekaMore.rl("scan_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanResult> STREAM_CODEC = StreamCodec.composite(
            // Set<Integer> on the wire as a var-int list (the explicit type argument keeps the Set type)
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).<Set<Integer>>map(HashSet::new, ArrayList::new),
            ScanResult::entityIds,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ScanResult::blocks,
            ScanResult::new);


    public boolean isEmpty(){
        return entityIds.isEmpty() && blocks.isEmpty();
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScanResult payload, IPayloadContext context){
        if (context.connection().getDirection().isClientbound()) {
            context.enqueueWork(() -> com.mekamore.client.RevealHighlightHandler.update(payload.blocks, payload.entityIds));
        }
    }
}
