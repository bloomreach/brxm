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
package org.hippoecm.frontend;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.application.IClassResolver;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.protocol.http.HttpSessionStore;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.request.RequestParameters;
import org.apache.wicket.request.target.coding.AbstractRequestTargetUrlCodingStrategy;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.session.ISessionStore;
import org.apache.wicket.settings.IExceptionSettings;
import org.apache.wicket.settings.IResourceSettings;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.UrlResourceStream;
import org.apache.wicket.util.resource.locator.IResourceStreamLocator;
import org.apache.wicket.util.resource.locator.ResourceStreamLocator;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends WebApplication {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(Main.class);

    /** Parameter name of the repository storage directory */
    public final static String REPOSITORY_ADDRESS_PARAM = "repository-address";
    public final static String REPOSITORY_DIRECTORY_PARAM = "repository-directory";
    public final static String DEFAULT_REPOSITORY_DIRECTORY = "WEB-INF/storage";
    public final static String MAXUPLOAD_PARAM = "upload-limit";
    public final static ValueMap DEFAULT_CREDENTIALS = new ValueMap("username=,password=");

    @Override
    protected void init() {
        super.init();

        getSecuritySettings().setAuthorizationStrategy(new IAuthorizationStrategy() {

            public boolean isActionAuthorized(Component component, Action action) {
                return true;
            }

            public boolean isInstantiationAuthorized(Class componentClass) {
                if (Home.class.isAssignableFrom(componentClass)) {
                    UserSession session = (UserSession) Session.get();
                    session.getJcrSession();
                }
                return true;
            }
        });

        getPageSettings().setVersionPagesByDefault(false);
        
        getApplicationSettings().setPageExpiredErrorPage(PageExpiredErrorPage.class);
        try {
            String cfgParam = getConfigurationParameter(MAXUPLOAD_PARAM, null);
            if(cfgParam != null && cfgParam.trim().length() > 0) {
                getApplicationSettings().setDefaultMaximumUploadSize(Bytes.valueOf(cfgParam));
            }
        } catch(StringValueConversionException ex) {
            log.warn("Unable to parse number as specified by "+MAXUPLOAD_PARAM, ex);
        }
        getApplicationSettings().setClassResolver(new IClassResolver() {
            public Class resolveClass(String name) throws ClassNotFoundException {
                if (Session.exists()) {
                    UserSession session = (UserSession) Session.get();
                    ClassLoader loader = session.getClassLoader();
                    if (loader != null) {
                        return session.getClassLoader().loadClass(name);
                    }
                }
                return Thread.currentThread().getContextClassLoader().loadClass(name);
            }
        });

        IResourceSettings resourceSettings = getResourceSettings();
        final IResourceStreamLocator oldLocator = resourceSettings.getResourceStreamLocator();
        final String layout = "/WEB-INF/" + getConfigurationParameter("config", "default") + "/";

        resourceSettings.setResourceStreamLocator(new ResourceStreamLocator() {
            @Override
            public IResourceStream locate(final Class clazz, final String path) {
                // EAR packaging
                try {
                    ServletContext layoutContext = getServletContext().getContext("/layout");
                    if (layoutContext != null) {
                        URL url = layoutContext.getResource(layout + path);
                        if (url != null) {
                            return new UrlResourceStream(url);
                        }
                    }
                } catch (MalformedURLException ex) {
                    log.warn("malformed url for layout override " + ex.getMessage());
                }
                // WAR packaging
                try {
                    URL url = getServletContext().getResource("/layout" + layout + path);
                    if (url != null) {
                        return new UrlResourceStream(url);
                    }
                } catch (MalformedURLException ex) {
                    log.warn("malformed url for layout override " + ex.getMessage());
                }
                return oldLocator.locate(clazz, path);
            }
        });

        // replace current loaders with own list, starting with component-specific
        List<IStringResourceLoader> loaders = new ArrayList<IStringResourceLoader>(resourceSettings
                .getStringResourceLoaders());
        resourceSettings.addStringResourceLoader(new IStringResourceLoader() {

            public String loadStringResource(Component component, String key) {
                IStringResourceProvider provider;
                if (component instanceof IStringResourceProvider) {
                    provider = (IStringResourceProvider) component;
                } else {
                    provider = (IStringResourceProvider) component.findParent(IStringResourceProvider.class);
                }
                if (provider != null) {
                    Map<String, String> keys = new MiniMap(5);
                    keys.put("hippo:key", key);

                    Locale locale = component.getLocale();
                    keys.put("hippo:language", locale.getLanguage());

                    String value = locale.getCountry();
                    if (value != null) {
                        keys.put("hippo:country", locale.getCountry());
                    }

                    value = locale.getVariant();
                    if (value != null) {
                        keys.put("hippo:variant", locale.getVariant());
                    }

                    value = component.getStyle();
                    if (value != null) {
                        keys.put("hippo:style", value);
                    }

                    String result = provider.getString(keys);
                    if (result != null) {
                        return result;
                    }
                }
                return null;
            }

            public String loadStringResource(Class clazz, String key, Locale locale, String style) {
                return null;
            }
        });
        for (IStringResourceLoader loader : loaders) {
            resourceSettings.addStringResourceLoader(loader);
        }

        mount(new AbstractRequestTargetUrlCodingStrategy("binaries") {

            public IRequestTarget decode(RequestParameters requestParameters) {
                String path = requestParameters.getPath().substring("binaries/".length());
                path = urlDecodePathComponent(path);
                try {
                    UserSession session = (UserSession) Session.get();
                    Node node = session.getJcrSession().getRootNode().getNode(path);
                    return new JcrResourceRequestTarget(new JcrNodeModel(node));
                } catch (PathNotFoundException e) {
                    log.error("binary not found " + e.getMessage());
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
                return null;
            }

            public CharSequence encode(IRequestTarget requestTarget) {
                // TODO Auto-generated method stub
                return null;
            }

            public boolean matches(IRequestTarget requestTarget) {
                // TODO Auto-generated method stub
                return false;
            }
        });

        if (Application.DEVELOPMENT.equals(getConfigurationType())) {
            // disable cache
            resourceSettings.getLocalizer().setEnableCache(false);
        } else {
            // don't throw on missing resource
            resourceSettings.setThrowExceptionOnMissingResource(false);

            // don't show exception page
            getExceptionSettings().setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_NO_EXCEPTION_PAGE);
        }
        
        resourceSettings.setLocalizer(new StagedLocalizer());
    }

    @Override
    protected void onDestroy() {
        if (repository != null) {
            repository.close();
            repository = null;
        }
    }

    @Override
    public Class getHomePage() {
        return org.hippoecm.frontend.Home.class;
    }

    @Override
    public Session newSession(Request request, Response response) {
        return new UserSession(request, new JcrSessionModel(DEFAULT_CREDENTIALS));
    }

    @Override
    public ISessionStore newSessionStore() {
        return new HttpSessionStore(this);
    }

    @Override
    public AjaxRequestTarget newAjaxRequestTarget(final Page page) {
        return new PluginRequestTarget(page);
    }

    @Override
    protected IRequestCycleProcessor newRequestCycleProcessor() {
        return new PluginRequestCycleProcessor();
    }

    public String getConfigurationParameter(String parameterName, String defaultValue) {
        return WebApplicationHelper.getConfigurationParameter(this, parameterName, defaultValue);
    }

    private HippoRepository repository;

    public HippoRepository getRepository() {
        if (repository == null) {
            String repositoryAddress = getConfigurationParameter(REPOSITORY_ADDRESS_PARAM, null);
            String repositoryDirectory = getConfigurationParameter(REPOSITORY_DIRECTORY_PARAM,
                    DEFAULT_REPOSITORY_DIRECTORY);
            try {
                if (repositoryAddress != null && !repositoryAddress.trim().equals("")) {
                    repository = HippoRepositoryFactory.getHippoRepository(repositoryAddress);
                } else {
                    repository = HippoRepositoryFactory.getHippoRepository(repositoryDirectory);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return repository;
    }

    public void resetConnection() {
        repository = null;
    }

}
