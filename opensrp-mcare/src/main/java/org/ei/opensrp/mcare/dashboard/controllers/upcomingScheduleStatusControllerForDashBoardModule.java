package org.ei.opensrp.mcare.dashboard.controllers;

import android.database.Cursor;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonRepository;

import java.text.SimpleDateFormat;
import java.util.Date;

import dashboard.opensrp.org.jandjdashboard.controller.upcomingScheduleStatusController;

/**
 * Created by raihan on 1/22/18.
 */

public class upcomingScheduleStatusControllerForDashBoardModule extends upcomingScheduleStatusController {



    @Override
    public String houseHoldVisitQuery(Date from, Date to) {
        CommonRepository commonRepository = Context.getInstance().commonrepository("household");
        Cursor cursor = commonRepository.RawCustomQueryForAdapter("select count(*) from alerts where scheduleName = 'FW CENSUS' and (date(startDate) BETWEEN date('"+format.format(from)+"') and date('"+format.format(to)+"'))");
        cursor.moveToFirst();
        try {
            String countofHouseholdVisits = cursor.getString(0);
            cursor.close();
            return countofHouseholdVisits;
        }catch (Exception e){
            cursor.close();
            return "";
        }
    }

    @Override
    public String elcoVisitQuery(Date from, Date to) {
        CommonRepository commonRepository = Context.getInstance().commonrepository("household");
        Cursor cursor = commonRepository.RawCustomQueryForAdapter("select count(*) from alerts where scheduleName = 'ELCO PSRF' and (date(startDate) BETWEEN date('"+format.format(from)+"') and date('"+format.format(to)+"'))");
        cursor.moveToFirst();
        try {
            String countofHouseholdVisits = cursor.getString(0);
            cursor.close();
            return countofHouseholdVisits;
        }catch (Exception e){
            cursor.close();
            return "";
        }
    }

    @Override
    public String eddQuery(Date from, Date to) {
        CommonRepository commonRepository = Context.getInstance().commonrepository("household");
        Cursor cursor = commonRepository.RawCustomQueryForAdapter("select count(*) from mcaremother where " +
                "(( Is_PNC is null or Is_PNC = '0') and FWWOMFNAME not null and FWWOMFNAME != \"\"   AND details  LIKE '%\"FWWOMVALID\":\"1\"%')" +
                "and date(FWPSRLMP,'+259 day') between date('"+format.format(from)+"') and  date('"+format.format(to)+"');");
        cursor.moveToFirst();
        try {
            String countofHouseholdVisits = cursor.getString(0);
            cursor.close();
            return countofHouseholdVisits;
        }catch (Exception e){
            cursor.close();
            return "";
        }
    }

    @Override
    public String ancVisitQuery(Date from, Date to){
        CommonRepository commonRepository = Context.getInstance().commonrepository("household");
        Cursor cursor = commonRepository.RawCustomQueryForAdapter("select count(*) from alerts where scheduleName = 'Ante Natal Care Reminder Visit' and (date(startDate) BETWEEN date('"+format.format(from)+"') and date('"+format.format(to)+"'))");
        cursor.moveToFirst();
        try {
            String countofHouseholdVisits = cursor.getString(0);
            cursor.close();
            return countofHouseholdVisits;
        }catch (Exception e){
            cursor.close();
            return "";
        }
    }

    @Override
    public String pncVisitQuery(Date from, Date to) {
        CommonRepository commonRepository = Context.getInstance().commonrepository("household");
        Cursor cursor = commonRepository.RawCustomQueryForAdapter("select count(*) from alerts where scheduleName = 'Post Natal Care Reminder Visit' and (date(startDate) BETWEEN date('"+format.format(from)+"') and date('"+format.format(to)+"'))");
        cursor.moveToFirst();
        try {
            String countofHouseholdVisits = cursor.getString(0);
            cursor.close();
            return countofHouseholdVisits;
        }catch (Exception e){
            cursor.close();
            return "";
        }
    }

    @Override
    public String neonatalVisitQuery(Date from, Date to) {
        CommonRepository commonRepository = Context.getInstance().commonrepository("household");
        Cursor cursor = commonRepository.RawCustomQueryForAdapter("select count(*) from alerts where scheduleName = 'Essential Newborn Care Checklist' and (date(startDate) BETWEEN date('"+format.format(from)+"') and date('"+format.format(to)+"'))");
        cursor.moveToFirst();
        try {
            String countofHouseholdVisits = cursor.getString(0);
            cursor.close();
            return countofHouseholdVisits;
        }catch (Exception e){
            cursor.close();
            return "";
        }
    }
}