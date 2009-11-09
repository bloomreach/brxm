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
package org.hippoecm.hst.services.support.jaxrs.content;

import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterImpl;
import org.hippoecm.hst.content.beans.manager.PersistableObjectBeanManagerWorkflowImpl;
import org.hippoecm.hst.content.beans.standard.HippoAsset;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDirectory;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetSelect;
import org.hippoecm.hst.content.beans.standard.HippoFixedDirectory;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoImage;
import org.hippoecm.hst.content.beans.standard.HippoMirror;
import org.hippoecm.hst.content.beans.standard.HippoRequest;
import org.hippoecm.hst.content.beans.standard.HippoResource;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetSearch;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstComponentFatalException;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseHstContentService {
    
    public static final String SITE_CONTENT_PATH = "org.hippoecm.hst.services.support.site.content.path"; 

    private static Logger log = LoggerFactory.getLogger(BaseHstContentService.class);

    private ObjectConverter objectConverter;
    
    public BaseHstContentService() {
        init();
    }
    
    protected HstRequestContext getHstRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }
    
    protected ContentPersistenceManager getContentPersistenceManager(HttpServletRequest servletRequest) throws LoginException, RepositoryException {
        return new PersistableObjectBeanManagerWorkflowImpl(getHstRequestContext(servletRequest).getSession(), getObjectConverter());
    }
    
    protected ObjectConverter getObjectConverter() {
        return objectConverter;
    }
    
    protected HippoBeanContent createHippoBeanContent(HippoBean bean) throws RepositoryException {
        HippoBeanContent beanContent = null;
        
        if (bean instanceof HippoFolderBean) {
            beanContent = new HippoFolderBeanContent((HippoFolderBean) bean);
        } else if (bean instanceof HippoDocumentBean) {
            beanContent = new HippoDocumentBeanContent((HippoDocumentBean) bean);
        } else {
            beanContent = new HippoBeanContent(bean);
        }
        
        return beanContent;
    }
    
    protected String getContentItemPath(final HttpServletRequest servletRequest, final List<PathSegment> pathSegments) {
        StringBuilder pathBuilder = new StringBuilder(80).append(getSiteContentPath(servletRequest));
        
        for (PathSegment pathSegment : pathSegments) {
            pathBuilder.append('/').append(pathSegment.getPath());
        }
        
        String path = pathBuilder.toString();
        
        String encoding = servletRequest.getCharacterEncoding();
        
        if (encoding == null) {
            encoding = "ISO-8859-1";
        }
        
        try {
            path = URLDecoder.decode(path, encoding);
        } catch (Exception e) {
            log.warn("Failed to decode. {}", path);
        }
        
        return path;
    }
    
    protected String getSiteContentPath(HttpServletRequest servletRequest) {
        return (String) servletRequest.getAttribute(BaseHstContentService.SITE_CONTENT_PATH);
    }
    
    protected String getRelativeItemContentPath(HttpServletRequest servletRequest, final ItemContent itemContent) {
        String itemContentPath = itemContent.getPath();
        String siteContentPath = getSiteContentPath(servletRequest);
        
        if (itemContentPath != null && itemContentPath.startsWith(siteContentPath)) {
            itemContentPath = itemContentPath.substring(siteContentPath.length());
        }
        
        return itemContentPath;
    }
    
    protected String getRequestURIBase(final UriInfo uriInfo) {
        String base = uriInfo.getBaseUri().toString();
        
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        
        return base;
    }
    
    protected void init() {
        Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs = new HashMap<String, Class<? extends HippoBean>>();
        List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClassNames();
        
        for (Class<? extends HippoBean> c : annotatedClasses) {
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, c, false) ;
        }
        
        // below the default present mapped mappings
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDocument.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFolder.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSearch.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoMirror.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSelect.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDirectory.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFixedDirectory.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoHtml.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoResource.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoRequest.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoAsset.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoImage.class, true);
        
        // builds a fallback jcrPrimaryNodeType array.
        String[] fallBackJcrNodeTypes = getFallBackJcrNodeTypes();
        objectConverter = new ObjectConverterImpl(jcrPrimaryNodeTypeClassPairs, fallBackJcrNodeTypes);
    }
    
    protected List<Class<? extends HippoBean>> getAnnotatedClassNames() {
        return Collections.emptyList();
    }
    
    protected String[] getFallBackJcrNodeTypes(){
        return new String[] { "hippo:facetselect", "hippo:mirror", "hippostd:directory", "hippostd:folder", "hippo:resource", "hippo:request", "hippostd:html", "hippo:document" };
    }
    
    private static void addJcrPrimaryNodeTypeClassPair(Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs,
            Class clazz, boolean builtinType) throws HstComponentException {
        String jcrPrimaryNodeType = null;

        if (clazz.isAnnotationPresent(Node.class)) {
            Node anno = (Node) clazz.getAnnotation(Node.class);
            jcrPrimaryNodeType = anno.jcrType();
        }

        if (jcrPrimaryNodeTypeClassPairs.get(jcrPrimaryNodeType) != null) {
            if (!builtinType) {
                throw new HstComponentFatalException("Annotated class for primarytype '"+jcrPrimaryNodeType+"' is duplicate. " +
                        "You might have configured a bean in 'beans-annotated-classes.xml' that does not have a annotation for the jcrType and" +
                        "inherits the jcrType from the bean it extends, resulting in 2 beans with the same jcrType. Correct your beans.");
            }
            return;
        }
        
        if (jcrPrimaryNodeType == null) {
            throw new IllegalArgumentException("There's no annotation for jcrType in the class: " + clazz);
        }

        jcrPrimaryNodeTypeClassPairs.put(jcrPrimaryNodeType,clazz);
    }
    
}
