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
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.header.templates.FinalTextTemplate;
import org.onehippo.yui.YahooNamespace;
import org.onehippo.yui.YuiNamespace;

public class YuiContext implements IYuiContext {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    Set<IHeaderContributor> templates;
    Set<IHeaderContributor> refs;
    Set<OnLoad> onloads;
    Set<IHeaderContributor> modules;
    YuiHeaderCache cache;

    class OnLoad implements IClusterable {
        private static final long serialVersionUID = 1L;

        String str;
        boolean win;

        public OnLoad(String str, boolean onWindowLoad) {
            this.str = str;
            this.win = onWindowLoad;
        }
    }

    public YuiContext(YuiHeaderCache cache) {
        this.cache = cache;
        modules = new LinkedHashSet<IHeaderContributor>();
        refs = new LinkedHashSet<IHeaderContributor>();
        templates = new LinkedHashSet<IHeaderContributor>();
        onloads = new LinkedHashSet<OnLoad>();
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

    public void addTemplate(DynamicTextTemplate template) {
        templates.add(template);
    }

    public void addCssReference(ResourceReference reference) {
        refs.add(cache.getCssReference(reference));
    }

    public void addJavascriptReference(ResourceReference reference) {
        refs.add(cache.getJavascriptReference(reference));
    }

    public void addOnDomLoad(String string) {
        onloads.add(new OnLoad(string, false));
    }

    public void addOnWinLoad(String string) {
        onloads.add(new OnLoad(string, true));
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

    public void renderOnloads(Set<OnLoad> _onloads, IHeaderResponse response) {
        for (OnLoad onload : _onloads) {
            if (onload.win) {
                response.renderOnLoadJavascript(onload.str);
            } else {
                response.renderOnDomReadyJavascript(onload.str);
            }
        }
    }
}
