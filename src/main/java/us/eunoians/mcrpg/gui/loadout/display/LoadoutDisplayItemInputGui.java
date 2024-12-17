package us.eunoians.mcrpg.gui.loadout.display;

import com.diamonddagger590.mccore.exception.CorePlayerOfflineException;
import com.diamonddagger590.mccore.exception.gui.InventoryAlreadyExistsForGuiException;
import com.diamonddagger590.mccore.gui.ClosableGui;
import com.diamonddagger590.mccore.gui.Gui;
import com.diamonddagger590.mccore.gui.slot.Slot;
import com.diamonddagger590.mccore.player.CorePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.gui.slot.loadout.display.LoadoutDisplayCancelItemEditSlot;
import us.eunoians.mcrpg.gui.slot.loadout.display.LoadoutDisplayItemConfirmSlot;
import us.eunoians.mcrpg.loadout.Loadout;

import java.util.Optional;
import java.util.Set;

/**
 * This GUI is used to allow players to input an item that they want to display
 */
public class LoadoutDisplayItemInputGui extends Gui implements ClosableGui {

    private static final Slot FILLER_GLASS_SLOT;
    private static final Slot PURPLE_GLASS_SLOT;
    private static final int INPUT_SLOT = 13;
    private static final Set<Integer> PURPLE_SLOTS = Set.of(INPUT_SLOT - 9, INPUT_SLOT - 1, INPUT_SLOT + 1, INPUT_SLOT + 9);
    private static final int RETURN_SLOT = 18;
    private static final int CONFIRM_SLOT = 26;

    // Create static slots
    static {
        // Create filler glass
        ItemStack fillerGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerGlassMeta = fillerGlass.getItemMeta();
        fillerGlassMeta.setDisplayName(" ");
        fillerGlass.setItemMeta(fillerGlassMeta);
        FILLER_GLASS_SLOT = new Slot() {

            @Override
            public boolean onClick(@NotNull CorePlayer corePlayer, @NotNull ClickType clickType) {
                return true;
            }

            @NotNull
            @Override
            public ItemStack getItem() {
                return fillerGlass;
            }
        };

        // Create purple glass
        ItemStack purpleGlass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta purpleGlassMeta = fillerGlass.getItemMeta();
        purpleGlassMeta.setDisplayName(" ");
        purpleGlass.setItemMeta(purpleGlassMeta);
        PURPLE_GLASS_SLOT = new Slot() {

            @Override
            public boolean onClick(@NotNull CorePlayer corePlayer, @NotNull ClickType clickType) {
                return true;
            }

            @NotNull
            @Override
            public ItemStack getItem() {
                return fillerGlass;
            }
        };
    }

    private final McRPGPlayer mcRPGPlayer;
    private final Player player;
    private final Loadout loadout;
    private boolean save = true;

    public LoadoutDisplayItemInputGui(@NotNull McRPGPlayer mcRPGPlayer, @NotNull Loadout loadout) {
        this.mcRPGPlayer = mcRPGPlayer;
        Optional<Player> playerOptional = mcRPGPlayer.getAsBukkitPlayer();
        if (playerOptional.isEmpty()) {
            throw new CorePlayerOfflineException(mcRPGPlayer);
        }
        this.player = playerOptional.get();
        this.loadout = loadout;
    }

    @Override
    protected void buildInventory() {
        if (this.inventory != null) {
            throw new InventoryAlreadyExistsForGuiException(this);
        } else {
            this.inventory = Bukkit.createInventory(player, 27, McRPG.getInstance().getMiniMessage().deserialize("<gold>Input Loadout Display Item"));
            paintInventory();
        }
    }

    @Override
    public void paintInventory() {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i == INPUT_SLOT) {
                continue;
            }
            setSlot(i, FILLER_GLASS_SLOT);
        }
        for (int i : PURPLE_SLOTS) {
            setSlot(i, PURPLE_GLASS_SLOT);
        }
        setSlot(RETURN_SLOT, new LoadoutDisplayCancelItemEditSlot(loadout));
        setSlot(CONFIRM_SLOT, new LoadoutDisplayItemConfirmSlot());
    }

    @Override
    public void registerListeners() {
        Bukkit.getPluginManager().registerEvents(this, McRPG.getInstance());
    }

    @Override
    public void unregisterListeners() {
        InventoryClickEvent.getHandlerList().unregister(this);
    }

    @Override
    public void onClose(InventoryCloseEvent inventoryCloseEvent) {
        Bukkit.broadcastMessage("on close");
        if (save) {
            saveLoadoutDisplayItem();
        }
        // Refund the item in the display slot but set it to air before giving it to the player
        ItemStack itemStack = inventory.getItem(INPUT_SLOT);
        inventory.setItem(INPUT_SLOT, new ItemStack(Material.AIR));
        if (itemStack != null && itemStack.getType() != Material.AIR) {
            player.getInventory().addItem(itemStack).values().forEach(leftover -> {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            });
        }
        // Open the new inventory after a tick delay
        Bukkit.getScheduler().scheduleSyncDelayedTask(McRPG.getInstance(), () -> {
            LoadoutDisplayHomeGui loadoutDisplayHomeGui = new LoadoutDisplayHomeGui(mcRPGPlayer, loadout);
            McRPG.getInstance().getGuiTracker().trackPlayerGui(mcRPGPlayer, loadoutDisplayHomeGui);
            player.openInventory(loadoutDisplayHomeGui.getInventory());
        }, 1L);
    }

    /**
     * Saves the {@link ItemStack} in the input slot as the display item for the
     * {@link Loadout}.
     */
    public void saveLoadoutDisplayItem() {
        ItemStack itemStack = inventory.getItem(INPUT_SLOT);
        if (itemStack != null && itemStack.getType() != Material.AIR) {
            loadout.getDisplay().setDisplayItem(itemStack);
        }
    }

    /**
     * Will make it so that this inventory does not save the display item when closed
     */
    public void cancelSave() {
        save = false;
    }
}
