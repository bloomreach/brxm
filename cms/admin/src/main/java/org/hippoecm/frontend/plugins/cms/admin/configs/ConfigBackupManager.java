/*
 *  Copyright 2011 Hippo.
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.iterator.NodeIterable;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigBackupManager {

    private static final String BACKUP_ROOT = "backups";
    private static final String CMS_BACKUP = "cms";
    private static final String HST_BACKUP = "hst";
    private static final String AUTO_BACKUP = "autosave";

    private static final String[] HST_BACKUP_PATHS = new String[] { "hst:hst" };
    private static final String[] CMS_BACKUP_PATHS = new String[] { "hippo:configuration", "hippo:namespaces" };

    // TODO: use proper nodetypes and namespace
    public static final String NT_ROOT = "nt:unstructured";
    public static final String NT_BACKUP = "nt:unstructured";
    public static final String PROP_CREATED = "created";
    public static final String PROP_CREATEDBY = "createdBy";

    /** logger */
    private static final Logger log = LoggerFactory.getLogger(ConfigBackupManager.class);

    private final HippoSession session;

    public ConfigBackupManager(HippoSession session) {
        this.session = session;
    }

    public void createConfigBackup(String name) throws RepositoryException {
        validateBackupName(name);
        createHstConfigBackup(name);
        createCmsConfigBackup(name);
    }

    public void restoreConfigBackup(String name) throws RepositoryException {
        createAutoSaveBackup(session);
        restoreHstConfigBackup(name);
        restoreCmsConfigBackup(name);
    }

    public boolean hasConfigBackup(String name) throws RepositoryException {
        Node backupRoot = getOrCreateBackupRoot();
        return backupRoot.hasNode(name);
    }

    public ConfigBackup getConfigBackup(String name) throws RepositoryException {
        if (!hasConfigBackup(name)) {
            throw new ItemNotFoundException("Backup '" + name + "' does not exist.");
        }
        Node backupNode = getOrCreateBackupNode(name);
        return createConfigBackupFromNode(backupNode);
    }

    public void removeConfigBackup(String name) throws RepositoryException {
        if (!hasConfigBackup(name)) {
            throw new ItemNotFoundException("Backup '" + name + "' does not exist.");
        }
        getOrCreateBackupNode(name).remove();
    }

    public List<ConfigBackup> listConfigBackups() throws RepositoryException {
        List<ConfigBackup> backups = new ArrayList<ConfigBackup>();
        Node backupRoot = getOrCreateBackupRoot();
        for (Node backupNode : new NodeIterable(backupRoot.getNodes())) {
            String path = backupNode.getPath();
            try {
                backups.add(createConfigBackupFromNode(backupNode));
            } catch (RepositoryException e) {
                log.error("Invalid or corrupt backup config: " + path, e);
            }
        }
        return backups;
    }

    private void validateBackupName(String name) throws RepositoryException {
        // All kind of checks can happen here...
        // areValideChars()
        if (hasConfigBackup(name)) {
            throw new ItemExistsException("Backup '" + name + "' already exists.");
        }
    }

    private ConfigBackup createConfigBackupFromNode(Node backupNode) throws RepositoryException {
        String name = backupNode.getName();
        String createdBy = null;
        if (backupNode.hasProperty(PROP_CREATEDBY)) {
            createdBy = backupNode.getProperty(PROP_CREATEDBY).getString();
        }
        Calendar created = null;
        if (backupNode.hasProperty(PROP_CREATED)) {
            created = backupNode.getProperty(PROP_CREATED).getDate();
        }
        return new ConfigBackup(name, createdBy, created);
    }

    private void createAutoSaveBackup(Session session) throws RepositoryException {

        Node backupNode = getCleanAutoSaveNode();

        createHstConfigBackup(backupNode.getName());
        createCmsConfigBackup(backupNode.getName());
    }

    private Node getCleanAutoSaveNode() throws RepositoryException {
        Node backupRoot = getOrCreateBackupRoot();
        if (backupRoot.hasNode(AUTO_BACKUP)) {
            backupRoot.getNode(AUTO_BACKUP).remove();
        }
        return getOrCreateBackupNode(AUTO_BACKUP);
    }

    private Node getOrCreateBackupNode(String name) throws RepositoryException {
        Node backupRoot = getOrCreateBackupRoot();
        if (backupRoot.hasNode(name)) {
            return backupRoot.getNode(name);
        }
        Node backupNode = backupRoot.addNode(name, NT_BACKUP);
        backupNode.addNode(HST_BACKUP, NT_BACKUP);
        backupNode.addNode(CMS_BACKUP, NT_BACKUP);
        setBackupMetaData(backupNode);
        return backupNode;
    }

    private Node getOrCreateBackupRoot() throws RepositoryException {
        if (!session.getRootNode().hasNode(BACKUP_ROOT)) {
            return session.getRootNode().addNode(BACKUP_ROOT, NT_ROOT);
        } else {
            return session.getRootNode().getNode(BACKUP_ROOT);
        }
    }

    private void setBackupMetaData(Node backupNode) throws RepositoryException {
        backupNode.setProperty(PROP_CREATEDBY, session.getUserID());
        backupNode.setProperty(PROP_CREATED, Calendar.getInstance());
    }

    private void createHstConfigBackup(String name) throws RepositoryException {
        Node rootNode = session.getRootNode();
        Node backupNode = getOrCreateBackupNode(name).getNode(HST_BACKUP);
        String backupPath = backupNode.getPath();
        for (String path : HST_BACKUP_PATHS) {
            session.copy(rootNode.getNode(path), backupPath + "/" + path);
        }
    }

    private void restoreHstConfigBackup(String name) throws RepositoryException {
        Node rootNode = session.getRootNode();
        Node backupNode = getOrCreateBackupNode(name).getNode(HST_BACKUP);

        for (String path : HST_BACKUP_PATHS) {
            rootNode.getNode(path).remove();
        }

        for (String path : HST_BACKUP_PATHS) {
            session.copy(backupNode.getNode(path), "/" + path);
        }
    }

    private void createCmsConfigBackup(String name) {
    }

    private void restoreCmsConfigBackup(String name) throws RepositoryException {
    }
}
