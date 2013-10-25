package com.redbottledesign.bitcoin.pool.checkpoint;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import com.github.fireduck64.sockthing.StratumServer;
import com.google.gson.InstanceCreator;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.agent.PayoutAgent.PayoutPersistenceCallback;
import com.redbottledesign.bitcoin.pool.agent.RoundAgent.RoundPersistenceCallback;
import com.redbottledesign.bitcoin.pool.drupal.DrupalShareSaver;
import com.redbottledesign.bitcoin.pool.drupal.DrupalShareSaver.BlockPersistenceCallback;
import com.redbottledesign.bitcoin.pool.drupal.node.Payout;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;

public class PersistenceCallbackCreator
implements InstanceCreator<PersistenceCallback<?>>
{
    private final StratumServer server;

    public PersistenceCallbackCreator(StratumServer server)
    {
        this.server = server;
    }

    @Override
    public PersistenceCallback<?> createInstance(Type type)
    {
        PersistenceCallback<?> result = null;

        if (type instanceof ParameterizedType)
        {
            ParameterizedType   parameterizedType   = (ParameterizedType)type;
            Type[]              typeArguments       = parameterizedType.getActualTypeArguments();

            if (typeArguments.length == 1)
            {
                Type typeArgument = typeArguments[0];

                // FIXME: This feels like a fragile hack.
                if (!(typeArgument instanceof WildcardType))
                {
                    if (typeArgument instanceof SolvedBlock)
                        result = new BlockPersistenceCallback((DrupalShareSaver)this.server.getShareSaver(), null);

                    else if (typeArgument instanceof Payout)
                        result = new PayoutPersistenceCallback(this.server.getPayoutAgent());

                    else if (typeArgument instanceof Round)
                        result = new RoundPersistenceCallback(this.server.getRoundAgent());
                }
            }
        }

        return result;
    }
}