package us.eunoians.mcrpg.gui;

import com.diamonddagger590.mccore.exception.CorePlayerOfflineException;
import com.diamonddagger590.mccore.gui.CoreGui;
import com.diamonddagger590.mccore.gui.component.GuiPage;
import com.diamonddagger590.mccore.gui.component.PaginatedGui;
import com.diamonddagger590.mccore.util.ChainComparator;
import com.diamonddagger590.mccore.util.LinkedNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.ability.Ability;
import us.eunoians.mcrpg.ability.AbilityData;
import us.eunoians.mcrpg.ability.UnlockableAbility;
import us.eunoians.mcrpg.ability.attribute.AbilityAttribute;
import us.eunoians.mcrpg.ability.attribute.AbilityAttributeManager;
import us.eunoians.mcrpg.ability.attribute.AbilityToggledOffAttribute;
import us.eunoians.mcrpg.ability.attribute.AbilityUnlockedAttribute;
import us.eunoians.mcrpg.ability.attribute.DisplayableAttribute;
import us.eunoians.mcrpg.entity.holder.SkillHolder;
import us.eunoians.mcrpg.entity.player.McRPGPlayer;
import us.eunoians.mcrpg.skill.Skill;
import us.eunoians.mcrpg.skill.SkillRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This Gui is used to display all abilities to a player.
 */
public class AbilityGui extends CoreGui implements PaginatedGui {

    private final McRPGPlayer mcRPGPlayer;
    private final Player player;
    private final Map<AbilityGuiSortType, List<Ability>> cachedSorts;
    private LinkedNode<AbilityGuiSortType> sortTypeNode;
    private AbilityGuiPage currentPage;

    public AbilityGui(@NotNull McRPGPlayer mcRPGPlayer) {
        this.mcRPGPlayer = mcRPGPlayer;
        Optional<Player> playerOptional = mcRPGPlayer.getAsBukkitPlayer();
        if (playerOptional.isEmpty()) {
            throw new CorePlayerOfflineException(mcRPGPlayer);
        }
        this.player = playerOptional.get();
        this.cachedSorts = new HashMap<>();
        this.sortTypeNode = AbilityGuiSortType.getFirstSortType();
        this.updateGuiPage(1);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return currentPage.getInventory();
    }

    @Override
    @EventHandler(priority = EventPriority.LOWEST)
    public void handleClick(@NotNull InventoryClickEvent inventoryClickEvent) {
        Inventory inventory = inventoryClickEvent.getClickedInventory();
        if (inventoryClickEvent.getWhoClicked() instanceof Player player
                && inventory != null
                && canProcessEvent(player, inventory)) {
            inventoryClickEvent.setCancelled(true);
            int clickedSlot = inventoryClickEvent.getSlot();
            // They clicked in the ability screen
            if (clickedSlot < 45) {
                int abilitySlot = clickedSlot + ((getCurrentPage().getPageNumber() - 1) * 45);
                List<Ability> abilities = cachedSorts.get(sortTypeNode.getNodeValue());
                if (abilities.size() > abilitySlot) {
                    Ability ability = abilities.get(abilitySlot);
                    SkillHolder skillHolder = mcRPGPlayer.asSkillHolder();
                    skillHolder.getAbilityData(ability).ifPresent(abilityData -> {
                        Optional<AbilityAttribute<?>> abilityAttributeOptional = abilityData.getAbilityAttribute(AbilityAttributeManager.ABILITY_TOGGLED_OFF_ATTRIBUTE_KEY);
                        if (abilityAttributeOptional.isPresent() && abilityAttributeOptional.get() instanceof AbilityToggledOffAttribute toggledOffAttribute) {
                            AbilityToggledOffAttribute abilityToggledOffAttribute = new AbilityToggledOffAttribute(!toggledOffAttribute.getContent());
                            abilityData.addAttribute(abilityToggledOffAttribute);
                            paintGuiPage();
                        }
                    });
                }
            }
            // Otherwise check if they clicked to change the sort type
            else if (clickedSlot == 49) {
                nextAbilitySortType();
            }
        }
    }

    @Override
    public void registerListeners() {
        Bukkit.getPluginManager().registerEvents(this, McRPG.getInstance());
    }

