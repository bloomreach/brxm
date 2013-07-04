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

package org.hippoecm.frontend.plugins.yui.header;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Application;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.onehippo.yui.YuiDependency;
import org.onehippo.yui.YuiNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Wicket Ajax request can generate a response containing additional javascript and css resources
 * to be loaded on the client. Subsequently requesting a component that depends on, for example,
 * six YUI-modules that have to be loaded in a pre-defined order, will lead to lot's of redundant 
 * data in the response, not to mention the IO and file parsing that is produced by the YUI module 
 * loading mechanism on the server.
 * 
 * <p>
 * This class uses the {@link CachedYuiDependencyResolver} to retrieve the <code>Set</code> 
 * of resources belonging to a YUI-module and caches them locally. {@link YuiContext}s will share
 * a reference to the resources, making it possible to skip static resources that have already been
 * loaded on the client.
 * </p>
 * 
 * <p>
 * It will automatically load the YUI-logger through it's local {@link IYuiContext} when running in Wicket development
 * mode. See {@link org.apache.wicket.Application#getConfigurationType}.<br/>
 * It will also the Wicket-Ajax javascript dependencies if needed and subsequently load the hippoajax YUI module, which
 * adds cleanup-hooks for YUI modules as well as some other utility methods.  
 * </p>
 */
public class YuiHeaderCache implements IHeaderContributor {

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(YuiHeaderCache.class);

    private static boolean isCacheEnabled() {
        return Application.get().getConfigurationType().equals(RuntimeConfigurationType.DEPLOYMENT);
    }

    private static boolean isDebugEnabled() {
        return Application.get().getConfigurationType().equals(RuntimeConfigurationType.DEVELOPMENT);
    }

    final Map<ResourceReference.Key, CachedHeaderContributor> referencesCache = new HashMap<ResourceReference.Key, CachedHeaderContributor>();
    final Map<String, Module> moduleCache = new HashMap<String, Module>();
    final Map<String, ModuleSet> moduleSetsCache = new HashMap<String, ModuleSet>();

    final YuiContext localContext = new YuiContext(this);

    private boolean loadWicketAjax = false;

    public YuiHeaderCache(boolean loadWicketAjax) {
        this.loadWicketAjax = loadWicketAjax;

        if (isDebugEnabled()) {
            localContext.addModule(HippoNamespace.NS, "hippologger");
        }
        if (loadWicketAjax) {
            localContext.addModule(HippoNamespace.NS, "hippoajax");
        }
    }

    public void renderHead(IHeaderResponse response) {
        if (loadWicketAjax) {
            CoreLibrariesContributor.contributeAjax(Application.get(), response);
        }
;
        final WebRequest req = (WebRequest) RequestCycle.get().getRequest();
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

    IHeaderContributor getDependenciesSet(YuiNamespace ns, String module) {
        if (moduleSetsCache.containsKey(module)) {
            return moduleSetsCache.get(module);
        } else {
            Set<YuiDependency> dependencies = CachedYuiDependencyResolver.getDependencies(ns, module);
            ModuleSet set = new ModuleSet(dependencies);
            moduleSetsCache.put(module, set);
            return set;
        }
    }

    IHeaderContributor getCssReference(ResourceReference reference) {
        if (referencesCache.containsKey(reference.getKey())) {
            return referencesCache.get(reference.getKey());
        } else {
            CssHeaderContributor ref = new CssHeaderContributor(reference);
            referencesCache.put(reference.getKey(), ref);
            return ref;
        }
    }

    IHeaderContributor getJavascriptReference(ResourceReference reference) {
        if (referencesCache.containsKey(reference.getKey())) {
            return referencesCache.get(reference.getKey());
        } else {
            JavascriptHeaderContributor ref = new JavascriptHeaderContributor(reference);
            referencesCache.put(reference.getKey(), ref);
            return ref;
        }
    }

    static abstract class CachedHeaderContributor implements IHeaderContributor {
        private static final long serialVersionUID = 1L;

        boolean rendered = false;

        public final void renderHead(IHeaderResponse response) {
            if (!rendered || !YuiHeaderCache.isCacheEnabled()) {
                onRenderHead(response);
                rendered = true;
            }
        }

        abstract public void onRenderHead(IHeaderResponse response);
    }

    static abstract class ResourceHeaderContributor extends CachedHeaderContributor {
        private static final long serialVersionUID = 1L;

        ResourceReference reference;

        ResourceHeaderContributor(ResourceReference reference) {
            this.reference = reference;
        }

        public ResourceReference.Key getSharedResourceKey() {
            return reference.getKey();
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

    static class CssHeaderContributor extends ResourceHeaderContributor {
        private static final long serialVersionUID = 1L;

        CssHeaderContributor(ResourceReference reference) {
            super(reference);
        }

        public void onRenderHead(IHeaderResponse response) {
            response.render(CssHeaderItem.forReference(reference, "screen")); //TODO: find out why screen
        }
    }

    static class JavascriptHeaderContributor extends ResourceHeaderContributor {
        private static final long serialVersionUID = 1L;

        JavascriptHeaderContributor(ResourceReference reference) {
            super(reference);
        }

        public void onRenderHead(IHeaderResponse response) {
            response.render(JavaScriptHeaderItem.forReference(reference));
        }
    }

    class Module extends CachedHeaderContributor {
        private static final long serialVersionUID = 1L;

        IHeaderContributor file;
        IHeaderContributor css;
        IHeaderContributor coreCss;

        public Module(YuiDependency dependency) {
            if (dependency.isSourceNotFound() || dependency.getNamespace() == null) {
                String errorMsg = "Unable to find source file for module " + dependency.getModule() + " in namespace "
                        + dependency.getNamespace();
                log.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            } else {
                final boolean debug = isDebugEnabled();

                String path = dependency.getFilePath(debug, isCacheEnabled());
                Class<? extends YuiNamespace> clazz = dependency.getNamespace().getClass();

                file = getJavascriptReference(new JavaScriptResourceReference(clazz, path));

                if (dependency.getHasCss()) {
                    css = getCssReference(new CssResourceReference(clazz, dependency.getCssPath()));
                }
                if (dependency.getHasCoreCss()) {
                    coreCss = getCssReference(new CssResourceReference(clazz, dependency.getCoreCssPath()));
                }
            }
        }

        public void onRenderHead(IHeaderResponse response) {
            if (file != null) {
                file.renderHead(response);
            }
            if (css != null) {
                css.renderHead(response);
            }
            if (coreCss != null) {
                coreCss.renderHead(response);
            }
        }

    }

    class ModuleSet extends CachedHeaderContributor {
        private static final long serialVersionUID = 1L;

        Module[] modules;
        int count = 0;

        public ModuleSet(Set<YuiDependency> dependencies) {
            modules = new Module[dependencies.size()];
            for (YuiDependency dep : dependencies) {
                if (moduleCache.containsKey(dep.getModule())) {
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

        public void onRenderHead(IHeaderResponse response) {
            for (Module mod : modules) {
                mod.renderHead(response);
            }
        }
    }

}
