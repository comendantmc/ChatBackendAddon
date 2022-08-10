package ru.org.twobtwot.chatbackendaddon.listeners;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import net.pistonmaster.pistonchat.api.PistonChatEvent;
import net.pistonmaster.pistonchat.api.PistonWhisperEvent;
import okhttp3.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.org.twobtwot.chatbackendaddon.ChatBackendAddon;
import ru.org.twobtwot.chatbackendaddon.structs.BackendResponse;
import ru.org.twobtwot.chatbackendaddon.structs.Message;
import ru.org.twobtwot.chatbackendaddon.utils.RoundRobin;

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
        if (!plugin.getConfig().getBoolean("enable"))
            return;

        Message msg = new Message(event.getPlayer().getName(), event.getMessage());
        BackendResponse response = backendProcess(msg, 0);
        if (response.replacement != null) {
            event.setMessage(response.replacement);
        } else if (!response.allowed) {
            event.setCancelled(true);
            plugin.getLogger().info(ChatColor.RED + "<" + event.getPlayer().getName() + "> " + event.getMessage());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWhisper(PistonWhisperEvent event) {
        if (!plugin.getConfig().getBoolean("enable") || event.getSender() == event.getReceiver()) return;

        Message msg = new Message(event.getSender().getName(), event.getMessage(), event.getReceiver().getName());
        BackendResponse response = backendProcess(msg, 0);
        if (response.replacement != null) {
            event.setMessage(response.replacement);
        } else if (!response.allowed) {
            event.setCancelled(true);
            plugin.getLogger().info(ChatColor.RED + "<" + event.getSender().getName() + "> " + event.getMessage());
        }
    }

    private BackendResponse backendProcess(Message msg, int retries) {
        BackendResponse responseOnError = new BackendResponse(
                plugin.getConfig().getBoolean("allow-on-error"),
                null
        );

        if (retries > plugin.getConfig().getInt("retries"))
            return responseOnError;

        RequestBody body = RequestBody.create(messageJsonAdapter.toJson(msg), JSON);
        Request request = new Request.Builder()
                .url(servers.iterator().next())
                .post(body)
                .build();

        int timeout = plugin.getConfig().getInt("timeout");
        OkHttpClient dirtyClient = client.newBuilder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();

        try (Response res = dirtyClient.newCall(request).execute()) {
            if (!res.isSuccessful()) {
                plugin.getLogger().severe(ChatColor.RED + "Request to a backend processor failed");
                return backendProcess(msg, retries + 1);
            }
            return backendResponseJsonAdapter.fromJson(Objects.requireNonNull(res.body()).source());
        } catch (IOException e) {
            plugin.getLogger().severe(ChatColor.RED + "Request to a backend processor failed");
            return backendProcess(msg, retries + 1);
        } catch (NullPointerException e) {
            plugin.getLogger().severe(ChatColor.RED + "Response from a backend processor is empty");
            return backendProcess(msg, retries + 1);
        }
    }
}