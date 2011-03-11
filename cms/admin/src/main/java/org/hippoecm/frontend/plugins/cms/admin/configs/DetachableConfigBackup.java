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

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DetachableConfigBackup extends LoadableDetachableModel {

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DetachableConfigBackup.class);

    private String name;

    public DetachableConfigBackup() {
    }

    public DetachableConfigBackup(final ConfigBackup config) {
        this(config.getName());
    }

    public DetachableConfigBackup(final String name) {
        this.name = name;
    }

    public ConfigBackup getConfigBackup() {
        return (ConfigBackup) getObject();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * used for dataview with ReuseIfModelsEqualStrategy item reuse strategy
     *
     * @see org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof DetachableConfigBackup) {
            DetachableConfigBackup other = (DetachableConfigBackup) obj;
            return name.equals(other.name);
        }
        return false;
    }

    /**
     * @see org.apache.wicket.model.LoadableDetachableModel#load()
     */
    @Override
    protected ConfigBackup load() {
        try {
            HippoSession session = (HippoSession) ((UserSession)Session.get()).getJcrSession();
            return new ConfigBackupManager(session).getConfigBackup(name);
        } catch (RepositoryException e) {
            log.error("Unable to re-attach backup with name '" + name + "'", e);
            return null;
        }
    }
}
