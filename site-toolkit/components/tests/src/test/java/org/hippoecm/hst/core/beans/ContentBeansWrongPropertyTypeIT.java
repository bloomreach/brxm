/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ContentBeansWrongPropertyTypeIT extends AbstractBeanTestCase {

    @Test
    public void test_get_multiple_where_single_property_is_stored() throws Exception {

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), getObjectConverter());
        TextBeanEnhanced homeBean = (TextBeanEnhanced)obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");

        assertNotNull(homeBean.getTitleWrong());

        assertTrue(homeBean.getTitleWrong().length == 1);

        assertTrue(homeBean.getTitleWrong()[0].equals(homeBean.getTitle()));
    }

    public static class TextBeanEnhanced extends TextBean {

        public String[] getTitleWrong() {
            return getMultipleProperty("unittestproject:title");
        }

    }

    @Override
    protected Collection<Class<? extends HippoBean>> getAnnotatedClasses() {
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
        annotatedClasses.add(TextBeanEnhanced.class);
        annotatedClasses.add(NewsBean.class);
        return annotatedClasses;
    }

}
