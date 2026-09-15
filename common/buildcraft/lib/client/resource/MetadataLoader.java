package buildcraft.lib.client.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;


/** Alternate metadata loader for {@link ResourceMetadata#getSection(MetadataSectionSerializer)} */
@Deprecated(forRemoval = true)
public class MetadataLoader {

    private static boolean hasRegistered = false;

    private static void register() {
        if (!hasRegistered) {
            hasRegistered = true;
        }
    }

    /** @param samePack If true, then only the data in the same resource pack will be returned. */
    @Nullable
    public static DataMetadataSection getData(ResourceLocation location, boolean samePack) {
        ResourceManager resManager = Minecraft.getInstance().getResourceManager();
        register();
        try {
            List<Resource> resources = resManager.getResourceStack(location);
            DataMetadataSection section = null;
            for (Resource resource : resources) {
                section = resource.metadata().getSection(DataMetadataSection.DESERIALISER).orElse(null);
                if (section != null || samePack) {
                    break;
                }
            }
            return section;
        } catch (FileNotFoundException fnfe) {
            // That's fine
            return null;
        } catch (IOException e) {
            // That's not fine
            e.printStackTrace();
            return null;
        }
    }
}
