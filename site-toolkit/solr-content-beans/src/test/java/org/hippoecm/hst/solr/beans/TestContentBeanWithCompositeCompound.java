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
import org.hippoecm.hst.solr.beans.compound.TestCompositeAddress;
import org.hippoecm.hst.solr.beans.compound.TestExplicitFieldEndingsAddress;

public class TestContentBeanWithCompositeCompound extends TestContentBean {

    private TestCompositeAddress rootAddress;

    public TestContentBeanWithCompositeCompound() {
    }

    public TestContentBeanWithCompositeCompound(String identifier, TestCompositeAddress rootAddress) {
       super(identifier);
       this.rootAddress = rootAddress;
    }

    @IndexField
    public TestCompositeAddress getRootAddress() {
        return rootAddress;
    }

    public void setRootAddress(final TestCompositeAddress rootAddress) {
        this.rootAddress = rootAddress;
    }

}
