package com.github.fireduck64.sockthing;

import static com.google.bitcoin.core.Utils.doubleDigest;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.sharesaver.ShareSaveException;
import com.github.fireduck64.sockthing.util.DiffMath;
import com.github.fireduck64.sockthing.util.HexUtil;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.VerificationException;
import com.redbottledesign.bitcoin.pool.VardiffCalculator;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.Block;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.BlockTemplate;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.Coinbase;

public class JobInfo
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JobInfo.class);

    private NetworkParameters network_params;
    private final StratumServer server;
    private final String jobId;
    private final BlockTemplate blockTemplate;
    private final long blockHeight;
    private final PoolUser poolUser;
    private final HashSet<String> submits;
    private final Sha256Hash shareTarget;
    private final double difficulty;
    private final BigInteger blockReward;
    private final BigInteger feeTotal;

    private final Coinbase coinbase;

    public JobInfo(StratumServer server, PoolUser poolUser, String jobId, BlockTemplate blockTemplate,
                   Coinbase coinbase)
    throws JSONException
    {
        this.poolUser       = poolUser;
        this.server         = server;
        this.jobId          = jobId;
        this.blockTemplate  = blockTemplate;
        this.blockHeight    = blockTemplate.getHeight();
        this.blockReward    = blockTemplate.getReward();
        this.feeTotal       = blockTemplate.getTotalFees();
        this.difficulty     = server.getBlockDifficulty();
        this.submits        = new HashSet<String>();
        this.coinbase       = coinbase;

        this.shareTarget = DiffMath.getTargetForDifficulty(poolUser.getDifficulty());

        // Update coinbase for the user we were provided
        this.coinbase.regenerateCoinbaseTransaction(poolUser);
    }

    public long getBlockHeight()
    {
        return this.blockHeight;
    }

    public Coinbase getCoinbase()
    {
        return this.coinbase;
    }

    public JSONObject getMiningNotifyMessage(boolean clean)
    throws org.json.JSONException
    {
        JSONObject msg = new JSONObject();

        msg.put("id", JSONObject.NULL);
        msg.put("method", "mining.notify");

        String  protocol  = "00000002";
        String  diffBits  = this.blockTemplate.getDifficultyBits();

        JSONArray params = new JSONArray();

        params.put(this.jobId);
        params.put(HexUtil.swapBytesInsideWord(HexUtil.swapEndianHexString(
          this.blockTemplate.getPreviousBlockHash()))); //correct

        params.put(Hex.encodeHexString(this.coinbase.getCoinbasePart1()));
        params.put(Hex.encodeHexString(this.coinbase.getCoinbasePart2()));
        params.put(getMerkleRoots());
        params.put(protocol); //correct
        params.put(diffBits); //correct
        params.put(HexUtil.getIntAsHex(this.blockTemplate.getCurrentTime())); //correct
        params.put(clean);

        msg.put("params", params);

        return msg;
    }

    public void validateSubmit(JSONArray params, SubmitResult submitResult)
    {
        String uniqueId = HexUtil.sha256(params.toString());

        try
        {
            this.validateSubmitInternal(params, submitResult);
        }

        catch (Throwable t)
        {
            submitResult.setOurResult("N");
            submitResult.setReason("Exception: " + t);

            if (LOGGER.isErrorEnabled())
                LOGGER.error("Error validating share: " + t.getMessage(), t);
        }

        finally
        {
            try
            {
              this.server.getShareSaver().saveShare(
                this.poolUser,
                submitResult,
                "sockthing/" + this.server.getInstanceId(),
                uniqueId,
                this.blockReward,
                this.feeTotal);
            }

            catch (ShareSaveException e)
            {
                submitResult.setOurResult("N");
                submitResult.setReason("Exception: " + e);

                if (LOGGER.isErrorEnabled())
                    LOGGER.error("Error saving share: " + e.getMessage(), e);
            }
        }
    }

    public String buildAndSubmitBlock(JSONArray params, Sha256Hash merkleRoot)
    throws JSONException, DecoderException
    {
        if (LOGGER.isInfoEnabled())
          LOGGER.info("WE CAN BUILD A BLOCK.  WE HAVE THE TECHNOLOGY.");

        String user = params.getString(0);
        String job_id = params.getString(1);
        byte[] extranonce2 = Hex.decodeHex(params.getString(2).toCharArray());
        String ntime = params.getString(3);
        String nonce = params.getString(4);

        long time = Long.parseLong(ntime,16);
        long target = Long.parseLong(blockTemplate.getDifficultyBits(), 16);
        long nonce_l = Long.parseLong(nonce,16);

        List<Transaction> lst = this.blockTemplate.getTransactions(this.coinbase.getCoinbaseTransaction());

        Block block = new Block(
            network_params,
            2,
            new Sha256Hash(blockTemplate.getPreviousBlockHash()),
            new Sha256Hash(HexUtil.swapEndianHexString(merkleRoot.toString())),
            time,
            target,
            nonce_l,
            lst);

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Constructed block hash: " + block.getHash());

        try
        {
            byte[] blockBytes;
            String ret;

            block.verifyTransactions();

            blockBytes = block.bitcoinSerialize();

            if (LOGGER.isDebugEnabled())
                LOGGER.debug(String.format("Block size: %d bytes.", blockBytes.length));

            ret = server.submitBlock(block);

            if (ret.equals("Y"))
            {
                coinbase.markSolved();

                if (LOGGER.isInfoEnabled())
                    LOGGER.info("Block VERIFIED: "+ this.blockHeight + " " + block.getHash());
            }

            return ret;
        }

        catch (VerificationException ex)
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Block failed verification: %s\n",
                        ex.getMessage(),
                        ExceptionUtils.getStackTrace(ex)));
            }

            return "N";
        }
    }

    protected void validateSubmitInternal(JSONArray params, SubmitResult submitResult)
    throws JSONException, DecoderException, ShareSaveException
    {
        boolean     ourResult               = true;
        String      ourResultReason         = null,
                    user                    = params.getString(0),
                    jobId                   = params.getString(1),
                    ntime                   = params.getString(3),
                    nonce                   = params.getString(4),
                    submitCanonicalString   = params.getString(2) + params.getString(3) + params.getString(4);
        byte[]      extraNonce2             = Hex.decodeHex(params.getString(2).toCharArray());
        String      userName                = poolUser.getName();
        Sha256Hash  blockhash               = null;
        String      blockHashString         = null,
                    targetHashString        = null;

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "validateSubmitInternal() - nonce: %s, extra2: %s",
                    HexUtil.swapEndianHexString(nonce),
                    params.getString(2)));
        }

        synchronized (this.submits)
        {
            if (this.submits.contains(submitCanonicalString))
            {
                ourResult       = false;
                ourResultReason = "duplicate";
            }

            else
            {
                this.submits.add(submitCanonicalString);
            }
        }

        if (ourResult != false)
        {
            switch (this.server.checkStale(this.blockHeight))
            {
                case REALLY_STALE:
                    ourResult       = false;
                    ourResultReason = "quite stale";
                    break;

                case SLIGHTLY_STALE:
                    ourResultReason = "slightly stale";
                    break;

                case CURRENT:
                default:
                    break;
            }
        }

        if (ourResult != false)
        {
            synchronized (this.coinbase)
            {
                Sha256Hash  merkleRoot;

                this.coinbase.setExtraNonce2(extraNonce2);
                this.coinbase.regenerateCoinbaseTransaction(this.poolUser);

                merkleRoot = this.calculateMerkleRoot();

                try
                {
                    StringBuilder   header = new StringBuilder();
                    String          headerString;
                    MessageDigest   md;
                    byte[]          pass;
                    double          shareDifficulty;

                    header.append("00000002");

                    header.append(HexUtil.swapBytesInsideWord(
                      HexUtil.swapEndianHexString(this.blockTemplate.getPreviousBlockHash())));

                    header.append(HexUtil.swapBytesInsideWord(merkleRoot.toString()));
                    header.append(ntime);
                    header.append(this.blockTemplate.getDifficultyBits());
                    header.append(nonce);
                    //header.append("000000800000000000000000000000000000000000000000000000000000000000000000000000000000000080020000");

                    headerString = header.toString();
                    headerString = HexUtil.swapBytesInsideWord(headerString);

                    if (LOGGER.isDebugEnabled())
                    {
                        LOGGER.debug(
                            String.format(
                                "validateSubmitInternal() - Header (%d): %s",
                                headerString.length(),
                                headerString));
                    }

                    md = MessageDigest.getInstance("SHA-256");

                    md.update(Hex.decodeHex(headerString.toCharArray()));

                    pass = md.digest();

                    md.reset();
                    md.update(pass);

                    blockhash = new Sha256Hash(
                      HexUtil.swapEndianHexString(
                        new Sha256Hash(md.digest()).toString()));

                    shareDifficulty = DiffMath.getDifficultyForHash(blockhash);

                    submitResult.setHash(blockhash);
                    submitResult.setNetworkDiffiult(this.difficulty);
                    submitResult.setOurDifficulty(shareDifficulty);

                    blockHashString     = blockhash.toString();
                    targetHashString    = this.shareTarget.toString();

                    if (blockHashString.compareTo(targetHashString) < 0)
                    {
                        if (LOGGER.isInfoEnabled())
                        {
                          LOGGER.info(
                              String.format("Share submitted: %s %d %s", userName, this.blockHeight, blockhash));
                        }
                    }
                    else
                    {
                        ourResult       = false;
                        ourResultReason = "H-not-zero";

                        if (LOGGER.isInfoEnabled())
                        {
                          LOGGER.info(
                              String.format(
                                  "Share rejected (%s, S:%s, T:%s): %s %d %s",
                                  submitResult.getReason(),
                                  blockHashString,
                                  targetHashString,
                                  userName,
                                  this.blockHeight,
                                  blockhash));
                        }
                    }

                    if (ourResult != false)
                    {
                        if (blockHashString.compareTo(this.blockTemplate.getTarget()) < 0)
                        {
                            submitResult.setUpstreamResult(this.buildAndSubmitBlock(params, merkleRoot));
                            submitResult.setHeight(this.blockHeight);

                            if (LOGGER.isInfoEnabled())
                            {
                              LOGGER.info(
                                  String.format(
                                      "Block submitted upstream (result: %s): %s %d %s",
                                      submitResult.getUpstreamResult(),
                                      userName,
                                      this.blockHeight,
                                      blockhash));
                            }
                        }

                        // Re-compute user's difficulty
                        if (VardiffCalculator.getInstance().computeDifficultyAdjustment(this.poolUser, shareDifficulty))
                            submitResult.setShouldSendDifficulty(true);
                    }
                }

                catch (NoSuchAlgorithmException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        submitResult.setOurResult((ourResult ? "Y" : "N"));
        submitResult.setReason(ourResultReason);

        if (LOGGER.isInfoEnabled())
        {
            if (ourResult == true)
            {
                LOGGER.info(
                    String.format(
                        "Share accepted: %s %d %s",
                        userName,
                        this.blockHeight,
                        blockhash));
            }

            else
            {
                LOGGER.info(
                    String.format(
                        "Share rejected (%s): %s %d %s",
                        submitResult.getReason(),
                        userName,
                        this.blockHeight,
                        blockhash));
            }
        }
    }

    protected Sha256Hash calculateMerkleRoot()
    throws JSONException
    {
        Sha256Hash  coinbaseHash    = this.coinbase.getCoinbaseTransaction().getHash(),
                    merkleRoot      = new Sha256Hash(HexUtil.swapEndianHexString(coinbaseHash.toString()));
        JSONArray   branches        = this.getMerkleRoots();

        for (int i = 0; i < branches.length(); i++)
        {
            Sha256Hash branchHash = new Sha256Hash(branches.getString(i));

            if (LOGGER.isDebugEnabled())
                LOGGER.debug(String.format("validateSubmitInternal() - Merkle %s %s.", merkleRoot, branchHash));

            merkleRoot = HexUtil.treeHash(merkleRoot, branchHash);
        }

        return merkleRoot;
    }

    protected JSONArray getMerkleRoots()
    throws JSONException
    {
        ArrayList<Sha256Hash> hashes = new ArrayList<Sha256Hash>();

        for (Transaction transaction : this.blockTemplate.getTransactions())
        {
            /* NOTE: transaction.getHash() works similarly, but returns the
             *       whole hash in big endian order. The code here is more
             *       efficient that converting a hash to a string, byte swapping
             *       it, and then converting it back into a hash.
             */
            byte[]      transactionBits     = transaction.bitcoinSerialize();
            Sha256Hash  transactionHashLE   = new Sha256Hash(doubleDigest(transactionBits));

            hashes.add(transactionHashLE);
        }

        JSONArray roots = new JSONArray();

        while (hashes.size() > 0)
        {
            ArrayList<Sha256Hash> next_lst = new ArrayList<Sha256Hash>();

            roots.put(hashes.get(0).toString());

            for (int i = 1; i < hashes.size(); i += 2)
            {
                if ((i + 1) == hashes.size())
                  next_lst.add(HexUtil.treeHash(hashes.get(i), hashes.get(i)));

                else
                  next_lst.add(HexUtil.treeHash(hashes.get(i), hashes.get(i+1)));
            }

            hashes = next_lst;
        }

        return roots;
    }
}
