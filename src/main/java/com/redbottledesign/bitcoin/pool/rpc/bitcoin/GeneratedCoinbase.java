package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.github.fireduck64.sockthing.WittyRemarksAgent;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;

/**
 * Generates a stratum-compatible coinbase transaction.
 *
 * The structure is as follows:
 *
 * <pre>
 * Transaction Header [42 bytes]  | Coinbase Part #1
 * Block Height  [4 bytes]        |
 * Extra Nonce 1 [4 bytes]
 * Extra Nonce 2 [4 bytes]
 * Random Number [4 bytes]        | Coinbase Part #2
 * Misc Text     [up to 42 bytes] |
 * </pre>
 */
public class GeneratedCoinbase
extends AbstractCoinbase
{
    protected static final int COINBASE1_TX_OFFSET = 0;

    protected static final int BLOCK_HEIGHT_SCRIPT_OFFSET   = 0;
    protected static final int BLOCK_HEIGHT_BYTE_LENGTH     = 4;

    protected static final int EXTRA1_SCRIPT_OFFSET = BLOCK_HEIGHT_SCRIPT_OFFSET + BLOCK_HEIGHT_BYTE_LENGTH;
    protected static final int EXTRA1_BYTE_LENGTH   = 4;

    protected static final int EXTRA2_SCRIPT_OFFSET = EXTRA1_SCRIPT_OFFSET + EXTRA1_BYTE_LENGTH;
    protected static final int EXTRA2_BYTE_LENGTH   = 4;

    protected static final int RANDOM_SCRIPT_OFFSET = EXTRA2_SCRIPT_OFFSET + EXTRA2_BYTE_LENGTH;
    protected static final int RANDOM_BYTE_LENGTH   = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratedCoinbase.class);

    private final BigInteger blockReward;
    private final BigInteger feeTotal;

    private String wittyRemark;

    public GeneratedCoinbase(StratumServer server, long blockHeight, BigInteger blockReward, BigInteger feeTotal,
                             byte[] extraNonce1)
    {
        super(server);

        server.getConfig().require("coinbase_text");

        this.blockReward    = blockReward;
        this.feeTotal       = feeTotal;

        this.initializeCoinbaseScriptBytes();
        this.setCoinbaseBlockHeight((int)blockHeight);
        this.setExtraNonce1(extraNonce1);
        this.setExtraNonce2(new byte[EXTRA2_BYTE_LENGTH]);
        this.randomizeCoinbaseScript();
    }

    @Override
    public int getTotalCoinbaseTransactionLength()
    {
        return this.getCoinbaseTransactionBytes().length;
    }

    @Override
    public int getCoinbase1Offset()
    {
        return 0;
    }

    @Override
    public int getCoinbase1Length()
    {
        return TX_HEADER_LENGTH + BLOCK_HEIGHT_BYTE_LENGTH;
    }

    @Override
    public int getExtraNonce1Offset()
    {
        return EXTRA1_SCRIPT_OFFSET;
    }

    @Override
    public int getExtraNonce1Length()
    {
        return EXTRA1_BYTE_LENGTH;
    }

    @Override
    public int getExtraNonce2Offset()
    {
        return EXTRA2_SCRIPT_OFFSET;
    }

    @Override
    public int getExtraNonce2Length()
    {
        return EXTRA2_BYTE_LENGTH;
    }

    @Override
    public int getCoinbase2Offset()
    {
        return TX_HEADER_LENGTH + BLOCK_HEIGHT_BYTE_LENGTH + EXTRA1_BYTE_LENGTH + EXTRA2_BYTE_LENGTH;
    }

    @Override
    public int getCoinbase2Length()
    {
        //So coinbase1 size - extranonce(1+8)
        return this.getTotalCoinbaseTransactionLength() - TX_HEADER_LENGTH - BLOCK_HEIGHT_BYTE_LENGTH - EXTRA1_BYTE_LENGTH - EXTRA2_BYTE_LENGTH;
    }

    /**
     * This can be reasonably overridden to do custom things.  I'd advise against
     * messing with the script bytes, but the outputs are easily changed.
     */
    @Override
    public void regenerateCoinbaseTransaction(PoolUser user)
    {
        StratumServer       server          = this.getServer();
        NetworkParameters   networkParams   = server.getNetworkParameters();
        Transaction         newTx           = new Transaction(networkParams);
        byte[]              scriptBytes     = this.getCoinbaseScriptBytes();

        newTx.addInput(new TransactionInput(networkParams, newTx, scriptBytes));
        server.getOutputMonster().addOutputs(user, newTx, this.blockReward, this.feeTotal);

        this.setCoinbaseTransaction(newTx);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "regenerateCoinbaseTransaction() - Coinbase Part #1: %s",
                    Hex.encodeHexString(this.getCoinbasePart1())));

            LOGGER.debug(
                String.format(
                    "regenerateCoinbaseTransaction() - Coinbase Extra Nonce #1: %s",
                    Hex.encodeHexString(this.getExtraNonce1())));

            LOGGER.debug(
                String.format(
                    "regenerateCoinbaseTransaction() - Coinbase Extra Nonce #2: %s",
                    Hex.encodeHexString(this.getExtraNonce2())));

            LOGGER.debug(
                String.format(
                    "regenerateCoinbaseTransaction() - Coinbase Part #2: %s",
                    Hex.encodeHexString(this.getCoinbasePart2())));
        }
    }

    @Override
    public void markSolved()
    {
        this.markWittyRemarkUsed();
    }

    protected void initializeCoinbaseScriptBytes()
    {
        StratumServer   server      = this.getServer();
        byte[]          scriptBytes;
        String          script,
                        remark;

        //The first entries here get replaced with data.
        //They are just being put in the string so that there are some place holders for
        //The data to go.
        // BLKH - Block height
        // EXT1+2 extranonce 1 and 2 from stratum
        // RNDN - Random number to make each coinbase different, so that workers
        //        can't submit duplicates to other jobs if the EXT1 is always the same
        script = "BLKH" + "EXT1" + "EXT2" + "RNDN" + "/SockThing/" + server.getConfig().get("coinbase_text");

        remark = this.getWittyRemark(server);

        if (remark != null)
        {
            this.wittyRemark = remark;

            script = script + '/' + remark;
        }

        scriptBytes = script.getBytes();

        // Truncate to exactly 100 bytes
        if (scriptBytes.length > 100)
        {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Truncating script bytes; too long for coinbase (> 100 bytes): " + script);

            scriptBytes = new String(scriptBytes, 0, 100).getBytes();
        }

        this.setCoinbaseScriptBytes(scriptBytes);
    }

    protected void setCoinbaseBlockHeight(int blockHeight)
    {
        byte[]      scriptBytes = this.getCoinbaseScriptBytes(),
                    heightBytes = new byte[BLOCK_HEIGHT_BYTE_LENGTH];
        ByteBuffer  bb          = ByteBuffer.wrap(heightBytes);

        bb.putInt(blockHeight);
        heightBytes[0] = 3;

        if ((heightBytes == null) || (heightBytes.length != BLOCK_HEIGHT_BYTE_LENGTH))
        {
            throw new IllegalArgumentException(
                String.format("Block height must be exactly %d bytes.", BLOCK_HEIGHT_BYTE_LENGTH));
        }

        for (int i = 0; i < heightBytes.length; i++)
        {
            scriptBytes[BLOCK_HEIGHT_SCRIPT_OFFSET + i] = heightBytes[i];
        }

        // Terrible endian hack
        scriptBytes[1] = heightBytes[3];
        scriptBytes[3] = heightBytes[1];

        this.setCoinbaseScriptBytes(scriptBytes);
    }

    protected void randomizeCoinbaseScript()
    {
        byte[]  scriptBytes     = this.getCoinbaseScriptBytes(),
                randBytes       = new byte[RANDOM_BYTE_LENGTH];
        Random  randomGenerator = new Random();

        randomGenerator.nextBytes(randBytes);

        for (int i = 0; i < randBytes.length; i++)
        {
            scriptBytes[RANDOM_SCRIPT_OFFSET + i] = randBytes[i];
        }

        this.setCoinbaseScriptBytes(scriptBytes);
    }

    protected String getWittyRemark(StratumServer server)
    {
        String              remark              = null;
        WittyRemarksAgent   wittyRemarksAgent   = server.getAgent(WittyRemarksAgent.class);

        if (wittyRemarksAgent != null)
            remark = wittyRemarksAgent.getNextRemark();

        return remark;
    }

    protected void markWittyRemarkUsed()
    {
        if (this.wittyRemark != null)
            this.getServer().getAgent(WittyRemarksAgent.class).markUsed(wittyRemark);
    }
}