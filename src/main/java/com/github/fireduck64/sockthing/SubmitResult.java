package com.github.fireduck64.sockthing;

import com.google.bitcoin.core.Sha256Hash;

public class SubmitResult
{
    private Sha256Hash hash;
    private String ourResult;
    private String upstreamResult;
    private String reason;
    private String clientVersion;
    private long height;
    private double workDifficulty;
    private double networkDifficulty;
    private boolean shouldSendDifficulty;

    public SubmitResult()
    {
        this.ourResult            = "Y";
        this.shouldSendDifficulty = false;
    }

    public Sha256Hash getHash()
    {
        return this.hash;
    }

    public void setHash(Sha256Hash hash)
    {
        this.hash = hash;
    }

    public String getOurResult()
    {
        return this.ourResult;
    }

    public void setOurResult(String ourResult)
    {
        this.ourResult = ourResult;
    }

    public String getUpstreamResult()
    {
        return this.upstreamResult;
    }

    public void setUpstreamResult(String upstreamResult)
    {
        this.upstreamResult = upstreamResult;
    }

    public String getReason()
    {
        return this.reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getClientVersion()
    {
        return this.clientVersion;
    }

    public void setClientVersion(String clientVersion)
    {
        this.clientVersion = clientVersion;
    }

    public long getHeight()
    {
        return this.height;
    }

    public void setHeight(long height)
    {
        this.height = height;
    }

    public double getWorkDifficulty()
    {
        return this.workDifficulty;
    }

    public void setOurDifficulty(double ourDifficulty)
    {
        this.workDifficulty = ourDifficulty;
    }

    public double getNetworkDifficulty()
    {
        return this.networkDifficulty;
    }

    public void setNetworkDiffiult(double networkDifficulty)
    {
        this.networkDifficulty = networkDifficulty;
    }

    public boolean shouldSendDifficulty()
    {
        return this.shouldSendDifficulty;
    }

    public void setShouldSendDifficulty(boolean shouldSendDifficulty)
    {
        this.shouldSendDifficulty = shouldSendDifficulty;
    }

    public static enum Status
    {
        CURRENT,
        SLIGHTLY_STALE,
        REALLY_STALE
    }
}