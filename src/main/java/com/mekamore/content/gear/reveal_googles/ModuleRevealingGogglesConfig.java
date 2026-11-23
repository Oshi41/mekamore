package com.mekamore.content.gear.reveal_googles;

import com.google.common.collect.ImmutableList;
import com.mekamore.MekaMore;
import com.mekamore.registries.MekaMoreConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import mekanism.api.SerializationConstants;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.gear.config.ModuleConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable module config holding a capped, de-duplicated, sorted list of
 * resource keys (blocks: {@code minecraft:block}, entities: {@code minecraft:entity_type}).
 *
 * <p>Each key is persisted/sent as {registry, location} so block and entity
 * filters stay distinguishable. Register with:
 * <pre>{@code
 * builder.addConfig(ModuleRevealingGogglesConfig.createDefault(FILTERS_KEY),
 *         ModuleRevealingGogglesConfig.CODEC, ModuleRevealingGogglesConfig.STREAM_CODEC);
 * }</pre>
 * (custom configs are not allowed on instance-based modules).
 */
@NothingNullByDefault
public class ModuleRevealingGogglesConfig extends ModuleConfig<List<ResourceKey<?>>> {

    /** Unique name of this config on the module. Use this one constant for registration, lookup and saving. */
    public static final ResourceLocation FILTERS_KEY = MekaMore.rl("reveal_filters");

    // ── Key codecs ────────────────────────────────────────────────────

    private static ResourceKey<?> key(ResourceLocation registry, ResourceLocation location) {
        return ResourceKey.create(ResourceKey.createRegistryKey(registry), location);
    }

    private static final Codec<ResourceKey<?>> KEY_CODEC = RecordCodecBuilder.<ResourceKey<?>>create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("registry").forGetter(ResourceKey::registry),
            ResourceLocation.CODEC.fieldOf("location").forGetter(ResourceKey::location)
    ).apply(i, ModuleRevealingGogglesConfig::key));

    private static final StreamCodec<ByteBuf, ResourceKey<?>> KEY_STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ResourceKey::registry,
            ResourceLocation.STREAM_CODEC, ResourceKey::location,
            ModuleRevealingGogglesConfig::key);

    private static final StreamCodec<ByteBuf, List<ResourceKey<?>>> KEYS_STREAM_CODEC =
            KEY_STREAM_CODEC.apply(ByteBufCodecs.list());

    private static final Comparator<ResourceKey<?>> ORDER =
            Comparator.<ResourceKey<?>, ResourceLocation>comparing(ResourceKey::registry)
                    .thenComparing(ResourceKey::location);

    // ── Config codecs ─────────────────────────────────────────────────

    public static final Codec<ModuleRevealingGogglesConfig> CODEC = RecordCodecBuilder.create(instance -> baseCodec(instance)
            .and(KEY_CODEC.listOf().fieldOf(SerializationConstants.VALUES).forGetter(ModuleConfig::get))
            .apply(instance, ModuleRevealingGogglesConfig::create));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModuleRevealingGogglesConfig> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ModuleConfig::name,
            KEYS_STREAM_CODEC, ModuleConfig::get,
            ModuleRevealingGogglesConfig::create);

    // ── Factories ─────────────────────────────────────────────────────

    public static ModuleRevealingGogglesConfig createDefault(ResourceLocation name) {
        // TODO remove
        return new ModuleRevealingGogglesConfig(name, ImmutableList.of(
                ResourceKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("iron_ore")),
                ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("enderman"))
        ));
    }

    public static ModuleRevealingGogglesConfig create(ResourceLocation name, List<ResourceKey<?>> keys) {
        return new ModuleRevealingGogglesConfig(name, sanitise(keys));
    }

    // ── State ─────────────────────────────────────────────────────────

    private final ImmutableList<ResourceKey<?>> values;

    private ModuleRevealingGogglesConfig(ResourceLocation name, ImmutableList<ResourceKey<?>> values) {
        super(name);
        this.values = values;
    }

    // ── ModuleConfig contract ─────────────────────────────────────────

    @Override
    public List<ResourceKey<?>> get() {
        return values; // already immutable
    }

    @Override
    public ModuleRevealingGogglesConfig with(List<ResourceKey<?>> value) {
        Objects.requireNonNull(value, "Value cannot be null.");
        ImmutableList<ResourceKey<?>> sanitised = sanitise(value);
        return sanitised.equals(values) ? this : new ModuleRevealingGogglesConfig(name(), sanitised);
    }

    @Override
    public StreamCodec<ByteBuf, ModuleConfig<List<ResourceKey<?>>>> namedStreamCodec(ResourceLocation name) {
        return KEYS_STREAM_CODEC.<ModuleConfig<List<ResourceKey<?>>>>map(
                keys -> create(name, keys), ModuleConfig::get);
    }

    // ── Convenience helpers for the GUI / module logic ────────────────

    public boolean containsBlock(ResourceLocation id) {
        return values.contains(ResourceKey.create(Registries.BLOCK, id));
    }

    public boolean containsEntity(ResourceLocation id) {
        return values.contains(ResourceKey.create(Registries.ENTITY_TYPE, id));
    }

    // ── equals / hashCode ─────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!super.equals(o)) {
            return false;
        }
        return values.equals(((ModuleRevealingGogglesConfig) o).values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), values);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** De-duplicates, sorts, and caps. Applied on every construction path, including decoded packets. */
    private static ImmutableList<ResourceKey<?>> sanitise(List<ResourceKey<?>> raw) {
        return raw.stream()
                .distinct()
                .sorted(ORDER)
                .limit(MekaMoreConfig.revealGooglesMaxFilters.get())
                .collect(ImmutableList.toImmutableList());
    }
}