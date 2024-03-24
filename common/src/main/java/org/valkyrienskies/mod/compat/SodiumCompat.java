package org.valkyrienskies.mod.compat;

import java.lang.reflect.Method;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import org.valkyrienskies.mod.mixin.ValkyrienCommonMixinConfigPlugin;

public class SodiumCompat {

    public static void onChunkAdded(final int x, final int z) {
        if (ValkyrienCommonMixinConfigPlugin.getVSRenderer() == VSRenderer.SODIUM) {
            try {
                Object instance = SodiumWorldRenderer.instance();
                Method method = SodiumWorldRenderer.class.getMethod("onChunkAdded", int.class, int.class);
                method.invoke(instance, x, z);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static void onChunkRemoved(final int x, final int z) {
        if (ValkyrienCommonMixinConfigPlugin.getVSRenderer() == VSRenderer.SODIUM) {
            try {
                Object instance = SodiumWorldRenderer.instance();
                Method method = SodiumWorldRenderer.class.getMethod("onChunkRemoved", int.class, int.class);
                method.invoke(instance, x, z);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

}
