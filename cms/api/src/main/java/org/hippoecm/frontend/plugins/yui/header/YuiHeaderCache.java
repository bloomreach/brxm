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

import java.util.HashMap;
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
        return Application.get().getConfigurationType().equals(Application.DEPLOYMENT);
    }

    private static boolean isDebugEnabled() {
        return Application.get().getConfigurationType().equals(Application.DEVELOPMENT);
    }

    final Map<String, CachedHeaderContributor> referencesCache = new HashMap<String, CachedHeaderContributor>();
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
        if (referencesCache.containsKey(reference.getSharedResourceKey())) {
            return referencesCache.get(reference.getSharedResourceKey());
        } else {
            CssHeaderContributor ref = new CssHeaderContributor(reference);
            referencesCache.put(ref.getSharedResourceKey(), ref);
            return ref;
        }
    }

    IHeaderContributor getJavascriptReference(ResourceReference reference) {
        if (referencesCache.containsKey(reference.getSharedResourceKey())) {
            return referencesCache.get(reference.getSharedResourceKey());
        } else {
            JavascriptHeaderContributor ref = new JavascriptHeaderContributor(reference);
            referencesCache.put(ref.getSharedResourceKey(), ref);
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

    static class CssHeaderContributor extends ResourceHeaderContributor {
        private static final long serialVersionUID = 1L;

        CssHeaderContributor(ResourceReference reference) {
            super(reference);
        }

        public void onRenderHead(IHeaderResponse response) {
            response.renderCSSReference(reference, "screen"); //TODO: find out why screen
        }
    }

    static class JavascriptHeaderContributor extends ResourceHeaderContributor {
        private static final long serialVersionUID = 1L;

        JavascriptHeaderContributor(ResourceReference reference) {
            super(reference);
        }

        public void onRenderHead(IHeaderResponse response) {
            response.renderJavascriptReference(reference);
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
