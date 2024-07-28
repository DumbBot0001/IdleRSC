package scripting.idlescript.other.AIOAIO.smelting;

import static bot.Main.log;

import bot.Main;
import com.google.common.collect.ImmutableMap;
import com.openrsc.client.entityhandling.instances.Item;
import controller.Controller;
import java.util.Map;
import java.util.stream.Collectors;
import models.entities.ItemId;
import scripting.idlescript.other.AIOAIO.AIOAIO;
import scripting.idlescript.other.AIOAIO.AIOAIO_Script_Utils;

public class Smelt {

  private static final Map<String, Map<Integer, OreConfig>> BAR_TYPE_TO_ORE_CONFIGS_AUTHENTIC =
      ImmutableMap.<String, Map<Integer, OreConfig>>builder()
          .put(
              "Bronze bar",
              ImmutableMap.<Integer, OreConfig>builder()
                  .put(ItemId.COPPER_ORE.getId(), new OreConfig(14, 15, 1))
                  .put(ItemId.TIN_ORE.getId(), new OreConfig(14, 15, 1))
                  .build())
          .put(
              "Iron bar",
              ImmutableMap.<Integer, OreConfig>builder()
                  .put(ItemId.IRON_ORE.getId(), new OreConfig(29, 30, 1))
                  .build())
          .put(
              "Silver bar",
              ImmutableMap.<Integer, OreConfig>builder()
                  .put(ItemId.SILVER.getId(), new OreConfig(29, 30, 1))
                  .build())
          .put(
              "Steel bar",
              ImmutableMap.<Integer, OreConfig>builder()
                  .put(ItemId.COAL.getId(), new OreConfig(18, 20, 2))
                  .put(ItemId.IRON_ORE.getId(), new OreConfig(9, 10, 1))
                  .build())
          .put(
              "Gold bar",
              ImmutableMap.<Integer, OreConfig>builder()
                  .put(ItemId.GOLD.getId(), new OreConfig(29, 30, 1))
                  .build())
          .put(
              "Mithril bar",
              ImmutableMap.<Integer, OreConfig>builder()
                  .put(ItemId.COAL.getId(), new OreConfig(20, 24, 4))
                  .put(ItemId.MITHRIL_ORE.getId(), new OreConfig(5, 6, 1))
                  .build())
          .put(
              "Adamantite bar",
              ImmutableMap.<Integer, OreConfig>builder()
                  .put(ItemId.COAL.getId(), new OreConfig(24, 24, 6))
                  .put(ItemId.ADAMANTITE_ORE.getId(), new OreConfig(4, 4, 1))
                  .build())
          .put(
              "Runite bar",
              ImmutableMap.<Integer, OreConfig>builder()
                  .put(ItemId.COAL.getId(), new OreConfig(24, 24, 8))
                  .put(ItemId.RUNITE_ORE.getId(), new OreConfig(3, 3, 1))
                  .build())
          .build();

  public static int run() {
    final Controller c = Main.getController();
    c.setBatchBarsOn();

    final Map<Integer, OreConfig> oreIdToOreConfig = getSmeltingItemRequirements(c);

    if (c.isAuthentic()) {
      if (!meetsReqs(c)) {
        c.log("Missing required level to smelt " + AIOAIO.state.currentTask.getName());
        AIOAIO.state.endTime = System.currentTimeMillis();
        return 50;
      } else if (!isInSmeltingRoom(c)) {
        walkTowardSmeltingRoom(c);
      } else if (hasMinimumSmeltingItemsInInventory(c, oreIdToOreConfig)) {
        performSmelt(c, oreIdToOreConfig);
      } else {
        withdrawSmeltingItems(c, oreIdToOreConfig);
      }
    } else {
      if (!meetsReqs(c)) {
        c.log("Missing required level to smelt " + AIOAIO.state.currentTask.getName());
        AIOAIO.state.endTime = System.currentTimeMillis();
        return 50;
      } else if (c.isBatching()) {
        return 680; // Wait to finish fishing
      } else if (!hasFullSmeltingItemsInInventory(c, oreIdToOreConfig)) {
        withdrawSmeltingItems(c, oreIdToOreConfig);
      } else {
        startSmeltingInauthentic(c, oreIdToOreConfig);
      }
    }
    return 250;
  }

