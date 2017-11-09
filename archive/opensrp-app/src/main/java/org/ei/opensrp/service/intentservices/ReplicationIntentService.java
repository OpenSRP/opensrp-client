package org.ei.opensrp.service.intentservices;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.ei.opensrp.Context;
import org.ei.opensrp.sync.CloudantSyncHandler;

import java.util.concurrent.CountDownLatch;


/**
 * Created by onamacuser on 18/03/2016.
 */
public class ReplicationIntentService extends IntentService {
    private static final String TAG = ReplicationIntentService.class.getCanonicalName();

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ReplicationIntentService(String name) {
        super(name);
    }

    public ReplicationIntentService() {

        super("ReplicationIntentService");

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            CloudantSyncHandler mCloudantSyncHandler = CloudantSyncHandler.getInstance(Context.getInstance().applicationContext());
            CountDownLatch mCountDownLatch = new CountDownLatch(2);
            mCloudantSyncHandler.setCountDownLatch(mCountDownLatch);
            mCloudantSyncHandler.startPullReplication();
            mCloudantSyncHandler.startPushReplication();

            mCountDownLatch.await();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }
}
