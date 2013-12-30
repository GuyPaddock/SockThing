package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.github.fireduck64.sockthing.WittyRemarksAgent;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

/**
 * Creates a stratum compatible coinbase transaction
 */
public class GeneratedCoinbase
extends AbstractCoinbase
{
    protected static final int RANDOM_OFF = 12;

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratedCoinbase.class);

    private final BigInteger blockReward;
    private final BigInteger feeTotal;

    private String wittyRemark;

    private boolean firstGen = true;

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

        // Generate initial transaction without user info
        this.regenerateCoinbaseTransaction(null);
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
        Transaction         priorTx         = this.getCoinbaseTransaction(),
                            newTx           = new Transaction(networkParams);
        byte[]              scriptBytes     = this.getCoinbaseScriptBytes();

        newTx.addInput(new TransactionInput(networkParams, newTx, scriptBytes));

        if (this.firstGen)
        {
            server.getOutputMonster().addOutputs(user, newTx, this.blockReward, this.feeTotal);

            this.firstGen = false;
        }
        else
        {
            for (TransactionOutput out : priorTx.getOutputs())
            {
                newTx.addOutput(out);
            }
        }

        this.setCoinbaseTransaction(newTx);
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
            scriptBytes[i + BLOCK_HEIGHT_OFF] = heightBytes[i];
        }

        // Terrible endian hack
        scriptBytes[1] = heightBytes[3];
        scriptBytes[3] = heightBytes[1];

        this.setCoinbaseScriptBytes(scriptBytes);
    }

    protected void randomizeCoinbaseScript()
    {
        byte[]  scriptBytes     = this.getCoinbaseScriptBytes(),
                randBytes       = new byte[4];
        Random  randomGenerator = new Random();

        randomGenerator.nextBytes(randBytes);

        for (int i = 0; i < 4; i++)
        {
            scriptBytes[i + RANDOM_OFF] = randBytes[i];
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