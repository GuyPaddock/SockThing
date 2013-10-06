
package com.redbottledesign.bitcoin.pool.drupal;

import java.util.Collection;
import java.util.HashMap;

import com.github.fireduck64.sockthing.PplnsAgent;
import com.github.fireduck64.sockthing.StratumServer;

public class DrupalPplnsAgent extends Thread implements PplnsAgent
{
    public static final long DB_CHECK_MS = 120000L;

    private long last_check;
    private HashMap<String, Double> lastMap;
    private final StratumServer server;

    public DrupalPplnsAgent(StratumServer server)
    {
        this.setDaemon(true);
        this.setName(this.getClass().getSimpleName());

        this.server = server;
    }

    @Override
    public synchronized HashMap<String, Double> getUserMap()
    {
        this.notifyAll();

        return this.lastMap;
    }

    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                this.updateUserMap();

                synchronized(this)
                {
                    this.wait(DB_CHECK_MS / 4);
                }
            }

            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
    }

    private void updateUserMap()
    {
//        if (System.currentTimeMillis() > last_check + DB_CHECK_MS)
//        {
//            Connection conn = null;
//            try
//            {
//                double network_diff = server.getDifficulty();
//                double diff1shares = 0.0;
//
//                conn = DB.openConnection("share_db");
//
//                PreparedStatement ps = conn.prepareStatement("select * from shares where our_result='Y' order by time desc limit ?");
//                ps.setLong(1, Math.round(network_diff/32.0));
//                ResultSet rs = ps.executeQuery();
//
//                HashMap<String, Double> slice_map = new HashMap<String,Double>(512, 0.5f);
//
//                while ((rs.next()) && (diff1shares + 1e-3 < network_diff))
//                {
//                    String user = rs.getString("username");
//                    long share_diff = rs.getLong("difficulty");
//                    double apply_diff = Math.min(share_diff, network_diff - diff1shares);
//
//                    /*System.out.println("Diffs:" +
//                        " share " + share_diff +
//                        " apply " + apply_diff +
//                        " network " + network_diff +
//                        " shares " + diff1shares);*/
//
//
//
//                    diff1shares+=apply_diff;
//
//                    double fee = 0.0175+(0.1325/Math.pow(share_diff,0.6));
//                    double slice = 25.0 *(1.0-(fee))*apply_diff/network_diff;
//
//                    if (!slice_map.containsKey(user))
//                    {
//                        slice_map.put(user, slice);
//                    }
//                    else
//                    {
//                        slice_map.put(user, slice + slice_map.get(user));
//                    }
//
//
//
//                }
//                rs.close();
//                ps.close();
//
//                //System.out.println(slice_map);
//                for(Map.Entry<String, Double> me : slice_map.entrySet())
//                {
//                    DecimalFormat df = new DecimalFormat("0.00000000");
//                    System.out.println(me.getKey() +": " +  df.format(me.getValue()));
//
//                }
//                System.out.println("Total: " + sum(slice_map.values()));
//                last_map = slice_map;
//
//                last_check=System.currentTimeMillis();
//            }
//            finally
//            {
//                DB.safeClose(conn);
//            }
//        }
    }

    public double sum(Collection<Double> vals)
    {
      double x = 0.0;

      for (Double d : vals)
      {
          x += d;
      }

      return x;
    }
}