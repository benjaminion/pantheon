package net.consensys.pantheon.ethereum.p2p.rlpx.handshake.ecies;

import net.consensys.pantheon.crypto.SECP256K1;
import net.consensys.pantheon.ethereum.rlp.BytesValueRLPInput;
import net.consensys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import net.consensys.pantheon.ethereum.rlp.RLPInput;
import net.consensys.pantheon.util.bytes.Bytes32;
import net.consensys.pantheon.util.bytes.BytesValue;

public class ResponderHandshakeMessageV4 implements ResponderHandshakeMessage {

  private final SECP256K1.PublicKey ephPublicKey;

  private final Bytes32 nonce;

  public static ResponderHandshakeMessageV4 create(
      final SECP256K1.PublicKey ephPublicKey, final Bytes32 nonce) {
    return new ResponderHandshakeMessageV4(ephPublicKey, nonce);
  }

  public static ResponderHandshakeMessageV4 decode(final BytesValue raw) {
    final RLPInput input = new BytesValueRLPInput(raw, true);
    input.enterList();
    return new ResponderHandshakeMessageV4(
        SECP256K1.PublicKey.create(input.readBytesValue()), input.readBytes32());
  }

  private ResponderHandshakeMessageV4(final SECP256K1.PublicKey ephPublicKey, final Bytes32 nonce) {
    this.ephPublicKey = ephPublicKey;
    this.nonce = nonce;
  }

  @Override
  public SECP256K1.PublicKey getEphPublicKey() {
    return ephPublicKey;
  }

  @Override
  public Bytes32 getNonce() {
    return nonce;
  }

  @Override
  public BytesValue encode() {
    final BytesValueRLPOutput temp = new BytesValueRLPOutput();
    temp.startList();
    temp.writeBytesValue(ephPublicKey.getEncodedBytes());
    temp.writeBytesValue(nonce);
    temp.writeIntScalar(InitiatorHandshakeMessageV4.VERSION);
    temp.endList();
    return temp.encoded();
  }
}