package org.valkyrienskies.mod.mixin.mod_compat.sodium;

import it.unimi.dsi.fastutil.objects.ReferenceSet;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.Frustum;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import org.joml.primitives.AABBd;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.mixinducks.mod_compat.sodium.RenderSectionManagerDuck;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class MixinRenderSectionManager implements RenderSectionManagerDuck {

    @Unique
    private final WeakHashMap<ClientShip, ChunkRenderList> shipRenderLists = new WeakHashMap<>();

    @Override
    public WeakHashMap<ClientShip, ChunkRenderList> getShipRenderLists() {
        return shipRenderLists;
    }

    @Shadow
    @Final
    private ClientLevel world;
    @Shadow
    @Final
    private ReferenceSet<RenderSection> sectionsWithGlobalEntities;
    @Shadow
    @Final
    private ChunkRenderer chunkRenderer;
    @Shadow
    @Final
    private Map<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildLists;
    @Shadow
    private SortedRenderLists renderLists;

    @Shadow
    protected abstract RenderSection getRenderSection(int x, int y, int z);

    @Inject(at = @At("TAIL"), method = "update")
    private void afterUpdate(final Camera camera, final Viewport viewport, final int frame,
        final boolean spectator, final CallbackInfo ci) {
        for (final ClientShip ship : VSGameUtilsKt.getShipObjectWorld(Minecraft.getInstance()).getLoadedShips()) {
            ship.getActiveChunksSet().forEach((x, z) -> {
                for (int y = world.getMinSection(); y < world.getMaxSection(); y++) {
                    final RenderSection section = getRenderSection(x, y, z);

                    if (section == null) {
                        continue;
                    }

                    if (section.getPendingUpdate() != null) {
                        final ArrayDeque<RenderSection> queue = this.rebuildLists.get(section.getPendingUpdate());
                        if (queue.size() < (2 << 4) - 1) {
                            queue.add(section);
                        }
                    }

                    SectionPos pos = section.getPosition();
                    final AABBd b2 = new AABBd(pos.minBlockX() - 6e-1, pos.minBlockY() - 6e-1, pos.minBlockZ() - 6e-1,
                        //              + 1.6                   + 1.6                   + 1.6
                        pos.maxBlockX() + 6e-1, pos.maxBlockY() + 6e-1, pos.maxBlockZ() + 6e-1)
                        .transform(ship.getRenderTransform().getShipToWorld());

                    Frustum frustum = null;
                    try {
                        Object instance = viewport;
                        Field field = Viewport.class.getDeclaredField("frustum");
                        frustum = (Frustum) field.get(instance);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }

                    if (!frustum.testAab((float) b2.minX, (float) b2.minY, (float) b2.minZ,
                            (float) b2.maxX, (float) b2.maxY, (float) b2.maxZ)) {
                        continue;
                    }

                    if (section.getGlobalBlockEntities() != null && section.getGlobalBlockEntities().length != 0) {
                        this.sectionsWithGlobalEntities.add(section);
                    }

                    //RenderRegion region = section.getRegion();
                    //ChunkRenderList renderList = region.getRenderList();
                    //this.shipRenderLists.putIfAbsent(ship, renderList);
                    //this.renderLists.add(renderList);
                }
            });
        }
    }

    @Inject(at = @At("TAIL"), method = "resetRenderLists")
    private void afterResetRenderLists(final CallbackInfo ci) {
        //shipRenderLists.values().forEach(ChunkRenderList::clear);
    }
}
