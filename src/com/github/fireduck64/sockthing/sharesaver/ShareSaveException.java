package com.github.fireduck64.sockthing.sharesaver;


public class ShareSaveException
extends Exception
{
  public ShareSaveException(Throwable throwable)
  {
      super(throwable);
  }

  public ShareSaveException(String message, Throwable ex)
  {
    super(message, ex);
  }
}
