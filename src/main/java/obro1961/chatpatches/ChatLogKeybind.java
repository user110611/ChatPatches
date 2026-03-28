package obro1961.chatpatches;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import obro1961.chatpatches.gui.ChatLogScreen;
import obro1961.chatpatches.gui.MuteScreen;
import org.lwjgl.glfw.GLFW;

public class ChatLogKeybind {
    public static KeyMapping openChatLog;
    public static KeyMapping openMute;

    public static void register() {
        openChatLog = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "text.chatpatches.chatlog.keybind",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                KeyMapping.Category.MISC
        ));

        openMute = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "text.chatpatches.mute.keybind",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openChatLog.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new ChatLogScreen(null));
                }
            }
            while (openMute.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new MuteScreen(null));
                }
            }
        });
    }
}