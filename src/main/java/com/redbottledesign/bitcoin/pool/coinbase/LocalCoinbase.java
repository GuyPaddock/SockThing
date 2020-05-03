package com.redbottledesign.bitcoin.pool.coinbase;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.output.OutputMonster;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;

/**
 * <p>A {@link Coinbase} for mining pools that have complete control over the
 * contents of their coinbase script.</p>
 *
 * <p>The structure of this coinbase is as follows:</p>
 *
 * <pre>
 * Transaction Header [42 bytes]  | Coinbase Part #1
 * Block Height  [4 bytes]        |
 * Extra Nonce 1 [4 bytes]
 * Extra Nonce 2 [4 bytes]
 * Random Number [4 bytes]        | Coinbase Part #2
 * Misc Text     [up to 42 bytes] |
 * </pre>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class LocalCoinbase
extends AbstractCoinbase
{
    /**
     * The offset, in bytes, from the start of the coinbase transaction to the
     * first part of the coinbase.
     */
    protected static final int COINBASE1_TX_OFFSET = 0;

    /**
     * The offset, in bytes, from the start of the coinbase script to the first
     * bytes of the block height being encoded in the script.
     */
    protected static final int BLOCK_HEIGHT_SCRIPT_OFFSET   = 0;

    /**
     * The length, in bytes, of the block height being encoded in the coinbase
     * script.
     */
    protected static final int BLOCK_HEIGHT_BYTE_LENGTH     = 4;

    /**
     * The offset, in bytes, from the start of the coinbase script to the first
     * byte of extra nonce #1.
     */
    protected static final int EXTRA1_SCRIPT_OFFSET = BLOCK_HEIGHT_SCRIPT_OFFSET + BLOCK_HEIGHT_BYTE_LENGTH;

    /**
     * The length, in bytes, of extra nonce #1.
     */
    protected static final int EXTRA1_BYTE_LENGTH   = 4;

    /**
     * The offset, in bytes, from the start of the coinbase script to the first
     * byte of extra nonce #2.
     */
    protected static final int EXTRA2_SCRIPT_OFFSET = EXTRA1_SCRIPT_OFFSET + EXTRA1_BYTE_LENGTH;

    /**
     * The length, in bytes, of extra nonce #2.
     */
    protected static final int EXTRA2_BYTE_LENGTH   = 4;

    /**
     * The offset, in bytes, from the start of the coinbase script to an
     * additional random number added by the pool.
     */
    protected static final int RANDOM_SCRIPT_OFFSET = EXTRA2_SCRIPT_OFFSET + EXTRA2_BYTE_LENGTH;

    /**
     * The length, in bytes, of the extra random number.
     */
    protected static final int RANDOM_BYTE_LENGTH   = 4;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalCoinbase.class);

    /**
     * The pool-specific text that is being added to the coinbase script.
     */
    private final String coinbaseText;

    /**
     * The witty remark (if any) that is being added to the coinbase script.
     */
    private final String wittyRemarkText;

    /**
     * The reward, in satoshis, that this coinbase will entitle the pool to
     * claim for solving the block.
     */
    private final BigInteger blockReward;

    /**
     * The total amount of transaction fees, in satoshis, that this coinbase
     * will entitle the pool to claim for solving the block.
     */
    private final BigInteger feeTotal;

    /**
     * The output monster, which is used to generate the outputs on the
     * coinbase transaction.
     */
    private final OutputMonster outputMonster;

    /**
     * The user for which this coinbase is being tailored.
     */
    private final PoolUser user;

    /**
     * Constructor for {@link LocalCoinbase} that generates a new coinbase
     * for the specified network; with the specified coinbase and witty remark
     * text; for the block with the specified height; having the specified
     * rewards and total fees; and using the provided extra nonce value.
     *
     * @param   networkParameters
     *          The parameters of the Bitcoin network the coinbase is being
     *          generated for.
     *
     * @param   coinbaseText
     *          The pool-specific text that should be added to the coinbase
     *          script.
     *
     * @param   wittyRemarkText
     *          The witty remark (if any) that should be added to the coinbase
     *          script.
     *
     * @param   blockHeight
     *          The height of the block to which the coinbase corresponds.
     *
     * @param   blockReward
     *          The reward, in satoshis, that this coinbase will entitle the
     *          pool to claim for solving the block.
     *
     * @param   feeTotal
     *          The total amount of transaction fees, in satoshis, that this
     *          coinbase will entitle the pool to claim for solving the block.
     *
     * @param   extraNonce1
     *          The pool-supplied extra nonce value for the user to which this
     *          coinbase corresponds.
     *
     * @param   outputMonster
     *          The output monster, which will be used to generate the outputs
     *          on the coinbase transaction.
     *
     * @param   user
     *          The user for which this coinbase is being tailored.
     */
    public LocalCoinbase(NetworkParameters networkParameters, String coinbaseText, String wittyRemarkText,
                             long blockHeight, BigInteger blockReward, BigInteger feeTotal, byte[] extraNonce1,
                             OutputMonster outputMonster, PoolUser user)
    {
        super(networkParameters);

        this.coinbaseText       = coinbaseText;
        this.wittyRemarkText    = wittyRemarkText;
        this.blockReward        = blockReward;
        this.feeTotal           = feeTotal;
        this.outputMonster      = outputMonster;
        this.user               = user;

        this.initializeCoinbaseScriptBytes();

        this.setCoinbaseBlockHeight((int)blockHeight);
        this.setExtraNonce1(extraNonce1);
        this.setExtraNonce2(new byte[EXTRA2_BYTE_LENGTH]);

        this.randomizeCoinbaseScript();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCoinbase1Offset()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCoinbase1Length()
    {
        return TX_HEADER_LENGTH + BLOCK_HEIGHT_BYTE_LENGTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExtraNonce1Offset()
    {
        return EXTRA1_SCRIPT_OFFSET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExtraNonce1Length()
    {
        return EXTRA1_BYTE_LENGTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExtraNonce2Offset()
    {
        return EXTRA2_SCRIPT_OFFSET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExtraNonce2Length()
    {
        return EXTRA2_BYTE_LENGTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCoinbase2Offset()
    {
        return TX_HEADER_LENGTH + BLOCK_HEIGHT_BYTE_LENGTH + EXTRA1_BYTE_LENGTH + EXTRA2_BYTE_LENGTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCoinbase2Length()
    {
        //So coinbase1 size - extranonce(1+8)
        return this.getTotalCoinbaseTransactionLength() - TX_HEADER_LENGTH - BLOCK_HEIGHT_BYTE_LENGTH - EXTRA1_BYTE_LENGTH - EXTRA2_BYTE_LENGTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void regenerateCoinbaseTransaction()
    {
        NetworkParameters   networkParams   = this.getNetworkParameters();
        Transaction         newTx           = new Transaction(networkParams);
        byte[]              scriptBytes     = this.getCoinbaseScriptBytes();

        newTx.addInput(new TransactionInput(networkParams, newTx, scriptBytes));

        this.getOutputMonster().addOutputs(user, newTx, this.blockReward, this.feeTotal);

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

    /**
     * Gets the pool-specific text that is being added to the coinbase script.
     *
     * @return  The coinbase text.
     */
    public String getCoinbaseText()
    {
        return this.coinbaseText;
    }

    /**
     * Gets the witty remark (if any) that is being added to the coinbase
     * script.
     *
     * @return  Either the witty remark text; or {@code null} if no witty
     *          remark is being added.
     */
    public String getWittyRemarkText()
    {
        return this.wittyRemarkText;
    }

    /**
     * Gets the reward, in satoshis, that this coinbase will entitle the pool
     * to claim for solving the block.
     *
     * @return  The block reward.
     */
    public BigInteger getBlockReward()
    {
        return this.blockReward;
    }

    /**
     * Gets the total amount of transaction fees, in satoshis, that this
     * coinbase will entitle the pool to claim for solving the block.
     *
     * @return  The total amount of fees.
     */
    public BigInteger getFeeTotal()
    {
        return this.feeTotal;
    }

    /**
     * Gets The output monster, which is used to generate the outputs on the
     * coinbase transaction.
     *
     * @return  The output monster.
     */
    public OutputMonster getOutputMonster()
    {
        return this.outputMonster;
    }

    /**
     * Gets the user for which this coinbase is being tailored.
     *
     * @return  The user to which the coinbase corresponds.
     */
    public PoolUser getUser()
    {
        return this.user;
    }

    /**
     * <p>Populates the coinbase script with placeholders for the following:</p>
     *
     * <ol>
     *   <li>Block Height  [4 bytes]</li>
     *   <li>Extra Nonce 1 [4 bytes]</li>
     *   <li>Extra Nonce 2 [4 bytes]</li>
     *   <li>Random Number [4 bytes]</li>
     *   <li>Coinbase text + Witty remark [up to 42 bytes]</li>
     * </ol>
     */
    protected void initializeCoinbaseScriptBytes()
    {
        byte[]          scriptBytes;
        String          script,
                        remark      = this.getWittyRemarkText();

        //The first entries here get replaced with data.
        //They are just being put in the string so that there are some place holders for
        //The data to go.
        // BLKH - Block height
        // EXT1+2 extranonce 1 and 2 from stratum
        // RNDN - Random number to make each coinbase different, so that workers
        //        can't submit duplicates to other jobs if the EXT1 is always the same
        script = "BLKH" + "EXT1" + "EXT2" + "RNDN" + "/SockThing/" + this.getCoinbaseText();

        if (remark != null)
            script = script + '/' + remark;

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

    /**
     * Sets the block height in the coinbase script.
     *
     * @param   blockHeight
     *          The block height to put into the script.
     */
    protected void setCoinbaseBlockHeight(int blockHeight)
    {
        byte[]      scriptBytes     = this.getCoinbaseScriptBytes(),
                    heightBytes     = new byte[BLOCK_HEIGHT_BYTE_LENGTH];
        ByteBuffer  wrapperBuffer   = ByteBuffer.wrap(heightBytes);

        wrapperBuffer.putInt(blockHeight);
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

    /**
     * Populates the random number in the coinbase script.
     */
    protected void randomizeCoinbaseScript()
    {
        byte[]  scriptBytes     = this.getCoinbaseScriptBytes(),
                randBytes       = new byte[RANDOM_BYTE_LENGTH];
        Random  randomGenerator = new Random();

        randomGenerator.nextBytes(randBytes);

        for (int byteIndex = 0; byteIndex < randBytes.length; byteIndex++)
        {
            scriptBytes[RANDOM_SCRIPT_OFFSET + byteIndex] = randBytes[byteIndex];
        }

        this.setCoinbaseScriptBytes(scriptBytes);
    }
}
