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
package org.hippoecm.frontend.plugins.yui.header;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.header.templates.FinalTextTemplate;
import org.onehippo.yui.YahooNamespace;
import org.onehippo.yui.YuiNamespace;

/**
 * The YuiContext uses the shared {@link YuiHeaderCache} service for loading/caching YUI-modules and header 
 * contributions.
 * 
 * <p>
 * Note: the <code>addModule(String module)</code> method will load the provided module within the 
 * {@link YahooNamespace} scope.
 * </p>
 * 
 * @see IYuiContext
 */
public class YuiContext implements IYuiContext {

    private static final long serialVersionUID = 1L;

    Set<IHeaderContributor> templates;
    Set<IHeaderContributor> refs;
    Set<Onload> onloads;
    Set<IHeaderContributor> modules;
    YuiHeaderCache cache;

    static class Onload implements IClusterable {
        private static final long serialVersionUID = 1L;

        public enum Type {
            DOM, WINDOW
        }

        IModel jsModel;
        Type type;

        public Onload(IModel jsModel) {
            this(jsModel, Type.WINDOW);
        }

        public Onload(IModel jsModel, Type type) {
            this.jsModel = jsModel;
            this.type = type;
        }

        public String getJsString() {
            return (String) jsModel.getObject();
        }
    }

    public YuiContext(YuiHeaderCache cache) {
        this.cache = cache;
        modules = new LinkedHashSet<IHeaderContributor>();
        refs = new LinkedHashSet<IHeaderContributor>();
        templates = new LinkedHashSet<IHeaderContributor>();
        onloads = new LinkedHashSet<Onload>();
    }

    public void addModule(String module) {
        addModule(YahooNamespace.NS, module);
    }

    public void addModule(YuiNamespace ns, String module) {
        modules.add(cache.getDependenciesSet(ns, module));
    }

    public void addTemplate(FinalTextTemplate template) {
        templates.add(template);
    }

    public void addTemplate(Class<?> clazz, String filename, Map<String, Object> parameters) {
        templates.add(new FinalTextTemplate(clazz, filename, parameters));
    }

    public void addTemplate(final DynamicTextTemplate template) {
        templates.add(new IHeaderContributor() {
            @Override
            public void renderHead(final IHeaderResponse response) {
                response.render(OnDomReadyHeaderItem.forScript(template.getString()));
            }
        });
    }

    public void addTemplate(IHeaderContributor template) {
        templates.add(template);
    }

    public void addCssReference(ResourceReference reference) {
        refs.add(cache.getCssReference(reference));
    }

    public void addJavascriptReference(ResourceReference reference) {
        refs.add(cache.getJavascriptReference(reference));
    }

    public void addOnDomLoad(String string) {
        addOnDomLoad(new Model(string));
    }

    public void addOnDomLoad(IModel model) {
        onloads.add(new Onload(model, Onload.Type.DOM));
    }

    public void addOnWinLoad(String string) {
        addOnWinLoad(new Model(string));
    }

    public void addOnWinLoad(IModel model) {
        onloads.add(new Onload(model, Onload.Type.WINDOW));
    }

    public void renderHead(IHeaderResponse response) {
        if (modules != null) {
            renderModules(modules, response);
        }
        if (refs != null) {
            renderReferences(refs, response);
        }
        if (templates != null) {
            renderTemplates(templates, response);
        }
        if (onloads != null) {
            renderOnloads(onloads, response);
        }
    }

    private void renderModules(Set<IHeaderContributor> modules, IHeaderResponse response) {
        for (IHeaderContributor ms : modules) {
            ms.renderHead(response);
        }
    }

    private void renderReferences(Set<IHeaderContributor> _references, IHeaderResponse response) {
        for (IHeaderContributor contrib : _references) {
            contrib.renderHead(response);
        }
    }

    private void renderTemplates(Set<IHeaderContributor> _templates, IHeaderResponse response) {
        for (IHeaderContributor contrib : _templates) {
            contrib.renderHead(response);
        }
    }

    public void renderOnloads(Set<Onload> _onloads, IHeaderResponse response) {
        for (Onload onload : _onloads) {
            switch (onload.type) {
            case WINDOW:
                response.render(OnLoadHeaderItem.forScript(onload.getJsString()));
                break;
            case DOM:
                response.render(OnDomReadyHeaderItem.forScript(onload.getJsString()));
                break;
            default:
                break;
            }
        }
    }

}
