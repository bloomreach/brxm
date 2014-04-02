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
package org.hippoecm.hst.core.container;

/**
 * HstComponent container constants
 *
 * @version $Id$
 */
public interface ContainerConstants {

    /**
     * The key used to bind the <code>HstRequest</code> to the underlying <code>HttpServletRequest</code>.
     */
    String HST_REQUEST = "org.hippoecm.hst.container.request";

    /**
     * The key used to bind the <code>HstResponse</code> to the underlying <code>HttpServletRequest</code>.
     */
    String HST_RESPONSE = "org.hippoecm.hst.container.response";

    /**
     * The attribute name used to set the request context object into the servlet request.
     */
    String HST_REQUEST_CONTEXT = "org.hippoecm.hst.core.request.HstRequestContext";

    /**
     * Default Addon module descriptor paths, which can be comma separated multiple path values.
     */
    String DEFAULT_ADDON_MODULE_DESCRIPTOR_PATHS = "META-INF/hst-assembly/addon/module.xml";

    /**
     * The reference namespace for container managed resource url.
     */
    String CONTAINER_REFERENCE_NAMESPACE = "org.hippoecm.hst.container.reference.namespace";

    /**
     * The key used to set forward path.
     */
    String HST_FORWARD_PATH_INFO = "org.hippoecm.hst.container.forward.path_info";

    /**
     * The key to indicate HstFilter should "reset" itself from being done, allowing multiple invokations.
     */
    String HST_RESET_FILTER = "org.hippoecm.hst.container.HstFilter.reset";

    /**
     * The head element attribute name prefix used as a hint for container to aggregate.
     */
    String HEAD_ELEMENT_CONTRIBUTION_HINT_ATTRIBUTE_PREFIX = "org.hippoecm.hst.container.head.element.contribution.hint.";

    /**
     * The category key hint for head elements. This category can be used to filter head elements during writing head
     * elements.
     */
    String HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE = HEAD_ELEMENT_CONTRIBUTION_HINT_ATTRIBUTE_PREFIX + "category";

    /**
     * The parameter name for custom error handler class name in the root component configuration
     */
    String CUSTOM_ERROR_HANDLER_PARAM_NAME = "org.hippoecm.hst.core.container.custom.errorhandler";

    /**
     * Subject session attribute name
     */
    String SUBJECT_ATTR_NAME = "org.hippoecm.hst.security.servlet.subject";

    /**
     * attribute that has a value when the request URI is already decoded.
     */
    String IS_REQUEST_URI_DECODED = "org.hippoecm.hst.container.HstContainerRequest.requestURI.isDecoded";

    /**
     * attribute that has a value when the request URI is already decoded.
     */
    String VIRTUALHOSTS_REQUEST_ATTR = "org.hippoecm.hst.configuration.hosting.VirtualHost.requestAttr";

    /**
     * Subject's repository credentials session attribute name (This one can be optionally and temporarily set in a
     * container that doesn't support JACC.)
     */
    String SUBJECT_REPO_CREDS_ATTR_NAME = "org.hippoecm.hst.security.servlet.subject.repo.creds";

    /**
     * Subject's repository credentials session attribute name (This one can be optionally and temporarily set in a
     * container that doesn't support JACC.)
     */
    String CMS_SSO_REPO_CREDS_ATTR_NAME = "org.hippoecm.hst.security.servlet.subject.repo.creds";

    /**
     * Preferred local request or session attribute name
     */
    String PREFERRED_LOCALE_ATTR_NAME = "org.hippoecm.hst.container.preferred.locale";

    /**
     * The dispatch URI scheme attribute name
     */
    String DISPATCH_URI_SCHEME = "org.hippoecm.hst.core.container.HstComponentWindow.dispatch.uri.scheme";

    String MOUNT_ALIAS_REST = "rest";

    String MOUNT_ALIAS_SITE = "site";

    String MOUNT_ALIAS_GALLERY = "gallery";

    String MOUNT_ALIAS_ASSETS = "assets";

    /**
     * 'composer_info' attr type name
     */
    String COMPOSER_MODE_ATTR_NAME = "org.hippoecm.hst.composer_info";

    /**
     * The parameter name used in the request to store whether or not all URLs that are created must be fully qualified
     */
    String HST_REQUEST_USE_FULLY_QUALIFIED_URLS = "org.hippoecm.hst.container.request.fqu";

    /**
     * The parameter name used in the request to store whether or not a different host than the one in the request needs
     * to be used
     */
    String RENDERING_HOST = "org.hippoecm.hst.container.render_host";

    String UNDECORATED_MOUNT = "org.hippoecm.hst.container.undecorated_mount";

    /**
     * The mount id of the site that is being viewed in the channel manager
     */
    String CMS_REQUEST_RENDERING_MOUNT_ID =  "org.hippoecm.hst.container.render_mount";

    /**
     * http session attribute to indicate a single sign on session is created through the cms
     */
    String CMS_SSO_AUTHENTICATED = "org.hippoecm.hst.container.sso_cms_authenticated";

    /**
     * If there is a correct cms sso authentication, then on the HttpSession we store the user id with this attribute name
     */
    String CMS_USER_ID_ATTR = "org.hippoecm.hst.container.cms_user_id";

    /**
     * The attribute used on the request to indicate that the request is from a CMS context *and* is a REST call
     * (page composer or cms-rest call)
     * that also might need to use the credentials from the cms (jcr session) user, for example a REST call that needs to modify the HST config
     */
    String CMS_REST_REQUEST_CONTEXT = "org.hippoecm.hst.container.sso_cms_rest_request_context";

    /**
     * The attribute used on the request or http session to indicate that the page should be rendered as some specific
     * variant
     */
    String RENDER_VARIANT = "org.hippoecm.hst.container.render_variant";

    /**
     * The 'default' prefix of HST component parameters. This prefix is used when no other prefix is set or the
     * configured prefix is empty.
     */
    String DEFAULT_PARAMETER_PREFIX = "hippo-default";

    /**
     * The current servlet filter chain request attribute name.
     */
    String HST_FILTER_CHAIN = "org.hippoecm.hst.container.filter.chain";

    String HST_JAAS_LOGIN_ATTEMPT_RESOURCE_URL_ATTR = "org.hippoecm.hst.security.servlet.LoginServlet.jaas_login_attempt_resource_url";

    String HST_JAAS_LOGIN_ATTEMPT_RESOURCE_TOKEN = "org.hippoecm.hst.security.servlet.LoginServlet.jaas_login_attempt_token";

}
