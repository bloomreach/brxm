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
package org.hippoecm.hst;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.SimpleTextPage;
import org.hippoecm.hst.content.beans.SimpleTextPageCopy;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDirectory;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoFacetSelect;
import org.hippoecm.hst.content.beans.standard.HippoFixedDirectory;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoRequest;
import org.hippoecm.hst.content.beans.standard.HippoResource;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetSearch;
import org.hippoecm.hst.test.AbstractHstTestCase;
 
/**
 * <p>
 * AbstractBeanSpringTestCase
 * </p> 
 * 
 */ 
public abstract class AbstractBeanTestCase extends AbstractHstTestCase{

    protected ObjectConverter getObjectConverter() {
        
        // builds ordered mapping from jcrPrimaryNodeType to class or interface(s).
        Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs = new HashMap<String, Class<? extends HippoBean>>();

        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDocument.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFolder.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSearch.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSelect.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDirectory.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFixedDirectory.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoHtml.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoResource.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoRequest.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, SimpleTextPage.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, SimpleTextPageCopy.class, true);
        
        // builds a fallback jcrPrimaryNodeType array.
        String [] fallBackJcrPrimaryNodeTypes = new String[] { "hippo:facetselect","hippostd:directory","hippostd:folder" , "hippo:resource", "hippo:request", "hippostd:html", "hippo:document" };
        
        ObjectConverter objectConverter = new ObjectConverterImpl(jcrPrimaryNodeTypeClassPairs, fallBackJcrPrimaryNodeTypes);
        return objectConverter;
    }
    
    private static void addJcrPrimaryNodeTypeClassPair(Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs, Class clazz, boolean builtinType) {
        String jcrPrimaryNodeType = null;
        if (clazz.isAnnotationPresent(Node.class)) {
            Node anno = (Node) clazz.getAnnotation(Node.class);
            jcrPrimaryNodeType = anno.jcrType();
        }
        
        if(jcrPrimaryNodeTypeClassPairs.get(jcrPrimaryNodeType) != null) {
            return;
        }
        
        if (jcrPrimaryNodeType == null) {
            throw new IllegalArgumentException("There's no annotation for jcrType in the class: " + clazz);
        }
        
        jcrPrimaryNodeTypeClassPairs.put(jcrPrimaryNodeType, clazz);
    }
    
}
