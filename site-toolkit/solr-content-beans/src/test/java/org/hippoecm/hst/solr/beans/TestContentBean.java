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
package org.hippoecm.hst.solr.beans;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.hst.content.beans.index.IndexField;

public class TestContentBean extends TestBaseContentBean {

    private String title;
    private String titleAsWell;
    private String summary;
    private String[] authors;
    private LinkedList<String> authorsAsList;
    private Calendar calendar;
    private Date date;
    private Long mileage;
    private Double price;
    private Boolean sold;
    private long primitiveMileage;
    private double primitivePrice;
    private boolean primitiveSold;

    public TestContentBean() {
       super();
    }

    public TestContentBean(String path) {
        super(path);
    }

    public TestContentBean(String identifier, String title, String summary, String[] authors,
                           Calendar calendar, Date date,
                           Long mileage, Double price, Boolean sold,
                           long primitiveMileage, double primitivePrice, boolean primitiveSold) {
        super(identifier);
        this.title = title;
        this.titleAsWell = title;
        this.summary = summary;
        this.authors = authors;
        this.authorsAsList = new LinkedList<String>();
        authorsAsList.addAll(Arrays.asList(getAuthors()));
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
    public String getTestNullField() {
        return null;
    }
    public void setTestNullField(final String nullField) {
       //
    }

    @IndexField
    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }


    @IndexField(name="titleAgain")
    public String getTitleAsWell() {
        return titleAsWell;
    }

    public void setTitleAsWell(final String titleAsWell) {
        this.titleAsWell = titleAsWell;
    }

    @IndexField
    public String getSummary() {
        return summary;
    }

    // deliberately for testing purposes no setter for summary here

    @IndexField
    public String[] getAuthors() {
        return authors;
    }

    public void setAuthors(final String[] authors) {
        this.authors = authors;
    }

    @IndexField
    public LinkedList<String> getAuthorsAsLinkdedList() {
        if (getAuthors() == null ) {
            return null;
        }
        LinkedList<String> list = new LinkedList<String>();
        list.addAll(Arrays.asList(getAuthors()));
        return list;
    }

    public void setAuthorsAsLinkdedList(LinkedList<String> authorsAsList) {
        this.authorsAsList = authorsAsList;
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
