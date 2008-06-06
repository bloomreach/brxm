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
package org.hippoecm.frontend.plugins.standards.sa.list;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrNodeModelComparator;
import org.hippoecm.frontend.model.SortableDataAdapter;
import org.hippoecm.frontend.sa.model.IJcrNodeModelListener;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.IJcrService;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractListingPlugin extends RenderPlugin implements IJcrNodeModelListener {

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

    //    public static final String PROVIDER_ID = "wicket.provider";

    public int pageSize = DEFAULT_PAGE_SIZE;
    public int viewSize = DEFAULT_VIEW_SIZE;

    public static final String USER_PATH_PREFIX = "/hippo:configuration/hippo:users/";

    protected List<IStyledColumn> columns;
    //    private ProviderReference providerRef;
    private SortableDataAdapter<JcrNodeModel> provider;
    private Map<String, Comparator<? super JcrNodeModel>> compare;

    public AbstractListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // register for flush notifications
        context.registerService(this, IJcrService.class.getName());

        compare = new HashMap<String, Comparator<? super JcrNodeModel>>();
        compare.put("name", new JcrNodeModelComparator("name"));
        compare.put(JcrConstants.JCR_PRIMARYTYPE, new JcrNodeModelComparator(JcrConstants.JCR_PRIMARYTYPE));
        compare.put("state", new JcrNodeModelComparator("state"));
        add(new EmptyPanel("table"));

        createTableColumns();
    }

    public void setDataProvider(IDataProvider provider) {
        this.provider = new SortableDataAdapter<JcrNodeModel>(provider, compare);
        this.provider.setSort("name", true);

        Component table = getTable("table", this.provider);
        table.setModel(getModel());
        replace(table);
    }

    public IDataProvider getDataProvider() {
        return provider.getDataProvider();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        // calculate list of node models
        // FIXME: move into separate service
        JcrNodeModel model = (JcrNodeModel) getModel();
        List<JcrNodeModel> entries = new ArrayList<JcrNodeModel>();
        Node node = (Node) model.getNode();
        try {
            while (node != null) {
                if (!node.isNodeType(HippoNodeType.NT_DOCUMENT) && !node.isNodeType(HippoNodeType.NT_HANDLE)
                        && !node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)
                        && !node.isNodeType(HippoNodeType.NT_REQUEST)) {
                    NodeIterator childNodesIterator = node.getNodes();
                    while (childNodesIterator.hasNext()) {
                        entries.add(new JcrNodeModel(childNodesIterator.nextNode()));
                    }
                    break;
                }
                node = node.getParent();
            }

        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        setDataProvider(new ListDataProvider(entries));
        redraw();
    }

    public void onFlush(JcrNodeModel nodeModel) {
        String nodePath = nodeModel.getItemModel().getPath();
        String myPath = ((JcrNodeModel) getModel()).getItemModel().getPath();
        if (myPath.startsWith(nodePath)) {
            modelChanged();
        }
    }

    // internals

    public void createTableColumns() {
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
                defaultColumns();
            }
            while (nodeIt.hasNext()) {
                Node n = nodeIt.nextNode();
                if (n.hasProperty(COLUMNNAME_PROPERTY) && n.hasProperty(PROPERTYNAME_PROPERTY)) {
                    String columnName = n.getProperty(COLUMNNAME_PROPERTY).getString();
                    String propertyName = n.getProperty(PROPERTYNAME_PROPERTY).getString();
                    columns.add(getNodeColumn(new Model(columnName), propertyName));
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
                    modifyDefaultPrefNode(prefNode);
                    userNode.save();
                }
            } catch (PathNotFoundException e1) {
                logError(e1);
                defaultColumns();
            } catch (ItemExistsException e1) {
                logError(e1);
                defaultColumns();
            } catch (VersionException e1) {
                logError(e1);
                defaultColumns();
            } catch (ConstraintViolationException e1) {
                logError(e1);
                defaultColumns();
            } catch (LockException e1) {
                logError(e1);
                defaultColumns();
            } catch (ValueFormatException e1) {
                logError(e1);
                defaultColumns();
            } catch (RepositoryException e1) {
                logError(e1);
                defaultColumns();
            }

        } catch (RepositoryException e) {
            logError(e);
            defaultColumns();
        }
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
     * @throws ItemExistsException
     * @throws PathNotFoundException
     * @throws NoSuchNodeTypeException
     * @throws LockException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @throws ValueFormatException
     */
    protected void modifyDefaultPrefNode(Node prefNode) throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException,
            RepositoryException, ValueFormatException {
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

        columns.add(getNodeColumn(new Model("Name"), "name"));
        columns.add(getNodeColumn(new Model("Type"), JcrConstants.JCR_PRIMARYTYPE));
        columns.add(getNodeColumn(new Model("State"), "state"));

    }

    private void defaultColumns() {
        columns.add(getNodeColumn(new Model("Name"), "name"));
        columns.add(getNodeColumn(new Model("Type"), JcrConstants.JCR_PRIMARYTYPE));
        columns.add(getNodeColumn(new Model("State"), "state"));
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
    protected IStyledColumn getNodeColumn(Model model, String propertyName) {
        return new NodeColumn(model, propertyName);
    }

    protected abstract Component getTable(String wicketId, ISortableDataProvider provider);

    protected abstract String getPluginUserPrefNodeName();

}