    @Override
    public void unregisterListeners() {
        InventoryClickEvent.getHandlerList().unregister(this);
    }

    @NotNull
    @Override
    public GuiPage getCurrentPage() {
        return currentPage;
    }

    @Override
    public int getMaxPage() {
        return (int) Math.max(1, Math.ceil((double) mcRPGPlayer.asSkillHolder().getAvailableAbilities().size() / 45));
    }

    @Override
    public void setCurrentPage(@NotNull GuiPage guiPage) {
        assert guiPage instanceof AbilityGuiPage;
        this.currentPage = (AbilityGuiPage) guiPage;
    }

    @Override
    public void progressPage(boolean forward) {
        int newPage = getCurrentPage().getPageNumber() + (forward ? 1 : -1);
        updateGuiPage(newPage);
    }

    public void nextAbilitySortType() {
        sortTypeNode = sortTypeNode.getNextNode();
        updateGuiPage(getCurrentPage().getPageNumber());
        paintGuiPage();
    }

    /**
     * Update the GUI page to be a new gui page
     *
     * @param page the page to update to
     */
    private void updateGuiPage(int page) {
        AbilityGuiSortType sortType = this.sortTypeNode.getNodeValue();
        AbilityGuiPage abilityGuiPage = new AbilityGuiPage(sortType, page, player);
        if (currentPage == null || page != currentPage.getPageNumber()) {
            setCurrentPage(abilityGuiPage);
        }
        paintGuiPage();
    }

    @Override
    public void paintGuiPage() {
        paintNavigationBar();
        paintAbilities();
    }

