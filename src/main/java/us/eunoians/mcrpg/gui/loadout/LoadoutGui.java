package us.eunoians.mcrpg.gui.loadout;

import com.diamonddagger590.mccore.gui.slot.Slot;
import com.diamonddagger590.mccore.player.CorePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.ability.impl.Ability;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.gui.ability.AbilitySortType;
import us.eunoians.mcrpg.gui.ability.PaginatedSortedAbilityGui;
import us.eunoians.mcrpg.gui.slot.loadout.InvalidLoadoutSlot;
import us.eunoians.mcrpg.gui.slot.loadout.LoadoutAbilitySlot;
import us.eunoians.mcrpg.gui.slot.loadout.LoadoutHomeSlot;
import us.eunoians.mcrpg.gui.slot.loadout.display.LoadoutDisplayHomeSlot;
import us.eunoians.mcrpg.loadout.Loadout;

import java.util.List;
import java.util.Set;

/**
 * This gui is a visual representation of a player's {@link Loadout}, showcasing the {@link Ability Abilities}
 * a player has in it and letting them click to change out the ability for a new one.
 */
public class LoadoutGui extends PaginatedSortedAbilityGui {

    private static final Slot FILLER_GLASS_SLOT;
    private static final int ABILITY_DISPLAY_SIZE = 18;
    private static final int NAVIGATION_ROW_START_INDEX = ABILITY_DISPLAY_SIZE;
    private static final int LOADOUT_SELECTION_SLOT_INDEX = NAVIGATION_ROW_START_INDEX;
    private static final int PREVIOUS_PAGE_SLOT_INDEX = NAVIGATION_ROW_START_INDEX + 2;
    private static final int SORT_SLOT_INDEX = NAVIGATION_ROW_START_INDEX + 4;
    private static final int NEXT_PAGE_SLOT_INDEX = NAVIGATION_ROW_START_INDEX + 6;
    private static final int LOADOUT_DISPLAY_EDIT_SLOT = NAVIGATION_ROW_START_INDEX + 8;

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
    }

    private final Loadout loadout;

    public LoadoutGui(@NotNull McRPGPlayer mcRPGPlayer, @NotNull Loadout loadout) {
        super(mcRPGPlayer);
        this.loadout = loadout;
    }

    @NotNull
    public Loadout getLoadout() {
        return loadout;
    }

    @NotNull
    @Override
    protected Inventory getInventoryForPage(int page) {
        return Bukkit.createInventory(getPlayer(), 27, McRPG.getInstance().getMiniMessage().deserialize("<gold>Editing Loadout " + loadout.getLoadoutSlot()));
    }

    @Override
    public int getNavigationRowStartIndex() {
        return NAVIGATION_ROW_START_INDEX;
    }

    @Override
    public @NotNull Set<NamespacedKey> getUnsortedAbilities() {
        return loadout.getAbilities();
    }

    @Override
    protected void paintAbilities(int page) {
        List<Ability> sortedAbilities = getSortedAbilitiesForPage(page);
        int difference = loadout.getAbilities().size() - sortedAbilities.size();
        int totalLoadoutSize = loadout.getAbilities().size() + loadout.getRemainingLoadoutSize() - difference;
        for (int i = 0; i < NAVIGATION_ROW_START_INDEX; i++) {
            if (i < sortedAbilities.size()) {
                setSlot(i, new LoadoutAbilitySlot(getMcRPGPlayer(), loadout, sortedAbilities.get(i)));
            } else if (i < totalLoadoutSize) {
                setSlot(i, new LoadoutAbilitySlot(getMcRPGPlayer(), loadout));
            } else {
                setSlot(i, new InvalidLoadoutSlot());
            }
        }
    }

    @Override
    protected void paintNavigationBar(int page) {
        // Paint the nav bar with filler glass
        for (int i = 0; i < 9; i++) {
            setSlot(NAVIGATION_ROW_START_INDEX + i, FILLER_GLASS_SLOT);
        }
        // Set the back slot
        setSlot(LOADOUT_SELECTION_SLOT_INDEX, new LoadoutHomeSlot(getMcRPGPlayer()));
        // Set the sort slot
        setSlot(SORT_SLOT_INDEX, getAbilitySortNode().getNodeValue().getSlot());
        // If the page is not the first page, then we need to put a previous arrow button
        if (page > 1) {
            setSlot(PREVIOUS_PAGE_SLOT_INDEX, PREVIOUS_PAGE_SLOT);
        }
        // If the page is not the max page, then we need to put a next arrow button
        if (page < getMaximumPage()) {
            setSlot(NEXT_PAGE_SLOT_INDEX, NEXT_PAGE_SLOT);
        }
        // Set the toggle loadout slot
        setSlot(LOADOUT_DISPLAY_EDIT_SLOT, new LoadoutDisplayHomeSlot(getLoadout()));
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
    protected @NotNull Set<AbilitySortType> getSkippedSortTypes() {
        return Set.of(AbilitySortType.DEFAULT_ABILITIES, AbilitySortType.UNLOCKED_ABILITIES, AbilitySortType.UPGRADEABLE_ABILITIES);
    }
}
