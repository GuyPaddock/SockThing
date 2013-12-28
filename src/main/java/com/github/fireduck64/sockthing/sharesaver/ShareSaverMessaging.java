package com.github.fireduck64.sockthing.sharesaver;

import java.math.BigInteger;

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
    protected String queueUrl;

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
        queueUrl = config.get("saver_messaging_sqs_queue_url");

        int read_threads = Integer.parseInt(config.get("saver_messaging_read_threads"));

        for(int i=0; i<read_threads; i++)
        {

            new MessageReadThread().start();
        }

    }


    @Override
    public void saveShare(PoolUser pu, SubmitResult submitResult, String source, String uniqueId,
                          BigInteger blockReward, BigInteger feeTotal)
    throws ShareSaveException
    {
        try
        {
            JSONObject msg = new JSONObject();

            msg.put("user", pu.getName());
            msg.put("worker", pu.getWorkerName());
            msg.put("difficulty", pu.getDifficulty());
            msg.put("source", source);
            msg.put("our_result", submitResult.getOurResult());
            msg.put("upstream_result", submitResult.getUpstreamResult());
            msg.put("reason", submitResult.getReason());
            msg.put("unique_id", uniqueId);
            msg.put("block_difficulty", submitResult.getNetworkDifficulty());
            msg.put("block_reward", blockReward);
            msg.put("height", submitResult.getHeight());

            String hash_str = null;
            if (submitResult.getHash() != null) hash_str = submitResult.getHash().toString();
            msg.put("hash", hash_str);

            msg.put("client", submitResult.getClientVersion());

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
            ReceiveMessageRequest recv_req = new ReceiveMessageRequest(queueUrl);
            recv_req.setWaitTimeSeconds(20);
            recv_req = recv_req.withAttributeNames("All");
            recv_req.setMaxNumberOfMessages(10);

            for(Message msg : sqs.receiveMessage(recv_req).getMessages())
            {
                int recv_count = Integer.parseInt(msg.getAttributes().get("ApproximateReceiveCount"));

                JSONObject sns_msg = new JSONObject(msg.getBody());

                JSONObject saveMsg = new JSONObject(sns_msg.getString("Message"));

                String worker = saveMsg.getString("worker");
                String user = saveMsg.getString("user");
                int difficulty = saveMsg.getInt("difficulty");
                String source = saveMsg.getString("source");
                String uniqueId = saveMsg.getString("unique_id");

                double block_difficulty = -1.0; //Meaning unknown
                BigInteger blockReward = BigInteger.valueOf(-1); //Meaning unknown
                BigInteger feeTotal = BigInteger.valueOf(-1); //Meaning unknown

                if (saveMsg.has("block_difficulty"))
                {
                    block_difficulty = saveMsg.getDouble("block_difficulty");
                }

                if (saveMsg.has("block_reward"))
                {
                    blockReward = new BigInteger(saveMsg.getString("block_reward"));
                }

                if (saveMsg.has("fee_total"))
                {
                    feeTotal = new BigInteger(saveMsg.getString("fee_total"));
                }

                PoolUser pu = new PoolUser(worker);
                pu.setName(user);
                pu.setDifficulty(difficulty);

                SubmitResult res = new SubmitResult();

                if (saveMsg.has("hash"))
                {
                    String hash_str = saveMsg.getString("hash");
                    res.setHash(new Sha256Hash(hash_str));
                }
                if (saveMsg.has("our_result"))
                {
                    res.setOurResult(saveMsg.getString("our_result"));
                }
                if (saveMsg.has("upstream_result"))
                {
                    res.setUpstreamResult(saveMsg.getString("upstream_result"));
                }
                if (saveMsg.has("reason"))
                {
                    res.setReason(saveMsg.getString("reason"));
                }
                if (saveMsg.has("client"))
                {
                    res.setClientVersion(saveMsg.getString("client"));
                }
                if (saveMsg.has("height"))
                {
                    res.setHeight(saveMsg.getInt("height"));
                } else {
                    res.setHeight(-1); // Meaning unknown
                }

                res.setNetworkDiffiult(block_difficulty);

                inner_saver.saveShare(pu, res, source, uniqueId, blockReward, feeTotal);

                sqs.deleteMessage(new DeleteMessageRequest(queueUrl, msg.getReceiptHandle()));
            }
        }
    }
}