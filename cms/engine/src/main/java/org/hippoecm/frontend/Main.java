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
package org.hippoecm.frontend;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.wicket.Component;
import org.apache.wicket.DefaultPageManagerProvider;
import org.apache.wicket.IPageRendererProvider;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.application.IClassResolver;
import org.apache.wicket.core.request.handler.BookmarkablePageRequestHandler;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.core.util.resource.locator.IResourceNameIterator;
import org.apache.wicket.core.util.resource.locator.IResourceStreamLocator;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderResponseDecorator;
import org.apache.wicket.page.IPageManagerContext;
import org.apache.wicket.pageStore.IDataStore;
import org.apache.wicket.pageStore.IPageStore;
import org.apache.wicket.protocol.http.BufferedWebResponse;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.cycle.RequestCycleListenerCollection;
import org.apache.wicket.request.handler.render.PageRenderer;
import org.apache.wicket.request.handler.render.WebPageRenderer;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.mount.IMountedRequestMapper;
import org.apache.wicket.request.mapper.mount.Mount;
import org.apache.wicket.request.mapper.mount.MountMapper;
import org.apache.wicket.request.mapper.mount.MountParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.caching.FilenameWithVersionResourceCachingStrategy;
import org.apache.wicket.request.resource.caching.version.LastModifiedResourceVersion;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.settings.IExceptionSettings;
import org.apache.wicket.settings.IResourceSettings;
import org.apache.wicket.util.IContextProvider;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.observation.JcrObservationManager;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.plugin.config.impl.JcrApplicationFactory;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.onehippo.sso.CredentialCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends PluginApplication {

    static final Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * Parameter name of the repository storage directory
     */
    public final static String REPOSITORY_ADDRESS_PARAM = "repository-address";
    public final static String REPOSITORY_DIRECTORY_PARAM = "repository-directory";
    public final static String REPOSITORY_USERNAME_PARAM = "repository-username";
    public final static String REPOSITORY_PASSWORD_PARAM = "repository-password";
    public final static String DEFAULT_REPOSITORY_DIRECTORY = "WEB-INF/storage";
    public final static String MAXUPLOAD_PARAM = "upload-limit";
    public final static String ENCRYPT_URLS = "encrypt-urls";
    public final static String OUTPUT_WICKETPATHS = "output-wicketpaths";
    public final static String PLUGIN_APPLICATION_NAME_PARAMETER = "config";

    /**
     * Custom Wicket {@link IRequestCycleListener} class names parameter
     * which can be comma or whitespace-separated string to set multiple {@link IRequestCycleListener}s.
     */
    public final static String REQUEST_CYCLE_LISTENERS_PARAM = "wicket.request.cycle.listeners";

    /**
     * Wicket RequestCycleSettings timeout configuration parameter name in development mode.
     */
    public final static String DEVELOPMENT_REQUEST_TIMEOUT_PARAM = "wicket.development.request.timeout";

    /**
     * Default Wicket RequestCycleSettings timeout milliseconds in development mode.
     */
    public final static long DEFAULT_DEVELOPMENT_REQUEST_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes

    /**
     * Wicket RequestCycleSettings timeout configuration parameter name in deployment mode.
     */
    public final static String DEPLOYMENT_REQUEST_TIMEOUT_PARAM = "wicket.deployment.request.timeout";

    // class in the root package, to make it possible to use the caching resource stream locator
    // for resources that are not associated with a class.
    private static final Class<?> CACHING_RESOURCE_STREAM_LOCATOR_CLASS;
    static {
        try {
            CACHING_RESOURCE_STREAM_LOCATOR_CLASS = Class.forName("CachingResourceStreamLocatorBaseKey");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private HippoRepository repository;

    @Override
    protected void init() {
        super.init();

        addRequestCycleListeners();

        getPageSettings().setVersionPagesByDefault(false);
//        getPageSettings().setAutomaticMultiWindowSupport(false);

//        getSessionSettings().setPageMapEvictionStrategy(new LeastRecentlyAccessedEvictionStrategy(1));

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

            @Override
            public Class resolveClass(String name) throws ClassNotFoundException {
                if (Session.exists()) {
                    UserSession session = UserSession.get();
                    ClassLoader loader = session.getClassLoader();
                    if (loader != null) {
                        return session.getClassLoader().loadClass(name);
                    }
                }
                return originalResolver.resolveClass(name);
            }

            @Override
            public Iterator<URL> getResources(String name) {
                List<URL> resources = new LinkedList<URL>();
                for (Iterator<URL> iter = originalResolver.getResources(name); iter.hasNext(); ) {
                    resources.add(iter.next());
                }
                if (Session.exists()) {
                    UserSession session = UserSession.get();
                    ClassLoader loader = session.getClassLoader();
                    if (loader != null) {
                        try {
                            for (Enumeration<URL> resourceEnum = session.getClassLoader().getResources(name); resourceEnum
                                    .hasMoreElements(); ) {
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

            @Override
            public ClassLoader getClassLoader() {
                return Main.class.getClassLoader();
            }
        });

        final IResourceSettings resourceSettings = getResourceSettings();

        // replace current loaders with own list, starting with component-specific
        List<IStringResourceLoader> loaders = resourceSettings.getStringResourceLoaders();
        loaders.add(0, new StringResourceProviderConsumer());
        loaders.add(1, new ClassFromKeyStringResourceLoader());
        loaders.add(new IStringResourceLoader() {

            @Override
            public String loadStringResource(final Class<?> clazz, final String key, final Locale locale, final String style, final String variation) {
                return null;
            }

            @Override
            public String loadStringResource(final Component component, String key, final Locale locale, final String style, final String variation) {
                if (key.contains(",")) {
                    key = key.substring(0, key.lastIndexOf(','));
                    return resourceSettings.getLocalizer().getStringIgnoreSettings(key, component, null, locale, style, variation);
                }
                return null;
            }
        });

        resourceSettings.setCachingStrategy(new FilenameWithVersionResourceCachingStrategy(new LastModifiedResourceVersion()));

        mount(new MountMapper("binaries", new IMountedRequestMapper() {

            @Override
            public IRequestHandler mapRequest(final Request request, final MountParameters mountParams) {
                String path = Strings.join("/", request.getUrl().getSegments());
                try {
                    javax.jcr.Session subSession = UserSession.get().getJcrSession();
                    Node node = ((HippoWorkspace) subSession.getWorkspace()).getHierarchyResolver().getNode(
                            subSession.getRootNode(), path);
                    // YUCK: no exception!
                    if (node == null) {
                        log.info("no binary found at " + path);
                    } else {
                        if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                            node = (Node) JcrHelper.getPrimaryItem(node);
                        }
                        return new JcrResourceRequestHandler(node);
                    }
                } catch (PathNotFoundException e) {
                    log.info("binary not found " + e.getMessage());
                } catch (javax.jcr.LoginException ex) {
                    log.warn(ex.getMessage());
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
                return null;
            }

            @Override
            public int getCompatibilityScore(final Request request) {
                return 1;
            }

            @Override
            public Mount mapHandler(final IRequestHandler requestHandler) {
                return null;
            }
        }));


        /*
         * HST SAML kind of authentication handler needed for Template Composer integration
         *
         */
        mount(new MountMapper("auth", new IMountedRequestMapper() {

            @Override
            public IRequestHandler mapRequest(final Request request, final MountParameters mountParams) {

                PluginUserSession userSession = (PluginUserSession) Session.get();
                final UserCredentials userCredentials = userSession.getUserCredentials();

                IRequestHandler requestTarget = new BookmarkablePageRequestHandler(new PageProvider(getHomePage(), null));

                IRequestParameters requestParameters = request.getRequestParameters();
                final List<StringValue> keyParams = requestParameters.getParameterValues("key");
                final List<StringValue> destinationUrlParams = requestParameters.getParameterValues("destinationUrl");

                if (userCredentials != null && keyParams.size() > 0 && destinationUrlParams.size() > 0) {

                    requestTarget = new IRequestHandler() {

                        @Override
                        public void respond(IRequestCycle requestCycle) {
                            String key = keyParams.get(0).toString();
                            String destinationUrl = destinationUrlParams.get(0).toString();
                            CredentialCipher cipher = CredentialCipher.getInstance();
                            String encryptedString = cipher.getEncryptedString(key, (SimpleCredentials) userCredentials.getJcrCredentials());

                            WebResponse response = (WebResponse) RequestCycle.get().getResponse();
                            if (destinationUrl.contains("?")) {
                                response.sendRedirect(destinationUrl + "&cred=" + encryptedString);
                            } else {
                                response.sendRedirect(destinationUrl + "?cred=" + encryptedString);
                            }
                        }

                        @Override
                        public void detach(IRequestCycle requestCycle) {
                            //Nothing to detach.
                        }
                    };
                }
                return requestTarget;
            }

            @Override
            public int getCompatibilityScore(final Request request) {
                return 0;
            }

            @Override
            public Mount mapHandler(final IRequestHandler requestHandler) {
                return null;
            }
        }));


        resourceSettings.setLocalizer(new StagedLocalizer());

        // caching resource stream locator implementation that allows the class argument to be null.
        final IResourceStreamLocator resourceStreamLocator = resourceSettings.getResourceStreamLocator();
        resourceSettings.setResourceStreamLocator(new IResourceStreamLocator() {
            @Override
            public IResourceStream locate(Class<?> clazz, final String path) {
                if (clazz == null) {
                    clazz = CACHING_RESOURCE_STREAM_LOCATOR_CLASS;
                }
                return resourceStreamLocator.locate(clazz, path);
            }

            @Override
            public IResourceStream locate(Class<?> clazz, final String path, final String style, final String variation, final Locale locale, final String extension, final boolean strict) {
                if (clazz == null) {
                    clazz = CACHING_RESOURCE_STREAM_LOCATOR_CLASS;
                }
                return resourceStreamLocator.locate(clazz, path, style, variation, locale, extension, strict);
            }

            @Override
            public IResourceNameIterator newResourceNameIterator(final String path, final Locale locale, final String style, final String variation, final String extension, final boolean strict) {
                return resourceStreamLocator.newResourceNameIterator(path, locale, style, variation, extension, strict);
            }
        });

        if (RuntimeConfigurationType.DEVELOPMENT.equals(getConfigurationType())) {
            // disable cache
            resourceSettings.getLocalizer().setEnableCache(false);

            final long timeout = NumberUtils.toLong(getConfigurationParameter(DEVELOPMENT_REQUEST_TIMEOUT_PARAM, null), DEFAULT_DEVELOPMENT_REQUEST_TIMEOUT_MS);

            if (timeout > 0L) {
                log.info("Setting wicket request timeout to {} ms.", timeout);
                getRequestCycleSettings().setTimeout(Duration.milliseconds(timeout));
            }

            getDebugSettings().setOutputMarkupContainerClassName(true);
        } else {
            // don't serialize pages for performance
            setPageManagerProvider(new DefaultPageManagerProvider(this) {

                @Override
                protected IPageStore newPageStore(final IDataStore dataStore) {
                    return new AmnesicPageStore();
                }
            });

            // don't throw on missing resource
            resourceSettings.setThrowExceptionOnMissingResource(false);

            // don't show exception page
            getExceptionSettings().setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_NO_EXCEPTION_PAGE);

            final long timeout = NumberUtils.toLong(getConfigurationParameter(DEPLOYMENT_REQUEST_TIMEOUT_PARAM, null));

            if (timeout > 0L) {
                log.info("Setting wicket request timeout to {} ms.", timeout);
                getRequestCycleSettings().setTimeout(Duration.milliseconds(timeout));
            }
        }

        String outputWicketpaths = obtainOutputWicketPathsParameter();

        if (outputWicketpaths != null && "true".equalsIgnoreCase(outputWicketpaths)) {
            getDebugSettings().setOutputComponentPath(true);
        }

        setHeaderResponseDecorator(new IHeaderResponseDecorator() {

            @Override
            public IHeaderResponse decorate(IHeaderResponse response) {
                boolean isIE = WebSession.get().getClientInfo().getProperties().isBrowserInternetExplorer();
                int version = WebSession.get().getClientInfo().getProperties().getBrowserVersionMajor();
                boolean isAjax = ((WebRequest) RequestCycle.get().getRequest()).isAjax();
                if (isIE && !isAjax && (0 < version && version < 10)) {
                    // Before IE10 the number of stylesheets is restricted to 31
                    return new CssImportingHeaderResponse(response);
                }
                return response;
            }

        });

        final IContextProvider<AjaxRequestTarget, Page> ajaxRequestTargetProvider = getAjaxRequestTargetProvider();
        setAjaxRequestTargetProvider(new IContextProvider<AjaxRequestTarget, Page>() {

            @Override
            public AjaxRequestTarget get(final Page context) {
                return new PluginRequestTarget(ajaxRequestTargetProvider.get(context));
            }
        });

        setPageRendererProvider(new IPageRendererProvider() {

            @Override
            public PageRenderer get(final RenderPageRequestHandler context) {
                return new WebPageRenderer(context) {

                    @Override
                    protected BufferedWebResponse renderPage(final Url targetUrl, final RequestCycle requestCycle) {
                        IRequestHandler scheduled = requestCycle.getRequestHandlerScheduledAfterCurrent();
                        if (scheduled == null) {
                            IRequestablePage page = getPage();
                            if (page instanceof Home) {
                                Home home = (Home) page;
                                home.processEvents();
                                home.render(null);
                            }
                        }
                        return super.renderPage(targetUrl, requestCycle);
                    }
                };
            }
        });

        if (log.isInfoEnabled()) {
            String applicationName = getPluginApplicationName();
            log.info("Hippo CMS application " + applicationName + " has started");
        }
    }

    @Override
    public void internalDestroy() {
        super.internalDestroy();
        if (log.isInfoEnabled()) {
            String applicationName = getPluginApplicationName();
            log.info("Hippo CMS application " + applicationName + " has stopped");
        }
    }

    /**
     * Tries to get the output wicket paths parameter from:
     * <ol>
     *     <li>The servlet init parameter</li>
     *     <li>The servlet context if the init parameter isn't set</li>
     * </ol>
     * @return the value of the output wicket paths parameter
     */
    private String obtainOutputWicketPathsParameter() {
        String outputWicketpaths = getInitParameter(OUTPUT_WICKETPATHS);

        if (outputWicketpaths == null) {
            outputWicketpaths = getServletContext().getInitParameter(OUTPUT_WICKETPATHS);
        }
        return outputWicketpaths;
    }

    @Override
    public String getPluginApplicationName() {
        return getConfigurationParameter(PLUGIN_APPLICATION_NAME_PARAMETER, "cms");
    }

    @Override
    public String getConfigurationParameter(String parameterName, String defaultValue) {
        String result = getInitParameter(parameterName);

        if (result == null || result.equals("")) {
            result = getServletContext().getInitParameter(parameterName);
        }

        if (result == null || result.equals("")) {
            result = defaultValue;
        }

        return result;
    }

    @Override
    public ResourceReference getPluginApplicationFavIconReference() {
        return new PackageResourceReference(Main.class, "hippo-" + getPluginApplicationName() + ".ico");
    }

    @Override
    public Class<PluginPage> getHomePage() {
        return org.hippoecm.frontend.PluginPage.class;
    }

    // ease testing by making page manager context available in the package
    @Override
    protected IPageManagerContext getPageManagerContext() {
        return super.getPageManagerContext();
    }

    @Override
    public UserSession newSession(Request request, Response response) {
        PluginUserSession userSession = new PluginUserSession(request);
        userSession.login();
        return userSession;
    }

    public IApplicationFactory getApplicationFactory(final javax.jcr.Session jcrSession) {
        return new JcrApplicationFactory(new JcrNodeModel("/" + HippoNodeType.CONFIGURATION_PATH + "/"
                + HippoNodeType.FRONTEND_PATH));
    }

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
            String repositoryUsername = getConfigurationParameter(REPOSITORY_USERNAME_PARAM, null);
            String repositoryPassword = getConfigurationParameter(REPOSITORY_PASSWORD_PARAM, null);
            PluginUserSession.setCredentials(new UserCredentials(repositoryUsername, repositoryPassword));
        }
        return repository;
    }

    public void resetConnection() {
        repository = null;
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

    /**
     * Adds the default built-in {@link IRequestCycleListener} or configured custom {@link IRequestCycleListener}s.
     * <P>
     * If no custom {@link IRequestCycleListener}s are configured, then this simply registers the default built-in
     * {@link RepositoryRuntimeExceptionHandlingRequestCycleListener}.
     * Otherwise, this registers only the custom configured {@link IRequestCycleListener}s.
     * </P>
     */
    private void addRequestCycleListeners() {
        String [] listenerClassNames = StringUtils.split(getConfigurationParameter(REQUEST_CYCLE_LISTENERS_PARAM, null), " ,\t\r\n");
        RequestCycleListenerCollection requestCycleListenerCollection = getRequestCycleListeners();

        if (listenerClassNames == null || listenerClassNames.length == 0) {
            requestCycleListenerCollection.add(new RepositoryRuntimeExceptionHandlingRequestCycleListener());
        } else {
            for (String listenerClassName : listenerClassNames) {
                try {
                    Class<?> clazz = Class.forName(listenerClassName);
                    IRequestCycleListener listener = (IRequestCycleListener) clazz.newInstance();
                    requestCycleListenerCollection.add(listener);
                } catch (Throwable th) {
                    log.error("Failed to register RequestCycleListener, " + listenerClassName, th);
                }
            }
        }
    }
}
