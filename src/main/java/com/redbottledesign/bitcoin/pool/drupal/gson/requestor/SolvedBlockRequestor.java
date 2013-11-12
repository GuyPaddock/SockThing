package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.gson.JsonEntityResultList;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.NodeRequestor;
import com.redbottledesign.gson.GsonUtils;

public class SolvedBlockRequestor
extends NodeRequestor<SolvedBlock>
{
    public SolvedBlockRequestor(SessionManager sessionManager)
    {
        super(sessionManager);
    }

    @SuppressWarnings("serial")
    public List<SolvedBlock> getUnconfirmedBlocks()
    throws IOException, DrupalHttpException
    {
        List<SolvedBlock>   unconfirmedBlocks   = Collections.emptyList();
        Map<String, Object> criteriaMap         = new HashMap<String, Object>()
            {{
                put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, SolvedBlock.CONTENT_TYPE);
                put(Node.DRUPAL_PUBLISHED_FIELD_NAME,   1);
                put(SolvedBlock.DRUPAL_FIELD_STATUS,    GsonUtils.getSerializedName(SolvedBlock.Status.class, SolvedBlock.Status.UNCONFIRMED));
            }};

        unconfirmedBlocks = this.requestEntitiesByCriteria(SolvedBlock.ENTITY_TYPE, criteriaMap);

        return unconfirmedBlocks;
    }

    @SuppressWarnings("serial")
    public SolvedBlock getBlock(final String blockHash, final long blockHeight)
    throws IOException, DrupalHttpException
    {
        SolvedBlock result;

        result =
            this.requestEntityByCriteria(
                SolvedBlock.ENTITY_TYPE,
                new HashMap<String, Object>()
                {{
                    put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, SolvedBlock.CONTENT_TYPE);
                    put(SolvedBlock.DRUPAL_FIELD_HASH,      blockHash);
                    put(SolvedBlock.DRUPAL_FIELD_HEIGHT,    blockHeight);
                }});

        return result;
    }

    @Override
    protected Type getListResultType()
    {
        return new TypeToken<JsonEntityResultList<SolvedBlock>>(){}.getType();
    }

    @Override
    protected Class<SolvedBlock> getNodeType()
    {
        return SolvedBlock.class;
    }
}