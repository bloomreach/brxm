/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.solr.beans.compound;


import java.util.Calendar;
import java.util.Date;

import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.standard.ContentBean;

public class TestAddress implements ContentBean {

    private String street;
    private int number;
    private Calendar calendar;
    private Date date;
    private Long mileage;
    private Double price;
    private Boolean sold;
    private long primitiveMileage;
    private double primitivePrice;
    private boolean primitiveSold;
    private Integer[] longlat;


    public TestAddress(String street, int number, Calendar calendar, Date date,
                       Long mileage, Double price, Boolean sold,
                       long primitiveMileage, double primitivePrice, boolean primitiveSold, Integer[] longlat) {
        this.street = street;
        this.number = number;
        this.calendar = calendar;
        this.date = date;
        this.mileage = mileage;
        this.price = price;
        this.sold = sold;
        this.primitiveMileage = primitiveMileage;
        this.primitivePrice = primitivePrice;
        this.primitiveSold = primitiveSold;
        this.longlat = longlat;
    }


    @IndexField
    public String getStreet() {
        return street;
    }

    @IndexField
    public int getNumber() {
        return number;
    }

    @IndexField
    public Calendar getCalendar() {
        return calendar;
    }

    @IndexField
    public Date getDate() {
        return date;
    }

    @IndexField
    public Long getMileage() {
        return mileage;
    }

    @IndexField
    public Double getPrice() {
        return price;
    }

    @IndexField
    public Boolean getSold() {
        return sold;
    }

    @IndexField
    public long getPrimitiveMileage() {
        return primitiveMileage;
    }

    @IndexField
    public double getPrimitivePrice() {
        return primitivePrice;
    }

    @IndexField
    public boolean isPrimitiveSold() {
        return primitiveSold;
    }

    @IndexField
    public Integer[] getLonglat() {
        return longlat;
    }

}
