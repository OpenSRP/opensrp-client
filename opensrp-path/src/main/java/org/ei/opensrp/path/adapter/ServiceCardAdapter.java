package org.ei.opensrp.path.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.apache.commons.lang3.StringUtils;
import org.ei.opensrp.domain.ServiceType;
import org.ei.opensrp.path.domain.Photo;
import org.ei.opensrp.path.domain.ServiceWrapper;
import org.ei.opensrp.path.view.ServiceCard;
import org.ei.opensrp.path.view.ServiceGroup;
import org.ei.opensrp.path.view.VaccineCard;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import util.ImageUtils;
import util.Utils;

import static util.Utils.getName;
import static util.Utils.getValue;

/**
 * Created by keyman on 15/05/2017.
 */
public class ServiceCardAdapter extends BaseAdapter {
    private static final String TAG = "ServiceCardAdapter";
    private final Context context;
    private HashMap<String, ServiceCard> serviceCards;
    private final ServiceGroup serviceGroup;

    public ServiceCardAdapter(Context context, ServiceGroup serviceGroup) throws JSONException {
        this.context = context;
        this.serviceGroup = serviceGroup;
        serviceCards = new HashMap<>();
    }

    @Override
    public int getCount() {
        List<String> types = serviceGroup.getServiceTypeKeys();
        if (types == null || types.isEmpty()) {
            return 0;
        }
        return types.size();
    }

    @Override
    public Object getItem(int position) {
        return serviceCards.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 231231 + position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            String type = serviceGroup.getServiceTypeKeys().get(position);
            if (!serviceCards.containsKey(type)) {
                ServiceCard serviceCard = new ServiceCard(context);
                serviceCard.setOnServiceStateChangeListener(serviceGroup);
                serviceCard.setOnClickListener(serviceGroup);
                serviceCard.getUndoB().setOnClickListener(serviceGroup);
                serviceCard.setId((int) getItemId(position));
                ServiceWrapper serviceWrapper = new ServiceWrapper();
                serviceWrapper.setId(serviceGroup.getChildDetails().entityId());
                serviceWrapper.setGender(serviceGroup.getChildDetails().getDetails().get("gender"));
                serviceWrapper.setName(type);
                serviceWrapper.setDefaultName(type);

                String dobString = Utils.getValue(serviceGroup.getChildDetails().getColumnmaps(), "dob", false);
                if (StringUtils.isNotBlank(dobString)) {
                    Calendar dobCalender = Calendar.getInstance();
                    DateTime dateTime = new DateTime(dobString);
                    dobCalender.setTime(dateTime.toDate());
                    serviceWrapper.setDob(new DateTime(dobCalender.getTime()));
                }

                Photo photo = ImageUtils.profilePhotoByClient(serviceGroup.getChildDetails());
                serviceWrapper.setPhoto(photo);

                String zeirId = getValue(serviceGroup.getChildDetails().getColumnmaps(), "zeir_id", false);
                serviceWrapper.setPatientNumber(zeirId);

                String firstName = getValue(serviceGroup.getChildDetails().getColumnmaps(), "first_name", true);
                String lastName = getValue(serviceGroup.getChildDetails().getColumnmaps(), "last_name", true);
                String childName = getName(firstName, lastName);
                serviceWrapper.setPatientName(childName.trim());

                serviceGroup.updateWrapper(type, serviceWrapper);
                serviceGroup.updateWrapper(serviceWrapper);
                serviceCard.setServiceWrapper(serviceWrapper);

                serviceCards.put(type, serviceCard);
            }

            return serviceCards.get(type);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return null;
    }

    public void update(ArrayList<ServiceWrapper> servicesToUpdate) {
        if (serviceCards != null) {
            if (servicesToUpdate == null) {// Update all vaccines
                for (ServiceCard curCard : serviceCards.values()) {
                    if (curCard != null) curCard.updateState();
                }
            } else {// Update just the vaccines specified
                for (ServiceWrapper currWrapper : servicesToUpdate) {
                    if (serviceCards.containsKey(currWrapper.getName())) {
                        serviceCards.get(currWrapper.getName()).updateState();
                    }
                }
            }
        }
    }

    public ArrayList<ServiceWrapper> getDueServices() {
        ArrayList<ServiceWrapper> dueVaccines = new ArrayList<>();
        if (serviceCards != null) {
            for (ServiceCard curCard : serviceCards.values()) {
                if (curCard != null && (curCard.getState().equals(VaccineCard.State.DUE)
                        || curCard.getState().equals(VaccineCard.State.OVERDUE))) {
                    dueVaccines.add(curCard.getServiceWrapper());
                }
            }
        }

        return dueVaccines;
    }

}