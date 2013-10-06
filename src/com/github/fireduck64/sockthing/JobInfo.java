package com.github.fireduck64.sockthing;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

import com.github.fireduck64.sockthing.sharesaver.ShareSaveException;
import com.github.fireduck64.sockthing.util.DiffMath;
import com.github.fireduck64.sockthing.util.HexUtil;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Coinbase;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;

public class JobInfo
{
    private NetworkParameters network_params;
    private final StratumServer server;
    private final String jobId;
    private final JSONObject blockTemplate;
    private final byte[] extranonce1;
    private final PoolUser pool_user;
    private final HashSet<String> submits;
    private final Sha256Hash shareTarget;
    private final double difficulty;
    private final long value;

    private final Coinbase coinbase;

    public JobInfo(StratumServer server, PoolUser poolUser, String jobId, JSONObject blockTemplate,
                   byte[] extranonce1)
    throws org.json.JSONException
    {
        this.pool_user = poolUser;
        this.server = server;
        this.jobId = jobId;
        this.blockTemplate = blockTemplate;
        this.extranonce1 = extranonce1;

        this.value = blockTemplate.getLong("coinbasevalue");
        this.difficulty = server.getBlockDifficulty();

        int height = blockTemplate.getInt("height");

        this.submits = new HashSet<String>();
        this.coinbase =
          new Coinbase(server, poolUser, height, BigInteger.valueOf(this.value), this.getFeeTotal(), extranonce1);

        this.shareTarget = DiffMath.getTargetForDifficulty(poolUser.getDifficulty());

    }

    public int getHeight()
        throws org.json.JSONException
    {
        return this.blockTemplate.getInt("height");
    }

    private BigInteger getFeeTotal()
        throws org.json.JSONException
    {
        long fee_total = 0;
        JSONArray transactions = this.blockTemplate.getJSONArray("transactions");

        for (int i = 0; i < transactions.length(); i++)
        {
            JSONObject  tx  = transactions.getJSONObject(i);
            long        fee = tx.getLong("fee");

            fee_total += fee;
        }

        return BigInteger.valueOf(fee_total);
    }

    public JSONObject getMiningNotifyMessage(boolean clean)
    throws org.json.JSONException
    {
        JSONObject msg = new JSONObject();

        msg.put("id", JSONObject.NULL);
        msg.put("method", "mining.notify");

        JSONArray roots = new JSONArray();
        /*for(int i=0; i<5; i++)
        {
            byte[] root = new byte[32];
            rnd.nextBytes(root);
            roots.put(Hex.encodeHexString(root));
        }*/


        String  protocol  = "00000002";
        String  diffBits  = this.blockTemplate.getString("bits");
        int     ntime     = (int)System.currentTimeMillis()/1000;
        String  ntimeStr  = HexUtil.getIntAsHex(ntime);

        JSONArray params = new JSONArray();

        params.put(this.jobId);
        params.put(HexUtil.swapBytesInsideWord(HexUtil.swapEndianHexString(
          this.blockTemplate.getString("previousblockhash")))); //correct

        params.put(Hex.encodeHexString(this.coinbase.getCoinbase1()));
        params.put(Hex.encodeHexString(this.coinbase.getCoinbase2()));
        params.put(getMerkleRoots());
        params.put(protocol); //correct
        params.put(this.blockTemplate.getString("bits")); //correct
        params.put(HexUtil.getIntAsHex(this.blockTemplate.getInt("curtime"))); //correct
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
        }

