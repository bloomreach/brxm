/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.hst.behavioral.providers;


import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.hst.util.PathUtils;
import org.onehippo.hst.behavioral.BehavioralNodeTypes;
import org.onehippo.hst.behavioral.util.AnnotatedContentBeanClassesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BehavioralTagDataProvider extends AbstractDataProvider {
    
    private static final Logger log = LoggerFactory.getLogger(BehavioralTagDataProvider.class);
            
    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";

    protected static final String DEFAULT_TAGS_JCR_PROPERYNAME = "hippostd:tags";
    
    private List<Class<? extends HippoBean>> annotatedClasses;
    private ObjectConverter objectConverter;
    private String tagsJcrPropertyName = DEFAULT_TAGS_JCR_PROPERYNAME;
    
    public BehavioralTagDataProvider(String id, String name, Node node) throws RepositoryException {
        super(id, name, node);
        if(node.hasProperty(BehavioralNodeTypes.BEHAVIORAL_PROVIDER_TERMS_PROPERTY)) {
            Property prop = node.getProperty(BehavioralNodeTypes.BEHAVIORAL_PROVIDER_TERMS_PROPERTY);
            if(prop.getType() == PropertyType.STRING) {
                tagsJcrPropertyName = prop.getValue().getString();
            } else {
                log.error("You have configured the your behavioral tag data provider incorrectly," +
                		" the property " + BehavioralNodeTypes.BEHAVIORAL_PROVIDER_TERMS_PROPERTY +
                		" must be a of type String");
            }
        }
    }

    @Override
    protected List<String> extractTerms(HttpServletRequest request) {
        HstRequestContext requestContext = (HstRequestContext)request.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        
        HippoBean bean = getBeanForResolvedSiteMapItem(requestContext);
        if(bean == null) {
            return null;
        }
        
        try {
            if(bean.getNode() != null && bean.getNode().hasProperty(tagsJcrPropertyName)) {
                Property prop = bean.getNode().getProperty(tagsJcrPropertyName);
                if(prop.getType() == PropertyType.STRING) {
                    List<String> terms = new ArrayList<String>();
                    if(prop.isMultiple()) {
                        for(Value val :prop.getValues() ) {
                            terms.add(val.getString());
                        }
                    } else {
                        terms.add(prop.getValue().getString());
                    }
                    return terms;
                } else {
                    log.warn("The property " + tagsJcrPropertyName + " of " + bean.getPath() + " is not of the required type String");
                }
            }
        } catch (RepositoryException e) {
            log.error("Could not extract terms from bean " + bean.getPath(), e);
        }
        
        
        return null;
        
    }
    
    
    protected HippoBean getBeanForResolvedSiteMapItem(HstRequestContext requestContext) {
        ObjectConverter converter = getObjectConverter(requestContext);
        
        String base = PathUtils.normalizePath(requestContext.getResolvedMount().getMount().getContentPath());
        String relPath = PathUtils.normalizePath(requestContext.getResolvedSiteMapItem().getRelativeContentPath());
        if(relPath == null) {
            return null;
        }
        try {
            if("".equals(relPath)) {
                return (HippoBean) converter.getObject(requestContext.getSession(), "/"+base);
            } else {
                return (HippoBean) converter.getObject(requestContext.getSession(), "/"+base + "/" + relPath);
            }
        } catch (ObjectBeanManagerException e) {
            log.error("ObjectBeanManagerException. Return null : {}", e);
        } catch (RepositoryException e) {
            log.error("Could not get bean for path " + relPath, e);
        }
        return null;
        
    }
    
    protected ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        if (objectConverter == null) {
            List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses(requestContext);
            objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }
        return objectConverter;
    }
    
    protected List<Class<? extends HippoBean>> getAnnotatedClasses(HstRequestContext requestContext) {
       annotatedClasses = AnnotatedContentBeanClassesScanner.scanAnnotatedContentBeanClasses(requestContext, requestContext.getServletContext().getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM));
       return annotatedClasses;
    }
}
