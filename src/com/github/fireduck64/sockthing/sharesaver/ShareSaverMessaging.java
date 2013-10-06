package com.github.fireduck64.sockthing.sharesaver;

import org.json.JSONObject;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.github.fireduck64.sockthing.SubmitResult;
import com.google.bitcoin.core.Sha256Hash;

/**
 * This saver saves shares to SQS and then reads them from SQS to process into
 * an inner saver as it can.
 */
public class ShareSaverMessaging implements ShareSaver
{
    protected StratumServer server;

    protected AmazonSNSClient sns;
    protected String topic_arn;

    protected AmazonSQSClient sqs;
    protected String queue_url;

    protected ShareSaver inner_saver;



    public ShareSaverMessaging(StratumServer server, ShareSaver inner_saver)
    {
        this.server = server;
        this.inner_saver = inner_saver;


        Config config = server.getConfig();

        config.require("saver_messaging_topic_arn");
        config.require("saver_messaging_aws_key");
        config.require("saver_messaging_aws_secret");
        config.require("saver_messaging_sqs_queue_url");
        config.require("saver_messaging_sqs_region");
        config.require("saver_messaging_read_threads");

        BasicAWSCredentials creds = new BasicAWSCredentials(
            config.get("saver_messaging_aws_key"),
            config.get("saver_messaging_aws_secret")
        );

        sns = new AmazonSNSClient(creds);

        topic_arn = config.get("saver_messaging_topic_arn");
        String region = topic_arn.split(":")[3];
        sns.setEndpoint("sns." + region + ".amazonaws.com");

        sqs = new AmazonSQSClient(creds);
        sqs.setEndpoint("sqs." + config.get("saver_messaging_sqs_region") + ".amazonaws.com");
        queue_url = config.get("saver_messaging_sqs_queue_url");

        int read_threads = Integer.parseInt(config.get("saver_messaging_read_threads"));

        for(int i=0; i<read_threads; i++)
        {

            new MessageReadThread().start();
        }

    }


    @Override
    public void saveShare(PoolUser pu, SubmitResult submit_result, String source, String unique_id, Long block_reward)
    throws ShareSaveException
    {
        try
        {
            JSONObject msg = new JSONObject();

            msg.put("user", pu.getName());
            msg.put("worker", pu.getWorkerName());
            msg.put("difficulty", pu.getDifficulty());
            msg.put("source", source);
            msg.put("our_result", submit_result.getOurResult());
            msg.put("upstream_result", submit_result.getUpstreamResult());
            msg.put("reason", submit_result.getReason());
            msg.put("unique_id", unique_id);
            msg.put("block_difficulty", submit_result.getNetworkDifficulty());
            msg.put("block_reward", block_reward);
            msg.put("height", submit_result.getHeight());

            String hash_str = null;
            if (submit_result.getHash() != null) hash_str = submit_result.getHash().toString();
            msg.put("hash", hash_str);

            msg.put("client", submit_result.getClientVersion());

            sns.publish(new PublishRequest(topic_arn, msg.toString(2), "share - " + pu.getName()));
        }
        catch(com.amazonaws.AmazonClientException e)
        {
            throw new ShareSaveException(e);
        }
        catch(org.json.JSONException e)
        {
            throw new ShareSaveException(e);
        }
    }

    public class MessageReadThread extends Thread
    {
        public MessageReadThread()
        {
            setDaemon(true);
        }

        @Override
        public void run()
        {
            while(true)
            {
                try
                {
                    doRun();
                }
                catch(Throwable t)
                {
                    t.printStackTrace();
                }

            }
        }

        public void doRun()
            throws com.github.fireduck64.sockthing.sharesaver.ShareSaveException, org.json.JSONException
        {
            ReceiveMessageRequest recv_req = new ReceiveMessageRequest(queue_url);
            recv_req.setWaitTimeSeconds(20);
            recv_req = recv_req.withAttributeNames("All");
            recv_req.setMaxNumberOfMessages(10);

            for(Message msg : sqs.receiveMessage(recv_req).getMessages())
            {
                int recv_count = Integer.parseInt(msg.getAttributes().get("ApproximateReceiveCount"));

                JSONObject sns_msg = new JSONObject(msg.getBody());

                JSONObject save_msg = new JSONObject(sns_msg.getString("Message"));

                String worker = save_msg.getString("worker");
                String user = save_msg.getString("user");
                int difficulty = save_msg.getInt("difficulty");
                String source = save_msg.getString("source");
                String unique_id = save_msg.getString("unique_id");

                double block_difficulty = -1.0; //Meaning unknown
                long block_reward = -1; //Meaning unknown
                if (save_msg.has("block_difficulty"))
                {
                    block_difficulty = save_msg.getDouble("block_difficulty");
                }

                if (save_msg.has("block_reward"))
                {
                    block_reward = save_msg.getLong("block_reward");
                }

                PoolUser pu = new PoolUser(worker);
                pu.setName(user);
                pu.setDifficulty(difficulty);

                SubmitResult res = new SubmitResult();
                if (save_msg.has("hash"))
                {
                    String hash_str = save_msg.getString("hash");
                    res.setHash(new Sha256Hash(hash_str));
                }
                if (save_msg.has("our_result"))
                {
                    res.setOurResult(save_msg.getString("our_result"));
                }
                if (save_msg.has("upstream_result"))
                {
                    res.setUpstreamResult(save_msg.getString("upstream_result"));
                }
                if (save_msg.has("reason"))
                {
                    res.setReason(save_msg.getString("reason"));
                }
                if (save_msg.has("client"))
                {
                    res.setClientVersion(save_msg.getString("client"));
                }
                if (save_msg.has("height"))
                {
                    res.setHeight(save_msg.getInt("height"));
                } else {
                    res.setHeight(-1); // Meaning unknown
                }

                res.setNetworkDiffiult(block_difficulty);

                inner_saver.saveShare(pu, res, source, unique_id, block_reward);

                sqs.deleteMessage(new DeleteMessageRequest(queue_url, msg.getReceiptHandle()));


            }

        }
    }

}

