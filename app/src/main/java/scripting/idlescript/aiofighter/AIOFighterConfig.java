package scripting.idlescript.aiofighter;

import java.util.Set;

public class AIOFighterConfig {

  private final int fightMode;

  private final Set<Integer> npcIds;

  private final int maxWander;

  private final int eatAtHp;

  private final Set<Integer> lootTable;

  public AIOFighterConfig(
      int fightMode, Set<Integer> npcIds, int maxWander, int eatAtHp, Set<Integer> lootTable) {
    this.fightMode = fightMode;
    this.npcIds = npcIds;
    this.maxWander = maxWander;
    this.eatAtHp = eatAtHp;
    this.lootTable = lootTable;
  }

  public int getFightMode() {
    return fightMode;
  }

  public Set<Integer> getNpcIds() {
    return npcIds;
  }

  public int getMaxWander() {
    return maxWander;
  }

  public int getEatAtHp() {
    return eatAtHp;
  }

  public Set<Integer> getLootTable() {
    return lootTable;
  }
}