    /**
     * Paint the bottom navigation bar of the GUI
     */
    private void paintNavigationBar() {
        MiniMessage miniMessage = McRPG.getInstance().getMiniMessage();

        ItemStack FILLER_GLASS = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = FILLER_GLASS.getItemMeta();
        meta.setDisplayName("");
        FILLER_GLASS.setItemMeta(meta);

        ItemStack SORT_BUTTON = new ItemStack(sortTypeNode.getNodeValue().getDisplayMaterial());
        ItemMeta sortMeta = SORT_BUTTON.getItemMeta();
        sortMeta.displayName(miniMessage.deserialize("<red>" + sortTypeNode.getNodeValue().getDisplayName()));
        sortMeta.lore(List.of(miniMessage.deserialize("<gray>Click to change how abilities are sorted")));
        SORT_BUTTON.setItemMeta(sortMeta);

        getInventory().setItem(49, SORT_BUTTON);

        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                getInventory().setItem(45 + i, FILLER_GLASS);
            }
        }
    }

    /**
     * Paint the abilities portion of the page
     */
    private void paintAbilities() {
        AbilityGuiSortType sortType = this.sortTypeNode.getNodeValue();
        List<Ability> abilities;
        if (cachedSorts.containsKey(sortType)) {
            abilities = cachedSorts.get(sortType);
        } else {
            abilities = mcRPGPlayer.asSkillHolder()
                    .getAvailableAbilities()
                    .stream()
                    .map(namespacedKey -> McRPG.getInstance().getAbilityRegistry().getRegisteredAbility(namespacedKey))
                    .sorted(sortType.getAbilityComparator())
                    .toList();
            cachedSorts.put(sortType, abilities);
        }

        int currentPage = getCurrentPage().getPageNumber();

        // Get the abilities that need to be displayed on this page
        int startRange = ((currentPage - 1) * 45);
        int endRange = Math.min(abilities.size(), currentPage * 45);
        abilities = abilities.subList(startRange, endRange);

        SkillRegistry skillRegistry = McRPG.getInstance().getSkillRegistry();
        SkillHolder skillHolder = mcRPGPlayer.asSkillHolder();
        MiniMessage miniMessage = McRPG.getInstance().getMiniMessage();
        Component blankLine = miniMessage.deserialize("");

        for (int i = 0; i < abilities.size(); i++) {

            Ability ability = abilities.get(i);
            ItemStack itemStack = ability.getGuiItem(mcRPGPlayer.asSkillHolder());
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.displayName(miniMessage.deserialize("<red>" + ability.getDisplayName()));

            List<Component> lore = new ArrayList<>();
            // Add skill information
            if (ability.getSkill().isPresent() && skillRegistry.isSkillRegistered(ability.getSkill().get())) {
                Skill skill = skillRegistry.getRegisteredSkill(ability.getSkill().get());
                lore.add(miniMessage.deserialize("<gray>Skill: <gold>" + skill.getDisplayName()));
            }

            // Add information about specific ability attributes
            Optional<AbilityData> abilityDataOptional = skillHolder.getAbilityData(ability);
            if (abilityDataOptional.isPresent()) {
                AbilityData abilityData = abilityDataOptional.get();
                for (AbilityAttribute<?> abilityAttribute : abilityData.getAllAttributes()) {
                    // If the attribute can be displayed
                    if (abilityAttribute instanceof DisplayableAttribute displayableAttribute) {
                        lore.add(miniMessage.deserialize("<gray>" + displayableAttribute.getDisplayName() + ": <gold>" + abilityAttribute.getContent()));
                    }
                }

                // If it is an unlockable ability, display information about unlocking it.
                if (ability instanceof UnlockableAbility unlockableAbility) {
                    abilityData.getAbilityAttribute(AbilityAttributeManager.ABILITY_UNLOCKED_ATTRIBUTE).ifPresent(abilityAttribute -> {
                        if (abilityAttribute instanceof AbilityUnlockedAttribute unlockedAttribute) {
                            if (unlockedAttribute.getContent()) {
                                lore.add(miniMessage.deserialize("<gray>You have unlocked this ability,"));
                            } else {
                                lore.add(miniMessage.deserialize("<gray>Unlock this ability when your <gold>" +
                                        skillRegistry.getRegisteredSkill(ability.getSkill().get()).getDisplayName() + " <gray>skill"));
                                lore.add(miniMessage.deserialize("<gray>reaches level <gold>" + unlockableAbility.getUnlockLevel() + "<gray>."));
                            }
                        }
                    });
                }

                // Custom handling of toggled since we enchant toggled on items
                Optional<AbilityAttribute<?>> abilityAttributeOptional = abilityData.getAbilityAttribute(AbilityAttributeManager.ABILITY_TOGGLED_OFF_ATTRIBUTE_KEY);
                if (abilityAttributeOptional.isPresent() && abilityAttributeOptional.get() instanceof AbilityToggledOffAttribute toggledOffAttribute) {
                    if (!toggledOffAttribute.getContent()) {
                        itemMeta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    lore.add(blankLine);
                    lore.add(miniMessage.deserialize("<gray>Click to toggle ability " + (toggledOffAttribute.getContent() ? "<green>on" : "<red>off")));
                }
            }
            itemMeta.lore(lore);
            itemStack.setItemMeta(itemMeta);
            // Add item to inventory
            getInventory().setItem(i, itemStack);
        }
    }

    // TODO fix some sort of caching or smth
    private static class AbilityGuiPage extends GuiPage {
        private final AbilityGuiSortType sortType;

        public AbilityGuiPage(@NotNull AbilityGuiSortType sortType, int pageNumber, @NotNull Player player) {
            super(pageNumber, Bukkit.createInventory(player, 54, McRPG.getInstance().getMiniMessage().deserialize("<gold>Skills Menu")));
            this.sortType = sortType;
        }

        @NotNull
        public AbilityGuiSortType getSortType() {
            return sortType;
        }
    }

    private enum AbilityGuiSortType {
        ALPHABETICAL(Material.BAMBOO_HANGING_SIGN, "Alphabetical Sort", Comparator.comparing(Ability::getDisplayName)),
        DEFAULT_ABILITIES(Material.REDSTONE, "Default Abilities Sort", new ChainComparator<>(ALPHABETICAL.getAbilityComparator(), Comparator.comparing(ability -> !(ability instanceof UnlockableAbility)))),
        SKILL(Material.DIAMOND_SWORD, "Sort by Skill", new ChainComparator<>(Comparator.comparing(ability -> ability.getSkill().isPresent()),
                DEFAULT_ABILITIES.getAbilityComparator(),
                // After we've sorted it so abilities with skills are put in front of abilities without skills, sort the skills by name
                (ability, ability1) -> {
                    SkillRegistry skillRegistry = McRPG.getInstance().getSkillRegistry();
                    Optional<Skill> skillOptional = Optional.ofNullable(ability.getSkill().isPresent() ? skillRegistry.getRegisteredSkill(ability.getSkill().get()) : null);
                    Optional<Skill> skillOptional1 = Optional.ofNullable(ability1.getSkill().isPresent() ? skillRegistry.getRegisteredSkill(ability1.getSkill().get()) : null);
                    // If one of these has a skill but the other doesn't, then we want to prioritize the one that has the skill first
                    if (skillOptional.isPresent() != skillOptional1.isPresent()) {
                        return skillOptional.isEmpty() ? -1 : 1;
                    }
                    // Sort by skill display names if both have skills
                    if (skillOptional.isPresent() && skillOptional1.isPresent()) {
                        Skill skill = skillOptional.get();
                        Skill skill1 = skillOptional1.get();
                        return skill.getDisplayName().compareTo(skill1.getDisplayName());
                    }
                    // Otherwise, they both don't have skills then say they're equal
                    return 0;
                },
                // Sort the abilities in alphabetical order
                ALPHABETICAL.getAbilityComparator())),
        UNLOCKED_ABILITIES(Material.DIAMOND, "Sort by Unlock Level", new ChainComparator<>(SKILL.getAbilityComparator(), Comparator.comparing(ability -> ability instanceof UnlockableAbility), (ability, ability1) -> {
            if (ability instanceof UnlockableAbility != ability1 instanceof UnlockableAbility) {
                return ability instanceof UnlockableAbility ? 1 : -1;
            }

            if (ability instanceof UnlockableAbility unlockableAbility && ability1 instanceof UnlockableAbility unlockableAbility1) {
                return Integer.compare(unlockableAbility.getUnlockLevel(), unlockableAbility1.getUnlockLevel());
            }
            return 0;
        }, ALPHABETICAL.getAbilityComparator())),
        ;

        private final static LinkedNode<AbilityGuiSortType> FIRST_SORT_TYPE = new LinkedNode<>(values()[0]);

        static {
            LinkedNode<AbilityGuiSortType> prev = FIRST_SORT_TYPE;
            // Using definition order as the link order
            for (AbilityGuiSortType type : values()) {
                // Skip our first node type
                if (type != FIRST_SORT_TYPE.getNodeValue()) {
                    LinkedNode<AbilityGuiSortType> next = new LinkedNode<>(type);
                    prev.setNext(next);
                    prev = next;
                }
            }
            // Set the tail of these linked nodes to start back at the head
            prev.setNext(FIRST_SORT_TYPE);
        }

        private final Material displayMaterial;
        private final String displayName;
        private final Comparator<Ability> abilityComparator;

        AbilityGuiSortType(@NotNull Material displayMaterial, @NotNull String displayName, @NotNull Comparator<Ability> abilityComparator) {
            this.displayMaterial = displayMaterial;
            this.displayName = displayName;
            this.abilityComparator = abilityComparator;
        }

        /**
         * Gets the {@link Material} used to display this sort.
         *
         * @return The {@link Material} used to display this sort.
         */
        @NotNull
        public Material getDisplayMaterial() {
            return this.displayMaterial;
        }

        /**
         * Gets the name to use when displaying this sort
         *
         * @return The name used to display this sort
         */
        @NotNull
        public String getDisplayName() {
            return this.displayName;
        }

        /**
         * Gets the {@link Comparator} used to sort {@link Ability Abilities}.
         *
         * @return The {@link Comparator} used to sort {@link Ability Abilities}
         */
        @NotNull
        public Comparator<Ability> getAbilityComparator() {
            return this.abilityComparator;
        }

        /**
         * Gets the first {@link LinkedNode} in the chain of sort types.
         *
         * @return The first {@link LinkedNode} in the chain of sort types.
         */
        @NotNull
        public static LinkedNode<AbilityGuiSortType> getFirstSortType() {
            return FIRST_SORT_TYPE;
        }
    }
}
