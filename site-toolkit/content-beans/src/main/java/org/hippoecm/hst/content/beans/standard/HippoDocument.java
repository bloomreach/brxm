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
package org.hippoecm.hst.content.beans.standard;

import java.util.Locale;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippo:document")
public class HippoDocument extends HippoItem implements HippoDocumentBean {

    private static Logger log = LoggerFactory.getLogger(HippoDocument.class);

    private javax.jcr.Node canonicalHandleNode;

    private boolean availableTranslationsMappingClassInitialized;

    @SuppressWarnings("rawtypes")
    private HippoAvailableTranslationsBean availableTranslationsMappingClass;

    /**
     * @param relPath
     * @return <code>HippoHtml</code> or <code>null</code> if no node exists as relPath or no node of type "hippostd:html"
     */
    public HippoHtml getHippoHtml(String relPath) {
        return getBean(relPath, HippoHtml.class);
    }
    
    /**
     * @param <T>
     * @param relPath
     * @param beanMappingClass
     * @return the {@link HippoCompoundBean} at <code>relPath</code> if there is a compound of type <code>beanMappingClass</code> and <code>null</code> otherwise
     */
    @SuppressWarnings("unchecked")
    public <T extends HippoCompoundBean> T getHippoCompound(String relPath, Class<T> beanMappingClass) {
        HippoBean compound = getBean(relPath, HippoCompoundBean.class);
        if(compound != null && beanMappingClass.isAssignableFrom(compound.getClass())) {
            return (T)compound;
        } else {
            log.debug("Cannot return compound of type '"+beanMappingClass.getName()+"' for relPath '{}' at '{}' because the compound is of type '"+compound.getClass().getName()+"'", relPath, this.getPath());
        }
        return null;
    }

    @Override
    public String getCanonicalHandleUUID() {
        try {
            return (getCanonicalHandleNode() == null) ? null : getCanonicalHandleNode().getIdentifier();
        } catch (RepositoryException e) {
            log.error("Cannot get handle uuid for node '"+getPath()+"'. Return null", e);
            return null;
        }
    }
    

    @Override
    public String getCanonicalHandlePath() {
        try {
            return (getCanonicalHandleNode() == null) ? null : getCanonicalHandleNode().getPath();
        } catch (RepositoryException e) {
            log.error("Cannot get handle path for node '"+getPath()+"'. Return null", e);
            return null;
        }
    }
    
    private javax.jcr.Node getCanonicalHandleNode() {
        if(canonicalHandleNode != null) {
            return canonicalHandleNode;
        }
        
        if(this.getNode() == null) {
            log.info("Cannot get handle uuid for detached node '{}'", this.getPath());
            return null;
        }
        try {
            // first get the canonical handle. Because we can have a document in a faceted resultset, we first need to get the 
            // canonical node of the document, and then fetch the parent
            javax.jcr.Node canonical = ((HippoNode)getNode()).getCanonicalNode();
            if(canonical == null) {
                log.info("We cannot get the canonical handle uuid for a document that does not have a canonical version. Node '{}'. Return null", getNode().getPath());
                return null;
            }
            canonicalHandleNode = canonical.getParent();
        } catch (RepositoryException e) {
            log.error("Cannot get handle uuid for node '"+this.getPath()+"'", e);
        }
        return canonicalHandleNode;
    }
    
    public String getLocaleString() {
        return getProperty(HippoTranslationNodeType.LOCALE);
    }
    
    public Locale getLocale() {
        String localeString = getLocaleString();
        try {
            return LocaleUtils.toLocale(localeString);
        } catch (IllegalArgumentException e) {
            log.info("Invalid locale '{}' for document '{}' : {}", new Object[] { localeString, getPath(), e.toString() });
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends HippoBean> HippoAvailableTranslationsBean<T> getAvailableTranslations(Class<T> beanMappingClass) {
        if(!availableTranslationsMappingClassInitialized) {
            availableTranslationsMappingClassInitialized = true;
            availableTranslationsMappingClass = new AvailableTranslations(getNode(), getObjectConverter());
            ((AvailableTranslations)availableTranslationsMappingClass).setBeanMappingClass(beanMappingClass);
        }
        return (HippoAvailableTranslationsBean<T>)availableTranslationsMappingClass;
    }
    
}
