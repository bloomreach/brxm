/**
 * Copyright (C) 2012 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.dev.updater;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.session.UserSession;


public class UpdaterRegistryEditor extends UpdaterEditor {

    public UpdaterRegistryEditor(final IModel<?> model, final Panel container) {
        super(model, container);
    }

    @Override
    protected boolean isStopButtonVisible() {
        return false;
    }

    @Override
    protected boolean isExecuteButtonEnabled() {
        return isExecutable();
    }

    @Override
    protected boolean isSaveButtonEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isDeleteButtonEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isNameFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isPathFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isQueryFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isBatchSizeFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isThrottleFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isDryRunCheckBoxVisible() {
        return false;
    }

    private boolean isExecutable() {
        if (method.equals("query")) {
            return isValidQuery();
        }
        return isValidPath();
    }

    private boolean isValidPath() {
        return visitorPath != null && visitorPath.startsWith("/");
    }

    private boolean isValidQuery() {
        if (visitorQuery != null) {
            final Session session = UserSession.get().getJcrSession();
            try {
                final QueryManager queryManager = session.getWorkspace().getQueryManager();
                queryManager.createQuery(visitorQuery, Query.XPATH);
                return true;
            } catch (InvalidQueryException e) {
                log.debug("Query is invalid: " + visitorQuery);
            } catch (RepositoryException e) {
                log.error("Error while validating jcr query", e);
            }
        }
        return false;
    }
}
