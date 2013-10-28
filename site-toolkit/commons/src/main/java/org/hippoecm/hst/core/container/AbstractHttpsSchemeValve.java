/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class with facility methods for implementing domain specific https scheme valve.
 *
 * Example Spring configuration:
 * <pre>
 *     &lt;bean id="httpsSchemeValve" init-method="initialize" destroy-method="destroy"
 *              class="com.example.hst.core.container.HttpsSchemeValve"&gt;
 *      &lt;property name="valveName" value="httpsSchemeExampleValve" /&gt;
 *      &lt;!-- Make sure it gets injected *after* the initializationValve --&gt;
 *      &lt;property name="afterValves" value="initializationValve"/&gt;
 *      &lt;property name="beforeValves" value="cmsSecurityValve"/&gt;
 *      &lt;!-- only use repository and configReaderCredentials below if you rely on getHstConfigSession() --&gt;
 *      &lt;property name="repository" ref="javax.jcr.Repository"/&gt;
 *      &lt;property name="configReaderCredentials" ref="javax.jcr.Credentials.hstconfigreader"/&gt;
 *      &lt;!-- value 301 below is default when not set --&gt;
 *      &lt;property name="redirectStatusCode" value="301"/&gt;
 *     &lt;/bean&gt;
 *
 *     &lt;!--below inject the valve in the  DefaultSitePipeline --&gt;
 *     &lt;bean class="org.springframework.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *          &lt;property name="targetObject"&gt;
 *              &lt;bean class="org.springframework.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *                  &lt;property name="targetObject" ref="org.hippoecm.hst.core.container.Pipelines" /&gt;
 *                  &lt;property name="targetMethod" value="getPipeline"/&gt;
 *                  &lt;property name="arguments"&gt;
 *                      &lt;value&gt;DefaultSitePipeline&lt;/value&gt;
 *                  &lt;/property&gt;
 *              &lt;/bean&gt;
 *          &lt;/property&gt;
 *          &lt;property name="targetMethod" value="addInitializationValve"/&gt;
 *          &lt;property name="arguments"&gt;
 *              &lt;ref bean="httpsSchemeValve" /&gt;
 *          &lt;/property&gt;
 *     &lt;/bean&gt;
 *
 * </pre>
 * Note above the init-method and destroy-method are optional and in most cases not needed.
 */
public abstract class AbstractHttpsSchemeValve extends AbstractOrderableValve {

    protected static final Logger log = LoggerFactory.getLogger(AbstractHttpsSchemeValve.class);

    public static final String HTTPS_SCHEME = "https";
    public static final String HTTP_SCHEME = "http";

    protected Repository repository;
    protected Credentials configReaderCredentials;
    protected int redirectStatusCode = HttpServletResponse.SC_MOVED_PERMANENTLY;

    /**
     * @param redirectStatusCode setter should only be invoked during Spring bean construction
     */
    public void setRedirectStatusCode(final int redirectStatusCode) {
        if (redirectStatusCode != HttpServletResponse.SC_MOVED_PERMANENTLY &&
            redirectStatusCode != HttpServletResponse.SC_TEMPORARY_REDIRECT &&
            redirectStatusCode != HttpServletResponse.SC_MOVED_TEMPORARILY) {
            log.warn("Ignoring invalid redirect status code '{}'. Only 301, 302 and 307 are supported.", String.valueOf(redirectStatusCode));
        } else {
            this.redirectStatusCode = redirectStatusCode;
        }
    }

    /**
     * @param repository setter should only be invoked during Spring bean construction
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * @param configReaderCredentials setter should only be invoked during Spring bean construction
     */
    public void setConfigReaderCredentials(Credentials configReaderCredentials) {
        this.configReaderCredentials = configReaderCredentials;
    }