  private static void withdrawSmeltingItems(
      Controller c, Map<Integer, OreConfig> oreIdToOreConfig) {
    AIOAIO.state.status = ("Withdrawing smelting items");
    if (!c.isInBank()) {
      AIOAIO_Script_Utils.towardsOpenBank();
    }

    Map<Integer, Integer> inventoryItemIdToQuantity =
        c.getInventoryItems().stream()
            .collect(
                Collectors.groupingBy(
                    item -> item.getItemDef().id, Collectors.summingInt(Item::getAmount)));

    // Deposit any excessive items I have in inven
    inventoryItemIdToQuantity.forEach(
        (itemId, totalAmount) -> {
          int requiredAmount = c.isAuthentic() && itemId == ItemId.SLEEPING_BAG.getId() ? 1 : 0;

          int excessAmount = totalAmount - requiredAmount;
          if (excessAmount > 0) {
            log("Depositing %d of item %d", excessAmount, itemId);
            c.depositItem(itemId, excessAmount);
            c.sleep(600 * 2);
          }
        });

    if (c.isAuthentic()
        && c.getInventoryItemCount(ItemId.SLEEPING_BAG.getId()) == 0
        && !AIOAIO_Script_Utils.towardsGetFromBank(ItemId.SLEEPING_BAG, 1, false)) {
      log("Sleeping bag missing from bank - might get stuck!");
    }

    // Withdraw any needed items
    oreIdToOreConfig.forEach(
        (oreId, oreConfig) -> {
          final int quantityToWithdrawFromBank =
              oreConfig.getQuantityToWithdrawFromBank(c.isAuthentic())
                  - c.getInventoryItemCount(oreId);
          if (c.getInventoryItemCount(oreId) >= quantityToWithdrawFromBank) {
            return;
          }
          if (!AIOAIO_Script_Utils.towardsGetFromBank(
              ItemId.getById(oreId), quantityToWithdrawFromBank, false)) {
            c.log(
                "Missing required items to smelt "
                    + AIOAIO.state.currentTask.getName()
                    + "; Need "
                    + quantityToWithdrawFromBank
                    + " "
                    + ItemId.getById(oreId).name()
                    + ", only have "
                    + c.getBankItemCount(oreId));
            AIOAIO.state.endTime = System.currentTimeMillis();
          }
        });
  }

  private static boolean hasFullSmeltingItemsInInventory(
      Controller c, Map<Integer, OreConfig> oreIdToOreConfig) {
    return oreIdToOreConfig.entrySet().stream()
        .allMatch(
            e ->
                c.getInventoryItemCount(e.getKey())
                    >= e.getValue().getQuantityToWithdrawFromBank(c.isAuthentic()));
  }

  private static boolean hasMinimumSmeltingItemsInInventory(
      Controller c, Map<Integer, OreConfig> oreIdToOreConfig) {
    return oreIdToOreConfig.entrySet().stream()
        .allMatch(
            e -> c.getInventoryItemCount(e.getKey()) >= e.getValue().getMinimumQuantityToSmelt());
  }

  /*
   * Returns a pair of itemId : amount
   */
  private static Map<Integer, OreConfig> getSmeltingItemRequirements(Controller c) {

    final Map<Integer, OreConfig> oreItemIdsAndQuantities;
    if (c.isAuthentic()) {
      oreItemIdsAndQuantities =
          BAR_TYPE_TO_ORE_CONFIGS_AUTHENTIC.get(AIOAIO.state.currentTask.getName());
    } else {
      throw new RuntimeException("Unimpl");
    }

    if (oreItemIdsAndQuantities == null) {
      throw new IllegalStateException("Unknown bar type: " + AIOAIO.state.currentTask.getName());
    }
    return oreItemIdsAndQuantities;
  }

  private static boolean meetsReqs(Controller c) {
    // TODO put in map
    switch (AIOAIO.state.currentTask.getName()) {
      case "Bronze bar":
        return c.getCurrentStat(c.getStatId("Smithing")) >= 1;
      case "Iron bar":
        return c.getCurrentStat(c.getStatId("Smithing")) >= 15;
      case "Silver bar":
        return c.getCurrentStat(c.getStatId("Smithing")) >= 20;
      case "Steel bar":
        return c.getCurrentStat(c.getStatId("Smithing")) >= 30;
      case "Gold bar":
        return c.getCurrentStat(c.getStatId("Smithing")) >= 40;
      case "Mithril bar":
        return c.getCurrentStat(c.getStatId("Smithing")) >= 50;
      case "Adamantite bar":
        return c.getCurrentStat(c.getStatId("Smithing")) >= 70;
      case "Runite bar":
        return c.getCurrentStat(c.getStatId("Smithing")) >= 85;
      default:
        throw new IllegalStateException(
            "Unknown smithing task: " + AIOAIO.state.currentTask.getName());
    }
  }

  private static boolean isInSmeltingRoom(Controller c) {
    return c.getNearestObjectById(118) != null;
  }

  private static void walkTowardSmeltingRoom(Controller c) {
    c.walkTowards(311, 545);
  }

  private static void performSmelt(Controller c, Map<Integer, OreConfig> oreConfigs) {
    int oreId = oreConfigs.keySet().stream().findAny().get();
    c.useItemIdOnObject(c.getNearestObjectById(118)[0], c.getNearestObjectById(118)[1], oreId);
    c.sleepUntilGainedXp();
    AIOAIO.state.status = "Smelting " + AIOAIO.state.currentTask.getName() + "...";
  }

  private static void startSmeltingInauthentic(Controller c, Map<Integer, OreConfig> oreConfigs) {
    AIOAIO.state.status = "Starting to smelt " + AIOAIO.state.currentTask.getName();
    if (!isInSmeltingRoom(c)) {
      walkTowardSmeltingRoom(c);
      return;
    }
    performSmelt(c, oreConfigs);
  }
}
