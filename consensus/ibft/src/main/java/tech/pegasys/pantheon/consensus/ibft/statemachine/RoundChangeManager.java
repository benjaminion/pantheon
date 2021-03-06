/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.consensus.ibft.statemachine;

import static tech.pegasys.pantheon.consensus.ibft.IbftHelpers.prepareMessageCountForQuorum;

import tech.pegasys.pantheon.consensus.ibft.ConsensusRoundIdentifier;
import tech.pegasys.pantheon.consensus.ibft.IbftHelpers;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.RoundChangeCertificate;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.RoundChangePayload;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.SignedData;
import tech.pegasys.pantheon.consensus.ibft.validation.RoundChangeMessageValidator;
import tech.pegasys.pantheon.consensus.ibft.validation.RoundChangeMessageValidator.MessageValidatorForHeightFactory;
import tech.pegasys.pantheon.ethereum.core.Address;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Responsible for handling all RoundChange messages received for a given block height
 * (theoretically, RoundChange messages for a older height should have been previously discarded,
 * and messages for a future round should have been buffered).
 *
 * <p>If enough RoundChange messages all targeting a given round are received (and this node is the
 * proposer for said round) - a newRound message is sent, and a new round should be started by the
 * controlling class.
 */
public class RoundChangeManager {

  public static class RoundChangeStatus {
    private final int quorumSize;

    // Store only 1 round change per round per validator
    @VisibleForTesting
    final Map<Address, SignedData<RoundChangePayload>> receivedMessages = Maps.newLinkedHashMap();

    private boolean actioned = false;

    public RoundChangeStatus(final int quorumSize) {
      this.quorumSize = quorumSize;
    }

    public void addMessage(final SignedData<RoundChangePayload> msg) {
      if (!actioned) {
        receivedMessages.putIfAbsent(msg.getSender(), msg);
      }
    }

    public boolean roundChangeReady() {
      return receivedMessages.size() >= quorumSize && !actioned;
    }

    public RoundChangeCertificate createRoundChangeCertificate() {
      if (roundChangeReady()) {
        actioned = true;
        return new RoundChangeCertificate(receivedMessages.values());
      } else {
        throw new IllegalStateException("Unable to create RoundChangeCertificate at this time.");
      }
    }
  }

  private static final Logger LOG = LogManager.getLogger();

  @VisibleForTesting
  final Map<ConsensusRoundIdentifier, RoundChangeStatus> roundChangeCache = Maps.newHashMap();

  private final int quorumSize;
  private final RoundChangeMessageValidator roundChangeMessageValidator;

  public RoundChangeManager(
      final long sequenceNumber,
      final Collection<Address> validators,
      final MessageValidatorForHeightFactory messageValidityFactory) {
    this.quorumSize = IbftHelpers.calculateRequiredValidatorQuorum(validators.size());
    this.roundChangeMessageValidator =
        new RoundChangeMessageValidator(
            messageValidityFactory,
            validators,
            prepareMessageCountForQuorum(quorumSize),
            sequenceNumber);
  }

  /**
   * Adds the round message to this manager and return a certificate if it passes the threshold
   *
   * @param msg The signed round change message to add
   * @return Empty if the round change threshold hasn't been hit, otherwise a round change
   *     certificate
   */
  public Optional<RoundChangeCertificate> appendRoundChangeMessage(
      final SignedData<RoundChangePayload> msg) {

    if (!isMessageValid(msg)) {
      LOG.info("RoundChange message was invalid.");
      return Optional.empty();
    }

    final RoundChangeStatus roundChangeStatus = storeRoundChangeMessage(msg);

    if (roundChangeStatus.roundChangeReady()) {
      return Optional.of(roundChangeStatus.createRoundChangeCertificate());
    }

    return Optional.empty();
  }

  private boolean isMessageValid(final SignedData<RoundChangePayload> msg) {
    return roundChangeMessageValidator.validateMessage(msg);
  }

  private RoundChangeStatus storeRoundChangeMessage(final SignedData<RoundChangePayload> msg) {
    final ConsensusRoundIdentifier msgTargetRound = msg.getPayload().getRoundIdentifier();

    final RoundChangeStatus roundChangeStatus =
        roundChangeCache.computeIfAbsent(
            msgTargetRound, ignored -> new RoundChangeStatus(quorumSize));

    roundChangeStatus.addMessage(msg);

    return roundChangeStatus;
  }

  /**
   * Clears old rounds from storage that have been superseded by a given round
   *
   * @param completedRoundIdentifier round identifier that has been identified as superseded
   */
  public void discardRoundsPriorTo(final ConsensusRoundIdentifier completedRoundIdentifier) {
    roundChangeCache.keySet().removeIf(k -> isAnEarlierRound(k, completedRoundIdentifier));
  }

  private boolean isAnEarlierRound(
      final ConsensusRoundIdentifier left, final ConsensusRoundIdentifier right) {
    return left.getRoundNumber() < right.getRoundNumber();
  }
}
