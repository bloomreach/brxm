/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.Calendar;
import java.util.List;

import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentMultiProperty;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentProperty;

/**
 * @version "$Id$"
 */
@PersistentNode(type = "essentials:document")
public class TestBeanDocument extends BaseDocument {

    public TestBeanDocument(final String name, final String parentPath) {
        super(name, parentPath);
    }

    public TestBeanDocument() {
    }

    @PersistentProperty(name = "intCounter")
    private int intCounter;

    @PersistentProperty(name = "booleanVal")
    private boolean booleanVal;


    @PersistentMultiProperty(name = "booleanValArray", type = boolean.class)
    private List<Boolean> booleanValArray;

    @PersistentMultiProperty(name = "intCounterArray", type = int.class)
    private List<Integer> intCounterArray;
    @PersistentProperty(name = "longCounter")
    private long longCounter;
    @PersistentMultiProperty(name = "longCounterArray", type = long.class)
    private List<Long> longCounterArray;
    @PersistentProperty(name = "doubleCounter")
    private double doubleCounter;
    @PersistentMultiProperty(name = "doubleCounterArray", type = double.class)
    private List<Double> doubleCounterArray;
    @PersistentProperty(name = "stringType")
    private String stringType;
    @PersistentMultiProperty(name = "stringTypeArray")
    private List<String> stringTypeArray;
    @PersistentProperty(name = "dateType")
    private Calendar dateType;
    @PersistentMultiProperty(name = "dateTypeArray", type = Calendar.class)
    private List<Calendar> dateTypeArray;

    public boolean isBooleanVal() {
        return booleanVal;
    }

    public void setBooleanVal(final boolean booleanVal) {
        this.booleanVal = booleanVal;
    }

    public List<Boolean> getBooleanValArray() {
        return booleanValArray;
    }

    public void setBooleanValArray(final List<Boolean> booleanValArray) {
        this.booleanValArray = booleanValArray;
    }


    public List<Integer> getIntCounterArray() {
        return intCounterArray;
    }

    public void setIntCounterArray(final List<Integer> intCounterArray) {
        this.intCounterArray = intCounterArray;
    }

    public List<Long> getLongCounterArray() {
        return longCounterArray;
    }

    public void setLongCounterArray(final List<Long> longCounterArray) {
        this.longCounterArray = longCounterArray;
    }

    public List<Double> getDoubleCounterArray() {
        return doubleCounterArray;
    }

    public void setDoubleCounterArray(final List<Double> doubleCounterArray) {
        this.doubleCounterArray = doubleCounterArray;
    }

    public List<String> getStringTypeArray() {
        return stringTypeArray;
    }

    public void setStringTypeArray(final List<String> stringTypeArray) {
        this.stringTypeArray = stringTypeArray;
    }

    public List<Calendar> getDateTypeArray() {
        return dateTypeArray;
    }

    public void setDateTypeArray(final List<Calendar> dateTypeArray) {
        this.dateTypeArray = dateTypeArray;
    }

    public Calendar getDateType() {
        return dateType;
    }

    public void setDateType(final Calendar dateType) {
        this.dateType = dateType;
    }

    public int getIntCounter() {
        return intCounter;
    }

    public void setIntCounter(final int intCounter) {
        this.intCounter = intCounter;
    }

    public long getLongCounter() {
        return longCounter;
    }

    public void setLongCounter(final long longCounter) {
        this.longCounter = longCounter;
    }

    public double getDoubleCounter() {
        return doubleCounter;
    }

    public void setDoubleCounter(final double doubleCounter) {
        this.doubleCounter = doubleCounter;
    }

    public String getStringType() {
        return stringType;
    }

    public void setStringType(final String stringType) {
        this.stringType = stringType;
    }
}
