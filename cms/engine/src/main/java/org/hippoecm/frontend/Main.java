/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.wicket.Component;
import org.apache.wicket.DefaultPageManagerProvider;
import org.apache.wicket.IPageRendererProvider;
import org.apache.wicket.ISessionListener;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.application.IClassResolver;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler.RedirectPolicy;
import org.apache.wicket.core.util.resource.locator.IResourceNameIterator;
import org.apache.wicket.core.util.resource.locator.IResourceStreamLocator;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.filter.AbstractHeaderResponseFilter;
import org.apache.wicket.markup.head.filter.FilteredHeaderItem;
import org.apache.wicket.markup.head.filter.FilteringHeaderResponse;
import org.apache.wicket.markup.head.filter.OppositeHeaderResponseFilter;
import org.apache.wicket.markup.html.IPackageResourceGuard;
import org.apache.wicket.page.IPageManagerContext;
import org.apache.wicket.pageStore.IDataStore;
import org.apache.wicket.pageStore.IPageStore;
import org.apache.wicket.protocol.http.BufferedWebResponse;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebResponse;
import org.apache.wicket.request.IExceptionMapper;
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
import org.apache.wicket.request.mapper.AbstractMapper;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.caching.FilenameWithVersionResourceCachingStrategy;
import org.apache.wicket.request.resource.caching.QueryStringWithVersionResourceCachingStrategy;
import org.apache.wicket.request.resource.caching.version.LastModifiedResourceVersion;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.settings.ExceptionSettings;
import org.apache.wicket.settings.ResourceSettings;
import org.apache.wicket.util.IContextProvider;
import org.apache.wicket.util.IProvider;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.diagnosis.DiagnosticsRequestCycleListener;
import org.hippoecm.frontend.errors.NavAppExceptionMapper;
import org.hippoecm.frontend.http.CsrfPreventionRequestCycleListener;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.observation.CmsEventDispatcherServiceImpl;
import org.hippoecm.frontend.observation.InternalCmsEventDispatcherService;
import org.hippoecm.frontend.observation.JcrObservationManager;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.plugin.config.impl.JcrApplicationFactory;
import org.hippoecm.frontend.service.WicketFaviconService;
import org.hippoecm.frontend.service.WicketFaviconServiceImpl;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.settings.GlobalSettings;
import org.hippoecm.frontend.util.RequestUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsContextService;
import org.onehippo.cms7.services.cmscontext.CmsContextServiceImpl;
import org.onehippo.cms7.services.cmscontext.CmsInternalCmsContextService;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.observation.CmsEventDispatcherService;
import org.onehippo.repository.security.JvmCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.wicket.markup.head.filter.FilteringHeaderResponse.DEFAULT_HEADER_FILTER_NAME;

public class Main extends PluginApplication {

    static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String FRONTEND_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH;
    private static final String WHITELISTED_CLASSES_FOR_PACKAGE_RESOURCES = "whitelisted.classes.for.package.resources";
    private static final String[] DEFAULT_WHITELISTED_CLASSES_FOR_PACKAGE_RESOURCES = {
            "org.hippoecm.", "org.apache.wicket.", "org.onehippo.", "wicket.contrib."
    };

    /**
     * Parameter name of the repository storage directory
     */
    public final static String REPOSITORY_ADDRESS_PARAM = "repository-address";
    public final static String REPOSITORY_DIRECTORY_PARAM = "repository-directory";
    public final static String REPOSITORY_USERNAME_PARAM = "repository-username";

    @SuppressWarnings("squid:S2068")
    public final static String REPOSITORY_PASSWORD_PARAM = "repository-password";

    public final static String DEFAULT_REPOSITORY_DIRECTORY = "WEB-INF/storage";
    public final static String MAXUPLOAD_PARAM = "upload-limit";
    public final static String ENCRYPT_URLS = "encrypt-urls";
    public final static String OUTPUT_WICKETPATHS = "output-wicketpaths";

    public final static String PLUGIN_APPLICATION_NAME_PARAMETER = "config";
    public static final String PLUGIN_APPLICATION_HIDE_PERSPECTIVE_MENU_PARAMETER = "hidePerspectiveMenu";

    // comma separated init parameter
    public final static String ACCEPTED_ORIGIN_WHITELIST = "accepted-origin-whitelist";
    /**
     * Custom Wicket {@link IRequestCycleListener} class names parameter which can be comma or whitespace-separated
     * string to set multiple {@link IRequestCycleListener}s.
     */
    public final static String REQUEST_CYCLE_LISTENERS_PARAM = "wicket.request.cycle.listeners";

