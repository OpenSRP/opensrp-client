package org.ei.opensrp.path.service.intent;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.ei.opensrp.Context;
import org.ei.opensrp.clientandeventmodel.Event;
import org.ei.opensrp.domain.Weight;
import org.ei.opensrp.repository.UniqueIdRepository;
import org.ei.opensrp.repository.WeightRepository;
import org.ei.opensrp.util.FileUtilities;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import util.JsonFormUtils;
import util.PathConstants;


/**
 * Created by keyman on 3/01/2017.
 */
public class WeightIntentService extends IntentService {
    private static final String TAG = WeightIntentService.class.getCanonicalName();
    private final WeightRepository weightRepository;


    public WeightIntentService() {

        super("PullUniqueOpenMRSUniqueIdsService");
        weightRepository = Context.getInstance().weightRepository();

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            List<Weight> weights = weightRepository.findUnSyncedBeforeTime(24);
            if (!weights.isEmpty()) {
                for (Weight weight : weights) {
                    String eventType = "Growth Monitoring";
                    String entityType = "weight";

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(JsonFormUtils.KEY, "Weight_Kgs");
                    jsonObject.put(JsonFormUtils.OPENMRS_ENTITY, "concept");
                    jsonObject.put(JsonFormUtils.OPENMRS_ENTITY_ID, "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                    jsonObject.put(JsonFormUtils.OPENMRS_ENTITY_PARENT, "");
                    jsonObject.put(JsonFormUtils.OPENMRS_DATA_TYPE, "decimal");
                    jsonObject.put(JsonFormUtils.VALUE, weight.getKg());


                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(jsonObject);

                    JsonFormUtils.createWeightEvent(getApplicationContext(), weight, eventType, entityType, jsonArray);

                    weightRepository.close(weight.getId());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }


    }
}
