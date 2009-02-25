package org.hippoecm.hst.provider;

import java.util.Calendar;
import java.util.Map;

public interface PropertyMap {

    public abstract Map<String, Boolean[]> getBooleanArrays();

    public abstract Map<String, Boolean> getBooleans();

    public abstract Map<String, Calendar[]> getCalendarArrays();

    public abstract Map<String, Calendar> getCalendars();

    public abstract Map<String, Double[]> getDoubleArrays();

    public abstract Map<String, Double> getDoubles();

    public abstract Map<String, Long[]> getLongArrays();

    public abstract Map<String, Long> getLongs();

    public abstract Map<String, String[]> getStringArrays();

    public abstract Map<String, String> getStrings();

    public Map<String, Object> getAllMapsCombined();
}