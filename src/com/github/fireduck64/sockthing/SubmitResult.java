package com.github.fireduck64.sockthing;

import com.google.bitcoin.core.Sha256Hash;

public class SubmitResult
{
    protected Sha256Hash hash;
    protected String ourResult="N";
    protected String upstreamResult;
    protected String reason;
    protected String clientVersion;
    protected int height;

    public SubmitResult()
    {
      ourResult = "N";
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
}
