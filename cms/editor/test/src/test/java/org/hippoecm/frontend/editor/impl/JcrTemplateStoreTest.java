/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.editor.template.JcrTemplateStore;
import org.hippoecm.editor.type.JcrTypeLocator;
import org.hippoecm.frontend.EditorTestCase;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.junit.Test;

public class JcrTemplateStoreTest extends EditorTestCase {

    @Test
    public void testCurrentTypeTemplate() throws Exception {
        ITypeLocator typeStore = new JcrTypeLocator();
        ITypeDescriptor type = typeStore.locate("test:test");

        IStore<IClusterConfig> templateStore = new JcrTemplateStore(typeStore);
        Map<String, Object> criteria = new MiniMap(1);
        criteria.put("type", type);
        Iterator<IClusterConfig> iter = templateStore.find(criteria);
        assertTrue(iter.hasNext());
        IClusterConfig config = iter.next();
        assertEquals(3, config.getPlugins().size());
    }

    @Test
    public void testHistoricTypeTemplate() throws Exception {
        ITypeLocator typeStore = new JcrTypeLocator();
        ITypeDescriptor type = typeStore.locate("test_0_0:test");

        IStore<IClusterConfig> templateStore = new JcrTemplateStore(typeStore);
        Map<String, Object> criteria = new MiniMap(1);
        criteria.put("type", type);
        Iterator<IClusterConfig> iter = templateStore.find(criteria);
        assertTrue(iter.hasNext());
        IClusterConfig config = iter.next();
        assertEquals(3, config.getPlugins().size());
    }

}
