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
package org.hippoecm.hst.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterImpl;
import org.hippoecm.hst.content.beans.standard.HippoAsset;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDirectory;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoFacetSelect;
import org.hippoecm.hst.content.beans.standard.HippoFixedDirectory;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImage;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoMirror;
import org.hippoecm.hst.content.beans.standard.HippoResource;
import org.hippoecm.hst.content.beans.standard.HippoStdPubWfRequest;
import org.hippoecm.hst.content.beans.standard.HippoTranslation;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetNavigation;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetResult;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetSearch;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetSubNavigation;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetsAvailableNavigation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * ObjectConverterUtils
 * @version $Id$
 */
public class ObjectConverterUtils {
    
    private static Logger log = LoggerFactory.getLogger(ObjectConverterUtils.class);
    
    private static final Class<?> [] DEFAULT_BUILT_IN_MAPPING_CLASSES = {
        HippoDocument.class,
        HippoFolder.class,
        HippoMirror.class,
        HippoFacetSelect.class,
        HippoDirectory.class,
        HippoFixedDirectory.class,
        HippoHtml.class,
        HippoResource.class,
        HippoStdPubWfRequest.class,
        HippoAsset.class,
        HippoGalleryImageSet.class,
        HippoGalleryImage.class,
        HippoTranslation.class,
        // facet navigation parts:
        HippoFacetSearch.class,
        HippoFacetNavigation.class,
        HippoFacetsAvailableNavigation.class,
        HippoFacetSubNavigation.class,
        HippoFacetResult.class
    };
    
    private static final String [] DEFAULT_FALLBACK_NODE_TYPES = { 
        "hippo:facetselect",
        "hippo:mirror",
        "hippostd:directory",
        "hippostd:folder",
        "hippogallery:image",
        "hippo:resource",
        "hippo:request",
        "hippostd:html",
        "hippo:document"
    };
    
    private ObjectConverterUtils() {
    }
    
    /**
     * Creates <CODE>ObjectConverter</CODE>, with ignoreDuplicates = false, which means that when there are two annotated beans with the same 
     * value for {@link Node#jcrType()}, an IllegalArgumentException is thrown.
     * @param annotatedClasses Annotated class mapping against jcr primary node types.
     * @return the ObjectConverter for these<code>annotatedClasses</code>
     * @throws IllegalArgumentException when two annotatedClasses have the same {@link Node#jcrType()} 
     */
    public static ObjectConverter createObjectConverter(final Collection<Class<? extends HippoBean>> annotatedClasses) throws IllegalArgumentException {
        return createObjectConverter(annotatedClasses, false);
    }
    
    /**
     * Creates <CODE>ObjectConverter</CODE>. 
     * @param annotatedClasses Annotated class mapping against jcr primary node types.
     * @param ignoreDuplicates Flag whether duplicate mapping for a node type is ignored or not. If it is false, it throws <CODE>IllegalArgumentException</CODE> on duplicate mappings.
     * @return the ObjectConverter for these<code>annotatedClasses</code>
     * @throws IllegalArgumentException when two annotatedClasses have the same {@link Node#jcrType()} and <code>ignoreDuplicates</code> is false
     */
    @SuppressWarnings("unchecked")
    public static ObjectConverter createObjectConverter(final Collection<Class<? extends HippoBean>> annotatedClasses, boolean ignoreDuplicates) throws IllegalArgumentException {
        return createObjectConverter(annotatedClasses, (Class<? extends HippoBean> []) DEFAULT_BUILT_IN_MAPPING_CLASSES, DEFAULT_FALLBACK_NODE_TYPES, ignoreDuplicates);
    }
    
