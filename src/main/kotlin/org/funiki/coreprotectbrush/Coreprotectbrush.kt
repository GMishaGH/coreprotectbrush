package org.funiki.coreprotectbrush

import net.coreprotect.CoreProtect
import net.coreprotect.CoreProtectAPI
import net.coreprotect.config.Config
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import java.util.logging.Level
import java.util.zip.CRC32
import kotlin.math.log

fun crc32(input: String): String {
    val crc = CRC32()
    crc.update(input.toByteArray(Charsets.UTF_8))
    return crc.value.toString(16).padStart(8, '0')
}

class BrushListener(private val plugin: Coreprotectbrush, val api: CoreProtectAPI, ) : Listener {
    var playersBlocks: MutableMap<String, Pair<Location, Int>> = mutableMapOf()

    @EventHandler
    fun onBrushUse(event: PlayerInteractEvent) {
        if (event.item?.type != Material.BRUSH) { return }
        if (event.action != Action.RIGHT_CLICK_BLOCK) { return }

        event.player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.config.getString("messages.block-history-top")?: "admin  debil"))

        val block = event.clickedBlock ?: return
        val lookup = api.blockLookup(block, plugin.config.getInt("lookup-time-seconds")?: 1209600)

        val template = plugin.config.getString("messages.block-history-entry")?: "%index% %action% %type% %hash%"
        val salt = plugin.config.getString("salt")?: ""

        var (lastBlock, iterValue) = playersBlocks[event.player.uniqueId.toString()]?: Pair(block.location, 0)
        if (lastBlock != block.location) {
            event.player.server.logger.info("ANOTHER BLOCK")
            iterValue = 0
        }
        iterValue++
        var n = 0
        for (i in lookup) {
            n += 1
            event.player.server.logger.info("${iterValue} ${n}")
            if ((iterValue - 1) * 5 >= n || n > (iterValue * 5)) { continue; }
            val data = api.parseResult(i)
            val raw = template
                .replace("%index%", n.toString())
                .replace("%action%", if (data.actionId == 0) "&c-" else "&a+")
                .replace("%type%", data.type.toString().lowercase())
                .replace("%hash%", crc32("${data.player}_${salt}"))
            val msg = ChatColor.translateAlternateColorCodes('&', raw)
            event.player.sendMessage(msg)
        }
        playersBlocks[event.player.uniqueId.toString()] = Pair(block.location, iterValue)
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val target = event.rightClicked

        val template = plugin.config.getString("messages.fingerprint")?: "%hash%"
        val salt = plugin.config.getString("salt")?: ""
        var targetName = target.name
        if (target !is Player) {
            targetName = "#" + target.type.name.lowercase()
        }
        val raw = template.replace("%hash%", crc32("${targetName}_${salt}"))
        val msg = ChatColor.translateAlternateColorCodes('&', raw)
        event.player.sendMessage(msg)

    }
}

class Coreprotectbrush : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        val api: CoreProtectAPI? = getCoreProtect()
        if (api != null) {
            logger.info("Testing CoreProtect API")
            api.testAPI()
            logger.info("CoreProtectAPI tested")
            server.pluginManager.registerEvents(BrushListener(this, api), this)
            logger.info( "Listeners started")
            logger.info("Done.")
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
