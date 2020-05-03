package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;
import com.redbottledesign.bitcoin.rpc.stratum.message.ValueResult;

/**
 * <p>Java representation of a Stratum {@code client.get_version} response
 * message, which is used to respond with what software a particular mining
 * client is using.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class ClientGetVersionResponse
extends ResponseMessage
{
    /**
     * Constructor for {@link ClientGetVersionResponse} that creates a new
     * instance with the specified request ID and authorization status.
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     *
     * @param   clientVersion
     *          The version of the mining software.
     */
    public ClientGetVersionResponse(String id, String clientVersion)
    {
        super(id, new ValueResult<>(clientVersion));
    }

    /**
     * Constructor for {@link ClientGetVersionResponse} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public ClientGetVersionResponse(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public ValueResult<String> getResult()
    {
        return (ValueResult<String>)super.getResult();
    }

    /**
     * Returns the client version number reported by the worker.
     *
     * @return  The client version string.
     */
    public String getClientVersion()
    {
        return this.getResult().getValue();
    }
}