        finally
        {
            try
            {
              this.server.getShareSaver().saveShare(
                this.pool_user,
                submitResult,
                "sockthing/" + this.server.getInstanceId(),
                uniqueId,
                this.value);
            }

            catch (ShareSaveException e)
            {
                submitResult.setOurResult("N");
                submitResult.setReason("Exception: " + e);
            }
        }
    }

    protected void validateSubmitInternal(JSONArray params, SubmitResult submitResult)
    throws org.json.JSONException, org.apache.commons.codec.DecoderException, ShareSaveException
    {
        String user = params.getString(0);
        String jobId = params.getString(1);
        byte[] extraNonce2 = Hex.decodeHex(params.getString(2).toCharArray());
        String ntime = params.getString(3);
        String nonce = params.getString(4);
        String submitCannonicalString = params.getString(2) + params.getString(3) + params.getString(4);

        synchronized (this.submits)
        {
            if (this.submits.contains(submitCannonicalString))
            {
                submitResult.setOurResult("N");
                submitResult.setReason("duplicate");
                return;
            }

            this.submits.add(submitCannonicalString);
        }

        int stale = this.server.checkStale(getHeight());

        if (stale >= 2)
        {
            submitResult.setOurResult("N");
            submitResult.setReason("quite stale");
            return;
        }

        if (stale == 1)
        {
            submitResult.setReason("slightly stale");
        }


        //nonce = HexUtil.swapEndianHexString(nonce);

        //System.out.println("nonce: " + nonce);
        //System.out.println("extra2: " + params.getString(2));


        /*extranonce2[0]=1;
        extranonce2[1]=0;
        extranonce2[2]=0;
        extranonce2[3]=0;
        nonce="00000000";*/

        Sha256Hash coinbaseHash;

        synchronized (this.coinbase)
        {
            this.coinbase.setExtranonce2(extraNonce2);

            coinbaseHash = this.coinbase.genTx().getHash();

            Sha256Hash merkle_root = new Sha256Hash(HexUtil.swapEndianHexString(coinbaseHash.toString()));

            JSONArray branches = getMerkleRoots();

            for (int i=0; i<branches.length(); i++)
            {
                Sha256Hash br = new Sha256Hash(branches.getString(i));

                //System.out.println("Merkle " + merkle_root + " " + br);
                merkle_root = HexUtil.treeHash(merkle_root, br);
            }

            try
            {
                StringBuilder header = new StringBuilder();

                header.append("00000002");

                header.append(HexUtil.swapBytesInsideWord(
                  HexUtil.swapEndianHexString(this.blockTemplate.getString("previousblockhash"))));

                header.append(HexUtil.swapBytesInsideWord(merkle_root.toString()));
                header.append(ntime);
                header.append(this.blockTemplate.getString("bits"));
                header.append(nonce);
                //header.append("000000800000000000000000000000000000000000000000000000000000000000000000000000000000000080020000");

                String header_str = header.toString();

                header_str = HexUtil.swapBytesInsideWord(header_str);

                System.out.println("Header: " + header_str);
                System.out.println("Header bytes: " + header_str.length());

                //header_str = HexUtil.swapWordHexString(header_str);

                MessageDigest md = MessageDigest.getInstance("SHA-256");

                md.update(Hex.decodeHex(header_str.toCharArray()));

                byte[] pass = md.digest();

                md.reset();
                md.update(pass);

                Sha256Hash blockhash = new Sha256Hash(
                  HexUtil.swapEndianHexString(
                    new Sha256Hash(md.digest()).toString()));

                System.out.println("Found block hash: " + blockhash);

                submitResult.setHash(blockhash);
                submitResult.setNetworkDiffiult(this.difficulty);
                submitResult.setOurDifficulty(DiffMath.getDifficultyForTarget(blockhash));

                if (blockhash.toString().compareTo(this.shareTarget.toString()) < 0)
                {
                    submitResult.setOurResult("Y");
                }
                else
                {
                    submitResult.setOurResult("N");
                    submitResult.setReason("H-not-zero");
                    return;
                }

                String upstreamResult = null;

                if (blockhash.toString().compareTo(this.blockTemplate.getString("target")) < 0)
                {
                    submitResult.setUpstreamResult(this.buildAndSubmitBlock(params, merkle_root));
                    submitResult.setHeight(getHeight());
                }
            }

            catch (NoSuchAlgorithmException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public String buildAndSubmitBlock(JSONArray params, Sha256Hash merkleRoot)
        throws org.json.JSONException, org.apache.commons.codec.DecoderException
    {
        System.out.println("WE CAN BUILD A BLOCK.  WE HAVE THE TECHNOLOGY.");

        String user = params.getString(0);
        String job_id = params.getString(1);
        byte[] extranonce2 = Hex.decodeHex(params.getString(2).toCharArray());
        String ntime = params.getString(3);
        String nonce = params.getString(4);

        long time = Long.parseLong(ntime,16);
        long target = Long.parseLong(blockTemplate.getString("bits"),16);
        long nonce_l = Long.parseLong(nonce,16);

        LinkedList<Transaction> lst = new LinkedList<Transaction>();

        lst.add(coinbase.genTx());
        JSONArray transactions = blockTemplate.getJSONArray("transactions");

        for(int i=0; i<transactions.length(); i++)
        {
            JSONObject tx = transactions.getJSONObject(i);
            try
            {
                Transaction tx_obj = new Transaction(network_params, Hex.decodeHex(tx.getString("data").toCharArray()));
                lst.add(tx_obj);
            }
            catch(com.google.bitcoin.core.ProtocolException e)
            {
                throw new RuntimeException(e);
            }
        }

        Block block = new Block(
            network_params,
            2,
            new Sha256Hash(blockTemplate.getString("previousblockhash")),
            new Sha256Hash(HexUtil.swapEndianHexString(merkleRoot.toString())),
            time,
            target,
            nonce_l,
            lst);

        System.out.println("Constructed block hash: " + block.getHash());

        try
        {
            block.verifyTransactions();
            System.out.println("Block VERIFIED");
            byte[] blockbytes = block.bitcoinSerialize();
            System.out.println("Bytes: " + blockbytes.length);

            String ret =  server.submitBlock(block);

            if (ret.equals("Y"))
            {
                coinbase.markRemark();
            }

            return ret;
        }
        catch (com.google.bitcoin.core.VerificationException e)
        {
            e.printStackTrace();
            return "N";
        }
    }

    public JSONArray getMerkleRoots()
        throws org.json.JSONException
    {
        ArrayList<Sha256Hash> hashes = new ArrayList<Sha256Hash>();

        JSONArray transactions = blockTemplate.getJSONArray("transactions");

        for (int i = 0; i < transactions.length(); i++)
        {
            JSONObject tx   = transactions.getJSONObject(i);
            Sha256Hash hash = new Sha256Hash(HexUtil.swapEndianHexString(tx.getString("hash")));

            hashes.add(hash);
        }

        JSONArray roots = new JSONArray();

        while(hashes.size() > 0)
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

            hashes=next_lst;
        }

        return roots;
    }
}
