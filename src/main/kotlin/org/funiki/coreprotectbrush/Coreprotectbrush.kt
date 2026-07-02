package org.funiki.coreprotectbrush

import net.coreprotect.CoreProtect
import net.coreprotect.CoreProtectAPI
import net.coreprotect.config.Config
import org.bukkit.ChatColor
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import java.util.zip.CRC32

fun crc32(input: String): String {
    val crc = CRC32()
    crc.update(input.toByteArray(Charsets.UTF_8))
    return crc.value.toString(16).padStart(8, '0')
}

class BrushListener(private val plugin: Coreprotectbrush, val api: CoreProtectAPI, ) : Listener {
    @EventHandler
    fun onBrushUse(event: PlayerInteractEvent) {
        if (event.item?.type != Material.BRUSH) { return }
        if (event.action != Action.LEFT_CLICK_BLOCK) { return }

        event.player.sendMessage(plugin.config.getString("messages.block-history-top")?: "admin  debil")

        val block = event.clickedBlock ?: return
        val lookup = api.blockLookup(block, 60*60*24*14)

        val template = plugin.config.getString("messages.block-history-entry")?: "%index% %action% %type% %hash%"
        val salt = plugin.config.getString("salt")?: ""

        var n = 0
        for (i in lookup) {
            n += 1
            val data = api.parseResult(i)

            val raw = template
                .replace("%index%", n.toString())
                .replace("%action%", if (data.actionId == 0) "&a+" else "&c-")
                .replace("%type%", data.type.toString().lowercase())
                .replace("%hash%", crc32("${data.player}_${salt}"))
            val msg = ChatColor.translateAlternateColorCodes('&', raw)
            event.player.sendMessage(msg)
        }
    }
}

class Coreprotectbrush : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        val api: CoreProtectAPI? = getCoreProtect()
        if (api != null) {
            api.testAPI()
            server.pluginManager.registerEvents(BrushListener(this, api), this)
        } else {
            logger.warning("CoreProtect API not initialized!")
        }
    }

    private fun getCoreProtect(): CoreProtectAPI? {
        val plugin = server.pluginManager.getPlugin("CoreProtect")
        if (plugin == null || plugin !is CoreProtect) {
            return null
        }

        val api = plugin.api
        if (!api.isEnabled || api.APIVersion() < 9) {
            return null
        }

        return api
    }
    override fun onDisable() {
        // Plugin shutdown logic
    }

}
