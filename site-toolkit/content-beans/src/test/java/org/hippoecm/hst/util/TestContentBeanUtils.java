/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * TestContentBeanUtils
 */
public class TestContentBeanUtils {

    @Test
    public void testIsBeanType() throws Exception {
        BaseBean base = new BaseBean();
        TextBean text = new TextBean();
        NewsBean news = new NewsBean();

        assertTrue(ContentBeanUtils.isBeanType(base, BaseBean.class.getName()));
        // We do not support simple name, but only FQCN.
        assertFalse(ContentBeanUtils.isBeanType(base, BaseBean.class.getSimpleName()));
        assertTrue(ContentBeanUtils.isBeanType(text, TextBean.class.getName()));
        // We do not support simple name, but only FQCN.
        assertFalse(ContentBeanUtils.isBeanType(text, TextBean.class.getSimpleName()));
        assertTrue(ContentBeanUtils.isBeanType(news, NewsBean.class.getName()));
        // We do not support simple name, but only FQCN.
        assertFalse(ContentBeanUtils.isBeanType(news, NewsBean.class.getSimpleName()));

        // test supertypes
        assertTrue(ContentBeanUtils.isBeanType(text, BaseBean.class.getName()));
        assertTrue(ContentBeanUtils.isBeanType(news, BaseBean.class.getName()));

        assertFalse(ContentBeanUtils.isBeanType(base, TextBean.class.getName()));
        assertFalse(ContentBeanUtils.isBeanType(base, TextBean.class.getSimpleName()));
        assertFalse(ContentBeanUtils.isBeanType(base, NewsBean.class.getName()));
        assertFalse(ContentBeanUtils.isBeanType(base, NewsBean.class.getSimpleName()));
        assertFalse(ContentBeanUtils.isBeanType(text, NewsBean.class.getName()));
        assertFalse(ContentBeanUtils.isBeanType(text, NewsBean.class.getSimpleName()));

        assertFalse(ContentBeanUtils.isBeanType(news, null));
        assertFalse(ContentBeanUtils.isBeanType(null, NewsBean.class.getName()));
        assertFalse(ContentBeanUtils.isBeanType(null, NewsBean.class.getSimpleName()));
    }

    public static class BaseBean {
    }

    public static class TextBean extends BaseBean {
    }

    public static class NewsBean extends TextBean {
    }

}
