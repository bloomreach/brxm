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
package org.hippoecm.hst.taglib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.jcr.JCRConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Menu component that scans for nodes in a certain repository location and 
 * builds a menu item structure. Per location, a menu object is kept in session.   
 *
 */
public class Menu {

    static final String PROPERTY_IS_MENU_ITEM = "hst:isMenuItem";
    static final String PROPERTY_MENU_LABEL = "hst:menuLabel";

    static final Logger logger = LoggerFactory.getLogger(Menu.class);

    private final List<MenuItem> menuItems = new ArrayList<MenuItem>();
    private final String id;
    
    /* Get menu object lazily from session. */
    public static Menu getMenu(final HttpSession session, final String id, final String location) {
        
        Menu menu = (Menu) session.getAttribute(Menu.class.getName() + "." + location);

        if (menu == null) {
            menu = new Menu(session, id, location);
            session.setAttribute(Menu.class.getName() + "." + location, menu);
        }
        
        return menu;
    }
    
    /** 
     * Constructor.
     */
    public Menu(HttpSession session, final String id, final String location) {
        super();
        
        this.id = id;
        
        createMenuItems(session, location);
    }

    public String getId() {
        return id;
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

    private void createMenuItems(final HttpSession session, final String location) {
        
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
          
                // on level 0, absence of the property means not to create one
                // so only documents and folder with the flag up
                if (node.hasProperty(PROPERTY_IS_MENU_ITEM)) {
                    if (node.getProperty(PROPERTY_IS_MENU_ITEM).getBoolean()) {
                        menuItems.add(new MenuItem(node, 0/*level*/));
                    }
                }    
            }
        } 
        catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }   

    }
}
