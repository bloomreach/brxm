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
package org.hippoecm.frontend.editor.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.editor.prototype.IPrototypeStore;
import org.hippoecm.editor.prototype.JcrPrototypeStore;
import org.hippoecm.editor.template.BuiltinTemplateStore;
import org.hippoecm.editor.template.ITemplateLocator;
import org.hippoecm.editor.template.ITemplateStore;
import org.hippoecm.editor.template.JcrTemplateStore;
import org.hippoecm.editor.template.TemplateLocator;
import org.hippoecm.editor.type.JcrDraftLocator;
import org.hippoecm.editor.type.JcrTypeLocator;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceFactory;
import org.hippoecm.frontend.types.ITypeLocator;

public class TemplateEngineFactory implements IServiceFactory<ITemplateEngine>, IDetachable {

    private static final long serialVersionUID = 1L;

    private Map<IPluginContext, TemplateEngine> engines = new HashMap<IPluginContext, TemplateEngine>();

    private ITypeLocator typeLocator;
    private ITemplateLocator templateLocator;
    private IPrototypeStore prototypeStore;

    public TemplateEngineFactory(String prefix) {
        if (prefix == null) {
            typeLocator = new JcrTypeLocator();
        } else {
            typeLocator = new JcrDraftLocator(prefix);
        }
        ITemplateStore jcrTemplateStore = new JcrTemplateStore(typeLocator);
        ITemplateStore builtinTemplateStore = new BuiltinTemplateStore(typeLocator);
        templateLocator = new TemplateLocator(new IStore[] { jcrTemplateStore, builtinTemplateStore });

        prototypeStore = new JcrPrototypeStore();
    }

    public ITemplateEngine getService(IPluginContext context) {
        if (!engines.containsKey(context)) {
            engines.put(context, new TemplateEngine(typeLocator, prototypeStore, templateLocator));
        }
        return engines.get(context);
    }

    public Class<? extends ITemplateEngine> getServiceClass() {
        return ITemplateEngine.class;
    }

    public void releaseService(IPluginContext context, ITemplateEngine service) {
        engines.remove(context);
    }

    public void detach() {
        for (TemplateEngine engine : engines.values()) {
            engine.detach();
        }
    }

}