    /**
     * <p>
     *     Implementations of this {@link AbstractHttpsSchemeValve} should decide in this method whether the request
     *     {@link org.hippoecm.hst.core.container.ValveContext#getServletRequest()} is required to be served over <code>https</code>,
     *     Whether the request in the end will actually be served over <code>https</code> also depends on other variables,
     *     see the javadoc from {@link #invoke(ValveContext)}
     * </p>
     * <p>
     *     Example 1: Assume you want documents of (sub)type <code>myproject:form</code> to be served over <code>https</code>,
     *     you can achieve this as follows:
     *     <pre>
     *        try {
     *            final HippoBean contentBean = context.getRequestContext().getContentBean();
     *            if (contentBean != null && contentBean.getNode().isNodeType("myproject:form")) {
     *              return true;
     *            }
     *            return false;
     *        } catch (RepositoryException e) {
     *            throw new RuntimeRepositoryException(e);
     *        }
     *     </pre>
     * </p>
     * <p>
     *     Example 2: Assume you want documents with mixin <code>myproject:secure</code> to be served over <code>https</code>,
     *    you can achieve this exactly the same as the code above. The <code>myproject:secure</code> can be added to
     *    prototype documents in the CMS to easily have by default some document marked to be secure
     * </p>
     * <p>
     *     Example 3: Assume you want to read some configuration from some node below /hippo:configuration, and
     *     depending on some regexp matching the current pathInfo to redirect or not
     *     <pre>
     *         Session configSession = getHstConfigSession();
     *         try {
     *             String regexp = configSession.getNode("/hippo:configuration/redirects/regexp").getProperty("matches").getString();
     *             return context.getServletRequest().getPathInfo().matches(regexp);
     *         } catch (PathNotFoundException e) {
     *         } catch (ValueFormatException e) {
     *         } catch (RepositoryException e) {
     *         } finally {
     *             configSession.logout();
     *         }
     *         return false;
     *     </pre>
     * </p>
     * <p>
     *      Example 4: Assume you want to redirect <b>any</b> content bean that contains any form. Assume your form is a compound
     *      with bean MyForm and in your base bean you have <code>public MyForm getForm()</code> that returns <code>null</code>
     *      in case there is no form.
     *      <pre>
     *          try {
     *              final HippoBean contentBean = context.getRequestContext().getContentBean();
     *              if (contentBean == null || !(contentBean instanceof MyBaseBean)) {
     *               return false;
     *              }
     *              return ((MyBaseBean)contentBean).getForm() != null;
     *              } catch (RepositoryException e) {
     *                  throw new RuntimeRepositoryException(e);
     *              }
     *      </pre>
     *
     * </p>
     * <p>
     *      Example 5: Assume you want to redirect <b>any</b> content bean that contains some boolean field (clickable in
     *      the cms document editor)
     *      that marks it to be secure. Assume you expose this property through MyBaseBean#isSecure();
     *      <pre>
     *          try {
     *              final HippoBean contentBean = context.getRequestContext().getContentBean();
     *              if (contentBean == null || !(contentBean instanceof MyBaseBean)) {
     *               return false;
     *              }
     *              return ((MyBaseBean)contentBean).isSecure();
     *              } catch (RepositoryException e) {
     *                  throw new RuntimeRepositoryException(e);
     *              }
     *      </pre>
     *
     * </p>
     * <p>
     *     Note that this method only gets conditionally invoked: For example if the request is already over
     *     <code>https</code> there is no pointing in checking this method. Same goes in case the request is for
     *     example a cms request. See {@link #invoke(ValveContext)} javadoc for more.
     * </p>
     * @return <code>true</code> when the request should be secure
     */
    public abstract boolean requiresHttps(ValveContext context);

    /**
     * <p>
     *     this valve might do a client side redirect status code {@link #getRedirectStatusCode()}. The redirect will be
     *     to the same URL as for the current request, only with scheme <code>https</code> instead.
     *     A redirect is done <b>only</b> if:
     *     <ol>
     *         <li>The request is <b>not</b> a {@link org.hippoecm.hst.core.request.HstRequestContext#isCmsRequest()} :
     *         For cms requests no redirect is done as the scheme of the cms host is used to piggyback on.</li>
     *         <li>The {@link org.hippoecm.hst.core.request.ResolvedSiteMapItem} is not <code>null</code>
     *         and <b>not</b> {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem#isSchemeAgnostic()}
     *         </li>
     *         <li>The {@link org.hippoecm.hst.core.request.ResolvedSiteMapItem} is <code>null</code> and
     *         {@link org.hippoecm.hst.core.request.ResolvedMount} is <b>not</b>
     *         {@link org.hippoecm.hst.configuration.hosting.Mount#isSchemeAgnostic()}
     *         </li>
     *         <li>{@link #requiresHttps(ValveContext)} returns <code>true</code></li>
     *     </ol>
     * </p>
     * <p>
     *     In case a redirect is done, processing valves are short-circuited and the cleanup valves are invoked directly
     *     after this valve.
     * </p>
     * <p>
     *     <b<Note</b> that the custom to be implemented {@link #requiresHttps(ValveContext)} is not always invoked.
     *     {@link #requiresHttps(ValveContext)} is for example skipped when the request is already over <code>https</code>,
     *     or when the mount/sitemapitem is marked to be scheme agnostic, or when the request is a cms request,
     * </p>
     * <p>
     *     <b>Note 2</b>: This valve <b>requires</b> that the matched host has 'https approved = true'. This can be
     *     achieved by setting the property <code>hst:customhttpssupport = true</code> on the matching hst:virtualhost or one
     *     of its ancestors.
     * </p>
     *
     */
    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();
        if (requestContext.isCmsRequest()) {
            context.invokeNext();
            return;
        }
        final HstSiteMapItem siteMapItem = (requestContext.getResolvedSiteMapItem() == null) ? null : requestContext.getResolvedSiteMapItem().getHstSiteMapItem();
        final Mount mount = requestContext.getResolvedMount().getMount();
        if (siteMapItem != null && siteMapItem.isSchemeAgnostic()) {
            context.invokeNext();
            return;
        } else {
            if (mount.isSchemeAgnostic()){
                context.invokeNext();
                return;
            }
        }

