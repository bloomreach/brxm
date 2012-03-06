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
package org.hippoecm.frontend.plugins.cms.admin.groups;


import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.hippoecm.frontend.plugins.cms.admin.HippoSecurityEventConstants;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Group implements Comparable<Group>, IClusterable {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(Group.class);

    private final static String PROP_DESCRIPTION = "hipposys:description";
    private final static String QUERY_ALL_LOCAL = "select * from hipposys:group where hipposys:securityprovider='internal'";
    private final static String QUERY_ALL = "select * from hipposys:group";
    private final static String QUERY_ALL_ROLES = "select * from hipposys:role";
    private final static String QUERY_GROUP_EXISTS = "SELECT * FROM hipposys:group WHERE fn:name()='{}'";

        
    private String path;
    private String groupname;
    
    private String description;
    private boolean external = false;

    private transient Node node;
    
    public static QueryManager getQueryManager() throws RepositoryException {
        return ((UserSession) Session.get()).getQueryManager();
    }
    

    public static boolean exists(String groupname) {
        String queryString = QUERY_GROUP_EXISTS.replace("{}", groupname);
        try {
            @SuppressWarnings({"deprecation"}) Query query = getQueryManager().createQuery(queryString, Query.SQL);
            if (query.execute().getNodes().hasNext()) {
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Unable to check if group '{}' exists, returning true", groupname, e);
            return true;
        }
        return false;
    }

    public static List<Group> getLocalGroups() {
        List<Group> groups = new ArrayList<Group>();
        NodeIterator iter;
        try {
            @SuppressWarnings({"deprecation"}) Query query = getQueryManager().createQuery(QUERY_ALL_LOCAL, Query.SQL);
            iter = query.execute().getNodes();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node != null) {
                    try {
                        groups.add(new Group(node));
                    } catch (RepositoryException e) {
                        log.warn("Unable to add group to list", e);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while querying for a list of local groups", e);
        }
        // TODO: remove when query can sort on node names
        Collections.sort(groups);
        return groups;
    }
    

    public static List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<Group>();
        NodeIterator iter;
        try {
            @SuppressWarnings({"deprecation"}) Query query = getQueryManager().createQuery(QUERY_ALL, Query.SQL);
            iter = query.execute().getNodes();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node != null) {
                    try {
                        groups.add(new Group(node));
                    } catch (RepositoryException e) {
                        log.warn("Unable to add group to list", e);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while querying for a list of local groups", e);
        }
        // TODO: remove when query can sort on node names
        Collections.sort(groups);
        return groups;
    }
    

    /**
     * FIXME: should move to roles class or something the like
     * when the admin perspective gets support for it
     *
     * @return A list of all roles defined in the system
     */
    public static List<String> getAllRoles() {
        List<String> roles = new ArrayList<String>();
        NodeIterator iter;
        try {
            @SuppressWarnings({"deprecation"}) Query query = getQueryManager().createQuery(QUERY_ALL_ROLES, Query.SQL);
            iter = query.execute().getNodes();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node != null) {
                    try {
                        roles.add(node.getName());
                    } catch (RepositoryException e) {
                        log.warn("Unable to add group to list", e);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while querying for a list of local groups", e);
        }
        // TODO: remove when query can sort on node names
        Collections.sort(roles);
        return roles;
    }
    
    public boolean isExternal() {
        return external;
    }

    public String getGroupname() {
        return groupname;
    }

    @SuppressWarnings({ "unused" })
    public void setGroupname(final String groupname) {
        this.groupname = groupname;
    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) throws RepositoryException {
        this.description = description;
    }


    //----------------------- constructors ---------//
    public Group() {
    }
    
    public Group(final Node node) throws RepositoryException {
        this.path = node.getPath().substring(1);
        this.groupname = NodeNameCodec.decode(node.getName());
        this.node = node;
        
        if (node.isNodeType(HippoNodeType.NT_EXTERNALGROUP)) {
            external = true;
        }

        if (node.hasProperty(PROP_DESCRIPTION)) {
            setDescription(node.getProperty(PROP_DESCRIPTION).getString());
        } else if (node.hasProperty("description")){
            setDescription(node.getProperty("description").getString());
            
        }
    }

    public List<String> getMembers() throws RepositoryException {
        List<String> members = new ArrayList<String>();
        if (node.hasProperty(HippoNodeType.HIPPO_MEMBERS)) {
            Value[] vals = node.getProperty(HippoNodeType.HIPPO_MEMBERS).getValues();
            for (Value val : vals) {
                members.add(val.getString());
            }
        }
        Collections.sort(members);
        return members;
    }

    //-------------------- persistence helpers ----------//
    /**
     * Wrapper needed for spi layer which doesn't know if a property exists or not
     * @param node
     * @param name
     * @param value
     * @throws RepositoryException
     */
    private void setOrRemoveStringProperty(Node node, String name, String value) throws RepositoryException {
        if (value == null && !node.hasProperty(name)) {
            return;
        }
        node.setProperty(name, value);
    }
    
    /**
     * Create a new group
     * @throws RepositoryException
     */
    public void create() throws RepositoryException {
        if (exists(getGroupname())) {
            throw new RepositoryException("Group already exists");
        }

        // FIXME: should be delegated to a groupmanager
        StringBuilder relPath = new StringBuilder();
        relPath.append(HippoNodeType.CONFIGURATION_PATH);
        relPath.append("/");
        relPath.append(HippoNodeType.GROUPS_PATH);
        relPath.append("/");
        relPath.append(NodeNameCodec.encode(getGroupname(), true));

        node = ((UserSession) Session.get()).getRootNode().addNode(relPath.toString(), HippoNodeType.NT_GROUP);
        setOrRemoveStringProperty(node, PROP_DESCRIPTION, getDescription());
        // save parent when adding a node
        node.getParent().getSession().save();
    }

    /**
     * save the current group
     * @throws RepositoryException
     */
    public void save() throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_GROUP)) {
            setOrRemoveStringProperty(node, PROP_DESCRIPTION, getDescription());
            node.getSession().save();
        } else {
            throw new RepositoryException("Only hipposys:group's can be edited.");
        }
    }

    /**
     * Delete the current group.
     *
     * @throws RepositoryException
     */
    public void delete() throws RepositoryException {
        Node parent = node.getParent();
        node.remove();
        parent.getSession().save();

        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final UserSession userSession = UserSession.get();
            HippoEvent event = new HippoEvent(userSession.getApplicationName())
                    .user(userSession.getJcrSession().getUserID())
                    .action("delete-group")
                    .category(HippoSecurityEventConstants.CATEGORY_GROUP_MANAGEMENT)
                    .message("deleted group " + groupname);
            eventBus.post(event);
        }   
    }

    public void removeMembership(String user) throws RepositoryException {
        List<String> members = getMembers();
        members.remove(user);
        node.setProperty(HippoNodeType.HIPPO_MEMBERS, members.toArray(new String[members.size()]));
        node.getSession().save();
    }

    public void addMembership(String user) throws RepositoryException {
        List<String> members = getMembers();
        members.add(user);
        node.setProperty(HippoNodeType.HIPPO_MEMBERS, members.toArray(new String[members.size()]));
        node.getSession().save();
    }

    //--------------------- default object -------------------//
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || (obj.getClass() != this.getClass())) {
            return false;
        }
        Group other = (Group) obj;
        return other.getPath().equals(getPath());
    }

    public int hashCode() {
        return (null == path ? 0 : path.hashCode());
    }
    
    public int compareTo(Group o) {
        return groupname.compareTo(o.getGroupname());
    }
}
