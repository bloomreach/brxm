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
package org.hippoecm.hst.component.support.ocm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.ServletConfig;

import org.apache.commons.digester.Digester;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.ocm.HippoStdFolder;
import org.hippoecm.hst.ocm.HippoStdDocument;
import org.hippoecm.hst.ocm.HippoStdHtml;
import org.hippoecm.hst.ocm.HippoStdNode;
import org.hippoecm.hst.ocm.manager.cache.NOOPObjectCache;
import org.hippoecm.hst.ocm.manager.impl.HstAnnotationMapperImpl;
import org.hippoecm.hst.ocm.manager.impl.HstObjectConverterImpl;
import org.hippoecm.hst.ocm.query.impl.HstQueryManagerImpl;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class BaseHstComponent extends GenericHstComponent {
    
    private static Logger log = LoggerFactory.getLogger(BaseHstComponent.class);

    public static final String OCM_ANNOTATED_CLASSES_CONF_PARAM = "ocm-annotated-classes";
    public static final String DEFAULT_OCM_ANNOTATED_CLASSES_CONF = "/WEB-INF/ocm-annotated-classes.xml";
    public static final String OCM_REQUEST_CONTEXT_ATTR_NAME = BaseHstComponent.class.getName() + ".ocm";
    
    private boolean ocmInitialized;
    private Mapper ocmMapper;
    private ObjectConverter ocmObjectConverter;
    private ObjectCache ocmRequestObjectCache;
    private Map ocmAtomicTypeConverters;

    public void init(ServletConfig servletConfig, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletConfig, componentConfig);

        if (!this.ocmInitialized) {
            initOCMObjects();
        }
    }
    
    protected String getParameter(String name, HstRequest request) {
        return (String)this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }
    
    protected HippoStdNode getContentNode(HstRequest request) {
        ResolvedSiteMapItem resolvedSitemapItem = request.getRequestContext().getResolvedSiteMapItem();

        String base = PathUtils.normalizePath(resolvedSitemapItem.getHstSiteMapItem().getHstSiteMap().getSite().getContentPath());
        String relPath = PathUtils.normalizePath(resolvedSitemapItem.getRelativeContentPath());
        return (HippoStdNode) getObjectContentManager(request).getObject("/"+base+ "/" + relPath);
    }
    
    protected ObjectContentManager getObjectContentManager(HstRequest request) {
        HstRequestContext requestContext = request.getRequestContext();
        ObjectContentManager ocm = (ObjectContentManager) requestContext.getAttribute(OCM_REQUEST_CONTEXT_ATTR_NAME);
        
        if (ocm == null) {
            try {
                QueryManager queryManager = new HstQueryManagerImpl(this.ocmMapper, this.ocmAtomicTypeConverters, requestContext.getSession().getValueFactory());
                ocm = new ObjectContentManagerImpl(this.ocmMapper, this.ocmObjectConverter, queryManager, this.ocmRequestObjectCache, requestContext.getSession());
                requestContext.setAttribute(OCM_REQUEST_CONTEXT_ATTR_NAME, ocm);
            } catch (UnsupportedRepositoryOperationException e) {
                throw new HstComponentException(e);
            } catch (RepositoryException e) {
                throw new HstComponentException(e);
            }
        }
        
        return ocm;
    }
    
    protected Mapper createMapper() {
        List<Class> annotatedClasses = new LinkedList<Class>();
        List<String> classNames = loadAnnotatedClassNames();
        
        for (String className : classNames) {
            Class clazz = null;
            
            try {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                annotatedClasses.add(clazz);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Skipped class registration into the mapper. Cannot load class: " + className);
                }
            }
        }
        
        return new HstAnnotationMapperImpl(annotatedClasses, "hst:document");
    }
    
    private synchronized void initOCMObjects() {
        if (!this.ocmInitialized) {
            this.ocmMapper = createMapper();
            DefaultAtomicTypeConverterProvider converterProvider = new DefaultAtomicTypeConverterProvider();
            this.ocmAtomicTypeConverters = converterProvider.getAtomicTypeConverters();
            this.ocmRequestObjectCache = new NOOPObjectCache();
            ProxyManager proxyManager = new ProxyManagerImpl();
            this.ocmObjectConverter = new HstObjectConverterImpl(this.ocmMapper, converterProvider, proxyManager, this.ocmRequestObjectCache);
            
            this.ocmInitialized = true;
        }
    }
    
    private List<String> loadAnnotatedClassNames() {
        List<String> classNames = new LinkedList<String>();

        String param = getServletConfig().getInitParameter(OCM_ANNOTATED_CLASSES_CONF_PARAM);
        String ocmAnnotatedClassesResourcePath = (param != null ? param : DEFAULT_OCM_ANNOTATED_CLASSES_CONF);

        InputStream in = null;
        
        try {
            in = new BufferedInputStream(getServletConfig().getServletContext().getResourceAsStream(ocmAnnotatedClassesResourcePath));
            
            Digester digester = new Digester();
            digester.setValidating(false);
            
            digester.push(classNames);
            digester.addCallMethod("jackrabbit-ocm/annotated-class", "add", 1);
            digester.addCallParam("jackrabbit-ocm/annotated-class", 0);
            
            digester.parse(in);
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("No ocm annotated classes configuration available: " + ocmAnnotatedClassesResourcePath);
            }
        } catch (SAXException e) {
            if (log.isWarnEnabled()) {
                log.warn("ocm annotated classes configuration is not valid: " + ocmAnnotatedClassesResourcePath);
            }
        } finally {
            if (in != null) try { in.close(); } catch (Exception ce) { }
        }
        
        if (!classNames.contains(HippoStdHtml.class.getName())) {
            classNames.add(HippoStdHtml.class.getName());
        }
        if (!classNames.contains(HippoStdDocument.class.getName())) {
            classNames.add(HippoStdDocument.class.getName());
        }
        if (!classNames.contains(HippoStdFolder.class.getName())) {
            classNames.add(HippoStdFolder.class.getName());
        }
        
        return classNames;
    }

}
