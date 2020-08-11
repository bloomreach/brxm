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
package org.onehippo.forge.sitemap.components.util;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.junit.Assert;
import org.junit.Test;
import org.onehippo.forge.sitemap.components.UrlInformationProvider;
import org.onehippo.forge.sitemap.generator.DefaultUrlInformationProvider;

import java.math.BigDecimal;

import static org.onehippo.forge.sitemap.components.util.ReflectionUtils.obtainInstanceForClass;

/**
 */
public class ReflectionUtilsTest {
    @Test
    public void testObtainInstanceForClass() throws Exception {
        UrlInformationProvider urlInformationProvider = obtainInstanceForClass(UrlInformationProvider.class,
                TestUrlInformationProvider.class.getName());

        Assert.assertNotNull(urlInformationProvider);
    }

    public static class TestUrlInformationProvider extends DefaultUrlInformationProvider {

        @Override
        public BigDecimal getPriority(HippoBean hippoBean) {
            return BigDecimal.valueOf(0);
        }
    }
}
