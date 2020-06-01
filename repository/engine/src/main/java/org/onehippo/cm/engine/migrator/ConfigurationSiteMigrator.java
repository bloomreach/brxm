/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.migrator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.path.JcrPath;

/**
 * <p>
 *    {@link ConfigurationSiteMigrator} classes annotated with {@link PreSiteMigrator} run <strong>after</strong> the {@link ConfigurationModel}
 *    is loaded but before the {@link ConfigurationModel} is applied to the JCR Nodes (thus before applied to config or content).
 *    Be aware that {@link ConfigurationSiteMigrator}s always run at startup when a site web application is being registered,
 *    hence should always have a fast initial check whether they have any work to do!
 * </p>
 * <p>
 *    {@link ConfigurationSiteMigrator} classes annotated with {@link PostSiteMigrator} run <strong>after</strong> the {@link ConfigurationModel}
 *    is applied to the JCR Nodes (to config or content).
 *    Be aware that {@link ConfigurationSiteMigrator}s always run at startup when a site web application is being registered,
 *    hence should always have a fast initial check whether they have any work to do!
 * </p>
 * <p>
 *    For a site-related migrator to run it has to implement this interface and have the {@link PreSiteMigrator} or {@link PostSiteMigrator} class annotation and be
 *    in one of the hippo internal packages. This currently matches ((org|com)\.onehippo\..*)|(org\.hippoecm\..*)). See
 *    {@link org.onehippo.cm.engine.ConfigurationServiceImpl#loadMigrators(Class, Class)} for the classpath scanning logic.
 *    There is no specific order in which migrators run so a migrator should not rely on other migrators.
 * </p>
 * <p>
 *     A {@link ConfigurationSiteMigrator} implementation must have a no-arg public constructor.
 * </p>
 */
public interface ConfigurationSiteMigrator {

    /**
     * <p>
     *    This method is expected to receive a clean Session (with no pending changes) and to save() its own changes. Changes that
     *    are not save()d will be discarded by the caller.
     * </p>
     * <p>
     *     Any exception thrown by this {@link ConfigurationSiteMigrator#migrate(Session, ConfigurationModel, JcrPath, boolean)} will be
     *     caught and logged by the calling {@link org.onehippo.cm.engine.ConfigurationServiceImpl} <strong>except</strong>
     *     if the {@link ConfigurationSiteMigrator} throws a {@link MigrationException} : This is a short-circuiting exception
     *     making the further repository bootstrap to directly stop and quit the repository startup.
     * </p>
     * @param session a JCR Session with no pending changes, and which should be returned to this state when this method returns
     * @param configurationModel the HCM model representing the full HCM state of the CMS/platform core, plus one or more sites,
     *                           including at least the one that uses the HST root node indicated by hstRoot
     * @param hstRoot the path of the HST root node for the site that should be migrated
     * @param autoExportRunning {@code true} when auto export is enabled. Note that during the {@link #migrate(Session, ConfigurationModel, JcrPath, boolean)}
     *                                      of {@link PreSiteMigrator}s the {@code autoExportRunning} is always false, because
     *                                      auto export is never started before the {@link PreSiteMigrator}s are executed
     *
     * @return {@code true} if something changed as a result of this migrator (used only as a hint for the caller)
     */
    boolean migrate(final Session session, final ConfigurationModel configurationModel, final JcrPath hstRoot,
                    final boolean autoExportRunning) throws RepositoryException;
}
