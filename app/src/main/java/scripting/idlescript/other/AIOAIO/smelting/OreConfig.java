package scripting.idlescript.other.AIOAIO.smelting;

import java.util.Objects;

public class OreConfig {

  private final int quantityToWithdrawFromBankAuthentic;

  private final int quantityToWithdrawFromBankInauthentic;

  private final int minimumQuantityToSmelt;

  public OreConfig(
      int quantityToWithdrawFromBankAuthentic,
      int quantityToWithdrawFromBankInauthentic,
      int minimumQuantityToSmelt) {
    this.quantityToWithdrawFromBankAuthentic = quantityToWithdrawFromBankAuthentic;
    this.quantityToWithdrawFromBankInauthentic = quantityToWithdrawFromBankInauthentic;
    this.minimumQuantityToSmelt = minimumQuantityToSmelt;
  }

  public int getQuantityToWithdrawFromBank(boolean authentic) {
    return authentic ? quantityToWithdrawFromBankAuthentic : quantityToWithdrawFromBankInauthentic;
  }

  public int getMinimumQuantityToSmelt() {
    return minimumQuantityToSmelt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OreConfig oreConfig = (OreConfig) o;
    return quantityToWithdrawFromBankAuthentic == oreConfig.quantityToWithdrawFromBankAuthentic
        && quantityToWithdrawFromBankInauthentic == oreConfig.quantityToWithdrawFromBankInauthentic
        && minimumQuantityToSmelt == oreConfig.minimumQuantityToSmelt;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        quantityToWithdrawFromBankAuthentic,
        quantityToWithdrawFromBankInauthentic,
        minimumQuantityToSmelt);
  }
}
