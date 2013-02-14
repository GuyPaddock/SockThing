
package com.google.bitcoin.core;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

import com.google.bitcoin.core.Address;
import java.math.BigInteger;
import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;

import java.util.Random;

/**
 * Creates a stratum compatible coinbase transaction
 */
public class Coinbase
{
    Transaction tx;

    byte[] tx_data;
    byte[] script_bytes;
    byte[] extranonce1;
    byte[] extranonce2;
    Address pay_to_addr;
    NetworkParameters params;
    BigInteger value;
   
    public static final int BLOCK_HEIGHT_OFF=0;
    public static final int EXTRA1_OFF=4;
    public static final int EXTRA2_OFF=8;
    public static final int RANDOM_OFF=12;


    public Coinbase(NetworkParameters params, int block_height, Address pay_to_addr, BigInteger value, byte[] extranonce1)
    {
        this.params = params;
        this.pay_to_addr = pay_to_addr;
        this.value = value;
        this.extranonce1 = extranonce1;
        extranonce2 = new byte[4];


        byte[] height_array = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(height_array);
        bb.putInt(block_height);
        height_array[0]=3;

        //The first entries here get replaced with data.
        //They are just being put in the string so that there are some place holders for
        //The data to go.
        // BLKH - Block height
        // EXT1+2 extranonce 1 and 2 from stratum
        // RNDN - Random number to make each coinbase different, so that workers
        //        can't submit duplicates to other jobs if the EXT1 is always the same
        String script = "BLKH" + "EXT1" + "EXT2" + "RNDN" + "/SockThing/Stratum";
        script_bytes= script.getBytes();


        for(int i=0; i<4; i++)
        {
            script_bytes[i+BLOCK_HEIGHT_OFF] = height_array[i];
        }

        //Terrible endian hack
        script_bytes[1]=height_array[3];
        script_bytes[3]=height_array[1];

       
                
        for(int i=0; i<4; i++)
        {
            script_bytes[i+EXTRA1_OFF ] = extranonce1[i];
        }

        byte[] rand = new byte[4];
        Random rnd = new Random();
        rnd.nextBytes(rand);
        for(int i=0; i<4; i++)
        {
            script_bytes[i+RANDOM_OFF]=rand[i];
        }
        //System.out.println("Script bytes: " + script.length());
        //System.out.println("Script: " + Hex.encodeHexString(script_bytes));

        genTx();

    }

    /**
     * This can be reasonably overridden to do custom things.  I'd advise against
     * messing with the script bytes, but the outputs are easily changed.
     */
    public Transaction genTx()
    {
        tx = new Transaction(params);
        tx.addInput(new TransactionInput(params, tx, script_bytes));
        tx.addOutput(value, pay_to_addr);

        tx_data = tx.bitcoinSerialize();

        return tx;

    }

    public void setExtranonce2(byte[] extranonce2)
    {
        for(int i=0; i<4; i++)
        {
            script_bytes[i+EXTRA2_OFF] = extranonce2[i];
        }
    }


    public byte[] getCoinbase1()
    {
        //This contains our standard 42 byte transaction header
        //then 4 bytes of block height for block v2
        int cb1_size=42+4;
        byte[] buff = new byte[42+4];

        for(int i=0; i<cb1_size; i++)
        {
            buff[i] = tx_data[i];
        }

        return buff;
    }

    public byte[] getCoinbase2()
    {
        //So coinbase1 size - extranonce(1+8)

        int sz = tx_data.length - 42 - 4 - 8;
        byte[] buff=new byte[sz];

        for(int i=0; i<sz; i++)
        {
            buff[i] = tx_data[i+42+8+4];
        }
        return buff;
    }


    public static void main(String args[]) throws Exception
    {
        NetworkParameters params = NetworkParameters.prodNet();
        byte[] extra1=new byte[4];
        for(int i=0; i<4; i++) extra1[i]=(byte)(i+1);

        Coinbase cb = new Coinbase(params, 32010, new Address(params, "15vkb5XdTrZW1oKByjaqdTsUjdSri2uREN"), BigInteger.valueOf(2500000000L), extra1);

        Transaction tx = cb.genTx();
        System.out.println(tx.getHash());
        System.out.println(tx);

        tx = cb.genTx();
        System.out.println(tx.getHash());
        System.out.println(tx);

        cb.setExtranonce2(extra1);
        tx=cb.genTx();
        System.out.println(tx.getHash());
        System.out.println(tx);

        for(TransactionOutput out : tx.getOutputs())
        {
            System.out.println("  out: " + out);
        }

        byte[] data = tx.bitcoinSerialize();
        System.out.println(Hex.encodeHexString(data));
        System.out.println(Hex.encodeHexString(cb.getCoinbase1()));
        System.out.println(Hex.encodeHexString(cb.getCoinbase2()));




    }
}
