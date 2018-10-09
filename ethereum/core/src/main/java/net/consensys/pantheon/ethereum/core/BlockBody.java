package net.consensys.pantheon.ethereum.core;

import net.consensys.pantheon.ethereum.rlp.RLPInput;
import net.consensys.pantheon.ethereum.rlp.RLPOutput;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BlockBody {

  private static final BlockBody EMPTY =
      new BlockBody(Collections.emptyList(), Collections.emptyList());

  private final List<Transaction> transactions;
  private final List<BlockHeader> ommers;

  public BlockBody(final List<Transaction> transactions, final List<BlockHeader> ommers) {
    this.transactions = transactions;
    this.ommers = ommers;
  }

  public static BlockBody empty() {
    return EMPTY;
  }

  /** @return The list of transactions of the block. */
  public List<Transaction> getTransactions() {
    return transactions;
  }

  /** @return The list of ommers of the block. */
  public List<BlockHeader> getOmmers() {
    return ommers;
  }

  /**
   * Writes Block to {@link RLPOutput}.
   *
   * @param output Output to write to
   */
  public void writeTo(final RLPOutput output) {
    output.startList();
    output.writeList(getTransactions(), Transaction::writeTo);
    output.writeList(getOmmers(), BlockHeader::writeTo);
    output.endList();
  }

  public static BlockBody readFrom(
      final RLPInput input, final BlockHashFunction blockHashFunction) {
    input.enterList();
    // TODO: Support multiple hard fork transaction formats.
    final BlockBody body =
        new BlockBody(
            input.readList(Transaction::readFrom),
            input.readList(rlp -> BlockHeader.readFrom(rlp, blockHashFunction)));
    input.leaveList();
    return body;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof BlockBody)) {
      return false;
    }
    final BlockBody other = (BlockBody) obj;
    return transactions.equals(other.transactions) && ommers.equals(other.ommers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactions, ommers);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("BlockBody{");
    sb.append("transactions=").append(transactions).append(", ");
    sb.append("ommers=").append(ommers);
    return sb.append("}").toString();
  }
}