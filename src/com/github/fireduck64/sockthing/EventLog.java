package com.github.fireduck64.sockthing;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;

public class EventLog
{
  private boolean logEnabled = false;
  private PrintStream logStream = null;
  private SimpleDateFormat dateFormat;

  public EventLog(Config conf)
  throws IOException
  {
    conf.require("event_log_enabled");

    this.logEnabled = conf.getBoolean("event_log_enabled");

    if (this.logEnabled)
    {
      conf.require("event_log_path");

      this.logStream  = new PrintStream(new FileOutputStream(conf.get("event_log_path"), true));
      this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    }
  }

  public void log(String msg)
  {
    if (!this.logEnabled)
      return;

    synchronized (this.logStream)
    {
      String line = this.dateFormat.format(new java.util.Date()) + " - " + msg;

      this.logStream.println(line);
      this.logStream.flush();
    }
  }
}