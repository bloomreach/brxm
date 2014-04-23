/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.utils.update;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util to add entries to the new updater engine API (from 7.8 and upwards). In this util you can easily create updater scrips and add them with code.
 * Almost every plug-in will need some DTAP integration.
 * There is a distinction between addToQueue (immediately add and run on startup) and addToRegistry (add to the updater engine registry for manual startup)
 *
 * @version "$Id$"
 */
public final class UpdateUtils {

    private UpdateUtils() {
    }

    private static Logger log = LoggerFactory.getLogger(UpdateUtils.class);

    public static final String UPDATE_UTIL_PATH = "/hippo:configuration/hippo:update/";

    /**
     * Copies an entry from registry directly to the queue so it can immediately be executed.
     *
     * @param context
     * @param name
     */
    public static void copyFromRegistryToQueue(final PluginContext context, final String name) {
        final Session session = context.createSession();
        try {
            if (session.itemExists(UPDATE_UTIL_PATH + UpdateType.REGISTRY.getPath() + '/' + name)) {
                session.getWorkspace().copy(UPDATE_UTIL_PATH + UpdateType.REGISTRY.getPath() + '/' + name, UPDATE_UTIL_PATH + UpdateType.QUEUE.getPath() + '/' + name);
                session.save();
            } else {
                throw new IllegalArgumentException("there is no updater in registry with name " + name);
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }finally {
            GlobalUtils.cleanupSession(session);
        }
    }

    public static void addToQueue(final PluginContext context, final InputStream in) {
        addToUpdaterInfo(context, UpdateType.QUEUE, in);
    }

    public static void addToRegistry(final PluginContext context, final InputStream in) {
        addToUpdaterInfo(context, UpdateType.REGISTRY, in);
    }

    public static void addToQueue(final PluginContext context, final UpdateConfig config) {
        addToUpdaterInfo(context, UpdateType.QUEUE, config);
    }

    public static void addToRegistry(final PluginContext context, final UpdateConfig config) {
        addToUpdaterInfo(context, UpdateType.REGISTRY, config);
    }

    public static void addToRegistry(final PluginContext context, final String name, final String query, final String script, final long batchSize, final long throttle, final boolean dryRun) {
        addToUpdaterInfo(context, UpdateType.REGISTRY, new UpdateConfig(name, script, query, batchSize, throttle, dryRun));
    }

    public static void addToQueue(final PluginContext context, final String name, final String query, final String script, final long batchSize, final long throttle, final boolean dryRun) {
        addToUpdaterInfo(context, UpdateType.QUEUE, new UpdateConfig(name, script, query, batchSize, throttle, dryRun));
    }

 /*   private static void addToUpdaterInfo(final PluginContext context, final UpdateType type, final String query, final String script, final long batchSize, final long throttle, final boolean dryRun) {
        addToUpdaterInfo(context, type, new UpdateConfig().setQuery(query).setScript(script).setBatchSize(batchSize).setDryRun(dryRun).setThrottle(throttle));
    }
   */

    /**
     * Uses the updater model to create a new queue or registry entry in the updater engine api
     *
     * @param context
     * @param type
     * @param config
     */
    private static void addToUpdaterInfo(PluginContext context, UpdateType type, UpdateConfig config) {
        final Session session = context.createSession();
        try {
            if (session.itemExists(UPDATE_UTIL_PATH + type.getPath())) {
                final Node updateTypeNode = session.getNode(UPDATE_UTIL_PATH + type.getPath());

                final Node updater = updateTypeNode.addNode(config.getName(), "hipposys:updaterinfo");
                updater.setProperty("hipposys:batchsize", config.getBatchSize());
                updater.setProperty("hipposys:dryrun", config.isDryRun());
                updater.setProperty("hipposys:query", config.getQuery());
                updater.setProperty("hipposys:script", config.getScript());
                updater.setProperty("hipposys:throttle", config.getThrottle());
                session.save();
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception occurred while trying to save Updater log info", e);
        }finally {
            GlobalUtils.cleanupSession(session);
        }

    }

    /**
     * uses an inputstream to create a new queue or registry entry in the updater engine api. the inpustream is the jcr s:node xml
     *
     * @param context
     * @param type
     * @param in
     */
    private static void addToUpdaterInfo(final PluginContext context, final UpdateType type, final InputStream in) {
        final Session session = context.createSession();
        try {
            if (session.itemExists(UPDATE_UTIL_PATH + type.getPath())) {
                if (session instanceof HippoSession) {
                    HippoSession hippoSession = (HippoSession) session;
                    hippoSession.importDereferencedXML(UPDATE_UTIL_PATH + type.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                            ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
                } else {
                    session.importXML(UPDATE_UTIL_PATH + type.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                }
                session.save();
            } else {
                throw new IllegalArgumentException("cannot access the path:" + UPDATE_UTIL_PATH + type.getPath());
            }
        } catch (RepositoryException e) {
            log.error("Repository exception while trying to acces: ", UPDATE_UTIL_PATH + type.getPath(), e);
        } catch (IOException e) {
            log.error("", e);
        } finally {
            IOUtils.closeQuietly(in);
            GlobalUtils.cleanupSession(session);
        }
    }

    /**
     * Updater engine types; queue or registry (see class information for difference)
     */
    public enum UpdateType {
        QUEUE("hippo:queue"), REGISTRY("hippo:registry");

        private String path;

        UpdateType(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    /**
     * Model for an updater.
     */
    public static class UpdateConfig {

        public static final int DEFAULT_BATCH_SIZE = 10;
        public static final int DEFAULT_THROTTLE_TIME = 1000;
        public static final boolean DEFAULT_DRY_RUN = true;
        private String name;
        private String script = "package org.hippoecm.frontend.plugins.cms.dev.updater\n" +
                '\n' +
                "import org.onehippo.repository.update.BaseNodeUpdateVisitor\n" +
                "import javax.jcr.Node\n" +
                '\n' +
                "class UpdaterTemplate extends BaseNodeUpdateVisitor {\n" +
                '\n' +
                "  boolean doUpdate(Node node) {\n" +
                "    log.debug \"Updating node ${node.path}\"\n" +
                "    return false\n" +
                "  }\n" +
                '\n' +
                "  boolean undoUpdate(Node node) {\n" +
                "    throw new UnsupportedOperationException('Updater does not implement undoUpdate method')\n" +
                "  }\n" +
                '\n' +
                '}';
        private String query;
        private long batchSize = DEFAULT_BATCH_SIZE;
        private long throttle = DEFAULT_THROTTLE_TIME;
        private boolean dryRun = DEFAULT_DRY_RUN;

        public UpdateConfig() {
        }

        public UpdateConfig(final String name, final String script, final String query, final long batchSize, final long throttle, final boolean dryRun) {
            if (StringUtils.isEmpty(name)) {
                throw new IllegalArgumentException("Name is mandatory");
            }
            this.name = name;
            this.script = script;
            this.query = query;
            this.batchSize = batchSize;
            this.throttle = throttle;
            this.dryRun = dryRun;
        }

        public String getScript() {
            return script;
        }

        public UpdateConfig setScript(final String script) {
            this.script = script;
            return this;
        }

        public String getQuery() {
            return query;
        }

        public UpdateConfig setQuery(final String query) {
            this.query = query;
            return this;
        }

        public long getBatchSize() {
            return batchSize;
        }

        public UpdateConfig setBatchSize(final long batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public long getThrottle() {
            return throttle;
        }

        public UpdateConfig setThrottle(final long throttle) {
            this.throttle = throttle;
            return this;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public UpdateConfig setDryRun(final boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}
