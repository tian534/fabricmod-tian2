package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class TemplateModClient implements ClientModInitializer {
    public static boolean protectionEnabled = false;
    public static double triggerY = 30.0;
    private static boolean triggered = false;

    @Override
    public void onInitializeClient() {
        // 注册客户端指令
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("fallprotect")
                    .then(ClientCommandManager.literal("on").executes(context -> {
                        protectionEnabled = true;
                        triggered = false;
                        context.getSource().sendFeedback(Text.literal("✅ 下坠保护开启，触发Y：" + triggerY), false);
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("off").executes(context -> {
                        protectionEnabled = false;
                        context.getSource().sendFeedback(Text.literal("❌ 下坠保护关闭"), false);
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("y")
                            .then(ClientCommandManager.argument("value", net.fabricmc.fabric.api.client.command.v2.FabricClientCommandType.DOUBLE)
                                    .executes(context -> {
                                        double val = context.getArgument("value", Double.class);
                                        triggerY = val;
                                        context.getSource().sendFeedback(Text.literal("📌 触发高度设置为 Y < " + triggerY), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("status").executes(context -> {
                        String state = protectionEnabled ? "✅ 已开启" : "❌ 已关闭";
                        context.getSource().sendFeedback(Text.literal("📋 保护状态："+state+"，触发阈值Y<"+triggerY), false);
                        return 1;
                    }))
            );
        });

        // 每tick检测
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            PlayerEntity player = client.player;

            if (!protectionEnabled || triggered) return;

            if (player.getY() < triggerY) {
                triggered = true;
                protectionEnabled = false;
                player.sendMessage(Text.literal("⚠️ Y过低！执行保护：下船并断开连接"), true);

                if (player.hasVehicle()) {
                    player.stopRiding();
                }
                client.disconnect();
            }
        });
    }
}
