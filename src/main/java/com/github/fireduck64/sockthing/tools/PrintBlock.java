package com.github.fireduck64.sockthing.tools;
import java.io.DataInputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Hex;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;

public class PrintBlock
{
    public static void main(String args[]) throws Exception
    {
        new PrintBlock(args[0]);
    }

    public PrintBlock(String hash)
    throws Exception
    {
        Block blk = getBlockFromS3(new Sha256Hash(hash));

        System.out.println(blk);

        byte[] block_bytes = blk.bitcoinSerialize();
        String megastring = Hex.encodeHexString(block_bytes);

        System.out.println(megastring);
    }

    public static Block getBlockFromS3(Sha256Hash hash)
    {
        try
        {
            Block blk;

            String url = "http://s3-us-west-2.amazonaws.com/bitcoin-blocks/" + hash.toString();
            URL u = new URL(url);
            URLConnection conn = u.openConnection();

            DataInputStream in = new DataInputStream(conn.getInputStream());
            int len = conn.getContentLength();
            byte buff[] = new byte[len];
            in.readFully(buff);

            blk = new Block(NetworkParameters.prodNet(), buff);

            in.close();
            System.out.println("Got block : " + hash + " from s3");

            return blk;
        }
        catch(Exception e)
        {
            System.out.println("S3 get failed");
            e.printStackTrace();
        }

        return null;
    }
}