    /**
     * Wicket RequestCycleSettings timeout configuration parameter name in development mode.
     */
    public final static String DEVELOPMENT_REQUEST_TIMEOUT_PARAM = "wicket.development.request.timeout";

    /**
     * Default Wicket RequestCycleSettings timeout milliseconds in development mode.
     */
    public final static long DEFAULT_DEVELOPMENT_REQUEST_TIMEOUT_MS = 10 * 60 * 1000L; // 10 minutes

    /**
     * Wicket RequestCycleSettings timeout configuration parameter name in deployment mode.
     */
    public final static String DEPLOYMENT_REQUEST_TIMEOUT_PARAM = "wicket.deployment.request.timeout";

    public static final String CMS_AS_IFRAME_QUERY_PARAMETER = "iframe";

    // class in the root package, to make it possible to use the caching resource stream locator
    // for resources that are not associated with a class.
    private static final Class<?> CACHING_RESOURCE_STREAM_LOCATOR_CLASS;

    private static final String BINARIES_MOUNT = "binaries";
    private static final String AUTH_MOUNT = "auth";

    static {
        try {
            CACHING_RESOURCE_STREAM_LOCATOR_CLASS = Class.forName("CachingResourceStreamLocatorBaseKey");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private HippoRepository repository;

    private CmsContextServiceImpl cmsContextServiceImpl;
    private CmsInternalCmsContextService cmsContextService;
    private CmsEventDispatcherService cmsEventDispatcherService;

    protected String repositoryFallbackUsername;
    protected String repositoryFallbackPassword;
    private WicketFaviconService wicketFaviconService;
    private IProvider<IExceptionMapper> exceptionMapperProvider;

    protected void initializeFallBackCredentials() {
        repositoryFallbackUsername = getConfigurationParameter(REPOSITORY_USERNAME_PARAM, null);
        repositoryFallbackPassword = getConfigurationParameter(REPOSITORY_PASSWORD_PARAM, null);
    }

    @Override
    protected void init() {
        super.init();

        initializeFallBackCredentials();

        wicketFaviconService = HippoServiceRegistry.getService(WicketFaviconService.class);
        if (wicketFaviconService == null) {
            wicketFaviconService = new WicketFaviconServiceImpl();
            HippoServiceRegistry.register(wicketFaviconService, WicketFaviconService.class);
        }

        addRequestCycleListeners();

        registerSessionListeners();

        getPageSettings().setVersionPagesByDefault(false);
//        getPageSettings().setAutomaticMultiWindowSupport(false);

//        getSessionSettings().setPageMapEvictionStrategy(new LeastRecentlyAccessedEvictionStrategy(1));

        // LatestBundledJQueryResourceReference to be removed when upgrading to Wicket 8.x
        getJavaScriptLibrarySettings().setJQueryReference(new LatestBundledJQueryResourceReference());

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
                List<URL> resources = new LinkedList<>();
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
                            log.error("Failed loading resources for name {}", name, e);
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

        final ResourceSettings resourceSettings = getResourceSettings();

        // replace current loaders with own list, starting with component-specific
        List<IStringResourceLoader> loaders = resourceSettings.getStringResourceLoaders();
        loaders.add(new ClassFromKeyStringResourceLoader());
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

        if (RuntimeConfigurationType.DEVELOPMENT.equals(getConfigurationType())) {
            resourceSettings.setCachingStrategy(new QueryStringWithVersionResourceCachingStrategy(new LastModifiedResourceVersion()));
        } else {
            resourceSettings.setCachingStrategy(new FilenameWithVersionResourceCachingStrategy(new LastModifiedResourceVersion()));
        }

        mount(new AbstractMapper() {

            @Override
            public IRequestHandler mapRequest(final Request request) {
                final Url url = request.getUrl();
                if (urlStartsWith(url, BINARIES_MOUNT)) {
                    final String fullPath = Strings.join("/", url.getSegments());
                    final String path = StringUtils.substring(fullPath, BINARIES_MOUNT.length());
                    try {
                        javax.jcr.Session subSession = UserSession.get().getJcrSession();
                        Node node = ((HippoWorkspace) subSession.getWorkspace()).getHierarchyResolver().getNode(
                                subSession.getRootNode(), path);
                        // YUCK: no exception!
                        if (node == null) {
                            log.info("no binary found at {}", path);
                        } else {
                            if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                                node = (Node) JcrHelper.getPrimaryItem(node);
                            }
                            return new JcrResourceRequestHandler(node);
                        }
                    } catch (PathNotFoundException e) {
                        log.info("binary not found: {}", e.getMessage());
                    } catch (javax.jcr.LoginException ex) {
                        log.warn(ex.getMessage());
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }
                return null;
            }

            @Override
            public int getCompatibilityScore(final Request request) {
                return 0;
            }

            @Override
            public Url mapHandler(final IRequestHandler requestHandler) {
                return null;
            }
        });

        String applicationName = getPluginApplicationName();

        if (PLUGIN_APPLICATION_VALUE_CMS.equals(applicationName)) {

            // the following is only applicable and needed for the CMS application, not the Console

            /*
             * HST SAML kind of authentication handler needed for Template Composer integration
             *
             */
            cmsContextService = (CmsInternalCmsContextService) HippoServiceRegistry.getService(CmsContextService.class);
            if (cmsContextService == null) {
                cmsContextServiceImpl = new CmsContextServiceImpl();
                cmsContextService = cmsContextServiceImpl;
                HippoServiceRegistry.register(cmsContextServiceImpl, CmsContextService.class, CmsInternalCmsContextService.class);
            }

            cmsEventDispatcherService = HippoServiceRegistry.getService(CmsEventDispatcherService.class);
            if (cmsEventDispatcherService == null) {
                cmsEventDispatcherService = new CmsEventDispatcherServiceImpl();
                HippoServiceRegistry.register(cmsEventDispatcherService, CmsEventDispatcherService.class, InternalCmsEventDispatcherService.class);
            }
            mount(new AbstractMapper() {

                @Override
                public IRequestHandler mapRequest(final Request request) {
                    if (urlStartsWith(request.getUrl(), AUTH_MOUNT)) {
                        IRequestHandler requestTarget = new RenderPageRequestHandler(new PageProvider(getHomePage(), null), RedirectPolicy.AUTO_REDIRECT);

                        IRequestParameters requestParameters = request.getRequestParameters();
                        final List<StringValue> cmsCSIDParams = requestParameters.getParameterValues("cmsCSID");
                        final List<StringValue> destinationPathParams = requestParameters.getParameterValues("destinationPath");
                        final String destinationPath = destinationPathParams != null && !destinationPathParams.isEmpty()
                                ? destinationPathParams.get(0).toString() : null;

                        HttpSession httpSession = ((ServletWebRequest) request).getContainerRequest().getSession();
                        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(httpSession);

                        if (cmsSessionContext == null) {
                            throw new IllegalStateException("SSO handshake not possible since there is no valid CMS session " +
                                    "Context");
                        }

                        if (destinationPath != null && destinationPath.startsWith("/")) {

                            requestTarget = new IRequestHandler() {

                                @Override
                                public void respond(IRequestCycle requestCycle) {
                                    String destinationUrl = RequestUtils.getFarthestUrlPrefix(request) + destinationPath;
                                    WebResponse response = (WebResponse) RequestCycle.get().getResponse();
                                    String cmsCSID = cmsCSIDParams == null ? null : cmsCSIDParams.get(0) == null ? null : cmsCSIDParams.get(0).toString();
                                    if (!cmsContextService.getId().equals(cmsCSID)) {
                                        // redirect to destinationURL and include marker that it is a retry. This way
                                        // the destination can choose to not redirect for SSO handshake again if it still does not
                                        // have a key
                                        if (destinationUrl.contains("?")) {
                                            response.sendRedirect(destinationUrl + "&retry");
                                        } else {
                                            response.sendRedirect(destinationUrl + "?retry");
                                        }
                                        return;
                                    }
                                    if (destinationUrl.contains("?")) {
                                        response.sendRedirect(destinationUrl + "&cmsCSID=" + cmsContextService.getId() + "&cmsSCID=" + cmsSessionContext.getId());
                                    } else {
                                        response.sendRedirect(destinationUrl + "?cmsCSID=" + cmsContextService.getId() + "&cmsSCID=" + cmsSessionContext.getId());
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
                    return null;
                }

                @Override
                public int getCompatibilityScore(final Request request) {
                    return 0;
                }

                @Override
                public Url mapHandler(final IRequestHandler requestHandler) {
                    return null;
                }
            });
        }

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

            // render comments with component class names
            getDebugSettings().setOutputMarkupContainerClassName(true);

            // do not render Wicket-specific markup since it can break CSS
            getMarkupSettings().setStripWicketTags(true);
        } else {
            // don't serialize pages for performance
            setPageManagerProvider(new DefaultPageManagerProvider(this) {

                @Override
                protected IPageStore newPageStore(final IDataStore dataStore) {
                    return new AmnesicPageStore(dataStore);
                }
            });

            // don't throw on missing resource
            resourceSettings.setThrowExceptionOnMissingResource(false);

            // Unexpected exceptions will be rendered by the nav-app
            getExceptionSettings().setUnexpectedExceptionDisplay(ExceptionSettings.SHOW_EXCEPTION_PAGE);
            exceptionMapperProvider = NavAppExceptionMapper::new;

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

        final IContextProvider<AjaxRequestTarget, Page> ajaxRequestTargetProvider = getAjaxRequestTargetProvider();
        setAjaxRequestTargetProvider(context -> new PluginRequestTarget(ajaxRequestTargetProvider.get(context)));

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

        final IPackageResourceGuard resourceGuard = createPackageResourceGuard();
        resourceSettings.setPackageResourceGuard(resourceGuard);

        if (log.isInfoEnabled()) {
            log.info("Hippo CMS application " + applicationName + " has started");
        }

        addHeaderResponseDecorator();
        getApplicationSettings().setAccessDeniedPage(PluginPage.class);
    }

    @Override
    public IProvider<IExceptionMapper> getExceptionMapperProvider() {
        return exceptionMapperProvider != null
            ? exceptionMapperProvider
            : super.getExceptionMapperProvider();
    }

    protected IPackageResourceGuard createPackageResourceGuard() {
        return new WhitelistedClassesResourceGuard() {
            @Override
            protected void onInit() {
                String[] classNamePrefixes = GlobalSettings.get().getStringArray(WHITELISTED_CLASSES_FOR_PACKAGE_RESOURCES);
                if (classNamePrefixes == null || classNamePrefixes.length == 0) {
                    log.info("No whitelisted package resources found, using the default whitelist: {}",
                            Arrays.asList(DEFAULT_WHITELISTED_CLASSES_FOR_PACKAGE_RESOURCES));
                    classNamePrefixes = DEFAULT_WHITELISTED_CLASSES_FOR_PACKAGE_RESOURCES;
                }
                addClassNamePrefixes(classNamePrefixes);

                // CMS7-8898: allow .woff2 files to be served
                addPattern("+*.woff2");
            }
        };
    }

    protected void registerSessionListeners() {
        getSessionListeners().add(new ISessionListener() {
            @Override
            public void onCreated(final Session session) {
                ((PluginUserSession) session).login();
            }

            @Override
            public void onUnbound(final String sessionId) {
            }
        });
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
     * Tries to get the output wicket paths parameter from: <ol> <li>The servlet init parameter</li> <li>The servlet
     * context if the init parameter isn't set</li> </ol>
     *
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
        return getConfigurationParameter(PLUGIN_APPLICATION_NAME_PARAMETER, PLUGIN_APPLICATION_VALUE_CMS);
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
        return this.wicketFaviconService.getFaviconResourceReference();
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
    protected WebResponse newWebResponse(final WebRequest webRequest, final HttpServletResponse httpServletResponse) {
        return new ResponseSplittingProtectingServletWebResponse(webRequest, httpServletResponse);
    }

    @Override
    public UserSession newSession(Request request, Response response) {
        return new PluginUserSession(request);
    }

    public IApplicationFactory getApplicationFactory(final javax.jcr.Session jcrSession) {
        return new JcrApplicationFactory(new JcrNodeModel(FRONTEND_PATH));
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
        }
        return repository;
    }


    public UserCredentials getFallbackCredentials() {
        if (repositoryFallbackUsername == null || repositoryFallbackPassword == null) {
            return new UserCredentials(JvmCredentials.getCredentials(UserCredentials.FRONTEND_SYSTEM_USER_ID));
        }
        return new UserCredentials(repositoryFallbackUsername, repositoryFallbackPassword);
    }


    public void resetConnection() {
        repository = null;
    }

    @Override
    protected void onDestroy() {
        if (cmsContextServiceImpl != null) {
            HippoServiceRegistry.unregister(cmsContextServiceImpl, CmsContextService.class);
            cmsContextServiceImpl = null;
        }
        cmsContextService = null;
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
     * Adds the default built-in {@link IRequestCycleListener} or configured custom {@link IRequestCycleListener}s. Note
     * that the default <code>CsrfPreventionRequestCycleListener</code> always gets added, regardless whether custom
     * {@link IRequestCycleListener}s are configured.
     * <p>
     * If no custom {@link IRequestCycleListener}s are configured, then this simply registers the default built-in
     * listeners such as {@link org.hippoecm.frontend.diagnosis.DiagnosticsRequestCycleListener} and {@link
     * RepositoryRuntimeExceptionHandlingRequestCycleListener}. Otherwise, this registers only the custom configured
     * {@link IRequestCycleListener}s. </P>
     */
    private void addRequestCycleListeners() {
        String[] listenerClassNames = StringUtils.split(getConfigurationParameter(REQUEST_CYCLE_LISTENERS_PARAM, null), " ,\t\r\n");
        RequestCycleListenerCollection requestCycleListenerCollection = getRequestCycleListeners();

        addCsrfPreventionRequestCycleListener(requestCycleListenerCollection);

        if (listenerClassNames == null || listenerClassNames.length == 0) {
            requestCycleListenerCollection.add(new DiagnosticsRequestCycleListener());
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

    private void addCsrfPreventionRequestCycleListener(final RequestCycleListenerCollection requestCycleListenerCollection) {
        final CsrfPreventionRequestCycleListener listener = new CsrfPreventionRequestCycleListener();
        // split on tab (\t), line feed (\n), carriage return (\r), form feed (\f), " ", and ","
        final String[] acceptedOrigins = StringUtils.split(getConfigurationParameter(ACCEPTED_ORIGIN_WHITELIST, null), " ,\t\f\r\n");
        if (acceptedOrigins != null && acceptedOrigins.length > 0) {
            for (String acceptedOrigin : acceptedOrigins) {
                listener.addAcceptedOrigin(acceptedOrigin);
            }
        }
        requestCycleListenerCollection.add(listener);
    }

    private static class ResponseSplittingProtectingServletWebResponse extends ServletWebResponse {

        public ResponseSplittingProtectingServletWebResponse(final WebRequest webRequest, final HttpServletResponse httpServletResponse) {
            super((ServletWebRequest) webRequest, httpServletResponse);
        }

        @Override
        public void addHeader(final String name, final String value) {
            if (containsCRorLF(value)) {
                throw new IllegalArgumentException("Header value must not contain CR or LF characters");
            }
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(final String name, final String value) {
            if (containsCRorLF(value)) {
                throw new IllegalArgumentException("Header value must not contain CR or LF characters");
            }
            super.setHeader(name, value);
        }

        @Override
        public void sendRedirect(String url) {
            Args.notNull(url, "url");
            if (containsCRorLF(url)) {
                throw new IllegalArgumentException("CR or LF detected in redirect URL: possible http response splitting attack");
            }

            if (url.equals("./")) {
                url += "?";
            }

            super.sendRedirect(url);
        }

        private boolean containsCRorLF(String s) {

            int length = s.length();

            for (int i = 0; i < length; ++i) {
                char c = s.charAt(i);
                if ('\n' == c || '\r' == c) {
                    return true;
                }
            }

            return false;
        }

    }

    private void addHeaderResponseDecorator() {

        final AbstractHeaderResponseFilter navAppFilter = new AbstractHeaderResponseFilter(NavAppPanel.NAVAPP_JAVASCRIPT_HEADER_ITEM) {
            @Override
            public boolean accepts(final HeaderItem item) {
                if (item instanceof FilteredHeaderItem) {
                    return ((FilteredHeaderItem) item).getFilterName().equals(this.getName());
                }
                return false;
            }
        };

        final FilteringHeaderResponse.IHeaderResponseFilter oppositeFilter = new OppositeHeaderResponseFilter(DEFAULT_HEADER_FILTER_NAME, navAppFilter);
        final List<FilteringHeaderResponse.IHeaderResponseFilter> filters = Arrays.asList(navAppFilter, oppositeFilter);

        setHeaderResponseDecorator(response -> new FilteringHeaderResponse(response, DEFAULT_HEADER_FILTER_NAME, filters) {

            final Predicate<HeaderItem> shouldRender =
                    isCmsApplication() && hasNoIFrameParameter()
                            ? NavAppUtils::isNavAppHeaderItem
                            : item -> !NavAppUtils.isNavAppHeaderItem(item);

            @Override
            public void render(final HeaderItem item) {
                if (shouldRender.test(item)) {
                    super.render(item);
                }
            }
        });
    }

    public static boolean isCmsApplication() {
        return PluginUserSession.get().getApplicationName().equals(PLUGIN_APPLICATION_VALUE_CMS);
    }

    public static boolean isConsoleApplication() {
        return PluginUserSession.get().getApplicationName().equals(PLUGIN_APPLICATION_VALUE_CONSOLE);
    }

    public static boolean hasNoIFrameParameter() {
        final RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle == null) {
            return true;
        }
        return requestCycle.getRequest().getQueryParameters().getParameterValue(CMS_AS_IFRAME_QUERY_PARAMETER).isNull();
    }

}
