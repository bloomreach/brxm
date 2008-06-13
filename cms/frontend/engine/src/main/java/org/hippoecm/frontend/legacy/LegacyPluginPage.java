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
package org.hippoecm.frontend.legacy;

import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.legacy.plugin.Plugin;

/**
 * @deprecated from the start, needed for handling legacy plugins
 * remove when all legacy plugins have been ported to new services architecture
 */
@Deprecated
public class LegacyPluginPage extends WebPage {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Plugin rootPlugin;

    public void setRootPlugin(Plugin plugin) {
        rootPlugin = plugin;
    }

    public Plugin getRootPlugin() {
        return rootPlugin;
    }

}
