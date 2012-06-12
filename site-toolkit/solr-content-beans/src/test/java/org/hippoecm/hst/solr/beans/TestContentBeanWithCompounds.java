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
package org.hippoecm.hst.solr.beans;


import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.solr.beans.compound.TestAddress;
import org.hippoecm.hst.solr.beans.compound.TestExplicitFieldEndingsAddress;

public class TestContentBeanWithCompounds extends TestContentBean {

    private TestAddress mainAddress;
    private List<TestAddress> allAddresses;
    private List<TestAddress> copyAddresses;


    private TestExplicitFieldEndingsAddress explicitFieldsAddress;
    
    public TestContentBeanWithCompounds() {
    }

    public TestContentBeanWithCompounds(String identifier, TestAddress mainAddress, List<TestAddress> allAddresses) {
       super(identifier);
       this.mainAddress = mainAddress;
       this.allAddresses = allAddresses;
       this.copyAddresses = new ArrayList<TestAddress>(allAddresses);
    }

    public TestContentBeanWithCompounds(String path, TestExplicitFieldEndingsAddress explicitFieldsAddress) {
        super(path);
        this.explicitFieldsAddress = explicitFieldsAddress;
    }

    @IndexField
    public TestAddress getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(final TestAddress mainAddress) {
        this.mainAddress = mainAddress;
    }

    @IndexField
    public List<TestAddress> getAllAddresses() {
        return allAddresses;
    }

    public void setAllAddresses(final List<TestAddress> allAddresses) {
        this.allAddresses = allAddresses;
    }

    @IndexField(name="theNameOfCopyAddresses")
    public List<TestAddress> getCopyAddresses() {
        return copyAddresses;
    }

    public void setCopyAddresses(final List<TestAddress> copyAddresses) {
        this.copyAddresses = copyAddresses;
    }


    @IndexField
    public TestExplicitFieldEndingsAddress getExplicitFieldsAddress() {
        return explicitFieldsAddress;
    }

    public void setExplicitFieldsAddress(final TestExplicitFieldEndingsAddress explicitFieldsAddress) {
        this.explicitFieldsAddress = explicitFieldsAddress;
    }

}
