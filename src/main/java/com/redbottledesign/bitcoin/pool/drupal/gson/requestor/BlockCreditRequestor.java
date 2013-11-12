package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.drupal.node.BlockCredit;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.gson.JsonEntityResultList;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.NodeRequestor;

public class BlockCreditRequestor
extends NodeRequestor<BlockCredit>
{
    public BlockCreditRequestor(SessionManager sessionManager)
    {
        super(sessionManager);
    }

    @SuppressWarnings("serial")
    public BlockCredit getCredit(final int blockId, final int recipientUserId, final BlockCredit.Type creditType)
    throws IOException, DrupalHttpException
    {
        BlockCredit result;

        result =
            this.requestEntityByCriteria(
                BlockCredit.ENTITY_TYPE,
                new HashMap<String, Object>()
                {{
                    put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, BlockCredit.CONTENT_TYPE);
                    put(BlockCredit.DRUPAL_FIELD_BLOCK,     blockId);
                    put(BlockCredit.DRUPAL_FIELD_RECIPIENT, recipientUserId);
                    put(BlockCredit.DRUPAL_FIELD_TYPE,      creditType.ordinal());
                }});

        return result;
    }

    @Override
    protected Type getListResultType()
    {
        return new TypeToken<JsonEntityResultList<BlockCredit>>(){}.getType();
    }

    @Override
    protected Class<BlockCredit> getNodeType()
    {
        return BlockCredit.class;
    }
}
