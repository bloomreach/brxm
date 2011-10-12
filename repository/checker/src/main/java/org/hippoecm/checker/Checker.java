/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.checker;

import java.io.File;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.VersioningConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.pool.Access;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Checker {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    static final Logger log = LoggerFactory.getLogger(Checker.class);
    private RepositoryConfig repConfig;

    public Checker(RepositoryConfig repConfig) {
        this.repConfig = repConfig;
    }

    public boolean check(boolean fix) {
        Progress progress = new Progress();
        progress.setLogger(log);
        boolean clean = true;
        PersistenceManager persistMgr = null;
        try {
            FileSystem fs = repConfig.getFileSystem();
            Traverse traverse = new Traverse();
            {
                VersioningConfig wspConfig = repConfig.getVersioningConfig();
                PersistenceManagerConfig pmConfig = wspConfig.getPersistenceManagerConfig();
                persistMgr = pmConfig.newInstance(PersistenceManager.class);
                persistMgr.init(new PMContext(
                        new File(repConfig.getHomeDir()), fs,
                        RepositoryImpl.ROOT_NODE_ID,
                        null,
                        null,
                        repConfig.getDataStore()));
                Repair repair = new Repair();
                {
                    BundleReader bundleReader = new BundleReader(persistMgr, false, repair);
                    int size = bundleReader.getSize();
                    log.info("Traversing through " + size + " bundles");
                    Iterable<NodeDescription> iterable = Coroutine.<NodeDescription>toIterable(bundleReader, size, progress);
                    clean &= traverse.checkVersionBundles(iterable, repair);
                    if (fix) {
                        repair.perform(bundleReader);
                    }
                }
            }
            for (WorkspaceConfig wspConfig : repConfig.getWorkspaceConfigs()) {
                PersistenceManagerConfig pmConfig = wspConfig.getPersistenceManagerConfig();
                persistMgr = pmConfig.newInstance(PersistenceManager.class);
                persistMgr.init(new PMContext(
                        new File(repConfig.getHomeDir()), fs,
                        RepositoryImpl.ROOT_NODE_ID,
                        null,
                        null,
                        repConfig.getDataStore()));
                Repair repair = new Repair();
                {
                    BundleReader bundleReader = new BundleReader(persistMgr, false, repair);
                    int size = bundleReader.getSize();
                    log.info("Traversing through " + size + " bundles");
                    Iterable<NodeDescription> iterable = Coroutine.<NodeDescription>toIterable(bundleReader, size, progress);
                    //traverse.checkVersionBundles(iterable);
                    clean &= traverse.checkBundles(iterable, repair);
                    if (fix) {
                        repair.perform(bundleReader);
                    }
                }
                switch(repair.getStatus()) {
                    case RECHECK:
                        log.warn("Repaired repository but another check is required");
                        break;
                    case PENDING:
                        log.info("Repaired repository");
                        break;
                    case FAILURE:
                        log.error("FAILED TO REPAIR REPOSITORY");
                        break;
                }
                {
                    ReferencesReader referenceReader = new ReferencesReader(persistMgr);
                    Iterable<NodeReference> iterable = Coroutine.<NodeReference>toIterable(referenceReader, referenceReader.getSize(), progress);
                    clean &= traverse.checkReferences(iterable);
                    if(fix) {
                        // repair.perform(referenceReader);
                    }
                }
                Access.close(persistMgr);
                persistMgr = null;
            }
            /*{
            IndicesReader indicesReader = new IndicesReader(new File(repConfig.getHomeDir()));
            Iterable<NodeIndexed> iterable = Coroutine.<NodeIndexed>toIterable(indicesReader, indicesReader.getSize(), progress);
            Iterable<UUID> corrupted = traverse.checkIndices(iterable);
            }*/
        } catch (RepositoryException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        } finally {
            if (persistMgr != null) {
                Access.close(persistMgr);
            }
        }
        return clean;
    }
}
