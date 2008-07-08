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
package org.hippoecm.hst.components;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.Context;
import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.hst.jcr.JCRConnector;
import org.hippoecm.hst.util.HSTNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Label component that retrieves internationalized labels from the repository,
 * based on a locale and a node or a key.
 */
public class Label {
    
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final Logger LOGGER = LoggerFactory.getLogger(Label.class);
    private static final String DEFAULT_I18N_LOCATION = "/i18n";
    private static boolean locationErrorLogged = false; 
    
    private static final String NULL_VALUE = "NULL_VALUE";

    private String value;

    
    /**
     * Constructor. 
     */
    public Label(final HttpSession session, final Context context, final Node node) {
        
        super();
        
        try {
            // remove base location from node's path
            String path = node.getPath();
            if (path.startsWith(context.getBaseLocation())) {
                path = path.substring(context.getBaseLocation().length());
            }
            
            // translate remaining path to a valid key as slashes are disallowed  
            // in a property name: replace slashes by dots and prefix by 'node'.
            String key = "node" + path.replace("/", ".");

            // from repository
            this.value = retrieveValue(session, context.getLocale(), key);
            
            // else capitalized node name as label
            if (this.value == null) {
                String label = node.getName().substring(0, 1).toUpperCase();
                if (node.getName().length() > 1) {
                    label += node.getName().substring(1);
                }
                this.value = label;
            }
        } catch (RepositoryException re) {
            throw new InstantiationError("RepositoryException while creating a Label from node: message=" + re.getMessage());
        }    
    }
    
    /**
     * Constructor with a key.
     */
    public Label(HttpSession session, final Locale locale, final String key) {
        super();

        this.value = retrieveValue(session, locale, key); 
    }

    public String getValue() {
        return value;
    }

    /** Get a value by locale and key from repository. */
    private String retrieveValue(final HttpSession session, final Locale locale, final String key) {

        // try from session cache
        Map<String, String> cache = getValueCache(session, locale);
        String value = (String) cache.get(key);
        
        // read from repository if not in cache
        if (value == null) {
        
            Session jcrSession = JCRConnector.getJCRSession(session);
    
            if (jcrSession == null) {
                throw new IllegalStateException("No JCR session to repository");
            }
    
            String i18nLocation = getI18nLocation(session);
            
            try {
                // remove starting / 
                while (i18nLocation.startsWith("/")) {
                    i18nLocation = i18nLocation.substring(1);
                }

                if (!jcrSession.getRootNode().hasNode(i18nLocation)) {

                    // log error only once
                    if (!locationErrorLogged) {
                        locationErrorLogged = true;
                        LOGGER.error("Cannot find node by i18n location " + getI18nLocation(session));
                    }
                }
                else {
                    
                    // iterate the handle, match language and remember fallback
                    Node i18n = jcrSession.getRootNode().getNode(i18nLocation);
                    
                    NodeIterator i18nIterator = i18n.getNodes();
                    Node matchI18nNode = null;
                    Node fallBackI18nNode = null;
                    while (i18nIterator.hasNext() && (matchI18nNode == null)) {
                        
                        Node subNode = i18nIterator.nextNode();
                        
                        // should be of correct type
                        if (!subNode.isNodeType(HSTNodeTypes.HST_I18N_DOCUMENT)) {
                            LOGGER.warn("i18n node " + i18n.getPath() 
                                    + " is not of type " + HSTNodeTypes.HST_I18N_DOCUMENT);
                        }    
                        
                        // match the node if the language matches or if no  
                        // language is set (this is the fallback!)
                        if (subNode.hasProperty(HSTNodeTypes.HIPPOSTD_LANGUAGE)) {
                            if (subNode.getProperty(HSTNodeTypes.HIPPOSTD_LANGUAGE).getString().equals(locale.getLanguage())) {
                                matchI18nNode = subNode;
                            }
                        }
                        else {
                            fallBackI18nNode = subNode;
                        }
                    }

                    // get property value from matching node
                    if ((matchI18nNode != null) && matchI18nNode.hasProperty(key)) {
                        value = matchI18nNode.getProperty(key).getString();
                    }   
                    // or from the fallback
                    else if ((fallBackI18nNode != null) && fallBackI18nNode.hasProperty(key)) {
                        value = fallBackI18nNode.getProperty(key).getString();
                    }    
                }

                // optimalization
                if (value == null) {
                    value = NULL_VALUE;
                }    

                // store in cache
                cache.put(key, value);

            }
            catch (RepositoryException re) {
                throw new IllegalStateException(re);
            }
        }
        
        // optimalization to not try to retrieve unconfigured keys over and over
        if (NULL_VALUE.equals(value)) {
            return null;
        }
        
        return value;
    }

    private Map<String, String> getValueCache(HttpSession session, Locale locale) {
        
        String attributeName = this.getClass().getName() + ".keyValueMap." + locale.toString();
       
        Map<String, String> cache = (Map<String, String>) session.getAttribute(attributeName);

        if (cache == null) {
            cache = new HashMap<String, String>();
            session.setAttribute(attributeName, cache);
        }
        
        return cache;
    }

    private String getI18nLocation(HttpSession session) {

        String attributeName = this.getClass().getName() + ".i18nLocation";
        String i18nLocation = (String) session.getAttribute(attributeName);
        
        // lazy in session
        if (i18nLocation == null) {

            // by configuration
            i18nLocation = HSTConfiguration.get(session.getServletContext(),
                HSTConfiguration.KEY_REPOSITORY_I18N_LOCATION, false/*not required*/);

            // by default
            if (i18nLocation == null) {
                i18nLocation = DEFAULT_I18N_LOCATION;
            }

            session.setAttribute(attributeName, i18nLocation);
        }

        return i18nLocation;
    }
}
