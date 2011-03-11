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
package org.hippoecm.frontend.plugins.cms.admin.configs;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.swing.tree.AbstractLayoutCache;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.PasswordHelper;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config implements Comparable<Config>, IClusterable {

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static final String PROP_BACKUP_NAME = "backupName";
    public static final String PROP_BACKUP_CREATION_DATE = "backupCreationDate";
    public static final String PROP_BACKUP_CREATED_BY = "backupCreatedBy";

    private String path;
    private String name;
    private String createdBy;
    private Calendar creationDate;

    //--------------- getters and setters -----------//
    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public String getCreationDateAsString() {
        return simpleFormattedCalendar(creationDate);
    }

    public void setCreationDate(final Calendar creationDate) {
        this.creationDate = creationDate;
    }

    private String simpleFormattedCalendar(Calendar cal) {
        if (cal != null) {
            Locale locale = Session.get().getLocale();
            return DateTimeFormat.forPattern("d-MMM-yyyy").withLocale(locale).print(new DateTime(cal));
        }
        return "";
    }



    //----------------------- constructors ---------//
    public Config() {
    }

    public Config(Node node) throws RepositoryException {
        this.path = node.getPath();
        this.name = getStringProperty(node, PROP_BACKUP_NAME, "");
        this.creationDate = getDateProperty(node, PROP_BACKUP_CREATION_DATE, null);
        this.createdBy = getStringProperty(node, PROP_BACKUP_CREATED_BY, "");
    }

    private String getStringProperty(Node node, String propName, String defaultValue) {
        try {
            return node.getProperty(propName).getString();
        } catch (RepositoryException e) {
            return defaultValue;
        }
    }

    private Calendar getDateProperty(Node node, String propName, Calendar defaultValue) {
        try {
            return node.getProperty(propName).getDate();
        } catch (RepositoryException e) {
            return defaultValue;
        }
    }

    //-------------------- persistence helpers ----------//
    /**
     * Create a new user
     * @throws javax.jcr.RepositoryException
     */
    public void create() throws RepositoryException {
//        if (userExists(getUsername())) {
//            throw new RepositoryException("User already exists");
//        }
//
//        // FIXME: should be delegated to a usermanager
//        StringBuilder relPath = new StringBuilder();
//        relPath.append(HippoNodeType.CONFIGURATION_PATH);
//        relPath.append("/");
//        relPath.append(HippoNodeType.USERS_PATH);
//        relPath.append("/");
//        relPath.append(NodeNameCodec.encode(getUsername(), true));
//
//        node = ((UserSession) Session.get()).getRootNode().addNode(relPath.toString(), NT_USER);
//        setOrRemoveStringProperty(node, PROP_EMAIL, getEmail());
//        setOrRemoveStringProperty(node, PROP_FIRSTNAME, getFirstName());
//        setOrRemoveStringProperty(node, PROP_LASTNAME, getLastName());
//        // save parent when adding a node
//        node.getParent().getSession().save();
    }

    //--------------------- default object -------------------//
    /**
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || (obj.getClass() != this.getClass())) {
            return false;
        }
        Config other = (Config) obj;
        return other.name.equals(name);
    }
    
    public int hashCode() {
        return name.hashCode();
    }
    
    public int compareTo(Config other) {
        return name.compareTo(other.name);
    }
}
