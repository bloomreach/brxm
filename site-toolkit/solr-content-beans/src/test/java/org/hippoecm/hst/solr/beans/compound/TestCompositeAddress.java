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


import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.standard.ContentBean;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;

public class TestCompositeAddress implements ContentBean {

    private String street;
    private int number;
    private int numberWithEndMapping;
    private TestCompositeAddress child;

    public TestCompositeAddress(String street, int number, TestCompositeAddress child) {
        this.street = street;
        this.number = number;
        this.numberWithEndMapping = number;
        this.child = child;
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

    @IndexField(name = "numberAsWell_i")
    public int getNumberWithEndMapping() {
        return numberWithEndMapping;
    }

    public void setNumberWithEndMapping(final int numberWithEndMapping) {
        this.numberWithEndMapping = numberWithEndMapping;
    }

    @IndexField
    public TestCompositeAddress getChild() {
        return child;
    }

}
