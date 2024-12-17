package us.eunoians.mcrpg.quest;

import com.diamonddagger590.mccore.database.Database;
import com.diamonddagger590.mccore.database.transaction.FailSafeTransaction;
import net.kyori.adventure.audience.Audience;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.ability.attribute.AbilityAttributeManager;
import us.eunoians.mcrpg.ability.attribute.AbilityTierAttribute;
import us.eunoians.mcrpg.ability.impl.Ability;
import us.eunoians.mcrpg.ability.impl.TierableAbility;
import us.eunoians.mcrpg.database.table.SkillDAO;
import us.eunoians.mcrpg.entity.holder.SkillHolder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * A placeholder quest reward that is ran whenever a player finishes their upgrade quest for a given ability
 */
public class UpgradeQuestReward implements QuestReward {

    @Override
    public void giveReward(@NotNull UUID uuid, @NotNull Quest quest) {
        var abilityHolderOptional = McRPG.getInstance().getEntityManager().getAbilityHolder(uuid);
        abilityHolderOptional.ifPresent(abilityHolder -> {
            NamespacedKey namespacedKey = new NamespacedKey(McRPG.getInstance(), quest.getConfigKey());
            abilityHolder.getAbilityData(namespacedKey).ifPresent(abilityData -> {
                abilityData.getAbilityAttribute(AbilityAttributeManager.ABILITY_TIER_ATTRIBUTE_KEY).ifPresent(abilityAttribute -> {
                    Ability ability = McRPG.getInstance().getAbilityRegistry().getRegisteredAbility(namespacedKey);
                    if (ability instanceof TierableAbility tierableAbility && abilityHolder instanceof SkillHolder skillHolder) {
                        int newTier = Math.min(tierableAbility.getMaxTier(), (int) abilityAttribute.getContent() + 1);
                        abilityData.addAttribute(new AbilityTierAttribute(newTier));
                        abilityData.removeAttribute(AbilityAttributeManager.ABILITY_QUEST_ATTRIBUTE);
                        Database database = McRPG.getInstance().getDatabase();
                        database.getDatabaseExecutorService().submit(() -> {
                            try (Connection connection = database.getConnection()) {
                                new FailSafeTransaction(connection, SkillDAO.savePlayerAbilityAttributes(connection, skillHolder)).executeTransaction();
                            }
                            catch (SQLException e) {
                                e.printStackTrace();
                            }
                        });
                        Audience audience = McRPG.getInstance().getAdventure().player(uuid);
                        audience.sendMessage(McRPG.getInstance().getMiniMessage().deserialize(String.format("<green>You have completed the upgrade quest for your <gold>%s ability<green>! It is now tier <gold>%d<green>.", ability.getDisplayName(), newTier)));
                    }
                });
            });
        });
    }
}
