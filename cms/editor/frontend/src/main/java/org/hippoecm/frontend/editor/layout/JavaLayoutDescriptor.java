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
package org.hippoecm.frontend.editor.layout;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.core.util.resource.UrlResourceStream;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.util.resource.IResourceStream;

public class JavaLayoutDescriptor implements ILayoutDescriptor {

    private static final long serialVersionUID = 1L;

    private String name;
    private String plugin;
    private URL icon;
    private Map<String, ILayoutPad> pads;
    
    public JavaLayoutDescriptor(String plugin) {
        this.plugin = plugin;
        this.name = plugin.substring(plugin.lastIndexOf('.') + 1);
        this.pads = new TreeMap<String, ILayoutPad>();
    }
    
    public void setIconLocation(URL url) {
        this.icon = url;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void addPad(ILayoutPad pad) {
        this.pads.put(pad.getName(), pad);
    }
    
    public IResource getIcon() {
        return new ResourceStreamResource() {
            private static final long serialVersionUID = 1L;

            @Override
            public IResourceStream getResourceStream() {
                return new UrlResourceStream(icon);
            }
            
        };
    }

    public Map<String, ILayoutPad> getLayoutPads() {
        return pads;
    }

    public String getPluginClass() {
        return plugin;
    }

    public String getVariant() {
        return null;
    }

    public IModel<String> getName() {
        return new Model<String>(name);
    }
    
}
