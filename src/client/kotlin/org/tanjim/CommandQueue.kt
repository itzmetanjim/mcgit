package org.tanjim

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.MinecraftClient
import java.util.concurrent.ConcurrentLinkedQueue

object CommandQueue {
    private val queue = ConcurrentLinkedQueue<String>()
    private var commandsPerTick = 1
    fun adjustSpeed(){
        val client=MinecraftClient.getInstance()
        if(client.isIntegratedServerRunning){
            commandsPerTick = 100
            return
        }
        val brand = client.player?.networkHandler?.brand?.lowercase() ?: "unknown" //spigot,paper,etc
        commandsPerTick = if(brand.contains("paper") || brand.contains("spigot")){1} else{5}

    }

    fun add(command: String) {
        queue.add(command)
    }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.player == null) return@register

            repeat(commandsPerTick) {
                val cmd = queue.poll() ?: return@register
                client.player?.networkHandler?.sendChatCommand(cmd)
            }

        }
        ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
            println("[MCGit] Joined world/server.")
            queue.clear()
            adjustSpeed()
        }
        ClientTickEvents.END_CLIENT_TICK.register{
            GitManager.processTick()
        }
    }
}