package net.consensys.pantheon.consensus.common;

import static net.consensys.pantheon.consensus.common.VoteType.ADD;
import static net.consensys.pantheon.consensus.common.VoteType.DROP;
import static org.assertj.core.api.Assertions.assertThat;

import net.consensys.pantheon.consensus.common.VoteProposer.Vote;
import net.consensys.pantheon.ethereum.core.Address;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

public class VoteProposerTest {
  private final Address localAddress = Address.fromHexString("0");

  @Test
  public void emptyProposerReturnsNoVotes() {
    final VoteProposer proposer = new VoteProposer();

    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.empty());
    assertThat(
            proposer.getVote(
                localAddress,
                new VoteTally(
                    Arrays.asList(
                        Address.fromHexString("0"),
                        Address.fromHexString("1"),
                        Address.fromHexString("2")))))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void demoteVotes() {
    final VoteProposer proposer = new VoteProposer();
    final Address a1 = Address.fromHexString("1");
    final Address a2 = Address.fromHexString("2");
    final Address a3 = Address.fromHexString("3");

    proposer.drop(a1);

    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.empty());
    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.singletonList(a1))))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.DROP)));
    assertThat(proposer.getVote(localAddress, new VoteTally(Arrays.asList(a1, a2, a3))))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.DROP)));
    assertThat(proposer.getVote(localAddress, new VoteTally(Arrays.asList(a2, a3))))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void promoteVotes() {
    final VoteProposer proposer = new VoteProposer();
    final Address a1 = Address.fromHexString("1");
    final Address a2 = Address.fromHexString("2");
    final Address a3 = Address.fromHexString("3");

    proposer.auth(a1);

    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.AUTH)));
    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.singletonList(a1))))
        .isEqualTo(Optional.empty());
    assertThat(proposer.getVote(localAddress, new VoteTally(Arrays.asList(a1, a2, a3))))
        .isEqualTo(Optional.empty());
    assertThat(proposer.getVote(localAddress, new VoteTally(Arrays.asList(a2, a3))))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.AUTH)));
  }

  @Test
  public void discardVotes() {
    final VoteProposer proposer = new VoteProposer();
    final Address a1 = Address.fromHexString("1");
    final Address a2 = Address.fromHexString("2");
    final Address a3 = Address.fromHexString("3");

    proposer.auth(a1);
    proposer.auth(a2);
    proposer.discard(a2);

    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.AUTH)));
    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.singletonList(a1))))
        .isEqualTo(Optional.empty());
    assertThat(proposer.getVote(localAddress, new VoteTally(Arrays.asList(a1, a2, a3))))
        .isEqualTo(Optional.empty());
    assertThat(proposer.getVote(localAddress, new VoteTally(Arrays.asList(a2, a3))))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.AUTH)));
  }

  @Test
  public void getVoteCyclesAllOptions() {
    final VoteProposer proposer = new VoteProposer();
    final Address a1 = Address.fromHexString("1");
    final Address a2 = Address.fromHexString("2");
    final Address a3 = Address.fromHexString("3");

    proposer.auth(a1);
    proposer.auth(a2);
    proposer.auth(a3);

    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a2, Vote.AUTH)));
    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a3, Vote.AUTH)));
    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.AUTH)));
    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a2, Vote.AUTH)));
  }

  @Test
  public void getVoteSkipsInvalidVotes() {
    final VoteProposer proposer = new VoteProposer();
    final Address a1 = Address.fromHexString("1");
    final Address a2 = Address.fromHexString("2");
    final Address a3 = Address.fromHexString("3");
    final Address a4 = Address.fromHexString("4");

    proposer.auth(a1);
    proposer.auth(a2);
    proposer.auth(a3);
    proposer.drop(a4);

    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a2, Vote.AUTH)));
    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a3, Vote.AUTH)));
    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.AUTH)));
    assertThat(proposer.getVote(localAddress, new VoteTally(Collections.emptyList())))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a2, Vote.AUTH)));
  }

  @Test
  public void revokesAuthVotesInTally() {
    final VoteProposer proposer = new VoteProposer();
    final Address a1 = Address.fromHexString("1");
    final Address a2 = Address.fromHexString("2");
    final Address a3 = Address.fromHexString("3");

    proposer.drop(a1);

    final VoteTally tally = new VoteTally(Arrays.asList(a2, a3));
    tally.addVote(localAddress, a1, ADD);

    assertThat(proposer.getVote(localAddress, tally))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.DROP)));
  }

  @Test
  public void revokesDropVotesInTally() {
    final VoteProposer proposer = new VoteProposer();
    final Address a1 = Address.fromHexString("1");
    final Address a2 = Address.fromHexString("2");
    final Address a3 = Address.fromHexString("3");

    proposer.auth(a1);

    final VoteTally tally = new VoteTally(Arrays.asList(a2, a3));
    tally.addVote(localAddress, a1, DROP);

    assertThat(proposer.getVote(localAddress, tally))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.AUTH)));
  }

  @Test
  public void revokesAuthVotesInTallyWhenValidatorIsInValidatorList() {
    final VoteProposer proposer = new VoteProposer();
    final Address a1 = Address.fromHexString("1");
    final Address a2 = Address.fromHexString("2");
    final Address a3 = Address.fromHexString("3");

    final VoteTally tally = new VoteTally(Arrays.asList(a1, a2, a3));
    tally.addVote(localAddress, a1, ADD);

    proposer.drop(a1);

    assertThat(proposer.getVote(localAddress, tally))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.DROP)));
  }

  @Test
  public void revokesDropVotesInTallyWhenValidatorIsInValidatorList() {
    final VoteProposer proposer = new VoteProposer();
    final Address a1 = Address.fromHexString("1");
    final Address a2 = Address.fromHexString("2");
    final Address a3 = Address.fromHexString("3");

    final VoteTally tally = new VoteTally(Arrays.asList(a1, a2, a3));
    tally.addVote(localAddress, a1, DROP);

    proposer.auth(a1);

    assertThat(proposer.getVote(localAddress, tally))
        .isEqualTo(Optional.of(new AbstractMap.SimpleEntry<>(a1, Vote.AUTH)));
  }
}