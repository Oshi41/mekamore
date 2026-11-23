package com.mekamore.registries;

import com.mekamore.network.packages.ScanResult;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class MekaMoreNetwork {
    public static void registerNetwork(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToClient(ScanResult.TYPE, ScanResult.STREAM_CODEC,  ScanResult::handle);
    }
}
