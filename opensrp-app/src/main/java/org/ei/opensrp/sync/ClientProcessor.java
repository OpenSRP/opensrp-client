package org.ei.opensrp.sync;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import org.ei.opensrp.cloudant.models.Client;
import org.ei.opensrp.cloudant.models.ClientEventModel;
import org.ei.opensrp.cloudant.models.Event;
import org.ei.opensrp.commonregistry.CommonRepository;
import org.ei.opensrp.util.AssetHandler;
import org.ei.opensrp.util.FormUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ClientProcessor {

    private static ClientProcessor instance;
    private CloudantDataHandler mCloudantDataHandler;
    private static final String TAG = "ClientProcessor";

    Context mContext;

    public ClientProcessor(Context context) {
        mContext = context;
        try {
            mCloudantDataHandler = CloudantDataHandler.getInstance(context);
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }

    }

    public static ClientProcessor getInstance(Context context) {
        if (instance == null) {
            instance = new ClientProcessor(context);
        }
        return instance;
    }

    public void processClient() throws Exception {
        CloudantDataHandler handler = CloudantDataHandler.getInstance(mContext);
        //this seems to be easy for now cloudant json to events model is crazy
        List<JSONObject> events = handler.getUpdatedEvents();
        String clienClassificationStr = getFileContents("ec_client_classification.json");
        JSONObject clientClassificationJson = new JSONObject(clienClassificationStr);
        //iterate through the events
        for (JSONObject event : events) {
            // JSONObject event = events.get(0);

            String baseEntityId = event.getString("base_entity_id");
            //for data integrity check if a client exists, if not pull one from cloudant and insert in drishti sqlite db

            //if(clientfromsqlite==null){
            JSONObject client = mCloudantDataHandler.getClientByBaseEntityId(baseEntityId);
            // }

            //  String eventJsonString = FormUtils.getInstance(mContext).getEventAsJsonString(event);
            JSONObject eventJsonObject = event;

            //get the client type classification
            JSONArray clientClasses = clientClassificationJson.getJSONArray("client_type");
            for (int i = 0; i < clientClasses.length(); i++) {
                JSONObject object = clientClasses.getJSONObject(i);
                String type = object.getString("type");

                JSONObject ruleObject = object.getJSONObject("rule");
                JSONArray fields = ruleObject.getJSONArray("fields");

                // keep checking if the event data matches the values expected by each rule, break the moment the rule fails
                for (int j = 0; j < fields.length(); j++) {
                    JSONObject fieldJson = fields.getJSONObject(j);
                    String dataSegment = fieldJson.has("data_segment") ? fieldJson.getString("data_segment") : null;
                    String fieldName = fieldJson.has("field_name") ? fieldJson.getString("field_name") : null;
                    String fieldValue = fieldJson.has("field_value") ? fieldJson.getString("field_value") : null;
                    String responseKey = fieldJson.has("response_key") ? fieldJson.getString("response_key") : null;
                    JSONArray responseValue = fieldJson.has("response_value") ? fieldJson.getJSONArray("response_value") : null;

                    List<String> responseValues = getValues(responseValue);


                    //some fields are in the main doc e.g event_type so fetch them from the main doc
                    if (dataSegment != null && !dataSegment.isEmpty()) {
                        JSONArray jsonDataSegment = eventJsonObject.getJSONArray(dataSegment);
                        //iterate in the segment e.g obs segment
                        for (int k = 0; k < jsonDataSegment.length(); k++) {
                            JSONObject segmentJsonObject = jsonDataSegment.getJSONObject(k);
                            //let's discuss this further, to get the real value in the doc we've to use the keys 'fieldcode' and 'value'

                            String docSegmentFieldValue = segmentJsonObject.get(fieldName) != null ? segmentJsonObject.get(fieldName).toString() : "";
                            String docSegmentResponseValue = segmentJsonObject.get(responseKey) != null ? segmentJsonObject.get(responseKey).toString() : "";


                            if (docSegmentFieldValue.equalsIgnoreCase(fieldValue) && responseValues.contains(docSegmentResponseValue)) {
                                //this is the event obs we're interested in put it in the respective bucket specified by type variable
                                    processCaseModel(event, client, type);
                            }

                        }


                    } else {
                        //fetch from the main doc
                        String docSegmentFieldValue = eventJsonObject.get(fieldName) != null ? eventJsonObject.get(fieldName).toString() : "";

                        if (docSegmentFieldValue.equalsIgnoreCase(fieldValue)) {

                                processCaseModel(event, client, type);
                        }

                    }
                }
            }
        }
    }

    private void processCaseModel(JSONObject event, JSONObject client, String clientType) {
        try {

            JSONObject columnMappings = getColumnMappings(clientType);
            JSONArray columns = columnMappings.getJSONArray("columns");
            String baseEntityId = client.getString("base_entity_id");
            String expectedEncounterType = event.has("event_type")?event.getString("event_type"):null;

            ContentValues contentValues = new ContentValues();
            //TODO FIX ME
            if (clientType.equalsIgnoreCase("ec_mcarechild")) {
                contentValues.put("base_entity_id", baseEntityId);
                contentValues.put("relationalid", baseEntityId);

            } else {
                //Add the base_entity_id
                contentValues.put("base_entity_id", baseEntityId);
            }

            Map<String, List<String>> relationshipMap = getRelationshipMap(baseEntityId);

            for (int i = 0; i < columns.length(); i++) {
                JSONObject colObject = columns.getJSONObject(i);
                String docType = colObject.getString("document_type");
                String columnName = colObject.getString("column_name");
                JSONObject jsonMapping = colObject.getJSONObject("json_mapping");
                String dataSegment = jsonMapping.has("data_segment") ? jsonMapping.getString("data_segment") : null;
                String fieldName = jsonMapping.getString("field_name");
                String fieldValue = jsonMapping.has("field_value") ? jsonMapping.getString("field_value") : null;
                String responseKey = jsonMapping.has("response_key") ? jsonMapping.getString("response_key") : null;
                String encounterType = jsonMapping.has("event_type") ? jsonMapping.getString("event_type") : null;

                String columnValue = null;

                JSONObject jsonDocument = docType.equalsIgnoreCase("Event") ? event : client;

                Object jsonDocSegment = null;

                if (dataSegment != null) {
                    //pick data from a specific section of the doc
                    jsonDocSegment = jsonDocument.get(dataSegment);

                } else {
                    //else the use the main doc as the doc segment
                    jsonDocSegment = jsonDocument;

                }

                //special handler needed to process address,
                if (dataSegment != null && dataSegment.equalsIgnoreCase("adresses")) {
                    Map<String, String> addressMap = getClientAddressAsMap(client);
                    if (addressMap.containsKey(fieldName)) {
                        contentValues.put(columnName, addressMap.get(fieldName).toString());
                    }
                    continue;
                }

                if (jsonDocSegment instanceof JSONArray) {

                    JSONArray jsonDocSegmentArray = (JSONArray) jsonDocSegment;

                    for (int j = 0; j < jsonDocSegmentArray.length(); j++) {
                        JSONObject jsonDocObject = jsonDocSegmentArray.getJSONObject(j);

                        if (fieldValue == null) {
                            //this means field_value and response_key are null so pick the value from the json object for the field_name
                            columnValue = jsonDocObject.getString(fieldName);
                        } else {
                            //this means field_value and response_key are not null e.g when retrieving some value in the events obs section
                            String expectedFieldValue = jsonDocObject.getString(fieldName);
                            //some events can only be differentiated by the event_type value eg pnc1,pnc2, anc1,anc2
                            //check if encountertype (the one in ec_client_fields.json) is null or it matches the encounter type from the ec doc we're processing
                            boolean encounterTypeMatches=(encounterType==null) || (encounterType!=null  && encounterType.equalsIgnoreCase(expectedEncounterType));

                            if (encounterTypeMatches && expectedFieldValue.equalsIgnoreCase(fieldValue) ) {
                                columnValue = jsonDocObject.getString(responseKey);
                            }
                        }
                        // after successfully retrieving the column name and value store it in Content value
                        if (columnValue != null) {
                            contentValues.put(columnName, columnValue);
                        }
                    }

                } else {
                    //e.g client attributes section
                    JSONObject jsonDocSegmentObject = (JSONObject) jsonDocSegment;
                    columnValue = jsonDocSegmentObject.has(fieldName) ? jsonDocSegmentObject.getString(fieldName) : "";
                    // after successfully retrieving the column name and value store it in Content value
                    if (columnValue != null) {
                        contentValues.put(columnName, columnValue);
                    }

                }



            }

            // save the values to db
            Long id = executeInsertStatement(contentValues, clientType);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public Map<String, List<String>> getRelationshipMap(String baseEntityId) throws Exception {
        Client client = mCloudantDataHandler.getClientDocumentByBaseEntityId(baseEntityId);
        Map<String, List<String>> relationshipMap = client.getRelationships();
        if (relationshipMap == null)
            relationshipMap = new HashMap<String, List<String>>();
        try {
            String relationalBaseEntityId = client.getRelationalBaseEntityId();
            if (relationalBaseEntityId != null && !relationalBaseEntityId.equals(client.getBaseEntityId())) { // by default the relationalBaseEntityId is the same as the baseEntityId
                //Build the relationship map
                String clienClassificationStr = getFileContents("ec_client_classification.json");
                JSONObject clientClassificationJson = new JSONObject(clienClassificationStr);
                JSONArray clientTypes = clientClassificationJson.getJSONArray("client_type");
                for (int i = 0; i < clientTypes.length(); i++) {
                    JSONObject obj = clientTypes.getJSONObject(i);
                    String tableName = obj.getString("type");
                    List<String> relationalIds = relationshipMap.get(tableName) != null ? relationshipMap.get(tableName) : new ArrayList<String>();
                    String query = "select * from " + tableName + " where base_entity_id = '" + relationalBaseEntityId + "'";
                    String base_entity_id = queryTableForColumnValue(query, tableName, "base_entity_id");
                    if (base_entity_id != null && !relationalIds.contains(base_entity_id)) { // entity exists in table
                        relationalIds.add(base_entity_id);
                    }
                    relationshipMap.put(tableName, relationalIds);
                }
            }
            client.setRelationships(relationshipMap);
            mCloudantDataHandler.updateDocument(client);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return client.getRelationships();
    }

    public String queryTableForColumnValue(String query, String tableName, String column) {
        String columnValue = null;
        Cursor cursor = null;
        try {
            cursor = queryTable(query, tableName);
            if (cursor != null && cursor.moveToFirst()) { // entity exists in table
                columnValue = cursor.getString(cursor.getColumnIndex(column));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //close the cursor
            if (cursor != null) {
                cursor.close();
            }
        }
        return columnValue;
    }

    public Map<String, String> getClientAddressAsMap(JSONObject client) {
        Map<String, String> addressMap = new HashMap<String, String>();
        try {
            String addressFieldsKey = "addressFields";
            String adressesKey = "adresses";

            if (client.has(adressesKey)) {
                JSONArray addressJsonArray = client.getJSONArray(adressesKey);
                if (addressJsonArray != null && addressJsonArray.length() > 0) {
                    JSONObject addressJson = addressJsonArray.getJSONObject(0);// Need to handle multiple addresses as well
                    if (addressJson.has(addressFieldsKey)) {
                        JSONObject addressFields = addressJson.getJSONObject(addressFieldsKey);
                        Iterator<String> it = addressFields.keys();
                        while (it.hasNext()) {
                            String key = it.next();
                            String value = addressFields.getString(key);
                            addressMap.put(key, value);
                        }
                    }

                    //retrieve the other fields as well
                    Iterator<String> it = addressJson.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        boolean shouldSkipNode = addressJson.get(key) instanceof JSONArray || addressJson.get(key) instanceof JSONObject;
                        if (!shouldSkipNode) {
                            String value = addressJson.getString(key);
                            addressMap.put(key, value);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return addressMap;
    }

    /**
     * Insert the a new record to the database and returns its id
     **/
    private Long executeInsertStatement(ContentValues values, String tableName) {
        CommonRepository cr = org.ei.opensrp.Context.getInstance().commonrepository(tableName);
        Long id = cr.executeInsertStatement(values, tableName);
        return id;
    }

    private Cursor queryTable(String sql, String tableName) {
        CommonRepository cr = org.ei.opensrp.Context.getInstance().commonrepository(tableName);
        Cursor c = cr.queryTable(sql);
        return c;
    }

    private JSONObject getColumnMappings(String registerName) {

        try {
            String clienClassificationStr = getFileContents("ec_client_fields.json");
            JSONObject clientClassificationJson = new JSONObject(clienClassificationStr);
            JSONArray bindObjects = clientClassificationJson.getJSONArray("bindobjects");

            for (int i = 0; i < bindObjects.length(); i++) {

                JSONObject bindObject = bindObjects.getJSONObject(i);
                if (bindObject.getString("name").equalsIgnoreCase(registerName)) {
                    return bindObject;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getFileContents(String fileName) {
        return AssetHandler.readFileFromAssetsFolder(fileName, mContext);
    }

    private List<String> getValues(JSONArray jsonArray) throws JSONException {
        List<String> values = new ArrayList<String>();
        if (jsonArray == null) {
            return values;
        }


        for (int i = 0; i < jsonArray.length(); i++) {
            values.add((String) jsonArray.get(i));
        }
        return values;
    }
}
