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
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.PasswordHelper;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class User implements Comparable<User>, IClusterable {

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(User.class);

    private static final String NT_FRONTEND_USER = "frontend:user";

    public static final String PROP_FIRSTNAME = "frontend:firstname";
    public static final String PROP_LASTNAME = "frontend:lastname";
    public static final String PROP_EMAIL = "frontend:email";
    public static final String PROP_PASSWORD = HippoNodeType.HIPPO_PASSWORD;
    public static final String PROP_PASSKEY = HippoNodeType.HIPPO_PASSKEY;
    public static final String PROP_PROVIDER = HippoNodeType.HIPPO_SECURITYPROVIDER;

    private final static String QUERY_USER_EXISTS = "SELECT * FROM hippo:user WHERE fn:name()='{}'";

    private final static String QUERY_LOCAL_MEMBERSHIPS = "SELECT * FROM hippo:group WHERE jcr:primaryType='hippo:group' AND hippo:members='{}'";
    private final static String QUERY_EXTERNAL_MEMBERSHIPS = "SELECT * FROM hippo:externalgroup WHERE hippo:members='{}'";

    private boolean external = false;
    private boolean active = true;
    private String path;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String provider;

    private Map<String, String> properties = new TreeMap<String, String>();
    private transient List<DetachableGroup> externalMemberships;

    private transient Node node;

    //-------------- static helpers -------------------//
    public static QueryManager getQueryManager() throws RepositoryException {
        return ((UserSession) Session.get()).getQueryManager();
    }

    public static boolean userExists(String username) {
        String queryString = QUERY_USER_EXISTS.replace("{}", username);
        try {
            Query query = getQueryManager().createQuery(queryString, Query.SQL);
            if (query.execute().getNodes().getSize() > 0) {
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Unable to check if user '{}' exists, returning true", username, e);
            return true;
        }
        return false;
    }

    /**
     * Generate password hash from string
     * @param password
     * @return the hash
     * @throws RepositoryException, the wrapper encoding errors
     */
    public static String createPasswordHash(String password) throws RepositoryException {
        try {
            return PasswordHelper.getHash(password.toCharArray());
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException("Unable to hash password", e);
        } catch (IOException e) {
            throw new RepositoryException("Unable to hash password", e);
        }
    }

    //--------------- getters and setters -----------//
    public String getProvider() {
        return provider;
    }

    public boolean isExternal() {
        return external;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<Entry<String, String>> getPropertiesList() {
        List<Entry<String, String>> l = new ArrayList<Entry<String, String>>();
        for (Entry<String, String> e : properties.entrySet()) {
            l.add(e);
        }
        return l;
    }

    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) {
            sb.append(firstName);
        }
        sb.append(" ");
        if (lastName != null) {
            sb.append(lastName);
        }
        if (sb.length() == 1) {
            return username;
        }
        return sb.toString();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPath() {
        return path;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    //----------------------- constructors ---------//
    public User() {
    }

    public User(final Node node) throws RepositoryException {
        this.node = node;
        this.path = node.getPath().substring(1);
        this.username = NodeNameCodec.decode(node.getName());

        if (node.isNodeType(HippoNodeType.NT_EXTERNALUSER)) {
            external = true;
        }

        PropertyIterator pi = node.getProperties();
        while (pi.hasNext()) {
            final Property p = pi.nextProperty();
            String name = p.getName();
            if (name.startsWith("jcr:")) {
                //skip
            } else if (name.equals(PROP_EMAIL) || name.equalsIgnoreCase("email")) {
                email = p.getString();
            } else if (name.equals(PROP_FIRSTNAME) || name.equalsIgnoreCase("firstname")) {
                firstName = p.getString();
            } else if (name.equals(PROP_LASTNAME) || name.equalsIgnoreCase("lastname")) {
                lastName = p.getString();
            } else if (name.equals(HippoNodeType.HIPPO_ACTIVE)) {
                active = p.getBoolean();
            } else if (name.equals(PROP_PASSWORD) || name.equals(PROP_PASSKEY)) {
                // do not expose password hash
            } else if (name.equals(PROP_PROVIDER)) {
                provider = p.getString();
            } else {
                properties.put(name, p.getString());
            }
        }
    }

    //--------------- group membership helpers ------------------//
    public List<DetachableGroup> getLocalMemberships() {
        String queryString = QUERY_LOCAL_MEMBERSHIPS.replace("{}", username);
        List<DetachableGroup> localMemberships = new ArrayList<DetachableGroup>();
        NodeIterator iter;
        try {
            Query query = getQueryManager().createQuery(queryString, Query.SQL);
            iter = query.execute().getNodes();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node != null) {
                    try {
                        localMemberships.add(new DetachableGroup(node.getPath()));
                    } catch (RepositoryException e) {
                        log.warn("Unable to add group to local memberships for user '{}'", username, e);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while querying local memberships of user '{}'", e);
        }
        return localMemberships;
    }

    public List<DetachableGroup> getExternalMemberships() {
        if (externalMemberships != null) {
            return externalMemberships;
        }
        externalMemberships = new ArrayList<DetachableGroup>();
        String queryString = QUERY_EXTERNAL_MEMBERSHIPS.replace("{}", username);
        NodeIterator iter;
        try {
            Query query = getQueryManager().createQuery(queryString, Query.SQL);
            iter = query.execute().getNodes();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node != null) {
                    try {
                        externalMemberships.add(new DetachableGroup(node.getPath()));
                    } catch (RepositoryException e) {
                        log.warn("Unable to add group to external memberships for user '{}'", username, e);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while querying external memberships of user '{}'", username, e);
        }
        return externalMemberships;
    }

    //-------------------- persistence helpers ----------//
    /**
     * Create a new user
     * @throws RepositoryException
     */
    public void create() throws RepositoryException {
        if (userExists(getUsername())) {
            throw new RepositoryException("User already exists");
        }

        // FIXME: should be delegated to a usermanager
        StringBuilder relPath = new StringBuilder();
        relPath.append(HippoNodeType.CONFIGURATION_PATH);
        relPath.append("/");
        relPath.append(HippoNodeType.USERS_PATH);
        relPath.append("/");
        relPath.append(NodeNameCodec.encode(getUsername(), true));

        node = ((UserSession) Session.get()).getRootNode().addNode(relPath.toString(), NT_FRONTEND_USER);
        setOrRemoveStringProperty(node, PROP_EMAIL, getEmail());
        setOrRemoveStringProperty(node, PROP_FIRSTNAME, getFirstName());
        setOrRemoveStringProperty(node, PROP_LASTNAME, getLastName());
        // save parent when adding a node
        node.getParent().getSession().save();
    }

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
     * save the current user
     * @throws RepositoryException
     */
    public void save() throws RepositoryException {
        if (node.isNodeType(NT_FRONTEND_USER)) {
            setOrRemoveStringProperty(node, PROP_EMAIL, getEmail());
            setOrRemoveStringProperty(node, PROP_FIRSTNAME, getFirstName());
            setOrRemoveStringProperty(node, PROP_LASTNAME, getLastName());
            node.setProperty(HippoNodeType.HIPPO_ACTIVE, isActive());
            node.getSession().save();
        } else {
            throw new RepositoryException("Only frontend:users can be edited.");
        }
    }

    /**
     * Delete the current user
     * @throws RepositoryException
     */
    public void delete() throws RepositoryException {
        Node parent = node.getParent();
        node.remove();
        parent.getSession().save();
    }

    /**
     * Save the current user's password with a hashing function
     * @param password
     * @throws RepositoryException
     */
    public void savePassword(String password) throws RepositoryException {
        node.setProperty(HippoNodeType.HIPPO_PASSWORD, createPasswordHash(password));
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
        User other = (User) obj;
        return other.getPath().equals(getPath());
    }
    
    public int hashCode() {
        return (null == path ? 0 : path.hashCode());
    }
    
    public int compareTo(User o) {
        
        String thisName = getUsername();
        String otherName = o.getUsername();
        // 
        int len1 = thisName.length();
        int len2 = otherName.length();
        int n = Math.min(len1, len2);
        char v1[] = thisName.toCharArray();
        char v2[] = otherName.toCharArray();
        int i = 0;
        int j = 0;

        if (i == j) {
            int k = i;
            int lim = n + i;
            while (k < lim) {
            char c1 = v1[k];
            char c2 = v2[k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
            }
        } else {
            while (n-- != 0) {
            char c1 = v1[i++];
            char c2 = v2[j++];
            if (c1 != c2) {
                return c1 - c2;
            }
            }
        }
        return len1 - len2;
    }
}