        final String currentScheme = HstRequestUtils.getFarthestRequestScheme(context.getServletRequest());
        if (HTTPS_SCHEME.equalsIgnoreCase(currentScheme)) {
            // already https, nothing to do
            context.invokeNext();
            return;
        }
        log.debug("Invoking #requiresHttps to find out whether request should be over https.");
        boolean secure = requiresHttps(context);
        if (!secure) {
            context.invokeNext();
            return;
        }

        String currentUrl = HstRequestUtils.createURLWithExplicitSchemeForRequest(HTTP_SCHEME, mount, context.getServletRequest());

        String defaultSupportedScheme = (siteMapItem == null) ? mount.getScheme() : siteMapItem.getScheme();
        if (!HTTPS_SCHEME.equalsIgnoreCase(defaultSupportedScheme)) {
            // check if hst by default supports https requests even if the current request is http and hst:scheme is 'http':
            // If hst does not support https requests a redirect to https would result in a redirect loop.
            // Instead of redirect to https we then serve request over http and log a warning
            if(!mount.getVirtualHost().isCustomHttpsSupported()) {
                log.warn("Current URL '{}' is over http but '{}' indicated preference over 'https' but virtualhost '{}' does not have" +
                        "'{}' = true. Set this property to true support url over https. Request will now be " +
                        "rendered over http.",
                        new String[]{currentUrl, this.getClass().getName()+"#requiresHttps", mount.getVirtualHost().getHostName(), HstNodeTypes.VIRTUALHOST_PROPERTY_CUSTOM_HTTPS_SUPPORT});
                context.invokeNext();
                return;
            }
        }

        // since the request is preferred over https, and the hst has https approved OR the defaultSupportedScheme is https, we can do a redirect

        final HttpServletResponse response = context.getServletResponse();
        // scheme is not https but request must be secure.
        final String redirect = HstRequestUtils.createURLWithExplicitSchemeForRequest(HTTPS_SCHEME, mount, context.getServletRequest());
        log.info("Client side redirect request '{}' to '{}'", currentUrl, redirect);
        if (getRedirectStatusCode() == HttpServletResponse.SC_MOVED_PERMANENTLY) {
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            // create fully qualified redirect to scheme https
            response.setHeader("Location", redirect);
            return;
        } else {
            try {
                response.sendRedirect(redirect);
            } catch (IOException e) {
                throw new ContainerException("Could not redirect", e);
            }
            return;
        }

    }

    /**
     * @return the response status code to use in case of a redirect. Default value is HttpServletResponse.SC_MOVED_PERMANENTLY
     */
    protected int getRedirectStatusCode() {
        return redirectStatusCode;
    }

    /**
     * @return <code>true</code> when the request scheme from the original host information requested by the client
     * is equals to 'https'
     */
    protected boolean isSchemeHttps(HttpServletRequest request) {
        return HTTPS_SCHEME.equalsIgnoreCase(getScheme(request));
    }

    /**
     * @return the request scheme from the original host information requested by the client
     */
    protected String getScheme(HttpServletRequest request) {
        return HstRequestUtils.getFarthestRequestScheme(request);
    }

    /**
     * <p>
     *     Returns the config user {@link Session} which in general has read-access to most parts of the repository.
     *     This {@link Session} might be useful if you need to for example read some jcr node below say '/hippo:configuration'
     *     where in general the preview and live jcr session users do not have read access.
     * </p>
     * <p>
     *     <bo>Note</bo> make sure that after you are finished with this {@link Session} you invoke
     *     {@link javax.jcr.Session#logout()}
     * </p>
     * @return the {@link Session} for the config user
     * @throws RepositoryException is login or some other repository exception happens
     * @throws IllegalStateException if not repository or configReaderCredentials are set
     */
    protected Session getHstConfigSession() {
        if (repository == null || configReaderCredentials == null) {
            throw new IllegalStateException("No repository or configReaderCredentials are set");
        }
        try {
            return repository.login(configReaderCredentials);
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("Could not login config user.",  e);
        }
    }
}
