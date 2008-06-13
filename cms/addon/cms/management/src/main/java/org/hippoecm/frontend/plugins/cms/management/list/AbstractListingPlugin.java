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
package org.hippoecm.frontend.plugins.cms.management.list;

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

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.model.PluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.cms.management.list.datatable.CustomizableDocumentListingDataTable;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.plugins.standards.sa.* instead
 */
@Deprecated
public abstract class AbstractListingPlugin extends Plugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected static final String LISTING_NODETYPE = "hippo:listing";
    protected static final String LISTINGPROPS_NODETYPE = "hippo:listingpropnode";

    protected static final String PROPERTYNAME_PROPERTY = "hippo:propertyname";

    protected static final String COLUMNNAME_PROPERTY = "hippo:columnname";

    private static final String PAGESIZE_PROPERTY = "hippo:pagesize";
    private static final String VIEWSIZE_PROPERTY = "hippo:viewsize";

    static final Logger log = LoggerFactory.getLogger(AbstractListingPlugin.class);

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_VIEW_SIZE = 5;

    public int pageSize = DEFAULT_PAGE_SIZE;
    public int viewSize = DEFAULT_VIEW_SIZE;

    //FIXME: remove this
    public static final String USER_PATH_PREFIX = "/hippo:configuration/hippo:users/";

    protected List<IStyledColumn> columns;
    private CustomizableDocumentListingDataTable dataTable;

    public AbstractListingPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        createTableColumns(pluginDescriptor, new JcrNodeModel(model));
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getModel());
            Node node = (Node) model.getNode();
            boolean selectParentModel = false;
            while (node != null) {
                try {
                    if (!node.isNodeType(HippoNodeType.NT_DOCUMENT) && !node.isNodeType(HippoNodeType.NT_HANDLE)
                            && !node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)
                            && !node.isNodeType(HippoNodeType.NT_REQUEST)) {
                        break;
                    } else {
                        if (node.isNodeType(HippoNodeType.NT_DOCUMENT) || node.isNodeType(HippoNodeType.NT_REQUEST)) {
                            selectParentModel = true;
                        }
                        node = node.getParent();
                    }
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            }

            List<String> entries = new ArrayList<String>();
            try {
                NodeIterator childNodesIterator = node.getNodes();
                while (childNodesIterator.hasNext()) {
                    entries.add(childNodesIterator.nextNode().getPath());
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            PluginModel listModel = new PluginModel();
            listModel.put("entries", entries);
            setModel(listModel);

            //TODO: shouldn't getTable() always use getModel()/getPluginModel()
            //instead of passing the model as a parameters?
            replace(dataTable = getTable(listModel));
            if (selectParentModel) {
                dataTable.setSelectedNode(model.getParentModel());
            } else {
                dataTable.setSelectedNode(model);
            }
            notification.getContext().addRefresh(this);
        }
    }

    public void createTableColumns(PluginDescriptor pluginDescriptor, JcrNodeModel model) {
        UserSession session = (UserSession) Session.get();
        columns = new ArrayList<IStyledColumn>();
        String userPrefListingSettingsLocation = USER_PATH_PREFIX + session.getJcrSession().getUserID() + "/"
                + getPluginUserPrefNodeName();
        String userNodeLocation = USER_PATH_PREFIX + session.getJcrSession().getUserID();
        try {
            Node userPrefNode = (Node) session.getJcrSession().getItem(userPrefListingSettingsLocation);

            pageSize = getPropertyIntValue(userPrefNode, PAGESIZE_PROPERTY, DEFAULT_PAGE_SIZE);
            viewSize = getPropertyIntValue(userPrefNode, VIEWSIZE_PROPERTY, DEFAULT_VIEW_SIZE);

            NodeIterator nodeIt = userPrefNode.getNodes();
            if (nodeIt.getSize() == 0) {
                defaultColumns(pluginDescriptor);
            }
            while (nodeIt.hasNext()) {
                Node n = nodeIt.nextNode();
                if (n.hasProperty(COLUMNNAME_PROPERTY) && n.hasProperty(PROPERTYNAME_PROPERTY)) {
                    String columnName = n.getProperty(COLUMNNAME_PROPERTY).getString();
                    String propertyName = n.getProperty(PROPERTYNAME_PROPERTY).getString();
                    columns.add(getNodeColumn(new Model(columnName), propertyName, getTopChannel()));
                }
            }
        } catch (PathNotFoundException e) {
            // The user preference node for the current plugin does not exist: create node now with default settings:
            log.debug("No user doclisting preference node found. Creating default doclisting preference node.");
            javax.jcr.Session jcrSession = session.getJcrSession();
            try {
                if (!jcrSession.itemExists(userPrefListingSettingsLocation)) {
                    Node userNode = ((Node) jcrSession.getItem(userNodeLocation));
                    // User doesn't have a user folder for this browse perspective yet
                    Node prefNode = createDefaultPrefNodeSetting(userNode);
                    modifyDefaultPrefNode(prefNode, getTopChannel());
                    userNode.save();
                }
            } catch (PathNotFoundException e1) {
                logError(e1);
                defaultColumns(pluginDescriptor);
            } catch (ItemExistsException e1) {
                logError(e1);
                defaultColumns(pluginDescriptor);
            } catch (VersionException e1) {
                logError(e1);
                defaultColumns(pluginDescriptor);
            } catch (ConstraintViolationException e1) {
                logError(e1);
                defaultColumns(pluginDescriptor);
            } catch (LockException e1) {
                logError(e1);
                defaultColumns(pluginDescriptor);
            } catch (ValueFormatException e1) {
                logError(e1);
                defaultColumns(pluginDescriptor);
            } catch (RepositoryException e1) {
                logError(e1);
                defaultColumns(pluginDescriptor);
            }

        } catch (RepositoryException e) {
            logError(e);
            defaultColumns(pluginDescriptor);
        }
        //FIXME: this should not be in this method..
        add(dataTable = getTable(model));
    }

    private Node createDefaultPrefNodeSetting(Node listingNode) throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException,
            RepositoryException, ValueFormatException {
        Node prefNode = listingNode.addNode(getPluginUserPrefNodeName(), LISTING_NODETYPE);
        prefNode.setProperty(PAGESIZE_PROPERTY, DEFAULT_PAGE_SIZE);
        prefNode.setProperty(VIEWSIZE_PROPERTY, DEFAULT_VIEW_SIZE);
        return prefNode;
    }

    private int getPropertyIntValue(Node userPrefNode, String property, int defaultValue) throws RepositoryException,
            ValueFormatException, PathNotFoundException {
        int value = 0;
        if (userPrefNode.hasProperty(property)
                && userPrefNode.getProperty(property).getValue().getType() == PropertyType.LONG) {
            value = (int) userPrefNode.getProperty(property).getLong();
            return value == 0 ? defaultValue : value;
        }
        // TODO : make sure it cannot be a string value. Currently, saving through the console makes the number
        // a string. Also fix this in the repository.cnd
        else if (userPrefNode.hasProperty(property)
                && userPrefNode.getProperty(property).getValue().getType() == PropertyType.STRING) {
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

    /**
     * Override this method in your subclass to change the default listing view
     * @param prefNode
     * @param channel
     * @throws ItemExistsException
     * @throws PathNotFoundException
     * @throws NoSuchNodeTypeException
     * @throws LockException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @throws ValueFormatException
     */
    protected void modifyDefaultPrefNode(Node prefNode, Channel channel) throws ItemExistsException,
            PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException,
            ConstraintViolationException, RepositoryException, ValueFormatException {
        // subclasses should override this if they want to change behavior

        Node pref = prefNode.addNode("name", LISTINGPROPS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, "Name");
        pref.setProperty(PROPERTYNAME_PROPERTY, "name");

        pref = prefNode.addNode("type", LISTINGPROPS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, "Type");
        pref.setProperty(PROPERTYNAME_PROPERTY, JcrConstants.JCR_PRIMARYTYPE);

        pref = prefNode.addNode("state", LISTINGPROPS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, "State");
        pref.setProperty(PROPERTYNAME_PROPERTY, "state");

        columns.add(getNodeColumn(new Model("Name"), "name", channel));
        columns.add(getNodeColumn(new Model("Type"), JcrConstants.JCR_PRIMARYTYPE, channel));
        columns.add(getNodeColumn(new Model("State"), "state", channel));

    }

    private void defaultColumns(PluginDescriptor pluginDescriptor) {
        columns.add(getNodeColumn(new Model("Name"), "name", getTopChannel()));
        columns.add(getNodeColumn(new Model("Type"), JcrConstants.JCR_PRIMARYTYPE, getTopChannel()));
        columns.add(getNodeColumn(new Model("State"), "state", getTopChannel()));
    }

    private void logError(Exception e1) {
        log.error("error creating user doclisting preference: \n " + e1 + " . \n  default doclisting will be shown");
    }

    /**
     * Override this method if you want custom table column / nodecells
     * @param model Model
     * @param propertyName String
     * @param channel Channel
     * @return IStyledColumn
     */
    protected IStyledColumn getNodeColumn(Model model, String propertyName, Channel channel) {
        return new NodeColumn(model, propertyName, channel);
    }

    //TODO: rename this to CustomizableListingDatatable?
    protected abstract CustomizableDocumentListingDataTable getTable(IPluginModel model);

    protected abstract String getPluginUserPrefNodeName();
}
