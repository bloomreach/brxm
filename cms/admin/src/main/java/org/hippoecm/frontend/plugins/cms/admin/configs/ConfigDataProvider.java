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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.users.DetachableUser;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigDataProvider extends SortableDataProvider {

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DetachableUser.class);

    private static final String BACKUPS_ROOT = "/backups";

    private static transient List<Config> configList = new ArrayList<Config>();

    private static String sessionId = "none";

    public ConfigDataProvider() {
    }

    public Iterator<Config> iterator(int first, int count) {
        List<Config> users = new ArrayList<Config>();
        for (int i = first; i < (count + first); i++) {
            users.add(configList.get(i));
        }
        return users.iterator();
    }

    public IModel model(Object object) {
        return new DetachableConfig((Config) object);
    }

    public int size() {
        populateConfigList();
        return configList.size();
    }

    /**
     * Populate list, refresh when a new session id is found or when dirty
     */
    private static void populateConfigList() {
        synchronized (ConfigDataProvider.class) {
            NodeIterator iter = null;
            try {
                iter = ((UserSession) Session.get()).getJcrSession().getNode(BACKUPS_ROOT).getNodes();
            } catch (RepositoryException e) {
                log.error("Could not fetch backed up configurations.", e);
                return;
            }

            configList.clear();

            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node != null) {
                    try {
                        configList.add(new Config(node));
                    } catch (RepositoryException e) {
                        log.warn("Unable to instantiate new configuration.", e);
                    }
                }
            }
            Collections.sort(configList);
            sessionId = Session.get().getId();
        }
    }

}
