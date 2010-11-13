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
package org.hippoecm.hst.demo.wicketexamples;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.hst.site.HstServices;

/**
 * This example wicket application is designed to demonstrate
 * how to access a JCR session pooling repository component provided by HST-2 container
 * and how an application can use simple pure JCR APIs for some purposes.
 * <P>
 * <EM>Please note that this example is not designed to support more advanced JCR repository features
 * such as workflow, virtual nodes, document templates or faceted navigation
 * which are supported by some JCR repository implementations.</EM> 
 * </P>
 * <P>
 * The wicket filter configuration for this application can have <CODE>basePath</CODE> and <CODE>itemsPerPage</CODE>
 * init parameters like the following example:
 * <CODE><PRE>
 *  &lt;filter>
 *    &lt;filter-name>WicketContentBrowser&lt;/filter-name>
 *    &lt;filter-class>org.apache.wicket.protocol.http.WicketFilter&lt;/filter-class>
 *    &lt;init-param>
 *      &lt;param-name>applicationClassName&lt;/param-name>
 *      &lt;param-value>org.hippoecm.hst.wicketexamples.WicketContentBrowserApplication&lt;/param-value>
 *    &lt;/init-param>
 *    &lt;init-param>
 *      &lt;param-name>binary-download-servlet-path&lt;/param-name>
 *      &lt;param-value>/site/binaries&lt;/param-value>
 *    &lt;/init-param>
 *    &lt;init-param>
 *      &lt;param-name>basePath&lt;/param-name>
 *      &lt;param-value>/&lt;/param-value>
 *    &lt;/init-param>
 *    &lt;init-param>
 *      &lt;param-name>itemsPerPage&lt;/param-name>
 *      &lt;param-value>10&lt;/param-value>
 *    &lt;/init-param>
 *  &lt;/filter>
 * </PRE></CODE>
 * The <CODE>binary-download-servlet-path</CODE> is for setting the binary content download servlet path.
 * This binary content download servlet path should be set with the context path.
 * The <CODE>basePath</CODE> is for setting the navigation base path and the <CODE>itemsPerPage</CODE> is for setting
 * the list item size of a page. If you don't configure those, the default value of <CODE>basePath</CODE> is '/' and
 * the default value of <CODE>itemsPerPage</CODE> is 10.
 * </P>
 * <P>
 * In this example application, you can also access the JCR session pooling repository in an indirect way or in a direct way.
 * The indirect way is to use JNDI, and the direct way is to directly access to the {@link org.hippoecm.hst.site.HstServices} and {@link org.hippoecm.hst.core.container.ComponentManager}
 * managed by the HST-2 container.
 * </P>
 * <P>
 * If you want to use the indirect way with JNDI, then you need to set an init parameter in the Wicket filter configuration
 * for this application like the following example:
 * <CODE><PRE>
 *  &lt;filter>
 *    &lt;filter-name>WicketContentBrowser&lt;/filter-name>
 *    &lt;filter-class>org.apache.wicket.protocol.http.WicketFilter&lt;/filter-class>
 *    &lt;init-param>
 *      &lt;param-name>applicationClassName&lt;/param-name>
 *      &lt;param-value>org.hippoecm.hst.wicketexamples.WicketContentBrowserApplication&lt;/param-value>
 *    &lt;/init-param>
 *    &lt;init-param>
 *      &lt;param-name>repository-res-ref-name&lt;/param-name>
 *      &lt;param-value>jcr/repository&lt;/param-value>
 *    &lt;/init-param>
 *    &lt;init-param>
 *      &lt;param-name>repository-user&lt;/param-name>
 *      &lt;param-value>admin&lt;/param-value>
 *    &lt;/init-param>
 *    &lt;init-param>
 *      &lt;param-name>repository-password&lt;/param-name>
 *      &lt;param-value>admin&lt;/param-value>
 *    &lt;/init-param>
 *  &lt;/filter>
 * </PRE></CODE>
 * If you want to know how to configure the JNDI resource for the JCR session pooling repository provided by HST,
 * please see the javadocs of {@link org.hippoecm.hst.core.jcr.pool.BasicPoolingRepositoryFactory} or {@link org.hippoecm.hst.core.jcr.pool.MultiplePoolingRepositoryFactory}.
 * </P>
 * <P>
 * If you don't configure the <CODE>repository-user</CODE> or <CODE>repository-password</CODE>, then
 * this example application would try logging in by default login feature by using {@link javax.jcr.Repository#login()}.
 * </P>
 * <P>
 * If you don't configure the <CODE>repository-res-ref-name</CODE>, then
 * this example application would use the direct way to use {@link org.hippoecm.hst.site.HstServices} instead of using JNDI.
 * </P>
 * 
 * @version $Id: WicketContentBrowserApplication.java 18546 2009-06-12 18:21:11Z wko $
 */
public class WicketContentBrowserApplication extends WebApplication {
    
    private Repository repository;
    private Credentials credentials;
    private boolean credentialsConfigured = true;
    private String binaryDownloadServletPath;
    
    @Override
    public Class getHomePage() {
        return WicketContentBrowserPage.class;
    }
    
    public Repository getDefaultRepository() throws Exception {
        if (repository == null) {
            String repositoryResourceReferenceName = getInitParameter("repository-res-ref-name");
            
            if (repositoryResourceReferenceName != null) {
                Context initCtx = new InitialContext();
                Context envCtx = (Context) initCtx.lookup("java:comp/env");
                repository = (Repository) envCtx.lookup(repositoryResourceReferenceName);
            } else {
                repository = HstServices.getComponentManager().getComponent("javax.jcr.Repository");
            }
        }
        
        return repository;
    }
    
    public Credentials getDefaultCredentials() {
        if (credentials == null && credentialsConfigured) {
            String user = getInitParameter("repository-user");
            String password = getInitParameter("repository-password");
            
            if (user != null && password != null) {
                credentials = new SimpleCredentials(user, password.toCharArray());
            } else {
                credentialsConfigured = false;
            }
        }
        
        return credentials;
    }
    
    public String getBinaryDownloadServletPath() {
        if (binaryDownloadServletPath == null) {
            binaryDownloadServletPath = getInitParameter("binary-download-servlet-path");
            
            if (binaryDownloadServletPath == null) {
                binaryDownloadServletPath = "/binaries";
            }
        }
        
        return binaryDownloadServletPath;
    }
    
}
