package pe.chalk.bukkit.inveitoryoverlay

import io.javalin.Javalin
import io.javalin.websocket.WsContext
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class InventoryOverlay: JavaPlugin(), Listener, CommandExecutor {
    private val port = 8850
    private var contexts = mutableSetOf<WsContext>()
    private var hostnames = mutableMapOf<UUID, String>()

    private val app = Javalin.create()
        .get("/") { it.result("not here; connect WebSocket to /ws") }
        .ws("/ws") { ws -> ws.onConnect { contexts.add(it) }; ws.onClose { contexts.remove(it) } }

    override fun onEnable() {
        this.app.start(port)
        this.server.pluginManager.registerEvents(this, this)
        this.getCommand("overlay")?.setExecutor(this)
    }

    override fun onDisable() {
        try {
            this.app.stop()
        } catch (error: Error) {
            // no error handling
        }
    }

    @EventHandler
    fun onPlayerLogin(event: PlayerLoginEvent) {
        broadcast(event, event.player)
        this.hostnames[event.player.uniqueId] = event.address.hostName
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        this.hostnames.remove(event.player.uniqueId)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            val host = "${this.hostnames[sender.uniqueId]}:${port}"
            val text = TextComponent("Click here to open overlay")
            text.clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, "http://${host}/index.html")

            sender.spigot().sendMessage(text)
        }
        return true
    }

    @EventHandler
    fun onPlayerPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity
        if (player is Player) broadcast(event, player)
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        broadcast(event, event.player)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (player is Player) broadcast(event, player)
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked
        if (player is Player) broadcast(event, player)
    }

    private val transformContents = { index: Int, item: ItemStack? ->
        if (item == null) mapOf("slot" to index, "type" to null)
        else mapOf(
            "slot" to index,
            "type" to item.type.key.key,
            "amount" to item.amount,
            "maxDurability" to item.type.maxDurability,
            "damage" to if (item is Damageable) item.damage else 0
        )
    }

    private fun broadcast(event: Event, player: Player) {
        server.scheduler.runTask(this, Runnable {
            val inventory = player.inventory
            val data = mapOf(
                "event" to event.eventName,
                "version" to description.apiVersion,
                "player" to player.displayName,
                "armor" to inventory.armorContents.mapIndexed(transformContents),
                "storage" to inventory.storageContents.mapIndexed(transformContents)
            )
            contexts.stream().filter { it.session.isOpen }.forEach { it.send(data) }
        })
    }
}