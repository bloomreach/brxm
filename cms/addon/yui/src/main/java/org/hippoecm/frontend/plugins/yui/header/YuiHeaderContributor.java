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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Application;
import org.apache.wicket.IClusterable;
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
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(YuiHeaderContributor.class);

    private static final List<String> debugModules = Collections.synchronizedList(new ArrayList<String>());
    private static boolean CACHE_ENABLED = Application.get().getConfigurationType().equals(Application.DEPLOYMENT);

    final Map<String, CachedHeaderContributor> referencesCache = new HashMap<String, CachedHeaderContributor>();
    final Map<String, Module> moduleCache = new HashMap<String, Module>();
    final Map<String, ModuleSet> moduleSetsCache = new HashMap<String, ModuleSet>();
    
    //final Set<ModuleSet> localModules = new LinkedHashSet<ModuleSet>();
    final YuiContext localContext = new YuiContext();
    
    private boolean loadWicketAjax = false;

    public YuiHeaderContributor(boolean loadWicketAjax) {
        this.loadWicketAjax = loadWicketAjax;

        if (log.isDebugEnabled()) {
            localContext.addModule(HippoNamespace.NS, "hippologger");
        }
        if (loadWicketAjax) {
            localContext.addModule(HippoNamespace.NS, "hippoajax");
        }
    }

    public void renderHead(IHeaderResponse response) {
        if (loadWicketAjax) {
            //TODO: cache wicket sources?
            new AbstractDefaultAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void respond(AjaxRequestTarget target) {
                }
            }.renderHead(response);
        }

        final WebRequestCycle requestCycle = (WebRequestCycle) RequestCycle.get();
        final WebRequest req = (WebRequest) requestCycle.getRequest();

        if (!req.isAjax()) {
            for (CachedHeaderContributor contrib : referencesCache.values()) {
                contrib.rendered = false;
            }
            for (ModuleSet set : moduleSetsCache.values()) {
                set.rendered = false;
            }
            for (Module mod : moduleCache.values()) {
                mod.rendered = false;
            }
        }
        localContext.renderHead(response);
    }

    private void renderModules(Set<ModuleSet> modules, IHeaderResponse response) {
        for(ModuleSet ms : modules) {
            ms.renderHead(response);
        }
    }

    private void renderTemplates(Set<IHeaderContributor> _templates, IHeaderResponse response) {
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
            //TODO: make configurable
            response.renderOnDomReadyJavascript(onload);
            //response.renderOnLoadJavascript(onload);
        }
    }

    private ModuleSet getDependenciesSet(YuiNamespace ns, String module) {
        if(moduleSetsCache.containsKey(module)) {
            return moduleSetsCache.get(module);
        } else {
            Set<YuiDependency> dependencies = CachedYuiDependencyResolver.getDependencies(ns, module);
            ModuleSet set = new ModuleSet(dependencies);
            moduleSetsCache.put(module, set);
            return set;
        }
    }

    private CachedHeaderContributor getCssReference(ResourceReference reference) {
        if(referencesCache.containsKey(reference.getSharedResourceKey())) {
            return referencesCache.get(reference.getSharedResourceKey());
        } else {
            CssHeaderContributor ref = new CssHeaderContributor(reference);
            referencesCache.put(ref.getSharedResourceKey(), ref);
            return ref;
        }
    }

    private CachedHeaderContributor getJavascriptReference(ResourceReference reference) {
        if(referencesCache.containsKey(reference.getSharedResourceKey())) {
            return referencesCache.get(reference.getSharedResourceKey());
        } else {
            JavascriptHeaderContributor ref = new JavascriptHeaderContributor(reference);
            referencesCache.put(ref.getSharedResourceKey(), ref);
            return ref;
        }
    }
    
    public class YuiContext implements IYuiContext {
        private static final long serialVersionUID = 1L;

        Set<IHeaderContributor> templates;
        Set<CachedHeaderContributor> refs;
        Set<String> onloads;
        Set<ModuleSet> modules;

        private void initModules() {
            if(modules == null) {
                modules = new LinkedHashSet<ModuleSet>();
            }
        }
        
        private void initRefs() {
            if(refs == null) {
                refs = new LinkedHashSet<CachedHeaderContributor>();
            }
        }
        
        private void initTemplates() {
            if(templates == null) {
                templates = new LinkedHashSet<IHeaderContributor>();
            }
        }
        
        private void initOnloads() {
            if(onloads == null) {
                onloads = new LinkedHashSet<String>();
            }
        }
        
        public void addModule(String module) {
            addModule(YahooNamespace.NS, module);
        }

        public void addModule(YuiNamespace ns, String module) {
            initModules();
            modules.add(getDependenciesSet(ns, module));
        }
        
        public void addTemplate(FinalTextTemplate template) {
            initTemplates();
            templates.add(template);
        }


        public void addTemplate(Class<?> clazz, String filename, Map<String, Object> parameters) {
            initTemplates();
            templates.add(new FinalTextTemplate(clazz, filename, parameters));
        }

        public void addTemplate(DynamicTextTemplate template) {
            initTemplates();
            templates.add(template);
        }

        public void addOnload(String string) {
            initOnloads();
            onloads.add(string);
        }

        public void addCssReference(ResourceReference reference) {
            initRefs();
            refs.add(getCssReference(reference));
        }

        public void addJavascriptReference(ResourceReference reference) {
            initRefs();
            refs.add(getJavascriptReference(reference));
        }

        public void renderHead(IHeaderResponse response) {
            if(modules != null) {
                renderModules(modules, response);
            }
            if(refs != null) {
                renderReferences(refs, response);
            }
            if(templates != null) {
                renderTemplates(templates, response);
            }
            if(onloads != null) {
                renderOnloads(onloads, response);
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
        
        public String getSharedResourceKey() {
            return reference.getSharedResourceKey();
        }

        //TODO: only override hashcode?
        /*
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
            if (obj == null) {
                return false;
            }
            return obj.hashCode() == hashCode();
        }
        */
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
    
    class Module implements IClusterable {
        private static final long serialVersionUID = 1L;
        
        CachedHeaderContributor file;
        CachedHeaderContributor css;
        CachedHeaderContributor coreCss;
        //String name;
        boolean rendered;
    
        public Module(YuiDependency dependency) {
            if (dependency.isSourceNotFound() || dependency.getNamespace() == null) {
                String errorMsg = "Unable to find source file for module " + dependency.getModule() + " in namespace "
                        + dependency.getNamespace();
                log.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            } else {
                //name = dependency.getModule();
                
                final boolean debug = log.isDebugEnabled()
                        && (debugModules.size() == 0 || debugModules.contains(dependency.getModule()));

                //TODO: add minified option
                final String path = dependency.getRealModulePath() + ((debug) ? "-debug" : "") + ".js";
                Class<? extends YuiNamespace> clazz = dependency.getNamespace().getClass();
                
                if (debug) {
                    file = getJavascriptReference(new ResourceReference(clazz, path));
                } else {
                    file = getJavascriptReference(new JavascriptResourceReference(clazz, path));
                }

                if (dependency.getHasCss()) {
                    css = getCssReference(new CompressedResourceReference(clazz, dependency.getCssPath()));
                }
                if (dependency.getHasCoreCss()) {
                    coreCss = getCssReference(new CompressedResourceReference(clazz, dependency.getCoreCssPath()));
                } 
            }
        }

        public void renderHead(IHeaderResponse response) {
            if(!rendered) {
                if(file != null) {
                    file.renderHead(response);
                }
                if(css != null) {
                    css.renderHead(response);
                }
                if(coreCss != null) {
                    coreCss.renderHead(response);
                }
            }
            rendered = true;
        }
        
    }
    
    class ModuleSet implements IClusterable {
        private static final long serialVersionUID = 1L;

        Module[] modules;
        int count = 0;
        boolean rendered;
        
        public ModuleSet(Set<YuiDependency> dependencies) {
            modules = new Module[dependencies.size()];
            for (YuiDependency dep : dependencies) {
                if(moduleCache.containsKey(dep.getModule())) {
                    addModule(moduleCache.get(dep.getModule()));
                } else {
                    Module mod = new Module(dep);
                    moduleCache.put(dep.getModule(), mod);
                    addModule(mod);
                }
            }
        }

        private void addModule(Module m) {
            modules[count++] = m;
        }

        public void renderHead(IHeaderResponse response) {
            if(!rendered) {
                for(Module mod : modules) {
                    mod.renderHead(response);
                }
            }
            rendered = true;
        }
    }
    
}
