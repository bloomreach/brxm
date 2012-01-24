/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugin.config.impl;

import java.util.Map;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClusterConfigDecoratorTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Test
    public void testDecoratedPluginConfigContainsSetProperty() {
        JavaPluginConfig baseConfig = new JavaPluginConfig("plugin");
        baseConfig.put("property", "${property}");
        JavaClusterConfig baseCluster = new JavaClusterConfig();
        baseCluster.addPlugin(baseConfig);
        baseCluster.getProperties().add("property");
        
        ClusterConfigDecorator decorated = new ClusterConfigDecorator(baseCluster, "cluster");
        decorated.put("property", "value");
        IPluginConfig decoratedPlugin = decorated.getPlugins().get(0);
        assertTrue(decoratedPlugin.containsKey("property"));
        assertEquals("value", decoratedPlugin.get("property"));
    }

    @Test
    public void testDecoratedPluginConfigDoesNotContainUnsetProperty() {
        JavaPluginConfig baseConfig = new JavaPluginConfig("plugin");
        baseConfig.put("property", "${property}");
        JavaClusterConfig baseCluster = new JavaClusterConfig();
        baseCluster.addPlugin(baseConfig);
        baseCluster.getProperties().add("property");
        
        ClusterConfigDecorator decorated = new ClusterConfigDecorator(baseCluster, "cluster");
        IPluginConfig decoratedPlugin = decorated.getPlugins().get(0);
        assertFalse(decoratedPlugin.containsKey("property"));

        assertEquals(0, decoratedPlugin.entrySet().size());
        int count = 0;
        for (Map.Entry<String, Object> entry : decoratedPlugin.entrySet()) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    public void testDecoratedPluginConfigContainsImplicitProperty() {
        JavaPluginConfig baseConfig = new JavaPluginConfig("plugin");
        JavaClusterConfig baseCluster = new JavaClusterConfig();
        baseCluster.addPlugin(baseConfig);
        baseCluster.getProperties().add("property");

        ClusterConfigDecorator decorated = new ClusterConfigDecorator(baseCluster, "cluster");
        decorated.put("property", "value");
        IPluginConfig decoratedPlugin = decorated.getPlugins().get(0);

        assertTrue(decoratedPlugin.containsKey("property"));
        assertEquals("value", decoratedPlugin.get("property"));

        assertEquals(1, decoratedPlugin.entrySet().size());
        int count = 0;
        for (Map.Entry<String, Object> entry : decoratedPlugin.entrySet()) {
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    public void testDecoratedPluginConfigCanAccessClusterProperty() {
        JavaPluginConfig baseConfig = new JavaPluginConfig("plugin");
        JavaClusterConfig baseCluster = new JavaClusterConfig();
        baseCluster.put("property", "${cluster.id}.value");
        baseCluster.addPlugin(baseConfig);

        ClusterConfigDecorator decorated = new ClusterConfigDecorator(baseCluster, "cluster");
        IPluginConfig decoratedPlugin = decorated.getPlugins().get(0);

        assertTrue(decoratedPlugin.containsKey("property"));
        assertEquals("cluster.value", decoratedPlugin.get("property"));

        assertEquals(1, decoratedPlugin.entrySet().size());
        int count = 0;
        for (Map.Entry<String, Object> entry : decoratedPlugin.entrySet()) {
            count++;
        }
        assertEquals(1, count);
    }


}
