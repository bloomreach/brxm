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
package org.hippoecm.frontend.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;

import org.apache.wicket.util.lang.Objects;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.map.HippoMap;
import org.hippoecm.frontend.model.map.IHippoMap;
import org.hippoecm.frontend.model.map.JcrMap;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.ClusterConfigDecorator;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PluginConfigTest extends PluginTest {

    String[] content = new String[] {
            "/test", "nt:unstructured",
            "/test/map", "nt:unstructured",
                "a", "b",
                "c", "d",
            "/test/config", "frontend:pluginconfig",
                "a", "b",
                "/test/config/sub", "frontend:pluginconfig",
                    "c", "d",
                    "/test/config/typed", "frontendtest:typed",
                        "d1", "3.0",
                        "d2", "3",
                        "l1", "1",
                        "l2", "-1",
                        "b1", "true",
                        "b2", "false",
            "/test/cluster", "frontend:plugincluster",
                "u", "v",
                "/test/cluster/plugin", "frontend:plugin",
                    "c", "d",
                    "x", "${cluster.id}",
                    "/test/cluster/plugin/sub", "frontend:pluginconfig",
                        "a", "b",
                        "y", "${cluster.id}",
                        "/test/cluster/plugin/sub/typed", "frontendtest:typed",
                            "d1", "3.0",
                            "d2", "3",
                            "l1", "1",
                            "l2", "-1",
                            "b1", "true",
                            "b2", "false"};

    @Test
    @SuppressWarnings("unchecked")
    public void testMap() throws Exception {
        build(session, content);
        
        IHippoMap map = new JcrMap(new JcrNodeModel(root.getNode("test/map")));
        assertEquals("b", map.get("a"));

        Set set = map.entrySet();
        assertEquals(2, set.size());

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("a", "b");
        expected.put("c", "d");

        Iterator iter;
        Map.Entry entry;
        String key;
        Set keys;
        Collection values;

        iter = set.iterator();
        assertTrue(iter.hasNext());
        for (int i = 0; i < 2; i++) {
            entry = (Map.Entry) iter.next();
            assertTrue(expected.containsKey(entry.getKey()));
            assertEquals(expected.get(entry.getKey()), entry.getValue());
            expected.remove(entry.getKey());
        }
        assertFalse(iter.hasNext());

        values = map.values();
        assertEquals(2, values.size());
        assertTrue(values.contains("b"));
        assertTrue(values.contains("d"));

        List<String> expectedKeys = new ArrayList(2);
        expectedKeys.add("a");
        expectedKeys.add("c");

        keys = map.keySet();
        assertEquals(2, keys.size());
        iter = keys.iterator();
        for (int i = 0; i < 2; i++) {
            assertTrue(iter.hasNext());
            key = (String) iter.next();
            assertTrue(expectedKeys.contains(key));
            expectedKeys.remove(key);
        }

        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue("b"));
        assertTrue(map.containsKey("c"));
        assertTrue(map.containsValue("d"));

        map.put("a", "c");
        assertEquals("c", map.get("a"));

        map.put("b", Boolean.TRUE);
        assertEquals(Boolean.TRUE, map.get("b"));

        HippoMap subMap = new HippoMap();
        subMap.put("x", "y");
        List list = new ArrayList();
        list.add(subMap);
        map.put("m", list);

        List checkList = (List) map.get("m");
        assertEquals(1, checkList.size());
        IHippoMap checkMap = (IHippoMap) checkList.get(0);
        assertEquals("y", checkMap.get("x"));
    }

    protected IPluginConfig getPluginConfig() throws Exception {
        return new JcrPluginConfig(new JcrNodeModel(root.getNode("test/config")));
    }

    @Test
    public void testConfig() throws Exception {
        build(session, content);
        
        IPluginConfig config = getPluginConfig();
        assertEquals("b", config.getString("a"));

        IPluginConfig subConfig = config.getPluginConfig("sub");
        assertEquals("d", subConfig.getString("c"));
        
        testTypedConfig(config.getPluginConfig("typed"));

        config.put("e", "f");
        assertEquals("f", config.getString("e"));

        subConfig = new JavaPluginConfig();
        subConfig.put("test", "test");
        config.put("x", subConfig);
        subConfig = config.getPluginConfig("x");
        assertEquals("test", subConfig.getString("test"));
    }
    
    @Test
    public void testCopying() throws Exception {
        build(session, content);

        IPluginConfig config = getPluginConfig();
        Set entries = config.entrySet();
        assertEquals(3, entries.size());

        Node previousNode = root.getNode("test").addNode("alt", "frontend:pluginconfig");
        JcrPluginConfig previous = new JcrPluginConfig(new JcrNodeModel(previousNode));

        IPluginConfig backup = new JavaPluginConfig(config);
        config.clear();
        config.putAll(previous);

        previous.clear();
        previous.putAll(backup);        

        entries = previous.entrySet();
        assertEquals(3, entries.size());
    }

    private void testTypedConfig(IPluginConfig config) {
        assertEquals(3.0d, config.getDouble("d1"), 0d);
        assertEquals(3.0d, config.getDouble("d2"), 0d);
        
        assertEquals(1, config.getInt("l1"));
        assertEquals(-1, config.getInt("l2"));

        assertEquals(1l, config.getLong("l1"));
        assertEquals(-1l, config.getLong("l2"));
        
        assertTrue(config.getBoolean("b1"));
        assertFalse(config.getBoolean("b2"));
    }

    protected IClusterConfig getClusterConfig() throws Exception {
        return new JcrClusterConfig(new JcrNodeModel(root.getNode("test/cluster")));
    }

    @Test
    public void testCluster() throws Exception {
        build(session, content);

        IClusterConfig config = getClusterConfig();
        List<IPluginConfig> plugins = config.getPlugins();
        assertEquals(1, plugins.size());
        assertEquals("v", config.getString("u"));

        assertEquals(0, config.getServices().size());
        assertEquals(0, config.getReferences().size());
        assertEquals(0, config.getProperties().size());

        IPluginConfig pluginConfig = plugins.get(0);
        assertEquals("d", pluginConfig.getString("c"));
        assertEquals("${cluster.id}", pluginConfig.getString("x"));

        IPluginConfig subConfig = pluginConfig.getPluginConfig("sub");
        assertEquals("b", subConfig.getString("a"));
        assertEquals("${cluster.id}", subConfig.getString("y"));
        
        testTypedConfig(subConfig.getPluginConfig("typed"));
    }

    @Test
    public void testClusterEditing() throws Exception {
        build(session, content);

        IClusterConfig config = getClusterConfig();
        List<IPluginConfig> plugins = config.getPlugins();
        {
            ArrayList<IPluginConfig> newList = new ArrayList<IPluginConfig>(plugins);
            newList.add(new JavaPluginConfig("abc"));
            config.setPlugins(newList);
        }
        
        assertEquals(2, plugins.size());

        Node node = root.getNode("test/cluster/abc");
        assertEquals("frontend:plugin", node.getPrimaryNodeType().getName());

        ArrayList<IPluginConfig> newList = new ArrayList<IPluginConfig>(plugins);
        IPluginConfig first = newList.set(0, newList.remove(1));
        newList.add(1, first);
        config.setPlugins(newList);

        assertEquals(2, plugins.size());

        assertEquals("abc", plugins.get(0).getName());

        List<String> services = config.getServices();
        services.add("testing");

        IClusterConfig testConfig = getClusterConfig();
        assertEquals(1, testConfig.getServices().size());
        assertEquals("testing", testConfig.getServices().get(0));
    }

    @Test
    public void testDecorator() throws Exception {
        build(session, content);

        IClusterConfig decorator = new ClusterConfigDecorator(getClusterConfig(), "cluster");
        List<IPluginConfig> plugins = decorator.getPlugins();
        assertEquals(1, plugins.size());

        IPluginConfig pluginConfig = plugins.get(0);
        assertEquals("d", pluginConfig.getString("c"));
        assertEquals("cluster", pluginConfig.getString("x"));

        IPluginConfig clone = new JavaPluginConfig(pluginConfig.getPluginConfig("sub"));
        assertTrue(clone.containsKey("u"));

        IPluginConfig subConfig = pluginConfig.getPluginConfig("sub");
        assertEquals("b", subConfig.getString("a"));
        assertEquals("cluster", subConfig.getString("y"));
        assertTrue(subConfig.containsKey("u"));

        testTypedConfig(subConfig.getPluginConfig("typed"));
    }

    @Test
    public void testSerialization() throws Exception {
        build(session, content);

        IPluginConfig original = getPluginConfig();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(original);

        InputStream is = new ByteArrayInputStream(os.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(is);
        IPluginConfig copy = (IPluginConfig) ois.readObject();
        assertEquals("b", copy.getString("a"));

        IPluginConfig subConfig = copy.getPluginConfig("sub");
        assertEquals("d", subConfig.getString("c"));

        testTypedConfig(copy.getPluginConfig("typed"));
        
        copy.put("e", "f");
        assertEquals("f", copy.getString("e"));
        assertEquals("f", original.getString("e"));
    }

    @Test
    public void testWrappedSerialization() throws Exception {
        build(session, content);

        IPluginConfig original = new JavaPluginConfig(getPluginConfig());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(original);

        InputStream is = new ByteArrayInputStream(os.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(is);
        IPluginConfig copy = (IPluginConfig) ois.readObject();
        assertEquals("b", copy.getString("a"));

        IPluginConfig subConfig = copy.getPluginConfig("sub");
        assertEquals("d", subConfig.getString("c"));

        testTypedConfig(copy.getPluginConfig("typed"));
    }

    @Test
    public void testCloning() throws Exception {
        build(session, content);

        IPluginConfig original = getPluginConfig();

        assertEquals("b", original.getString("a"));
        Set entries = original.entrySet();

        IPluginConfig copy = (IPluginConfig) Objects.cloneObject(original);

        assertEquals("b", copy.getString("a"));

        IPluginConfig subConfig = copy.getPluginConfig("sub");
        assertEquals("d", subConfig.getString("c"));
        
        testTypedConfig(copy.getPluginConfig("typed"));
        
        copy.put("e", "f");
        assertEquals("f", copy.getString("e"));
        assertEquals("f", original.getString("e"));
    }
    
}
