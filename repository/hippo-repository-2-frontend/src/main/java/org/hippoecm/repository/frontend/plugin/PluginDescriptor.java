/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.frontend.plugin;

import org.apache.wicket.IClusterable;

public class PluginDescriptor implements IClusterable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String className;
    
    //wicket component path, looks like 0:some:path:id 
    private String path;

    public PluginDescriptor(String path, String className) {
        this.id = path.substring(path.lastIndexOf(":") + 1);
        this.className = className;
        this.path = path;
    }

    public PluginDescriptor(Plugin plugin) {
        this.id = plugin.getId();
        this.className = plugin.getClass().getName();
        this.path = plugin.getPath();
    }

    public String getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public String getPath() {
        return path;
    }

}
