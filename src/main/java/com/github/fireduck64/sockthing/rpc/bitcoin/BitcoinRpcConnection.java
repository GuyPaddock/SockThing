package com.github.fireduck64.sockthing.rpc.bitcoin;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Block;

public interface BitcoinRpcConnection
{
    public abstract double getDifficulty()
    throws IOException, JSONException;

    public abstract int getBlockCount()
    throws IOException, JSONException;

    public abstract BlockTemplate getCurrentBlockTemplate()
    throws IOException, JSONException;

    public abstract long getBlockConfirmationCount(String blockHash)
    throws IOException, JSONException;

    public abstract JSONObject getBlockInfo(String blockHash)
    throws IOException, JSONException;

    public abstract boolean submitBlock(BlockTemplate blockTemplate, Block block)
    throws IOException, JSONException;

    public abstract String sendPayment(double amount, Address payFromAddress, Address payToAddress)
    throws IOException, JSONException;
}
