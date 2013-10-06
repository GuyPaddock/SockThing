package com.github.fireduck64.sockthing;

import com.google.bitcoin.core.Sha256Hash;

public class SubmitResult
{
    private Sha256Hash hash;
    private String ourResult;
    private String upstreamResult;
    private String reason;
    private String clientVersion;
    private int height;
    private double ourDifficulty;
    private double networkDifficulty;

    public SubmitResult()
    {
      this.ourResult = "Y";
    }

    public Sha256Hash getHash()
    {
      return hash;
    }

    public void setHash(Sha256Hash hash)
    {
      this.hash = hash;
    }

    public String getOurResult()
    {
      return ourResult;
    }

    public void setOurResult(String ourResult)
    {
      this.ourResult = ourResult;
    }

    public String getUpstreamResult()
    {
      return upstreamResult;
    }

    public void setUpstreamResult(String upstreamResult)
    {
      this.upstreamResult = upstreamResult;
    }

    public String getReason()
    {
      return reason;
    }

    public void setReason(String reason)
    {
      this.reason = reason;
    }

    public String getClientVersion()
    {
      return clientVersion;
    }

    public void setClientVersion(String clientVersion)
    {
      this.clientVersion = clientVersion;
    }

    public int getHeight()
    {
      return height;
    }

    public void setHeight(int height)
    {
      this.height = height;
    }

    public double getOurDifficulty()
    {
      return this.ourDifficulty;
    }

    public void setOurDifficulty(double ourDifficulty)
    {
      this.ourDifficulty = ourDifficulty;
    }

    public double getNetworkDifficulty()
    {
      return this.networkDifficulty;
    }

    public void setNetworkDiffiult(double networkDifficulty)
    {
      this.networkDifficulty = networkDifficulty;
    }
}