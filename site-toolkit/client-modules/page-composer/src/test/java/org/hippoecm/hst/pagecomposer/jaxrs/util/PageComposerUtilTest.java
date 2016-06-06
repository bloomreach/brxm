/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.util;

import java.util.Map;

import javax.jcr.Node;

import org.junit.Test;
import org.onehippo.repository.mock.MockNodeFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PageComposerUtilTest {

    @Test
    public void getAnnotatedDefaultValues() throws Exception {
        Node node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-empty-component.xml");
        Map<String, String> defaultAnnotated =  PageComposerUtil.getAnnotatedDefaultValues(node);
        assertTrue(defaultAnnotated.containsKey("parameterOne"));
        assertEquals(defaultAnnotated.get("parameterOne"), "");
        assertTrue(defaultAnnotated.containsKey("parameterTwo"));
        assertEquals(defaultAnnotated.get("parameterTwo"), "test");
    }
}