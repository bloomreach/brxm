/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.freemarker.DelegatingTemplateLoader;
import org.hippoecm.hst.freemarker.HstClassTemplateLoader;
import org.hippoecm.hst.freemarker.RepositoryTemplateLoader;
import org.hippoecm.hst.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.Configurable;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class HstFreemarkerServlet extends FreemarkerServlet {

    private static final Logger log = LoggerFactory.getLogger(HstFreemarkerServlet.class);

    private static final long serialVersionUID = 1L;

    private static final String ATTR_JSP_TAGLIBS_MODEL = ".freemarker.JspTaglibs";

    private boolean lookupVirtualWebappLibResourcePathsChecked;

    private boolean lookupVirtualWebappLibResourcePathsEnabled;

    private boolean taglibModelInitialized;
    
    private RepositoryTemplateLoader repositoryTemplateLoader;

    @Override
    public void init() throws ServletException {
        super.init();

        Configuration conf = super.getConfiguration();

        if (!hasInitParameter(Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY)) {
            log.info("No '"+Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY+"' init param set. HST will set FreeMarker servlet to log and *continue* " +
                    "(TemplateExceptionHandler.IGNORE_HANDLER) rendering in case of template exceptions. ");
            conf.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
        }

        conf.setLocalizedLookup(false);
        
    }
    
    
    
    @Override
    public void destroy() {
        super.destroy();
        repositoryTemplateLoader.destroy();
    }


    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        try {
            super.doGet(request, response);
        } catch (ServletException e) {
            if (log.isDebugEnabled()) {
                log.warn("Freemarker template exception : ", e);
            } else {
                log.warn("Freemarker template exception : {}", e.toString());
            }
        }
    }

    /**
     * Special dispatch info is included when the request contains the attribute {@link ContainerConstants#DISPATCH_URI_SCHEME}. For example
     * this value is 'classpath' or 'jcr' to load a template from a classpath or repository
     */
    @Override 
    protected String requestUrlToTemplatePath(HttpServletRequest request)
    {
        String path = super.requestUrlToTemplatePath(request);
        if(request.getAttribute(ContainerConstants.DISPATCH_URI_SCHEME) != null){            
            path = request.getAttribute(ContainerConstants.DISPATCH_URI_SCHEME) + ":" + path;   
        }
        return path;
    }

    @Override
    protected TemplateModel createModel(ObjectWrapper wrapper, ServletContext servletContext,
            final HttpServletRequest request, final HttpServletResponse response) throws TemplateModelException {
        
        if (!lookupVirtualWebappLibResourcePathsChecked) {
            Set libPaths = servletContext.getResourcePaths("/WEB-INF/lib");
            lookupVirtualWebappLibResourcePathsEnabled = (libPaths == null || libPaths.isEmpty());
            lookupVirtualWebappLibResourcePathsChecked = true;
        }
        
        if (!lookupVirtualWebappLibResourcePathsEnabled) {
            return super.createModel(wrapper, servletContext, request, response);
        }
        
        TemplateModel params = super.createModel(wrapper, servletContext, request, response);
        
        if (!taglibModelInitialized) {
            ProxyFactory factory = new ProxyFactory();
            Interceptor interceptor = new Interceptor() {
                public Object intercept(Invocation invocation) throws Throwable {
                    Method method = invocation.getMethod();
                    String methodName = method.getName();
                    Object [] args = invocation.getArguments();
                    
                    if ("getResourcePaths".equals(methodName) && args.length > 0 && ("/WEB-INF/lib".equals(args[0]) || "/WEB-INF/lib/".equals(args[0]))) {
                        ClassLoader loader = Thread.currentThread().getContextClassLoader();
                        
                        if (loader instanceof URLClassLoader) {
                            URL [] urls = ((URLClassLoader) loader).getURLs();
                            
                            if (urls != null) {
                                Set<String> paths = new HashSet<String>();
                                
                                for (int i = 0; i < urls.length; i++) {
                                    String url = urls[i].toString();
                                    paths.add(url);
                                }
                                
                                return paths;
                            }
                        }
                    } else if ("getResourceAsStream".equals(methodName) && args.length > 0 && args[0].toString().startsWith("file:")) {
                        URL url = new URL((String) args[0]);
                        return url.openStream();
                    } else if ("getResource".equals(methodName) && args.length > 0 && args[0].toString().startsWith("file:")) {
                        URL url = new URL((String) args[0]);
                        return url;
                    }
                    
                    return invocation.proceed();
                }
            };
            
            ServletContext virtualContext = (ServletContext) factory.createInterceptorProxy(servletContext, interceptor, new Class [] { ServletContext.class });
            
            TaglibFactory taglibs = new TaglibFactory(virtualContext);
            servletContext.setAttribute(ATTR_JSP_TAGLIBS_MODEL, taglibs);
            
            taglibModelInitialized = true;
        }
        
        if (params instanceof AllHttpScopesHashModel) {
            ((AllHttpScopesHashModel) params).putUnlistedModel(KEY_JSP_TAGLIBS, (TemplateModel) servletContext.getAttribute(ATTR_JSP_TAGLIBS_MODEL));
        }
        
        return params;
    }

    protected boolean hasInitParameter(String paramName) {
        if (getServletConfig().getInitParameter(paramName) != null) {
            return true;
        }
        if (getServletConfig().getServletContext().getInitParameter(paramName) != null) {
            return true;
        }
        return false;
    }

    /**
     * Overrides {@link FreemarkerServlet#createTemplateLoader(String)} in order to use
     * {@link MultiTemplateLoader} instead which cascades {@link HstClassTemplateLoader} and {@link RepositoryTemplateLoader}
     * until it finds a template by the <code>templatePath</code>.
     */
    @Override
    protected TemplateLoader createTemplateLoader(String templatePath) throws IOException {
        TemplateLoader defaultTemplateLoader = 
                new DelegatingTemplateLoader(super.createTemplateLoader(templatePath), null, new String [] { "classpath:", "jcr:" });
        TemplateLoader classTemplateLoader =  new HstClassTemplateLoader(getClass());
        // repository template loader
        repositoryTemplateLoader = new RepositoryTemplateLoader();
        TemplateLoader[] loaders = new TemplateLoader[] { defaultTemplateLoader, classTemplateLoader, repositoryTemplateLoader };
        TemplateLoader multiLoader = new MultiTemplateLoader(loaders);
        return multiLoader;
    }
}
