/*
 *  Copyright 2009 Hippo.
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

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;

public class RenderContext implements IRenderContext {
    private static final long serialVersionUID = 1L;

    public static final String VARIANT = "wicket.variant";

    private IPluginConfig config;

    public RenderContext(IPluginContext context, IPluginConfig config) {
        this.config = config;
    }

    public ILayoutDescriptor getLayoutDescriptor() {
        // locate resource stream
        String className = config.getString(IPlugin.CLASSNAME);
        String variant = config.getString(VARIANT);
        String name = className + (variant != null ? "_" + variant : "");

        return new XmlLayoutDescriptor(new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                return ((UserSession) Session.get()).getClassLoader();
            }

            public void setObject(Object object) {
                // TODO Auto-generated method stub
                
            }

            public void detach() {
                // TODO Auto-generated method stub
                
            }
            
        }, name);
    }

}
