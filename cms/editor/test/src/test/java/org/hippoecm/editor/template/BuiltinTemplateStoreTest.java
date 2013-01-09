/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.editor.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.types.BuiltinTypeDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.JavaTypeDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BuiltinTemplateStoreTest {

    Map<String, ITypeDescriptor> types;
    private ITypeLocator typeLocator;

    private final class TypeLocator implements ITypeLocator {
        public List<ITypeDescriptor> getSubTypes(String type) throws StoreException {
            return Collections.EMPTY_LIST;
        }

        public ITypeDescriptor locate(String type) throws StoreException {
            if (types.containsKey(type)) {
                return types.get(type);
            }
            throw new StoreException("type " + type + " not found");
        }

        public void detach() {
        }
    }

    @Before
    public void setUp() throws Exception {
        typeLocator = new TypeLocator();
        types = new HashMap<String, ITypeDescriptor>();
        JavaTypeDescriptor type = new JavaTypeDescriptor("a", "a", typeLocator);
        JavaFieldDescriptor field = new JavaFieldDescriptor("field",
                new BuiltinTypeDescriptor("String", typeLocator));
        type.addField(field);
        types.put("a", type);
    }

    @Test
    public void testCreatedTemplateOnlyContainsExistingPlugins() {
        BuiltinTemplateStore builtinStore = new BuiltinTemplateStore(typeLocator);
        Map<String, Object> criteria = new MiniMap(1);
        criteria.put("type", types.get("a"));

        Iterator<IClusterConfig> clusters = builtinStore.find(criteria);
        assertTrue(clusters.hasNext());

        IClusterConfig cluster = clusters.next();
        List<IPluginConfig> plugins = cluster.getPlugins();
        assertEquals(1, plugins.size());
    }

    @Test
    public void testCreatedTemplateOnlyContainsFieldsFromSupersWithPlugins() throws StoreException {
        JavaTypeDescriptor type = new JavaTypeDescriptor("b", "b", typeLocator);
        List<String> supers = new LinkedList<String>();
        supers.add("a");
        type.setSuperTypes(supers);

        types.put("b", type);
        types.put("String", new JavaTypeDescriptor("String", "String", null));

        final BuiltinTemplateStore builtinStore = new BuiltinTemplateStore(typeLocator);
        builtinStore.setTemplateLocator(new ITemplateLocator() {
            private static final long serialVersionUID = 1L;

            public IClusterConfig getTemplate(Map<String, Object> criteria) throws StoreException {
                if (criteria.containsKey("type")) {
                    ITypeDescriptor type = (ITypeDescriptor) criteria.get("type");
                    if (type.getName().equals("String")) {
                        return new JavaClusterConfig();
                    }
                }
                throw new StoreException("no template found");
            }
            
        });
        Map<String, Object> criteria = new MiniMap(1);
        criteria.put("type", types.get("b"));

        Iterator<IClusterConfig> clusters = builtinStore.find(criteria);
        assertTrue(clusters.hasNext());

        IClusterConfig cluster = clusters.next();
        List<IPluginConfig> plugins = cluster.getPlugins();
        assertEquals(1, plugins.size());
    }

    @Test
    public void testCreatedTemplateIsComparable() {
        // generate a template for "string" as well
        BuiltinTemplateStore builtinStore = new BuiltinTemplateStore(typeLocator);
        builtinStore.setTemplateLocator(new TemplateLocator(new IStore[] { builtinStore }));
        types.put("String", new JavaTypeDescriptor("String", "String", null));

        Map<String, Object> criteria = new MiniMap(1);
        criteria.put("type", types.get("a"));

        Iterator<IClusterConfig> clusters = builtinStore.find(criteria);
        assertTrue(clusters.hasNext());

        IClusterConfig cluster = clusters.next();
        assertTrue(cluster.getReferences().contains("model.compareTo"));
    }

}
