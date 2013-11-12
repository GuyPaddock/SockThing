package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.drupal.node.WorkShare;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.gson.JsonEntityResultList;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.NodeRequestor;

public class WorkShareRequestor
extends NodeRequestor<WorkShare>
{
    public WorkShareRequestor(SessionManager sessionManager)
    {
        super(sessionManager);
    }

    @SuppressWarnings("serial")
    public WorkShare getShare(final String jobHash, final int submitterUserId, final int roundId)
    throws IOException, DrupalHttpException
    {
        WorkShare result;

        result =
            this.requestEntityByCriteria(
                WorkShare.ENTITY_TYPE,
                new HashMap<String, Object>()
                {{
                    put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, WorkShare.CONTENT_TYPE);
                    put(WorkShare.DRUPAL_FIELD_JOB_HASH,    jobHash);
                    put(WorkShare.DRUPAL_FIELD_SUBMITTER,   submitterUserId);
                    put(WorkShare.DRUPAL_FIELD_ROUND,       roundId);
                }});

        return result;
    }

    @Override
    protected Type getListResultType()
    {
        return new TypeToken<JsonEntityResultList<WorkShare>>(){}.getType();
    }

    @Override
    protected Class<WorkShare> getNodeType()
    {
        return WorkShare.class;
    }
}
