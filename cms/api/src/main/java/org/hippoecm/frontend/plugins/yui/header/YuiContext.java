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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Response;
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
    Set<HeaderItem> refs;
    Set<Onload> onloads;
    Set<HeaderItem> modules;
    YuiHeaderCache cache;

    static class Onload implements IClusterable {
        private static final long serialVersionUID = 1L;

        public enum Type {
            DOM, WINDOW
        }

        IModel<String> jsModel;
        Type type;

        public Onload(IModel<String> jsModel, Type type) {
            this.jsModel = jsModel;
            this.type = type;
        }

        public String getJsString() {
            return jsModel.getObject();
        }
    }

    public YuiContext(YuiHeaderCache cache) {
        this.cache = cache;
        modules = new LinkedHashSet<HeaderItem>();
        refs = new LinkedHashSet<HeaderItem>();
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
        refs.add(cache.getJavaScriptReference(reference));
    }

    public void addOnDomLoad(String string) {
        addOnDomLoad(Model.of(string));
    }

    public void addOnDomLoad(IModel<String> model) {
        onloads.add(new Onload(model, Onload.Type.DOM));
    }

    public void addOnWinLoad(String string) {
        addOnWinLoad(Model.of(string));
    }

    public void addOnWinLoad(IModel<String> model) {
        onloads.add(new Onload(model, Onload.Type.WINDOW));
    }

    public HeaderItem getHeaderItem() {
        return new HeaderItem() {

            @Override
            public Iterable<?> getRenderTokens() {
                return Collections.emptyList();
            }

            @Override
            public void render(final Response response) {
            }

            @Override
            public List<HeaderItem> getDependencies() {
                List<HeaderItem> items = new ArrayList<HeaderItem>();
                if (modules != null) {
                    items.addAll(modules);
                }
                if (refs != null) {
                    items.addAll(refs);
                }
                return items;
            }
        };

    }

    public void renderHead(IHeaderResponse response) {
        response.render(getHeaderItem());

        if (templates != null) {
            renderTemplates(templates, response);
        }
        if (onloads != null) {
            renderOnloads(onloads, response);
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
