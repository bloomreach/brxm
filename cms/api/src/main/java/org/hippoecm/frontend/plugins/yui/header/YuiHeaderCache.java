/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Application;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.io.IClusterable;
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

    final Map<ResourceReference.Key, CachedHeaderItem> referencesCache = new HashMap<ResourceReference.Key, CachedHeaderItem>();
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
            for (CachedHeaderItem contrib : referencesCache.values()) {
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

    HeaderItem getDependenciesSet(YuiNamespace ns, String module) {
        if (moduleSetsCache.containsKey(module)) {
            return moduleSetsCache.get(module);
        } else {
            Set<YuiDependency> dependencies = CachedYuiDependencyResolver.getDependencies(ns, module);
            ModuleSet set = new ModuleSet(dependencies);
            moduleSetsCache.put(module, set);
            return set;
        }
    }

    HeaderItem getCssReference(ResourceReference reference) {
        if (referencesCache.containsKey(reference.getKey())) {
            return referencesCache.get(reference.getKey());
        } else {
            CssHeaderContributor ref = new CssHeaderContributor(reference);
            referencesCache.put(reference.getKey(), ref);
            return ref;
        }
    }

    HeaderItem getJavaScriptReference(ResourceReference reference) {
        if (referencesCache.containsKey(reference.getKey())) {
            return referencesCache.get(reference.getKey());
        } else {
            JavaScriptHeaderContributor ref = new JavaScriptHeaderContributor(reference);
            referencesCache.put(reference.getKey(), ref);
            return ref;
        }
    }

    static abstract class CachedHeaderItem extends HeaderItem implements IClusterable {
        private static final long serialVersionUID = 1L;

        boolean rendered = false;

        @Override
        public Iterable<?> getRenderTokens() {
            return Collections.emptyList();
        }

        boolean shouldRender() {
            return (!rendered || !YuiHeaderCache.isCacheEnabled());
        }
    }

    static abstract class ResourceHeaderContributor extends CachedHeaderItem {
        private static final long serialVersionUID = 1L;

        ResourceReference reference;

        ResourceHeaderContributor(ResourceReference reference) {
            this.reference = reference;
        }

        abstract HeaderItem getHeaderItem();

        @Override
        public void render(final Response response) {
            if (shouldRender()) {
                getHeaderItem().render(response);
                rendered = true;
            }
        }
    }

    static class CssHeaderContributor extends ResourceHeaderContributor {
        private static final long serialVersionUID = 1L;

        CssHeaderContributor(ResourceReference reference) {
            super(reference);
        }

        @Override
        HeaderItem getHeaderItem() {
            return CssHeaderItem.forReference(reference, "screen"); //TODO: find out why screen
        }
    }

    static class JavaScriptHeaderContributor extends ResourceHeaderContributor {
        private static final long serialVersionUID = 1L;

        JavaScriptHeaderContributor(ResourceReference reference) {
            super(reference);
        }

        @Override
        HeaderItem getHeaderItem() {
            return JavaScriptHeaderItem.forReference(reference);
        }
    }

    class Module extends CachedHeaderItem {
        private static final long serialVersionUID = 1L;

        HeaderItem file;
        HeaderItem css;
        HeaderItem coreCss;
        private final String modulePath;

        public Module(YuiDependency dependency) {
            modulePath = dependency.getModulePath();
            if (dependency.isSourceNotFound() || dependency.getNamespace() == null) {
                String errorMsg = "Unable to find source file for module " + dependency.getModule() + " in namespace "
                        + dependency.getNamespace();
                log.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            } else {
                final boolean debug = isDebugEnabled();

                String path = dependency.getFilePath(debug, isCacheEnabled());
                Class<? extends YuiNamespace> clazz = dependency.getNamespace().getClass();

                file = getJavaScriptReference(new JavaScriptResourceReference(clazz, path));

                if (dependency.getHasCss()) {
                    css = getCssReference(new CssResourceReference(clazz, dependency.getCssPath()));
                }
                if (dependency.getHasCoreCss()) {
                    coreCss = getCssReference(new CssResourceReference(clazz, dependency.getCoreCssPath()));
                }
            }
        }

        @Override
        public Iterable<?> getRenderTokens() {
            return Collections.singletonList(modulePath);
        }

        @Override
        public List<HeaderItem> getDependencies() {
            List<HeaderItem> items = new ArrayList<>(3);
            if (file != null) {
                items.add(file);
            }
            if (css != null) {
                items.add(css);
            }
            if (coreCss != null) {
                items.add(coreCss);
            }
            return items;
        }

        public void render(Response response) {
        }

    }

    class ModuleSet extends CachedHeaderItem {
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

        @Override
        public void render(final Response response) {
        }

        @Override
        public List<HeaderItem> getDependencies() {
            List<HeaderItem> items = new ArrayList<>(modules.length);
            Collections.addAll(items, modules);
            return items;
        }
    }

}
