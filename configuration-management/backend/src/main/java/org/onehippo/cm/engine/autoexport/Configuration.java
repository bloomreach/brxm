/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.JcrUtils;

class Configuration {

    private final Node node;
    private final String nodePath;
    private Boolean enabled;
    private Long lastRevision;

    public Configuration(final Node node) throws RepositoryException {
        this.node = node;
        this.nodePath = node.getPath();
    }

    public String getModuleConfigPath() {
        return nodePath;
    }

    public Session getModuleSession() throws RepositoryException {
        return node.getSession();
    }

    public synchronized boolean isEnabled() {
        if (enabled == null) {
            if ("false".equals(System.getProperty(Constants.SYSTEM_ENABLED_PROPERTY_NAME))) {
                enabled = false;
            } else {
                try {
                    enabled = JcrUtils.getBooleanProperty(node, Constants.CONFIG_ENABLED_PROPERTY_NAME, false);
                } catch (RepositoryException e) {
                    AutoExportModule.log.error("Failed to read AutoExport configuration", e);
                    enabled = false;
                }
            }
        }
        return enabled;
    }

    public synchronized void setEnabled(final boolean enabled) throws RepositoryException {
        node.setProperty(Constants.CONFIG_ENABLED_PROPERTY_NAME, enabled);
        node.getSession().save();
    }

    public synchronized long getLastRevision() throws RepositoryException {
        if (lastRevision == null) {
            lastRevision = JcrUtils.getLongProperty(node, Constants.CONFIG_LAST_REVISION_PROPERTY_NAME, -1l);
        }
        return lastRevision;
    }

    public synchronized void setLastRevision(final long lastRevision) throws RepositoryException {
        node.setProperty(Constants.CONFIG_LAST_REVISION_PROPERTY_NAME, lastRevision);
        this.lastRevision = lastRevision;
        node.getSession().save();
    }
}
