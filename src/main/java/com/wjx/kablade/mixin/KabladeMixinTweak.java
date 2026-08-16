package com.wjx.kablade.mixin;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.CoreModManager;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.security.CodeSource;
import java.util.List;

@SuppressWarnings("unused")
public class KabladeMixinTweak implements ITweaker{
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {

    }

    @SuppressWarnings("CallToPrintStackTrace")
    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        MixinBootstrap.init();
        // Cleanroom discovers mods before cascading their TweakClass, so ModDiscoverer has
        // already been loaded by the time this method runs. Keep the LoadController hook, but
        // do not ask Mixin to transform an already loaded discovery class.
        boolean cleanroom = classLoader.getResource("com/cleanroommc/boot/Main.class") != null;
        Mixins.addConfiguration(cleanroom
                ? "mixins.kablade.cleanroom_late.json"
                : "mixins.kablade.mixin_late.json");
        CodeSource codeSource = this.getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            URL location = codeSource.getLocation();
            try {
                File file = new File(toURI(location));
                if (file.isFile()) {
                    CoreModManager.getIgnoredMods().remove(file.getName());
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            LogManager.getLogger().warn("No CodeSource, if this is not a development environment we might run into problems!");
            LogManager.getLogger().warn(this.getClass().getProtectionDomain());
        }
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }

    public static URI toURI(URL url) throws IOException, URISyntaxException {
        URLConnection connection = url.openConnection();
        if (connection instanceof JarURLConnection) {
            JarURLConnection jarURLConnection = (JarURLConnection) connection;
            return jarURLConnection.getJarFileURL().toURI();
        } else  {
            return url.toURI();
        }
    }
}
