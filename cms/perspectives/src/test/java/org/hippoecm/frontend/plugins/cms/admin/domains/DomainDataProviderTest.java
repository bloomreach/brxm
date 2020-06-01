/*
 * Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.frontend.PluginTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 */
public class DomainDataProviderTest extends PluginTest {

    @Test
    public void testIterator() throws Exception {

        DomainDataProvider provider = new DomainDataProvider();
        long numDomains = provider.getDomainList().size();

        final int pageSize = 6;
        Iterator firstPage = provider.iterator(0, pageSize);

        int firstPageSize = 0;
        while (firstPage.hasNext()) {
            firstPage.next();
            firstPageSize++;
        }

        Iterator secondPage = provider.iterator(pageSize, pageSize);
        int secondPageSize = 0;
        while (secondPage.hasNext()) {
            secondPage.next();
            secondPageSize++;
        }

        assertTrue(firstPageSize == 6);
        assertTrue(secondPageSize == (numDomains - firstPageSize));
    }
}
