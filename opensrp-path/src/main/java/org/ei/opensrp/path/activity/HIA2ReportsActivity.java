package org.ei.opensrp.path.activity;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.ei.opensrp.Context;
import org.ei.opensrp.path.R;
import org.ei.opensrp.path.application.VaccinatorApplication;
import org.ei.opensrp.path.domain.Hia2Indicator;
import org.ei.opensrp.path.fragment.DailyTalliesFragment;
import org.ei.opensrp.path.fragment.DraftMonthlyFragment;
import org.ei.opensrp.path.fragment.SentMonthlyFragment;
import org.ei.opensrp.path.repository.HIA2IndicatorsRepository;
import org.ei.opensrp.path.view.LocationPickerView;
import org.ei.opensrp.repository.AllSharedPreferences;
import org.ei.opensrp.util.FormUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;

import util.JsonFormUtils;
import util.barcode.BarcodeIntentIntegrator;
import util.barcode.BarcodeIntentResult;

/**
 * Created by coder on 6/7/17.
 */
public class HIA2ReportsActivity extends AppCompatActivity {
    private static String TAG = ChildSmartRegisterActivity.class.getCanonicalName();
    private static final int REQUEST_CODE_GET_JSON = 3432;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private TabLayout tabLayout;

    private int monthlyCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hia2_reports);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        toolbar.setTitle("");
        setTitle("");


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout.setupWithViewPager(mViewPager);

    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return DailyTalliesFragment.newInstance();
                case 1:
                    return DraftMonthlyFragment.newInstance();
                case 2:
                    return SentMonthlyFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.hia2_daily_tallies);
                case 1:
                    return String.format(getString(R.string.hia2_draft_monthly), monthlyCount);
                case 2:
                    return getString(R.string.hia2_sent_monthly);

            }
            return null;
        }
    }

    private Fragment currentFragment() {
        if (mViewPager == null || mSectionsPagerAdapter == null) {
            return null;
        }

        return mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());
    }

    public void startFormActivity(String formName, String entityId, String metaData) {
        try {
            Fragment currentFragment = currentFragment();

            if (currentFragment instanceof DraftMonthlyFragment) {
                HIA2IndicatorsRepository hIA2IndicatorsRepository = VaccinatorApplication.getInstance().hIA2IndicatorsRepository();
                List<Hia2Indicator> hia2Indicators = hIA2IndicatorsRepository.fetchAll();
                if (hia2Indicators == null || hia2Indicators.isEmpty()) {
                    return;
                }

                JSONObject form = FormUtils.getInstance(this).getFormJson(formName);
                JSONObject step1 = form.getJSONObject("step1");
                step1.put("title", "May 2007 Draft");

                JSONArray fields1 = new JSONArray();
                JSONArray fields2 = new JSONArray();
                JSONArray fields3 = new JSONArray();

                for (Hia2Indicator hia2Indicator : hia2Indicators) {
                    JSONObject jsonObject = new JSONObject();
                    if (hia2Indicator.getLabel() == null) {
                        hia2Indicator.setLabel("");
                    }
                    String label = hia2Indicator.getLabel();
                    jsonObject.put("key", label.replace(" ", "_"));
                    jsonObject.put("type", "edit_text");
                    jsonObject.put("hint", label);
                    jsonObject.put("value", String.valueOf((new Random()).nextInt(10) + 1));
                    jsonObject.put("openmrs_entity_parent", "");
                    jsonObject.put("openmrs_entity", "");
                    jsonObject.put("openmrs_entity_id", "");

                    if (hia2Indicator.getIndicatorCode() != null && hia2Indicator.getIndicatorCode().contains("CHN1")) {
                        fields1.put(jsonObject);
                    } else if (hia2Indicator.getIndicatorCode() != null && hia2Indicator.getIndicatorCode().contains("CHN2")) {
                        fields2.put(jsonObject);
                    } else if (hia2Indicator.getIndicatorCode() != null && hia2Indicator.getIndicatorCode().contains("CHN3")) {
                        fields3.put(jsonObject);
                    }

                }

                JSONArray sections = step1.getJSONArray(JsonFormConstants.SECTIONS);

                JSONObject section1 = new JSONObject();
                section1.put(JsonFormConstants.NAME, "Section 1");
                section1.put(JsonFormConstants.FIELDS, fields1);
                sections.put(section1);

                JSONObject section2 = new JSONObject();
                section2.put(JsonFormConstants.NAME, "Section 2");
                section2.put(JsonFormConstants.FIELDS, fields2);
                sections.put(section2);

                JSONObject section3 = new JSONObject();
                section3.put(JsonFormConstants.NAME, "Section 3");
                section3.put(JsonFormConstants.FIELDS, fields3);
                sections.put(section3);

                Intent intent = new Intent(this, PathJsonFormActivity.class);
                intent.putExtra("json", form.toString());
                Log.d(TAG, "form is " + form.toString());
                startActivityForResult(intent, REQUEST_CODE_GET_JSON);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GET_JSON) {
            if (resultCode == RESULT_OK) {

                String jsonString = data.getStringExtra("json");
                Log.d("JSONResult", jsonString);
            }
        }
    }

}
