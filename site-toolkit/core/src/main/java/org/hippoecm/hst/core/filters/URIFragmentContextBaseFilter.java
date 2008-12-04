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
package org.hippoecm.hst.core.filters;

import org.hippoecm.hst.caching.observation.EventListenerImpl;
import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.Timer;
import org.hippoecm.hst.core.context.ContextBase;
import org.hippoecm.hst.core.mapping.RelativeURLMappingImpl;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.mapping.URLMappingManager;
import org.hippoecm.hst.core.request.URLBaseHttpRequestServletWrapper;
import org.hippoecm.hst.jcr.JCRConnectionException;
import org.hippoecm.hst.jcr.JcrSessionPoolManager;
import org.hippoecm.hst.jcr.ReadOnlyPooledSession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>
 * This filter initializes both the content ContextBase and the ContextBase that refers to the hst:configuration
 * location. Both contextBases are made available on the request as attributes.<br/>
 * <p/>
 * The content ContextBase is determines the root of the content in the repository from a prefix of the
 * requestURI.
 * <p/>
 * <p/>
 * </p>
 */
public class URIFragmentContextBaseFilter extends HstBaseFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(URIFragmentContextBaseFilter.class);

    private static final String CONTENT_BASE_INIT_PARAMETER = "contentBase";
    private static final String URI_LEVEL_INIT_PARAMETER = "levels";
    private static final String RELATIVE_HST_CONFIGURATION_LOCATION = "/hst:configuration/hst:configuration";

    private String contentBase;
    private int uriLevels;
    private Session observer;
    private EventListener listener;
    private volatile boolean isListenerRegistered = false;
    private FilterConfig filterConfig;
    private final JcrSessionPoolManager jcrSessionPoolManager = new JcrSessionPoolManager();


    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        this.filterConfig = filterConfig;
        contentBase = ContextBase.stripFirstSlash(getInitParameter(filterConfig, CONTENT_BASE_INIT_PARAMETER, true));
        try {
            uriLevels = Integer.parseInt(getInitParameter(filterConfig, URI_LEVEL_INIT_PARAMETER, true));
        } catch (NumberFormatException e) {
            throw new ServletException("The init-parameter " + URI_LEVEL_INIT_PARAMETER + " is not an int.");
        }

    }


    public void destroy() {
        jcrSessionPoolManager.dispose();
        try {
            if (listener != null && observer != null && observer.getWorkspace() != null) {
                ObservationManager obMgr = observer.getWorkspace().getObservationManager();
                if (obMgr != null) {
                    obMgr.removeEventListener(listener);
                    observer.logout();
                }
                log.debug("Destroy succesfully disposed all jcr sessions");
            }
        } catch (UnsupportedRepositoryOperationException e) {
            log.error("UnsupportedRepositoryOperationException during 'destroy()' of filter in disposing jcr sessions and unregistering listener. " + e.getMessage());
        } catch (RepositoryException e) {
            log.error("RepositoryException during 'destroy()' of filter in disposing jcr sessions and unregistering listener. " + e.getMessage());
        }
    }

    public void doFilter(ServletRequest req, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse httpResponse = ((HttpServletResponse) response);
        synchronized (this) {
            if (!isListenerRegistered) {
                registerEventListener(filterConfig);
                isListenerRegistered = true;
            }
        }
        if (ignoreRequest(request)) {
            filterChain.doFilter(request, response);
        } else {
            handleRequestForThisFilter(response, filterChain, request, httpResponse);
        }

    }

    private void handleRequestForThisFilter(ServletResponse response, FilterChain filterChain, HttpServletRequest request, HttpServletResponse httpResponse) throws IOException, ServletException {
        String requestURI = request.getRequestURI().replaceFirst(request.getContextPath(), "");

        String uriPrefix = getLevelPrefix(requestURI, uriLevels);
        if (uriPrefix == null) {
            log.warn("The number of slashes in the url in lower then the configure levels '({})' in your web.xml \n"
                    + "Either, the url is wrong, or you should change the levels in value in your web.xml", uriLevels);
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (isBinaryRequest(requestURI, uriPrefix)) {
            forwardToBinaryServlet(request, response, requestURI, uriPrefix);
            return;
        }

        Session session = null;
        long requesttime = System.currentTimeMillis();
        try {
            session = putSessionAndMappedUrlOnRequest(request, uriPrefix);

            ContextBase contentContextBase = determineContentContextBase(request, httpResponse, uriPrefix);
            if (contentContextBase == null) {
                return;
            }

            ContextBase hstConfigurationContextBase = determineHstConfigurationContextBase(request, httpResponse, uriPrefix);
            if (hstConfigurationContextBase == null) {
                return;
            }

            HttpServletRequestWrapper prefixStrippedRequest = createPrefixStrippedRequest(request, requestURI, uriPrefix, contentContextBase, hstConfigurationContextBase);

            filterChain.doFilter(prefixStrippedRequest, response);
        } finally {
            if (session != null && session instanceof ReadOnlyPooledSession) {
                ((ReadOnlyPooledSession) session).getJcrSessionPool().release(request.getSession());
            }
            Timer.log.debug("Handling request took " + (System.currentTimeMillis() - requesttime));
        }
    }

    private HttpServletRequestWrapper createPrefixStrippedRequest(HttpServletRequest request, String requestURI, String uriPrefix,
                                                                  ContextBase contentContextBase, ContextBase hstConfigurationContextBase) {
        HttpServletRequestWrapper prefixStrippedRequest = new URLBaseHttpRequestServletWrapper(request,
                uriPrefix);

        prefixStrippedRequest.setAttribute(HSTHttpAttributes.CURRENT_CONTENT_CONTEXTBASE_REQ_ATTRIBUTE,
                contentContextBase);
        prefixStrippedRequest.setAttribute(
                HSTHttpAttributes.CURRENT_HSTCONFIGURATION_CONTEXTBASE_REQ_ATTRIBUTE,
                hstConfigurationContextBase);
        prefixStrippedRequest.setAttribute(HSTHttpAttributes.ORIGINAL_REQUEST_URI_REQ_ATTRIBUTE, requestURI);
        prefixStrippedRequest.setAttribute(HSTHttpAttributes.URI_PREFIX_REQ_ATTRIBUTE, uriPrefix);
        return prefixStrippedRequest;
    }

    private ContextBase determineHstConfigurationContextBase(HttpServletRequest request, HttpServletResponse httpResponse, String uriPrefix) throws IOException {
        ContextBase hstConfigurationContextBase = null;
        try {
            hstConfigurationContextBase = new ContextBase(TEMPLATE_CONTEXTBASE_NAME, contentBase + uriPrefix
                    + RELATIVE_HST_CONFIGURATION_LOCATION, request);
        } catch (PathNotFoundException e) {
            log.warn("cannot get hstConfigurationContextBase : PathNotFoundException " + e.getMessage() + " for '" + contentBase + uriPrefix + "'");
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RepositoryException e) {
            log.warn("cannot get hstConfigurationContextBase : RepositoryException " + e.getMessage() + " for '" + contentBase + uriPrefix + "'");
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        return hstConfigurationContextBase;
    }

    private ContextBase determineContentContextBase(HttpServletRequest request, HttpServletResponse httpResponse, String uriPrefix) throws IOException {
        ContextBase contentContextBase = null;
        try {
            contentContextBase = new ContextBase(uriPrefix, contentBase + uriPrefix, request);
        } catch (PathNotFoundException e) {
            log.warn("cannot get contentContextBase : PathNotFoundException {} for '" + contentBase + uriPrefix + "'", e.getMessage(),contentBase + uriPrefix);
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RepositoryException e) {
            log.warn("cannot get contentContextBase : RepositoryException " + e.getMessage() + " for '" + contentBase + uriPrefix + "'");
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return contentContextBase;
    }

    private Session putSessionAndMappedUrlOnRequest(HttpServletRequest request, String uriPrefix) {
        Session session;
        session = jcrSessionPoolManager.getSession(request);
        request.setAttribute(HSTHttpAttributes.JCRSESSION_MAPPING_ATTR, session);

        URLMapping urlMapping = URLMappingManager.getUrlMapping(session, request,
                uriPrefix, contentBase + uriPrefix + RELATIVE_HST_CONFIGURATION_LOCATION, uriLevels);

        URLMapping relativeURLMapping = new RelativeURLMappingImpl(request.getRequestURI(), urlMapping);
        request.setAttribute(HSTHttpAttributes.URL_MAPPING_ATTR, relativeURLMapping);
        return session;
    }

    private void forwardToBinaryServlet(HttpServletRequest request, ServletResponse response, String requestURI,
                                        String uriPrefix) throws ServletException, IOException {
        String forward = requestURI.substring(uriPrefix.length());
        log.debug("Forwarding request to binaries servlet: '{}' --> '{}'", request.getRequestURI(),forward);
        RequestDispatcher dispatcher = request.getRequestDispatcher(forward);
        dispatcher.forward(request, response);
    }

    private boolean isBinaryRequest(String requestURI, String uriPrefix) {
        return requestURI.substring(uriPrefix.length()).startsWith("/binaries");
    }

    /**
     * Returns the prefix of a String where the prefix has a specified number of
     * slashes.
     *
     * @param requestURI
     * @param levels
     * @return
     */
    private String getLevelPrefix(String requestURI, int levels) {
        String[] splittedURI = requestURI.split("/");
        if (splittedURI.length <= levels) {
            return null;
        }
        StringBuffer levelPrefix = new StringBuffer();
        for (int i = 1; i <= levels; i++) {
            levelPrefix.append("/").append(splittedURI[i]);
        }
        return levelPrefix.toString();
    }

    private void registerEventListener(FilterConfig filterConfig) {
        String repositoryLocation = HSTConfiguration.get(filterConfig.getServletContext(), HSTConfiguration.KEY_REPOSITORY_ADRESS);
        String username = HSTConfiguration.get(filterConfig.getServletContext(), HSTConfiguration.KEY_REPOSITORY_USERNAME);
        String password = HSTConfiguration.get(filterConfig.getServletContext(), HSTConfiguration.KEY_REPOSITORY_PASSWORD);
        SimpleCredentials smplCred = new SimpleCredentials(username, (password != null ? password.toCharArray() : null));

        HippoRepositoryFactory.setDefaultRepository(repositoryLocation);
        try {
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            observer = repository.login(smplCred);
        } catch (LoginException e) {
            throw new JCRConnectionException("Cannot login with credentials");
        } catch (RepositoryException e) {
            log.error("Problem while obtaining a session from the repository for registering the event listeners");
            throw new JCRConnectionException("Failed to initialize repository");
        }

        ObservationManager obMgr;
        try {
            obMgr = observer.getWorkspace().getObservationManager();
            listener = new EventListenerImpl();
            obMgr.addEventListener(listener, Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                    "/",
                    true, null, null, true);

        } catch (UnsupportedRepositoryOperationException e) {
            log.error("Tried to perform something on the repository while adding eventlisteners that we cannot do",e);
        } catch (RepositoryException e) {
            log.error("Problem in the repository while addind event listeners",e);
        }
    }


}


