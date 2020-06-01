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
package org.hippoecm.frontend.plugin.config;

import java.util.List;

import javax.jcr.Session;

import org.apache.wicket.model.IDetachable;

/**
 * The plugin application configuration service.
 * The default cluster will be started when the page is (re)loaded, it can start other clusters as necessary.
 */
public interface IPluginConfigService extends IDetachable {

    boolean checkPermission(Session session);

    IClusterConfig getDefaultCluster();

    IClusterConfig getCluster(String key);

    List<String> listClusters(String folder);

    boolean isSaveOnExitEnabled();


}
