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
package org.hippoecm.frontend.plugins.yui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Application;
import org.apache.wicket.RequestContext;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WicketEventReference;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.settings.IDebugSettings;
import org.onehippo.yui.YuiDependency;
import org.onehippo.yui.YuiDependencyResolver;
import org.onehippo.yui.YuiNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YuiHeaderContributor extends AbstractHeaderContributor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(YuiHeaderContributor.class);

    private static final YuiDependency HIPPO_LOG_DEPENDENCY = new YuiDependency(HippoNamespace.NS, "hippologger");

    private static final Map<String, ResourceReference> moduleCache = Collections
            .synchronizedMap(new HashMap<String, ResourceReference>());
    private static final Map<String, YuiHeaderContributor> modulesLoaded = Collections
            .synchronizedMap(new HashMap<String, YuiHeaderContributor>());

    private static boolean cacheEnabled = true;
    private static YuiDependencyResolver dependencyResolver = null;


    private static final List<String> debugModules = Collections.synchronizedList(new ArrayList<String>());

    private List<IHeaderContributor> contributors = new ArrayList<IHeaderContributor>();

    //TODO: use thread_local for cache to implement a more decent mechanism than this

    public YuiHeaderContributor() {
        //Make sure all Wicket javascript is loaded first (copied from AbstractDefaultAjaxBehavior)
        contributors.add(new IHeaderContributor() {
            private static final long serialVersionUID = 1L;

            public void renderHead(IHeaderResponse response) {
                response.renderJavascriptReference(WicketEventReference.INSTANCE);
                response.renderJavascriptReference(WicketAjaxReference.INSTANCE);

                final IDebugSettings debugSettings = Application.get().getDebugSettings();
                if (debugSettings.isAjaxDebugModeEnabled()) {
                    response.renderJavascriptReference(new JavascriptResourceReference(
                            AbstractDefaultAjaxBehavior.class, "wicket-ajax-debug.js"));
                    response.renderJavascript("wicketAjaxDebugEnable=true;", "wicket-ajax-debug-enable");
                }

                RequestContext context = RequestContext.get();
                if (context.isPortletRequest()) {
                    response.renderJavascript("Wicket.portlet=true", "wicket-ajax-portlet-flag");
                }

            }
        });
    }

    @Override
    public IHeaderContributor[] getHeaderContributors() {
        if (log.isDebugEnabled()) {
            Set<YuiDependency> dependencies = dependencyResolver.resolveDependencies(HIPPO_LOG_DEPENDENCY);
            for (YuiDependency dep : dependencies) {
                addModuleHeaderContributor(dep);
            }
            addModuleHeaderContributor(HIPPO_LOG_DEPENDENCY);
        }
        return contributors.toArray(new IHeaderContributor[] {});
    }

    private void addModuleHeaderContributor(YuiDependency dependency) {
        contributors.add(new YuiModuleHeaderContributor(dependency));
    }

    public static void debugModule(String module) {
        if (!debugModules.contains(module)) {
            debugModules.add(module);
        }
    }

    public static void debugModules(String... modules) {
        for (String s : modules) {
            debugModule(s);
        }
    }

    public static YuiHeaderContributor forModule(String module) {
        return forModule(new YuiDependency(module));
    }

    public static YuiHeaderContributor forModule(YuiNamespace namespace, String module) {
        return forModule(new YuiDependency(namespace, module, null));
    }

    public static YuiHeaderContributor forModule(YuiDependency dependency) {
        if(dependencyResolver == null) {
            cacheEnabled = Application.get().getConfigurationType().equals(Application.DEPLOYMENT);
            dependencyResolver = new YuiDependencyResolver(cacheEnabled);
        }

        if (cacheEnabled && modulesLoaded.containsKey(dependency.getModule())) {
            return modulesLoaded.get(dependency.getModule());
        }

        YuiHeaderContributor yhc = new YuiHeaderContributor();
        Set<YuiDependency> dependencies = dependencyResolver.resolveDependencies(dependency);
        for (YuiDependency dep : dependencies) {
            yhc.addModuleHeaderContributor(dep);
        }
        if (dependency.getOptionalDependencies() != null) {
            for (YuiDependency optsDependency : dependency.getOptionalDependencies()) {
                yhc.addModuleHeaderContributor(optsDependency);
            }
        }
        yhc.addModuleHeaderContributor(dependency);
        if(cacheEnabled)
            modulesLoaded.put(dependency.getModule(), yhc);
        return yhc;
    }

    class YuiModuleHeaderContributor implements IHeaderContributor {
        private static final long serialVersionUID = 1L;

        private YuiDependency dependency;

        public YuiModuleHeaderContributor(YuiDependency dependency) {
            this.dependency = dependency;
        }

        public void renderHead(IHeaderResponse response) {
            if (dependency.isSourceNotFound() || dependency.getNamespace() == null) {
                log.error("Unable to find source file for module " + dependency.getModule() + " in namespace "
                        + dependency.getNamespace());
            } else {
                final boolean debug = log.isDebugEnabled()
                        && (debugModules.size() == 0 || debugModules.contains(dependency.getModule()));
                final String path = dependency.getRealModulePath() + ((debug) ? "-debug" : "") + ".js";
                Class<? extends YuiNamespace> clazz = dependency.getNamespace().getClass();

                final ResourceReference moduleScript;
                if (moduleCache.containsKey(path)) {
                    moduleScript = moduleCache.get(path);
                } else {
                    if (debug) {
                        moduleScript = new ResourceReference(clazz, path);
                    } else {
                        moduleScript = new JavascriptResourceReference(clazz, path);
                    }
                    moduleCache.put(path, moduleScript);
                }
                response.renderJavascriptReference(moduleScript);

                if (dependency.getHasCss()) {
                    renderCSSReference(dependency.getCssPath(), clazz, response);
                }
                if (dependency.getHasCoreCss()) {
                    renderCSSReference(dependency.getCoreCssPath(), clazz, response);
                }
            }
        }

    }

    private void renderCSSReference(String path, Class<? extends YuiNamespace> clazz, IHeaderResponse response) {
        final String assetPath = path;
        final ResourceReference assetRef;
        if (moduleCache.containsKey(assetPath)) {
            assetRef = moduleCache.get(assetPath);
        } else {
            assetRef = new CompressedResourceReference(clazz, assetPath);
            moduleCache.put(assetPath, assetRef);
        }

        response.renderCSSReference(assetRef, "screen");
    }

}
