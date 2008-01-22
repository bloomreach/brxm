/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.generic.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.model.Model;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.datatable.ICustomizableDocumentListingDataTable;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractListingPlugin extends Plugin {

    protected static final String USERSETTINGS_NODETYPE = "hippo:usersettings";

    protected static final String PROPERTYNAME_PROPERTY = "propertyname";

    protected static final String COLUMNNAME_PROPERTY = "columnname";

    private static final String PAGESIZE_PROPERTY = "pagesize";
    private static final String VIEWSIZE_PROPERTY = "viewsize";

    static final Logger log = LoggerFactory.getLogger(AbstractListingPlugin.class);

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_VIEW_SIZE = 5;

    public int pageSize = DEFAULT_PAGE_SIZE;
    public int viewSize = DEFAULT_VIEW_SIZE;

    public static final String USER_PATH_PREFIX = "/hippo:configuration/hippo:users/";

    protected ICustomizableDocumentListingDataTable dataTable;
    protected List<IStyledColumn> columns;

    public AbstractListingPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);
        this.createTableColumns(pluginDescriptor, (JcrNodeModel) getPluginModel());
    }


    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel nodeModel = new JcrNodeModel(notification.getModel());
            if (!nodeModel.equals(getModel())) {
                setModel(nodeModel);
                remove((Component)dataTable);
                addTable(nodeModel, pageSize, viewSize);
                notification.getContext().addRefresh(this);
            }
        }
        // don't propagate the notification to children
    }

    public void createTableColumns(PluginDescriptor pluginDescriptor, JcrNodeModel model) {
        UserSession session = (UserSession) Session.get();
        columns = new ArrayList<IStyledColumn>();
        String userPrefNodeLocation = USER_PATH_PREFIX + session.getJcrSession().getUserID() + "/" + getPluginUserPrefNodeName();
        try {
            Node userPrefNode = (Node) session.getJcrSession().getItem(userPrefNodeLocation);

            pageSize = getPropertyIntValue(userPrefNode, PAGESIZE_PROPERTY, DEFAULT_PAGE_SIZE);
            viewSize = getPropertyIntValue(userPrefNode, VIEWSIZE_PROPERTY, DEFAULT_VIEW_SIZE);

            NodeIterator nodeIt = userPrefNode.getNodes();
            if(nodeIt.getSize() == 0) {
                defaultColumns(pluginDescriptor);
            }
            while(nodeIt.hasNext()) {
                Node n = nodeIt.nextNode();
                if(n.hasProperty(COLUMNNAME_PROPERTY) && n.hasProperty(PROPERTYNAME_PROPERTY)) {
                    String columnName = n.getProperty(COLUMNNAME_PROPERTY).getString();
                    String propertyName = n.getProperty(PROPERTYNAME_PROPERTY).getString();
                    columns.add(getNodeColumn(new Model(columnName), propertyName , pluginDescriptor.getIncoming()));
                }
            }
        } catch (PathNotFoundException e) {
            // The user preference node for the current plugin does not exist: create node now with default settings:
            log.debug("No user doclisting preference node found. Creating default doclisting preference node.");
            javax.jcr.Session jcrSession = session.getJcrSession();
            try {
                if(!jcrSession.itemExists(userPrefNodeLocation)) {
                    // User doesn't have a user folder yet
                    Node userNode = (Node) jcrSession.getItem(USER_PATH_PREFIX + session.getJcrSession().getUserID());
                    Node prefNode = createDefaultPrefNodeSetting(userNode, pluginDescriptor.getIncoming());
                    modifyDefaultPrefNode(prefNode, pluginDescriptor.getIncoming());
                    userNode.save();
                }
            } catch (PathNotFoundException e1) {
                logError(e1.getMessage());
                defaultColumns(pluginDescriptor);
            } catch (ItemExistsException e1) {
                logError(e1.getMessage());
                defaultColumns(pluginDescriptor);
            } catch (VersionException e1) {
                logError(e1.getMessage());
                defaultColumns(pluginDescriptor);
            } catch (ConstraintViolationException e1) {
                logError(e1.getMessage());
                defaultColumns(pluginDescriptor);
            } catch (LockException e1) {
                logError(e1.getMessage());
                defaultColumns(pluginDescriptor);
            } catch (ValueFormatException e1) {
                logError(e1.getMessage());
                defaultColumns(pluginDescriptor);
            } catch (RepositoryException e1) {
                logError(e1.getMessage());
                defaultColumns(pluginDescriptor);
            }

        } catch (RepositoryException e) {
            logError(e.getMessage());
            defaultColumns(pluginDescriptor);
        }
        addTable(model, pageSize, viewSize);
    }

  
    private int getPropertyIntValue(Node userPrefNode,String property, int defaultValue) throws RepositoryException, ValueFormatException, PathNotFoundException {
        int value = 0;
        if(userPrefNode.hasProperty(property) && userPrefNode.getProperty(property).getValue().getType() == PropertyType.LONG ){
            value = (int)userPrefNode.getProperty(property).getLong();
            return value == 0 ? defaultValue : value;
        }
        // TODO : make sure it cannot be a string value. Currently, saving through the console makes the number
        // a string. Also fix this in the repository.cnd
        else if(userPrefNode.hasProperty(property) && userPrefNode.getProperty(property).getValue().getType() == PropertyType.STRING ){
            String pageSizeString = userPrefNode.getProperty(property).getString();
            try {
                value = Integer.parseInt(pageSizeString);
                return value == 0 ? defaultValue : value;
            } catch (NumberFormatException e) {
                // do nothing. Keep pageSize default
            }
        }
        return defaultValue;
    }

    private Node createDefaultPrefNodeSetting(Node userNode, Channel channel) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException, ValueFormatException {
        Node prefNode = userNode.addNode(getPluginUserPrefNodeName(), USERSETTINGS_NODETYPE);
        prefNode.setProperty(PAGESIZE_PROPERTY, DEFAULT_PAGE_SIZE);
        prefNode.setProperty(VIEWSIZE_PROPERTY, DEFAULT_VIEW_SIZE);
        return prefNode;
    }
    
    /**
     * Override this method in your subclass to change the default listing view
     * @param prefNode
     * @param incoming
     * @throws ItemExistsException
     * @throws PathNotFoundException
     * @throws NoSuchNodeTypeException
     * @throws LockException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @throws ValueFormatException
     */
    protected void modifyDefaultPrefNode(Node prefNode, Channel incoming) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException, ValueFormatException {
        // subclasses should override this if they want to change behavior
        
        Node pref = prefNode.addNode("name",USERSETTINGS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, "Name");
        pref.setProperty(PROPERTYNAME_PROPERTY, "name");

        pref = prefNode.addNode("type",USERSETTINGS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, "Type");
        pref.setProperty(PROPERTYNAME_PROPERTY, "jcr:primaryType");
        columns.add(getNodeColumn(new Model("Name"), "name" , incoming));
        columns.add(getNodeColumn(new Model("Type"), "jcr:primaryType" , incoming));
        
    }



    private void defaultColumns(PluginDescriptor pluginDescriptor) {
        columns.add(getNodeColumn(new Model("Name"), "name" , pluginDescriptor.getIncoming()));
        columns.add(getNodeColumn(new Model("Type"), "jcr:primaryType" , pluginDescriptor.getIncoming()));
    }
    private void logError(String message) {
        log.error("error creating user doclisting preference " + message );
        log.error("default doclisting will be shown");
    }


    /**
     * Override this method if you want custom table column / nodecells
     * @param model Model
     * @param propertyName String
     * @param incoming Channel
     * @return IStyledColumn
     */
    protected IStyledColumn getNodeColumn(Model model, String propertyName, Channel incoming) {
        return new NodeColumn(model, propertyName, incoming);
    }


    protected abstract void addTable(JcrNodeModel nodeModel, int pageSize, int viewSize);

    protected abstract String getPluginUserPrefNodeName();
}
