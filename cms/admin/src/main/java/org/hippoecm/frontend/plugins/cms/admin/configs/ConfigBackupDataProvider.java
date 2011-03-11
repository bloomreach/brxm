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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.users.DetachableUser;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigBackupDataProvider extends SortableDataProvider {

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DetachableUser.class);

    private static final String BACKUPS_ROOT = "/backups";

    private static transient List<ConfigBackup> configBackupList = new ArrayList<ConfigBackup>();

    private static String sessionId = "none";

    public ConfigBackupDataProvider() {
    }

    public Iterator<ConfigBackup> iterator(int first, int count) {
        List<ConfigBackup> users = new ArrayList<ConfigBackup>();
        for (int i = first; i < (count + first); i++) {
            users.add(configBackupList.get(i));
        }
        return users.iterator();
    }

    public IModel model(Object object) {
        return new DetachableConfigBackup((ConfigBackup) object);
    }

    public int size() {
        populateConfigBackupList();
        return configBackupList.size();
    }

    /**
     * Populate list, refresh when a new session id is found or when dirty
     */
    private static void populateConfigBackupList() {
        HippoSession session = (HippoSession) ((UserSession) Session.get()).getJcrSession();

        configBackupList.clear();

        ConfigBackupManager manager = new ConfigBackupManager(session);

        try {
            configBackupList.addAll(manager.listConfigBackups());
        } catch (RepositoryException e) {
            log.error("Cannot populate list of backup configurations.", e);
        }
        Collections.sort(configBackupList);
        sessionId = Session.get().getId();
    }

}
