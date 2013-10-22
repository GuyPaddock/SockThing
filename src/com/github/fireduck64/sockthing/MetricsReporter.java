
package com.github.fireduck64.sockthing;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;

public class MetricsReporter extends Thread
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsReporter.class);

    StratumServer server;
    AmazonCloudWatchClient cw;

    LinkedBlockingQueue<PutMetricDataRequest> put_queue;

    public MetricsReporter(StratumServer server)
    {
        this.server = server;

        Config conf = server.getConfig();

        conf.require("metrics_enabled");

        if (!conf.getBoolean("metrics_enabled"))
        {
            return;
        }

        put_queue = new LinkedBlockingQueue<PutMetricDataRequest>();

        conf.require("metrics_aws_region");
        conf.require("metrics_aws_key");
        conf.require("metrics_aws_secret");

        cw = new AmazonCloudWatchClient(
            new BasicAWSCredentials(
                conf.get("metrics_aws_key"), conf.get("metrics_aws_secret")));

        cw.setEndpoint("monitoring."+conf.get("metrics_aws_region")+".amazonaws.com");

        this.setDaemon(true);
        this.start();
    }

    public String getNamespace()
    {
        return "sockthing/" + server.getInstanceId();
    }

    public String getGlobalNamespace()
    {
        return "sockthing";
    }

    public void metricCount(String name, double count)
    {
        PutMetricDataRequest    req = new PutMetricDataRequest();
        LinkedList<MetricDatum> lst = new LinkedList<MetricDatum>();
        MetricDatum             md  = new MetricDatum();

        md.setMetricName(name);
        md.setValue(count);

        lst.add(md);

        req.setMetricData(lst);
        req.setNamespace(getNamespace());

        try
        {
            if (put_queue != null)
                put_queue.put(req);
        }

        catch (InterruptedException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public void metricTime(String name, double milliseconds)
    {
        PutMetricDataRequest    req = new PutMetricDataRequest();
        LinkedList<MetricDatum> lst = new LinkedList<MetricDatum>();
        MetricDatum             md  = new MetricDatum();

        md.setMetricName(name);
        md.setValue(milliseconds);
        md.setUnit("Milliseconds");

        lst.add(md);

        req.setMetricData(lst);
        req.setNamespace(getGlobalNamespace());

        try
        {
            if (put_queue != null)
                put_queue.put(req);
        }

        catch (InterruptedException ex)
        {
            throw new RuntimeException(ex);
        }
    }


    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                this.doRun();
            }

            catch (Throwable ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Metrics reporting failed: %s\n%s",
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }
        }
    }

    private void doRun() throws Exception
    {
        PutMetricDataRequest put = put_queue.take();

        if (put != null)
        {
            cw.putMetricData(put);

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Metrics: " + put);
        }
    }
}