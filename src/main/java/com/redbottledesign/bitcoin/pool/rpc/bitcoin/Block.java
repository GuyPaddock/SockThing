package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.util.List;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;

public class Block
extends com.google.bitcoin.core.Block
{

    public Block(NetworkParameters params, byte[] payloadBytes, boolean parseLazy, boolean parseRetain, int length)
    throws ProtocolException
    {
        super(params, payloadBytes, parseLazy, parseRetain, length);
    }

    public Block(NetworkParameters params, byte[] payloadBytes)
    throws ProtocolException
    {
        super(params, payloadBytes);
    }

    public Block(NetworkParameters params, long version, Sha256Hash prevBlockHash, Sha256Hash merkleRoot, long time,
                 long difficultyTarget, long nonce, List<Transaction> transactions)
    {
        super(params, version, prevBlockHash, merkleRoot, time, difficultyTarget, nonce, transactions);
    }

    @Override
    public int getOptimalEncodingMessageSize()
    {
        // Suppress default behavior
        return 0;
    }
}
