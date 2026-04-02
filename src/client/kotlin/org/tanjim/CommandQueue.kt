package org.tanjim

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import java.util.concurrent.ConcurrentLinkedQueue

object CommandQueue {
    private val queue = ConcurrentLinkedQueue<String>()
    private var commandsPerTick = 1
    fun adjustSpeed(){
        val client= Minecraft.getInstance()
        if(client.isLocalServer){
            commandsPerTick = 100
            return
        }
        val serverBrand = client.player?.connection?.serverBrand()?.lowercase() ?: "unknown" //spigot,paper,etc
        commandsPerTick = if(serverBrand.contains("paper") || serverBrand.contains("spigot")){1} else{5}

    }

    fun add(command: String) {
        queue.add(command)
    }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.player == null) return@register

            repeat(commandsPerTick) {
                val cmd = queue.poll() ?: return@register
                client.player?.connection?.sendCommand(cmd)
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