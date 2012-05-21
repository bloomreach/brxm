/*
 *  Copyright 2012 Hippo.
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


    public TestAddress(String street, int number, Calendar calendar, Date date,
                       Long mileage, Double price, Boolean sold,
                       long primitiveMileage, double primitivePrice, boolean primitiveSold) {
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
    }


    @IndexField
    public String getStreet() {
        return street;
    }

    public void setStreet(final String street) {
        this.street = street;
    }


    @IndexField
    public int getNumber() {
        return number;
    }

    public void setNumber(final int number) {
        this.number = number;
    }

    @IndexField
    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(final Calendar calendar) {
        this.calendar = calendar;
    }

    @IndexField
    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    @IndexField
    public Long getMileage() {
        return mileage;
    }

    public void setMileage(final Long mileage) {
        this.mileage = mileage;
    }

    @IndexField
    public Double getPrice() {
        return price;
    }

    public void setPrice(final Double price) {
        this.price = price;
    }

    @IndexField
    public Boolean getSold() {
        return sold;
    }

    public void setSold(final Boolean sold) {
        this.sold = sold;
    }

    @IndexField
    public long getPrimitiveMileage() {
        return primitiveMileage;
    }

    public void setPrimitiveMileage(final long primitiveMileage) {
        this.primitiveMileage = primitiveMileage;
    }

    @IndexField
    public double getPrimitivePrice() {
        return primitivePrice;
    }

    public void setPrimitivePrice(final double primitivePrice) {
        this.primitivePrice = primitivePrice;
    }

    @IndexField
    public boolean isPrimitiveSold() {
        return primitiveSold;
    }

    public void setPrimitiveSold(final boolean primitiveSold) {
        this.primitiveSold = primitiveSold;
    }
}
