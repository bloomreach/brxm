/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.domains;

import java.util.Iterator;

import javax.jcr.Node;

import org.hippoecm.frontend.PluginTest;
import org.junit.Test;

import junit.framework.Assert;

/**
 */
public class DomainDataProviderTest extends PluginTest {

    @Test
    public void testDataProviderQuery() throws Exception {
        DomainDataProvider provider = new DomainDataProvider();
        Node domainsFolder = root.getNode("hippo:configuration/hippo:domains");
        
        Assert.assertEquals(domainsFolder.getNodes().getSize(), provider.getDomainList().size());
    }

    @Test
    public void testDirtyDataProvider() throws Exception {
        DomainDataProvider provider = new DomainDataProvider();
        Node domainsFolder = root.getNode("hippo:configuration/hippo:domains");
        
        Assert.assertEquals(domainsFolder.getNodes().getSize(), provider.getDomainList().size());
        
        //add a custom domain
        domainsFolder.addNode("myTestDomain1", "hipposys:domain");
        session.save();

        DomainDataProvider.setDirty();
        Assert.assertEquals(domainsFolder.getNodes().getSize(), provider.getDomainList().size());

        //Cleanup
        domainsFolder.getNode("myTestDomain1").remove();
        session.save();
    }

    @Test
    public void testIterator() throws Exception {
        Node domainsFolder = root.getNode("hippo:configuration/hippo:domains");
        long numDomains = domainsFolder.getNodes().getSize();

        DomainDataProvider provider = new DomainDataProvider();

        final int pageSize = 6;
        Iterator firstPage = provider.iterator(0, pageSize);
        Iterator secondPage = provider.iterator(pageSize, pageSize);

        int firstPageSize = 0;
        while (firstPage.hasNext()) {
            firstPage.next();
            firstPageSize++;
        }

        int secondPageSize = 0;
        while (secondPage.hasNext()) {
            secondPage.next();
            secondPageSize++;
        }

        Assert.assertTrue(firstPageSize == 6);
        Assert.assertTrue(secondPageSize == (numDomains - firstPageSize));
    }
}
