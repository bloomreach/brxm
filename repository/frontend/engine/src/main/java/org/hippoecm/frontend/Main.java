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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.servlet.ServletContext;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.application.IClassResolver;
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
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.session.UnbindingHttpSessionStore;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
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
    public final static String PLUGIN_APPLICATION_NAME = "config";
    public final static String ENCRYPT_URLS = "encrypt-urls";
    public final static String OUTPUT_WICKETPATHS = "output-wicketpaths";

    @Override
    protected void init() {
        super.init();

        getPageSettings().setVersionPagesByDefault(false);

        getApplicationSettings().setPageExpiredErrorPage(PageExpiredErrorPage.class);
        try {
            String cfgParam = getConfigurationParameter(MAXUPLOAD_PARAM, null);
            if (cfgParam != null && cfgParam.trim().length() > 0) {
                getApplicationSettings().setDefaultMaximumUploadSize(Bytes.valueOf(cfgParam));
            }
        } catch (StringValueConversionException ex) {
            log.warn("Unable to parse number as specified by " + MAXUPLOAD_PARAM, ex);
        }
        final IClassResolver originalResolver = getApplicationSettings().getClassResolver();
        getApplicationSettings().setClassResolver(new IClassResolver() {
            public Class resolveClass(String name) throws ClassNotFoundException {
                if (Session.exists()) {
                    UserSession session = (UserSession) Session.get();
                    ClassLoader loader = session.getClassLoader();
                    if (loader != null) {
                        return session.getClassLoader().loadClass(name);
                    }
                }
                return originalResolver.resolveClass(name);
            }

            public Iterator<URL> getResources(String name) {
                List<URL> resources = new LinkedList<URL>();
                for (Iterator<URL> iter = originalResolver.getResources(name); iter.hasNext();) {
                    resources.add(iter.next());
                }
                if (Session.exists()) {
                    UserSession session = (UserSession) Session.get();
                    ClassLoader loader = session.getClassLoader();
                    if (loader != null) {
                        try {
                            for (Enumeration<URL> resourceEnum = session.getClassLoader().getResources(name); resourceEnum
                                    .hasMoreElements();) {
                                resources.add(resourceEnum.nextElement());
                            }
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                return resources.iterator();
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
                } else if (component != null) {
                    provider = component.findParent(IStringResourceProvider.class);
                } else {
                    return null;
                }
                if (provider != null) {
                    Map<String, String> keys = new MiniMap<String, String>(5);
                    keys.put(HippoNodeType.HIPPO_KEY, key);

                    Locale locale = component.getLocale();
                    keys.put(HippoNodeType.HIPPO_LANGUAGE, locale.getLanguage());

                    String value = locale.getCountry();
                    if (value != null) {
                        keys.put("country", locale.getCountry());
                    }

                    value = locale.getVariant();
                    if (value != null) {
                        keys.put("variant", locale.getVariant());
                    }

                    value = component.getStyle();
                    if (value != null) {
                        keys.put("style", value);
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
                String pathInfo = StringUtils.removeEnd(requestParameters.getPath().substring("binaries/".length()), "/");
                String pathInParams = StringUtils.removeStart(getRequestParameter(requestParameters, "_path", true), "/");
                String path = null;
                if (!StringUtils.isEmpty(pathInfo)) {
                    path = StringUtils.join(new String [] { pathInfo, pathInParams }, '/');
                } else {
                    path = pathInParams;
                }
                path = urlDecodePathComponent(StringUtils.removeStart(path, "/"));
                // FIXME: workaround use a subsession because we can't refresh the session
                // the logout is actually performed by the JcrResourceRequestTarget
                UserSession session = (UserSession) Session.get();
                IValueMap credentials = session.getCredentials();
                try {
                    javax.jcr.Session subSession = session.getJcrSession().impersonate(
                            new javax.jcr.SimpleCredentials(credentials.getString("username"), credentials.getString(
                                    "password").toCharArray()));
                    Node node = ((HippoWorkspace) subSession.getWorkspace()).getHierarchyResolver().getNode(
                            subSession.getRootNode(), path);
                    return new JcrResourceRequestTarget(node);
                } catch (PathNotFoundException e) {
                    log.info("binary not found " + e.getMessage());
                } catch (javax.jcr.LoginException ex) {
                    log.warn(ex.getMessage());
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
                return null;
            }

            public CharSequence encode(IRequestTarget requestTarget) {
                return null;
            }

            public boolean matches(IRequestTarget requestTarget) {
                return false;
            }
            
            private String getRequestParameter(RequestParameters requestParameters, String paramName, boolean concatenateAll) {
                String paramValue = null;
                Object param = requestParameters.getParameters().get(paramName);
                
                if (param != null) {
                    if (param.getClass().isArray() && ArrayUtils.getLength(param) > 0) {
                        if (concatenateAll) {
                            paramValue = StringUtils.join((String []) param);
                        } else {
                            paramValue = ((String []) param)[0];
                        }
                    } else {
                        paramValue = param.toString();
                    }
                }
                
                return paramValue;
            }
        });

        resourceSettings.setLocalizer(new StagedLocalizer());

        if (Application.DEVELOPMENT.equals(getConfigurationType())) {
            // disable cache
            resourceSettings.getLocalizer().setEnableCache(false);

            getDebugSettings().setOutputMarkupContainerClassName(true);
        } else {
            // don't throw on missing resource
            resourceSettings.setThrowExceptionOnMissingResource(false);

            // don't show exception page
            getExceptionSettings().setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_NO_EXCEPTION_PAGE);
        }

        String outputWicketpaths = getInitParameter(OUTPUT_WICKETPATHS);
        if (outputWicketpaths != null && "true".equalsIgnoreCase(outputWicketpaths)) {
            getDebugSettings().setOutputComponentPath(true);
        }
    }

    @Override
    protected void onDestroy() {
        if (repository != null) {
            // remove listeners
            JcrObservationManager jom = JcrObservationManager.getInstance();
            EventListenerIterator eli;
            try {
                eli = jom.getRegisteredEventListeners();
                log.info("Number of listeners to remove: {}", eli.getSize());
                while (eli.hasNext()) {
                    EventListener el = eli.nextEventListener();
                    try {
                        jom.removeEventListener(el);
                    } catch (RepositoryException e) {
                        log.warn("Unable to remove listener", e);
                    }
                }
            } catch (RepositoryException e) {
                log.error("Unable to get registered event listeners for shutdown", e);
            }

            // close
            repository.close();
            repository = null;
        }
    }

    @Override
    public Class<Home> getHomePage() {
        return org.hippoecm.frontend.Home.class;
    }

    @Override
    public UserSession newSession(Request request, Response response) {
        return new UserSession(request);
    }

    @Override
    public ISessionStore newSessionStore() {
        // in development mode, use disk page store to serialize page at the end of a request.
        // in production, skip serialization for better performance.
        if (Application.DEVELOPMENT.equals(getConfigurationType())) {
            return super.newSessionStore();
        } else {
            return new UnbindingHttpSessionStore(this);
        }
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

    public HippoRepository getRepository() throws RepositoryException {
        if (repository == null) {
            String repositoryAddress = getConfigurationParameter(REPOSITORY_ADDRESS_PARAM, null);
            String repositoryDirectory = getConfigurationParameter(REPOSITORY_DIRECTORY_PARAM,
                    DEFAULT_REPOSITORY_DIRECTORY);
            if (repositoryAddress != null && !repositoryAddress.trim().equals("")) {
                repository = HippoRepositoryFactory.getHippoRepository(repositoryAddress);
            } else {
                repository = HippoRepositoryFactory.getHippoRepository(repositoryDirectory);
            }
        }
        return repository;
    }

    public void resetConnection() {
        repository = null;
    }

}
