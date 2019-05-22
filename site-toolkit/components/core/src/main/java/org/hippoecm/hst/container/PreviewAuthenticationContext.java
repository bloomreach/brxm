package org.hippoecm.hst.container;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.cache.Cache;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewAuthenticationContext {

    private final static Logger log = LoggerFactory.getLogger(PreviewAuthenticationHandler.class);

    private static final String PREVIEW_AUTHENTICATION_REQUEST_ATTR_NAME = PreviewAuthenticationContext.class.getName() + ".token";

    private Credentials credentials;
    private boolean unauthorized;
    private String renderingHost;
    private boolean channelManagerRequest;
    private boolean previewRequest;
    private boolean redirected;
    private String cmsSessionContextId;

    // TODO how to avoid customers that have implemented their own JWT tokens from being handled by this?
    public PreviewAuthenticationContext(final VirtualHosts vHosts, final HstContainerRequest request,
                                        final HttpServletResponse res, final Cache<String, SimpleCredentials> cmsUserRegistry) throws ContainerException {

        // check whether there is an access token present

        // TODO token handling by https://medium.com/@hantsy/protect-rest-apis-with-spring-security-and-jwt-5fbc90305cc5

        String authorization = request.getHeader("Authorization");
        if (authorization != null ) {

            // TODO replace custom token stuff below with https://medium.com/@hantsy/protect-rest-apis-with-spring-security-and-jwt-5fbc90305cc5
            authorization = StringUtils.trim(authorization);
            if (authorization.startsWith("Bearer")) {
                final String accessToken = StringUtils.trim(StringUtils.substringAfter(authorization, "Bearer"));
                try {
                    final AccessToken token = new AccessToken(accessToken);

                    // validate cms session context id

                    cmsSessionContextId = token.getCmsSCID();

                    final SimpleCredentials cmsUser = cmsUserRegistry.getIfPresent(cmsSessionContextId);
                    if (cmsUser != null) {
                        // cmsSCID is known and thus is the token valid

                        credentials = cmsUser;

                        if (token.isPreviewRequest()) {
                            previewRequest = true;
                        }

                        if (token.isChannelManagerRequest()) {
                            channelManagerRequest = true;
                        }

                        request.setAttribute(PREVIEW_AUTHENTICATION_REQUEST_ATTR_NAME, this);

                    } else {
                        log.info("Invalid access token.");
                        unauthorized = true;
                        return;
                    }

                    return;
                } catch (Exception e) {
                    log.info("Invalid access token : {}.", e.toString());
                    unauthorized = true;
                    return;
                }
            }
        }

        renderingHost = HstRequestUtils.getRenderingHost(request);
        if (requestIsForChannelManager(vHosts, renderingHost, request)) {
            channelManagerRequest = true;
            if (!CmsSSOAuthenticationHandler.isAuthenticated(request)) {
                if (!CmsSSOAuthenticationHandler.authenticate(request, res, cmsUserRegistry)) {
                    redirected = true;
                    return;
                }
            }
        } else {
            channelManagerRequest = false;
        }


    }

    /**
     * @return PreviewAuthenticationContext for the current {@code request} oir {@link null} if there is not such context
     */
    public static PreviewAuthenticationContext get(final HttpServletRequest request) {
       return (PreviewAuthenticationContext) request.getAttribute(PREVIEW_AUTHENTICATION_REQUEST_ATTR_NAME);
    }

    // returns true if the request is meant to serve a page (model) in the Channel Manager
    private boolean requestIsForChannelManager(VirtualHosts vHosts, final String renderingHost, final HstContainerRequest req) {
        if (renderingHost == null) {
            return false;
        }
        if(vHosts.getCmsPreviewPrefix() == null || "".equals(vHosts.getCmsPreviewPrefix())) {
            return true;
        }
        if (req.getPathInfo() == null || req.getPathInfo().isEmpty()) {
            return false;
        }
        if(req.getPathInfo().substring(1).startsWith(vHosts.getCmsPreviewPrefix())) {
            return true;
        }
        return false;

    }

    public Credentials getCredentials() {
        return credentials;
    }

    public String getCmsSessionContextId() {
        return cmsSessionContextId;
    }

    public String getRenderingHost() {
        return renderingHost;
    }

    public boolean isPreviewRequest() {
        return previewRequest;
    }

    public boolean isChannelManagerRequest() {
        return channelManagerRequest;
    }

    public boolean isRedirected() {
        return redirected;
    }

    public boolean isUnauthorized() {
        return unauthorized;
    }
}
