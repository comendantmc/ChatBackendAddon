package ru.org.twobtwot.chatbackendaddon.Listeners;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import net.pistonmaster.pistonchat.api.PistonChatEvent;
import net.pistonmaster.pistonchat.api.PistonWhisperEvent;
import net.pistonmaster.pistonchat.utils.CommonTool;
import okhttp3.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.org.twobtwot.chatbackendaddon.ChatBackendAddon;
import ru.org.twobtwot.chatbackendaddon.Structs.BackendResponse;
import ru.org.twobtwot.chatbackendaddon.Structs.Message;
import ru.org.twobtwot.chatbackendaddon.Utils.RoundRobin;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class PistonChatListener implements Listener {
    private final ChatBackendAddon plugin;
    private final OkHttpClient client = new OkHttpClient();
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<Message> messageJsonAdapter = moshi.adapter(Message.class);
    private final JsonAdapter<BackendResponse> backendResponseJsonAdapter = moshi.adapter(BackendResponse.class);
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final RoundRobin<String> servers;

    public PistonChatListener(ChatBackendAddon plugin) {
        this.plugin = plugin;
        servers = new RoundRobin<>(plugin.getConfig().getStringList("servers"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(PistonChatEvent event) {
        handleMessage(event.getPlayer(), event.getMessage(),
                () -> event.setCancelled(true),
                event::setMessage);
    }

    @EventHandler(ignoreCancelled = true)
    public void onWhisper(PistonWhisperEvent event) {
        if (event.getSender() == event.getReceiver()) return;

        handleMessage(event.getSender(), event.getMessage(),
                () -> event.setCancelled(true),
                event::setMessage);
    }

    public void handleMessage(CommandSender sender, String message, Runnable cancelEvent, Consumer<String> modifyMessage) {
        if (!plugin.getConfig().getBoolean("enable"))
            return;

        BackendResponse response = backendProcess(sender, message, 0);
        if (response.replacement != null) {
            modifyMessage.accept(response.replacement);
        } else if (!response.allowed) {
            cancelEvent.run();
            plugin.getLogger().info(ChatColor.RED + "<" + sender.getName() + "> " + message);
        }
    }

    private BackendResponse backendProcess(CommandSender sender, String message, int retries) {
        BackendResponse responseOnError = new BackendResponse(
                plugin.getConfig().getBoolean("allow-on-error"),
                null
        );
        int timeout = plugin.getConfig().getInt("timeout");

        if (retries > plugin.getConfig().getInt("retries"))
            return responseOnError;

        RequestBody body = RequestBody.create(messageJsonAdapter.toJson(new Message(sender.getName(), message)), JSON);
        Request request = new Request.Builder()
                .url(servers.iterator().next())
                .post(body)
                .build();

        OkHttpClient dirtyClient = client.newBuilder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();

        try (Response res = dirtyClient.newCall(request).execute()) {
            if (!res.isSuccessful()) {
                plugin.getLogger().severe(ChatColor.RED + "Request to a backend processor failed");
                return backendProcess(sender, message, retries + 1);
            }
            return backendResponseJsonAdapter.fromJson(Objects.requireNonNull(res.body()).source());
        } catch (IOException e) {
            plugin.getLogger().severe(ChatColor.RED + "Request to a backend processor failed");
            return backendProcess(sender, message, retries + 1);
        } catch (NullPointerException e) {
            plugin.getLogger().severe(ChatColor.RED + "Response from a backend processor is empty");
            return backendProcess(sender, message, retries + 1);
        }
    }
}