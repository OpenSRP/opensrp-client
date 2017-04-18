package org.ei.opensrp.view.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.ei.opensrp.AllConstants;
import org.ei.opensrp.Context;
import org.ei.opensrp.R;
import org.ei.opensrp.broadcastreceivers.OpenSRPClientBroadCastReceiver;
import org.ei.opensrp.sync.CloudantSyncHandler;
import org.ei.opensrp.event.Listener;
import org.ei.opensrp.service.ZiggyService;
import org.ei.opensrp.view.controller.ANMController;
import org.ei.opensrp.view.controller.FormController;
import org.ei.opensrp.view.controller.NavigationController;

import java.util.Map;

import static android.widget.Toast.LENGTH_SHORT;
import static org.ei.opensrp.AllConstants.*;
import static org.ei.opensrp.event.Event.ON_LOGOUT;
import static org.ei.opensrp.util.Log.logInfo;

public abstract class SecuredActivity extends ActionBarActivity {
    protected Context context;
    protected Listener<Boolean> logoutListener;
    protected FormController formController;
    protected ANMController anmController;
    protected NavigationController navigationController;
    private String metaData;
    private OpenSRPClientBroadCastReceiver openSRPClientBroadCastReceiver;
    protected ZiggyService ziggyService;

    public static final String LOG_TAG = "SecuredActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = Context.getInstance().updateApplicationContext(this.getApplicationContext());
        ziggyService = context.ziggyService();

        logoutListener = new Listener<Boolean>() {
            public void onEvent(Boolean data) {
                finish();
            }
        };
        ON_LOGOUT.addListener(logoutListener);

        if (context.IsUserLoggedOut()) {
            DrishtiApplication application = (DrishtiApplication) getApplication();
            application.logoutCurrentUser();
            return;
        }

        formController = new FormController(this);
        anmController = context.anmController();
        navigationController = new NavigationController(this, anmController);
        onCreation();
        

       // Intent replicationServiceIntent = new Intent(this, ReplicationIntentService.class);
        //startService(replicationServiceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (context.IsUserLoggedOut()) {
            DrishtiApplication application = (DrishtiApplication) getApplication();
            application.logoutCurrentUser();
            return;
        }

        onResumption();
        setupReplicationBroadcastReceiver();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.switchLanguageMenuItem) {
            String newLanguagePreference = context.userService().switchLanguagePreference();
            Toast.makeText(this, "Language preference set to " + newLanguagePreference + ". Please restart the application.", LENGTH_SHORT).show();

            return super.onOptionsItemSelected(item);
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(openSRPClientBroadCastReceiver);

    }

    protected abstract void onCreation();

    protected abstract void onResumption();

    public void startFormActivity(String formName, String entityId, String metaData) {
        launchForm(formName, entityId, metaData, FormActivity.class);
    }

    public void startMicroFormActivity(String formName, String entityId, String metaData) {
        launchForm(formName, entityId, metaData, MicroFormActivity.class);
    }

    private void launchForm(String formName, String entityId, String metaData, Class formType) {
        this.metaData = metaData;

        Intent intent = new Intent(this, formType);
        intent.putExtra(FORM_NAME_PARAM, formName);
        intent.putExtra(ENTITY_ID_PARAM, entityId);
        addFieldOverridesIfExist(intent);
        startActivityForResult(intent, FORM_SUCCESSFULLY_SUBMITTED_RESULT_CODE);
    }

    private void addFieldOverridesIfExist(Intent intent) {
        if (hasMetadata()) {
            Map<String, String> metaDataMap = new Gson().fromJson(
                    this.metaData, new TypeToken<Map<String, String>>() {
                    }.getType());
            if (metaDataMap.containsKey(FIELD_OVERRIDES_PARAM)) {
                intent.putExtra(FIELD_OVERRIDES_PARAM, metaDataMap.get(FIELD_OVERRIDES_PARAM));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (isSuccessfulFormSubmission(resultCode)) {
            logInfo("Form successfully saved. MetaData: " + metaData);
            if (hasMetadata()) {
                Map<String, String> metaDataMap = new Gson().fromJson(metaData, new TypeToken<Map<String, String>>() {
                }.getType());
                if (metaDataMap.containsKey(ENTITY_ID) && metaDataMap.containsKey(ALERT_NAME_PARAM)) {
                    Context.getInstance().alertService().changeAlertStatusToInProcess(metaDataMap.get(ENTITY_ID), metaDataMap.get(ALERT_NAME_PARAM));
                }
            }
        }
    }

    private boolean isSuccessfulFormSubmission(int resultCode) {
        return resultCode == AllConstants.FORM_SUCCESSFULLY_SUBMITTED_RESULT_CODE;
    }

    private boolean hasMetadata() {
        return this.metaData != null && !this.metaData.equalsIgnoreCase("undefined");
    }

    /**
     * Called by CloudantSyncHandler when it receives a replication complete callback.
     * CloudantSyncHandler takes care of calling this on the main thread.
     */
    public void replicationComplete() {
        //Toast.makeText(getApplicationContext(), "Replication Complete", Toast.LENGTH_LONG).show();
    }

    /**
     * Called by TasksModel when it receives a replication error callback.
     * TasksModel takes care of calling this on the main thread.
     */
    public void replicationError() {
        Log.e(LOG_TAG, "error()");
        //Toast.makeText(getApplicationContext(), "Replication Error", Toast.LENGTH_LONG).show();
    }

    private void setupReplicationBroadcastReceiver() {
        // The filter's action is BROADCAST_ACTION
        IntentFilter opensrpClientIntentFilter = new IntentFilter(CloudantSync.ACTION_DATABASE_CREATED);
        opensrpClientIntentFilter.addAction(CloudantSync.ACTION_REPLICATION_COMPLETED);
        opensrpClientIntentFilter.addAction(CloudantSync.ACTION_REPLICATION_ERROR);
        opensrpClientIntentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        opensrpClientIntentFilter.addAction("android.intent.action.TIME_SET");

        openSRPClientBroadCastReceiver = new OpenSRPClientBroadCastReceiver(this);
        // Registers the OpenSRPClientBroadCastReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(openSRPClientBroadCastReceiver, opensrpClientIntentFilter);
    }


    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

}