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

package org.hippoecm.frontend.plugins.yui.header;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.header.templates.FinalTextTemplate;
import org.onehippo.yui.YahooNamespace;
import org.onehippo.yui.YuiDependency;
import org.onehippo.yui.YuiNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YuiHeaderContributor implements IHeaderContributor {
    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(YuiHeaderContributor.class);

    private static final List<String> debugModules = Collections.synchronizedList(new ArrayList<String>());
    private static boolean CACHE_ENABLED = Application.get().getConfigurationType().equals(Application.DEPLOYMENT);

    final Set<CachedHeaderContributor> referencesCache = new LinkedHashSet<CachedHeaderContributor>();
    final Set<CachedHeaderContributor> referencesOnPage = new LinkedHashSet<CachedHeaderContributor>();
    final Set<String> modulesOnPage = new HashSet<String>();
    
    final Set<CachedHeaderContributor> localReferences= new LinkedHashSet<CachedHeaderContributor>();
    
    private boolean loadWicketAjax = false;
    
    public YuiHeaderContributor(boolean loadWicketAjax) {
        this.loadWicketAjax = loadWicketAjax;
        
        if (log.isDebugEnabled()) {
            addNewModule(HippoNamespace.NS, "hippologger", localReferences);
        }
        if(loadWicketAjax) {
            addNewModule(HippoNamespace.NS, "hippoajax", localReferences);
        }
    }
    
    public void renderHead(IHeaderResponse response) {
        if(loadWicketAjax) {
            //TODO: cache wicket sources?
            new AbstractDefaultAjaxBehavior() {
                private static final long serialVersionUID = 1L;
                
                @Override
                protected void respond(AjaxRequestTarget target) {
                }
            }.renderHead(response);
        }

        final WebRequestCycle requestCycle = (WebRequestCycle)RequestCycle.get();
        final WebRequest req = (WebRequest) requestCycle.getRequest();
        
        if (!req.isAjax()) {
            referencesOnPage.clear();
            modulesOnPage.clear();
            
            if (log.isDebugEnabled()) {
                modulesOnPage.add("hippologger");
            }
            if (loadWicketAjax) {
                modulesOnPage.add("hippoajax");
            }
            for(CachedHeaderContributor contrib : referencesCache) {
                contrib.rendered = false;
            }
            if(!CACHE_ENABLED) {
                referencesCache.clear();
            }
        }
        renderReferences(localReferences, response);
    }

    private void renderHeadModule(IHeaderResponse response, YuiContext helper) {
        renderReferences(helper.refs, response);
        renderTemplates(helper.templates, response);
        renderOnloads(helper.onloads, response);
    }

    private void renderTemplates(Set<IHeaderContributor> _templates, IHeaderResponse response)  {
        for (IHeaderContributor contrib : _templates) {
            contrib.renderHead(response);
        }
    }
    
    private void renderReferences(Set<CachedHeaderContributor> _references, IHeaderResponse response) {
        for (CachedHeaderContributor contrib : _references) {
            if (!contrib.rendered || !CACHE_ENABLED) {
                contrib.renderHead(response);
                contrib.rendered = true;
            }
        }
    }
    
    public void renderOnloads(Set<String> _onloads, IHeaderResponse response) {
        for (String onload : _onloads) {
            //TODO: test this
            response.renderOnDomReadyJavascript(onload);
            //response.renderOnLoadJavascript(onload);
        }
    }

    public class YuiContext implements IYuiContext {
        private static final long serialVersionUID = 1L;

        final Set<IHeaderContributor> templates = new LinkedHashSet<IHeaderContributor>();
        final Set<CachedHeaderContributor> refs = new LinkedHashSet<CachedHeaderContributor>();
        final Set<String> onloads = new LinkedHashSet<String>();

        public void addModule(String module) {
            addNewModule(YahooNamespace.NS, module, refs);
        }

        public void addModule(YuiNamespace ns, String module) {
            addNewModule(ns, module, refs);
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

        public void addOnload(String string) {
            onloads.add(string);
        }

        public void addCssReference(ResourceReference reference) {
            addNewCssReference(reference, refs);
        }

        public void addJavascriptReference(ResourceReference reference) {
            addNewJavascriptReference(reference, refs);
        }

        public void renderHead(IHeaderResponse response) {
            YuiHeaderContributor.this.renderHeadModule(response, this);
        }
    }

    private void addNewJavascriptReference(ResourceReference reference, Set<CachedHeaderContributor> refs) {
        addNewReference(new JavascriptHeaderContributor(reference), refs);
    }

    private void addNewCssReference(ResourceReference reference, Set<CachedHeaderContributor> refs) {
        addNewReference(new CssHeaderContributor(reference), refs);
    }

    private void addNewReference(CachedHeaderContributor contrib, Set<CachedHeaderContributor> refs) {
        if (!referencesCache.contains(contrib)) {
            referencesCache.add(contrib);
        } else if (CACHE_ENABLED) {
            for(CachedHeaderContributor c : referencesCache) {
                if(contrib.equals(c)) {
                    contrib = c;
                    break;
                }
            }
        }

        if (!referencesOnPage.contains(contrib)) {
            referencesOnPage.add(contrib);
            refs.add(contrib);
        } else if(!CACHE_ENABLED) {
            refs.add(contrib);
        }
    }

    private void addNewModule(YuiNamespace ns, String module, Set<CachedHeaderContributor> refs) {
        if (modulesOnPage.contains(module)) {
            return;
        }
        
        Set<YuiDependency> dependencies = CachedYuiDependencyResolver.getDependencies(ns, module);
        for (YuiDependency dependency : dependencies) {
            if(!modulesOnPage.contains(dependency.getModule())) {
                addYuiDependency(dependency, refs);
                modulesOnPage.add(dependency.getModule());
            }
        }
    }

    private void addYuiDependency(YuiDependency dependency, Set<CachedHeaderContributor> refs) {
        if (dependency.isSourceNotFound() || dependency.getNamespace() == null) {
            String errorMsg = "Unable to find source file for module " + dependency.getModule() + " in namespace "
                    + dependency.getNamespace();
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        } else {
            final boolean debug = log.isDebugEnabled()
                    && (debugModules.size() == 0 || debugModules.contains(dependency.getModule()));

            //TODO: add minified option
            final String path = dependency.getRealModulePath() + ((debug) ? "-debug" : "") + ".js";
            Class<? extends YuiNamespace> clazz = dependency.getNamespace().getClass();

            if (debug) {
                addNewJavascriptReference(new ResourceReference(clazz, path), refs);
            } else {
                addNewJavascriptReference(new JavascriptResourceReference(clazz, path), refs);
            }

            if (dependency.getHasCss()) {
                addNewCssReference(new CompressedResourceReference(clazz, dependency.getCssPath()), refs);
            }
            if (dependency.getHasCoreCss()) {
                addNewCssReference(new CompressedResourceReference(clazz, dependency.getCoreCssPath()),refs);
            }
        }
    }

    abstract class CachedHeaderContributor implements IHeaderContributor {
        private static final long serialVersionUID = 1L;
        
        boolean rendered = false;
    }

    abstract class ResourceHeaderContributor extends CachedHeaderContributor {
        private static final long serialVersionUID = 1L;

        ResourceReference reference;

        ResourceHeaderContributor(ResourceReference reference) {
            this.reference = reference;
        }

        //TODO: only override hashcode?
        @Override
        public String toString() {
            return reference.toString();
        }

        @Override
        public int hashCode() {
            return reference.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            int yourHash = obj.hashCode();
            int myHash = hashCode();
            return obj.hashCode() == hashCode();
        }
    }

    class CssHeaderContributor extends ResourceHeaderContributor {
        private static final long serialVersionUID = 1L;

        CssHeaderContributor(ResourceReference reference) {
            super(reference);
        }

        public void renderHead(IHeaderResponse response) {
            response.renderCSSReference(reference, "screen"); //TODO: find out why screen
        }
    }

    class JavascriptHeaderContributor extends ResourceHeaderContributor {
        private static final long serialVersionUID = 1L;

        JavascriptHeaderContributor(ResourceReference reference) {
            super(reference);
        }

        public void renderHead(IHeaderResponse response) {
            response.renderJavascriptReference(reference);
        }
    }
}