    /**
     * Creates <CODE>ObjectConverter</CODE>.
     * @param annotatedClasses Annotated class mapping against jcr primary node types.
     * @param builtInMappingClasses Built-in class mappings against the default built-in jcr primary node types.
     * @param fallbackNodeTypes If no bean found for the node type, a fallback node type is to be selected as ordered by using <CODE>node.isNodeType(fallbackNodeType)</CODE>
     * @param ignoreDuplicates Flag whether duplicate mapping for a node type is ignored or not. If it is false, it throws <CODE>IllegalArgumentException</CODE> on duplicate mappings.
     * @return
     */
    public static ObjectConverter createObjectConverter(final Collection<Class<? extends HippoBean>> annotatedClasses, final Class<? extends HippoBean> [] builtInMappingClasses, final String [] fallbackNodeTypes, boolean ignoreDuplicates) throws IllegalArgumentException {
        Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs = new HashMap<String, Class<? extends HippoBean>>();
        
        if (annotatedClasses != null && !annotatedClasses.isEmpty()) {
            for (Class<? extends HippoBean> c : annotatedClasses) {
                addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, c, false, ignoreDuplicates) ;
            }
        }
        
        if (builtInMappingClasses != null) {
            for (Class<? extends HippoBean> clazz : builtInMappingClasses) {
                addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, clazz, true, ignoreDuplicates);
            }
        }
        
        return new ObjectConverterImpl(jcrPrimaryNodeTypeClassPairs, fallbackNodeTypes);
    }
    
    /**
     * Returns the default built-in fallback jcr primary node types
     * @return
     */
    public static String [] getDefaultFallbackNodeTypes() {
        String [] fallbackTypes = new String[DEFAULT_FALLBACK_NODE_TYPES.length];
        System.arraycopy(DEFAULT_FALLBACK_NODE_TYPES, 0, fallbackTypes, 0, DEFAULT_FALLBACK_NODE_TYPES.length);
        return fallbackTypes;
    }
    
    /**
     * Collects bean classes annotated by {@link org.hippoecm.hst.content.beans.Node} from a XML Resource URL.
     * <P>
     * Each annotated class name must be written inside <CODE>&lt;annotated-class/&gt;</CODE> element 
     * as child of the root element, &lt;hst-content-beans/&gt;,
     * like the following example:
     * <PRE><XMP>
     * <hst-content-beans>
     *   <annotated-class>org.hippoecm.hst.demo.beans.TextBean</annotated-class>
     *   <annotated-class>org.hippoecm.hst.demo.beans.NewsBean</annotated-class>
     *   <annotated-class>org.hippoecm.hst.demo.beans.ProductBean</annotated-class>
     *   <annotated-class>org.hippoecm.hst.demo.beans.CommentBean</annotated-class>
     *   <annotated-class>org.hippoecm.hst.demo.beans.CommentLinkBean</annotated-class>
     *   <annotated-class>org.hippoecm.hst.demo.beans.ImageLinkBean</annotated-class>
     * </hst-content-beans>
     * </XMP></PRE>
     * </P>
     * @param url
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    @SuppressWarnings("unchecked")
    public static List<Class<? extends HippoBean>> getAnnotatedClasses(final URL url) throws IOException, SAXException, ParserConfigurationException {
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
        
        InputStream is = null;
        BufferedInputStream bis = null;
        
        try {
            is = url.openStream();
            bis = new BufferedInputStream(is);
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bis);
            Element root = document.getDocumentElement();
            NodeList nodeList = root.getElementsByTagName("annotated-class");
            
            int size = nodeList.getLength();
            Element elem = null;
            String className = null;
            Class<?> clazz = null;
            
            for (int i = 0; i < size; i++) {
                elem = (Element) nodeList.item(i);
                className = elem.getTextContent().trim();
                
                try {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    log.warn("Skipped class registration into the mapper. Cannot load class: {}.", className);
                }
                
                if (HippoBean.class.isAssignableFrom(clazz)) {
                    annotatedClasses.add((Class<? extends HippoBean>) clazz);
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("Skipped class registration into the mapper. Type is not HippoBean: {}.", className);
                    }
                }
            }
        } finally {
            if (bis != null) {
                try { bis.close(); } catch (Exception ignore) { }
            }
            if (is != null) {
                try { is.close(); } catch (Exception ignore) { }
            }
        }
        
        return annotatedClasses;
    }
    
    /**
     * Collects bean classes annotated by {@link org.hippoecm.hst.content.beans.Node}
     * from the location specified by <CODE>locationPattern</CODE>.
     * Class resources will be collected by the specified <CODE>resourceScanner</CODE>.
     *  
     * @param resourceScanner
     * @param locationPatterns
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @see {@link ClasspathResourceScanner}
     */
    @SuppressWarnings("unchecked")
    public static List<Class<? extends HippoBean>> getAnnotatedClasses(final ClasspathResourceScanner resourceScanner, String ... locationPatterns) throws IOException, SAXException, ParserConfigurationException {
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
        Set<String> annotatedClassNames = resourceScanner.scanClassNamesAnnotatedBy(Node.class, false, locationPatterns);
        
        if (annotatedClassNames != null && !annotatedClassNames.isEmpty()) {
            Class<?> clazz = null;
            
            for (String className : annotatedClassNames) {
                try {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    log.info("ObjectConverterUtils skipped annotated class registration. The class cannot be loaded: {}.", className);
                    continue;
                }

                int mod = clazz.getModifiers();

                if (!Modifier.isPublic(mod)) {
                    log.info("ObjectConverterUtils skipped annotated class registration. The class must be a *public* class: {}.", className);
                    continue;
                }

                if (HippoBean.class.isAssignableFrom(clazz)) {
                    annotatedClasses.add((Class<? extends HippoBean>) clazz);
                } else {
                    log.info("ObjectConverterUtils skipped annotated class registration. The class must be type of {}: {}.", HippoBean.class, className);
                }
            }
        }
        
        return annotatedClasses;
    }
    
    private static void addJcrPrimaryNodeTypeClassPair(Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs, Class<? extends HippoBean> clazz, boolean builtinType, boolean ignoreDuplicates) throws IllegalArgumentException {
        String jcrPrimaryNodeType = null;
        
        if (clazz.isAnnotationPresent(Node.class)) {
            Node anno = (Node) clazz.getAnnotation(Node.class);
            jcrPrimaryNodeType = anno.jcrType();
        }
        
        if (jcrPrimaryNodeType == null) {
            throw new IllegalArgumentException("There's no annotation for jcrType in the class: " + clazz);
        }
        
        if (jcrPrimaryNodeTypeClassPairs.containsKey(jcrPrimaryNodeType)) {
            if (builtinType) {
                log.debug("Builtin annotated class '{}' for primary type '{}' is overridden with already registered class '{}'. Builtin version is ignored.", 
                		new Object[]{clazz.getName(), jcrPrimaryNodeType, jcrPrimaryNodeTypeClassPairs.get(jcrPrimaryNodeType).getName()});
            } else if (ignoreDuplicates) {
                log.debug("Duplicate annotated class '{}' found for primary type '{}'. The already registered class '{}' is preserved.", 
                		new Object[]{clazz.getName(), jcrPrimaryNodeType, jcrPrimaryNodeTypeClassPairs.get(jcrPrimaryNodeType).getName()});
            } else {
                throw new IllegalArgumentException("Annotated class '" + clazz.getName() + "' for primarytype '" + jcrPrimaryNodeType +
                		"' is a duplicate of already registered class '" + jcrPrimaryNodeTypeClassPairs.get(jcrPrimaryNodeType).getName() + "'. " +
                        "You might have configured a bean that does not have a annotation for the jcrType and " +
                        "inherits the jcrType from the bean it extends, resulting in 2 beans with the same jcrType. Correct your beans.");
            }
            
            return;
        }
        
        jcrPrimaryNodeTypeClassPairs.put(jcrPrimaryNodeType, clazz);
    }
    
}
