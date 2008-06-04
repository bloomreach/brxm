/*
 * Copyright 2008 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.util.HSTNodeTypes;
import org.hippoecm.hst.jcr.JCRConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Menu component that scans for nodes in a certain repository location and 
 * builds a menu item structure. Per location, a menu object is kept in session.   
 *
 */
public class Menu {

    static final Logger logger = LoggerFactory.getLogger(Menu.class);

    private final List<MenuItem> menuItems = new ArrayList<MenuItem>();
    
    /* Get menu object lazily from session.
     *
     * @param session the HTTP session
     * @param location absolute path in the repository from where to generate a site map
     * @param excludedDocumentNames optional names of documents that are not
     *      included in the site map
     */
    public static Menu getMenu(final HttpSession session, final String location, final String[] excludedDocumentNames) {
        
        Menu menu = (Menu) session.getAttribute(Menu.class.getName() + "." + location);

        if (menu == null) {
            menu = new Menu(session, location, excludedDocumentNames);
            session.setAttribute(Menu.class.getName() + "." + location, menu);
        }
        
        return menu;
    }
    
    /** 
     * Constructor.
     */
    public Menu(HttpSession session, final String location, final String[] excludedDocumentNames) {
        super();
        
        createMenuItems(session, location, excludedDocumentNames);
    }

    public List<MenuItem> getItems() {
        return menuItems;
    }

    public void setActive(String activePath) {
 
        // loop menu items
        Iterator<MenuItem> items = menuItems.iterator();
        while (items.hasNext()) {
            items.next().setActive(activePath);
        }
    }

    private void createMenuItems(final HttpSession session, final String location,
            final String[] excludedDocumentNames) {
        
        Session jcrSession = JCRConnector.getJCRSession(session);
        
        if (jcrSession == null) {
            throw new IllegalStateException("No JCR session to repository");
        }

        String path = location;
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        try {
            if (!jcrSession.getRootNode().hasNode(path)) {
                logger.error("Cannot find node by location " + location);
                return;
            }

            Node rootNode = jcrSession.getRootNode().getNode(path);
            
            // loop the nodes and create items from them if applicable
            NodeIterator nodes = rootNode.getNodes();
            while (nodes.hasNext()) {
                
                Node node = (Node) nodes.next();
          
                // on level 0, only add nodes with the flag up
                if (node.isNodeType(HSTNodeTypes.HST_MENU_ITEM)) {
                    menuItems.add(new MenuItem(node, 0/*level*/, excludedDocumentNames));
                }
            }
        } 
        catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }   

    }
}
