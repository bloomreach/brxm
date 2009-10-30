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
package org.hippoecm.frontend.plugins.yui.header;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.header.templates.FinalTextTemplate;
import org.onehippo.yui.YahooNamespace;
import org.onehippo.yui.YuiNamespace;

/**
 * The YuiContext uses the shared {@link YuiHeaderCache} service to load YUI-modules
 * and minimize the response footprint.  
 */
public class YuiContext implements IYuiContext {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addModule(java.lang.String)
     */
    public void addModule(String module) {
        addModule(YahooNamespace.NS, module);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addModule(org.onehippo.yui.YuiNamespace, java.lang.String)
     */
    public void addModule(YuiNamespace ns, String module) {
        modules.add(cache.getDependenciesSet(ns, module));
    }

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addTemplate(org.hippoecm.frontend.plugins.yui.header.templates.FinalTextTemplate)
     */
    public void addTemplate(FinalTextTemplate template) {
        templates.add(template);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addTemplate(java.lang.Class, java.lang.String, java.util.Map)
     */
    public void addTemplate(Class<?> clazz, String filename, Map<String, Object> parameters) {
        templates.add(new FinalTextTemplate(clazz, filename, parameters));
    }

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addTemplate(org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate)
     */
    public void addTemplate(DynamicTextTemplate template) {
        templates.add(template);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addCssReference(org.apache.wicket.ResourceReference)
     */
    public void addCssReference(ResourceReference reference) {
        refs.add(cache.getCssReference(reference));
    }

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addJavascriptReference(org.apache.wicket.ResourceReference)
     */
    public void addJavascriptReference(ResourceReference reference) {
        refs.add(cache.getJavascriptReference(reference));
    }

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addOnDomLoad(java.lang.String)
     */
    public void addOnDomLoad(String string) {
        addOnDomLoad(new Model(string));
    }

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addOnDomLoad(org.apache.wicket.model.IModel)
     */
    public void addOnDomLoad(IModel model) {
        onloads.add(new Onload(model, Onload.Type.DOM));
    }

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addOnWinLoad(java.lang.String)
     */
    public void addOnWinLoad(String string) {
        addOnWinLoad(new Model(string));
    }

    /* (non-Javadoc)
     * @see org.hippoecm.frontend.plugins.yui.header.IYuiContext#addOnWinLoad(org.apache.wicket.model.IModel)
     */
    public void addOnWinLoad(IModel model) {
        onloads.add(new Onload(model, Onload.Type.WINDOW));
    }

    /* (non-Javadoc)
     * @see org.apache.wicket.markup.html.IHeaderContributor#renderHead(org.apache.wicket.markup.html.IHeaderResponse)
     */
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
                response.renderOnLoadJavascript(onload.getJsString());
                break;
            case DOM:
                response.renderOnDomReadyJavascript(onload.getJsString());
                break;
            default:
                break;
            }
        }
    }

}
