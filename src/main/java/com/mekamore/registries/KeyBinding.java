package com.mekamore.registries;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class KeyBinding {
    public static KeyMapping ZOOM;

    public static void register(RegisterKeyMappingsEvent event) {
        ZOOM = new KeyMapping(
                "key.mekamore.module_zoom",
                GLFW.GLFW_KEY_Z,
                "key.categories.mekamore"
        );

        event.register(ZOOM);
    }
}
