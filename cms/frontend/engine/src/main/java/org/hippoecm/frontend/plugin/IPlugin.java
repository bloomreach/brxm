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
package org.hippoecm.frontend.plugin;

import org.apache.wicket.IClusterable;

/**
 * Marker interface for classes that can be instantiated as a plugin.
 * There has to be a constructor with signature <init>(IPluginContext, IPluginConfig);
 * <p>
 * In general, the services that are available to a plugin may come and go at any moment,
 * so the plugin lifecycle is rather minimal.  The {@link IActivator} interface may be
 * implemented as well, it is provided to ease subclassing. 
 */
public interface IPlugin extends IClusterable {
    final static String SVN_ID = "$Id$";

    String CLASSNAME = "plugin.class";
}
