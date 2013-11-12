package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.agent.persistence.dedupe.DuplicateDetector;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.gson.JsonEntityResultList;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.NodeRequestor;
import com.redbottledesign.drupal.gson.requestor.SortOrder;
import com.redbottledesign.gson.GsonUtils;

public class RoundRequestor
extends NodeRequestor<Round>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateDetector.class);

    private static final String JSON_PARAM_LIMIT = "limit";

    public RoundRequestor(SessionManager sessionManager)
    {
        super(sessionManager);
    }

    @SuppressWarnings("serial")
    public Round getRound(final Date startDate)
    throws IOException, DrupalHttpException
    {
        Round               result              = null;
        final long          startTimeInSeconds  = TimeUnit.SECONDS.convert(startDate.getTime(), TimeUnit.MILLISECONDS);
        Map<String, Object> criteriaMap         = new HashMap<String, Object>()
        {{
            put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME,   Round.CONTENT_TYPE);
            put(Round.DRUPAL_FIELD_ROUND_DATES_START, startTimeInSeconds);
        }};

        result = this.requestEntityByCriteria(Round.ENTITY_TYPE, criteriaMap);

        return result;
    }

    @SuppressWarnings("serial")
    public Round requestCurrentRound()
    throws IOException, DrupalHttpException
    {
        Round               currentRound    = null;
        Map<String, Object> criteriaMap     = new HashMap<String, Object>()
        {{
            put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, Round.CONTENT_TYPE);
            put(Node.DRUPAL_PUBLISHED_FIELD_NAME,   1);
            put(JSON_PARAM_LIMIT,                   1);
        }};

        // Find the oldest round.
        currentRound =
            this.requestEntityByCriteria(
                Round.ENTITY_TYPE,
                criteriaMap,
                Round.DRUPAL_FIELD_ROUND_DATES,
                SortOrder.DESCENDING);

        return currentRound;
    }

    @SuppressWarnings("serial")
    public List<Round> requestAllOpenRounds()
    throws IOException, DrupalHttpException
    {
        List<Round>         openRounds  = Collections.emptyList();
        Map<String, Object> criteriaMap = new HashMap<String, Object>()
        {{
            put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, Round.CONTENT_TYPE);
            put(Node.DRUPAL_PUBLISHED_FIELD_NAME,   1);
            put(Round.DRUPAL_FIELD_ROUND_STATUS,    GsonUtils.getSerializedName(Round.Status.class, Round.Status.OPEN));
        }};

        // Find the oldest round.
        openRounds =
            this.requestEntitiesByCriteria(
                Round.ENTITY_TYPE,
                criteriaMap,
                Round.DRUPAL_FIELD_ROUND_DATES,
                SortOrder.DESCENDING);

        return openRounds;
    }

    @Override
    protected Type getListResultType()
    {
        return new TypeToken<JsonEntityResultList<Round>>(){}.getType();
    }

    @Override
    protected Class<Round> getNodeType()
    {
        return Round.class;
    }
}