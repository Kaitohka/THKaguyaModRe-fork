package thkaguyamod;

import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 五つの難題MOD+ (Itutu no Nandai MOD+) — NeoForge 1.21.1 port.
 *
 * <p>Ported from the 1.7.10 Forge mod "THKaguyaMod" v2.90 (modid {@code THKaguyaMod}).
 * The modid is lowercased to {@code thkaguyamod} because modern FML requires lowercase
 * modids; this matches the resource domain already used by the original mod.
 */
@Mod(THKaguyaMod.MOD_ID)
public class THKaguyaMod {

    public static final String MOD_ID = "thkaguyamod";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public THKaguyaMod() {
        LOGGER.info("Itutu no Nandai MOD+ (NeoForge port) loading.");
    }
}
