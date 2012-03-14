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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.VersioningConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.pool.Access;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle.ChildNodeEntry;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
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
            NamespaceRegistry nsReg = new NamespaceRegistryImpl(fs);
            Traverse traverse = new Traverse();
            {
                VersioningConfig wspConfig = repConfig.getVersioningConfig();
                PersistenceManagerConfig pmConfig = wspConfig.getPersistenceManagerConfig();
                persistMgr = pmConfig.newInstance(PersistenceManager.class);
                persistMgr.init(new PMContext(
                        new File(repConfig.getHomeDir()), fs,
                        RepositoryImpl.ROOT_NODE_ID,
                        nsReg, null,
                        repConfig.getDataStore()));
                Repair repair = new Repair();
                {
                    BundleReader bundleReader = new BundleReader(persistMgr, false, repair);
                    log.info("Traversing bundles");
                    Iterable<NodeDescription> iterable = Coroutine.<NodeDescription>toIterable(bundleReader, progress);
                    clean &= traverse.checkVersionBundles(iterable, repair);
                    if (fix) {
                        repair.perform(bundleReader);
                    }
                }
            }
            for (WorkspaceConfig wspConfig : repConfig.getWorkspaceConfigs()) {
                log.info("Checking workspace with name: '" + wspConfig.getName() +"'");
                PersistenceManagerConfig pmConfig = wspConfig.getPersistenceManagerConfig();
                persistMgr = pmConfig.newInstance(PersistenceManager.class);
                persistMgr.init(new PMContext(
                        new File(repConfig.getHomeDir()), fs,
                        RepositoryImpl.ROOT_NODE_ID,
                        nsReg, null,
                        repConfig.getDataStore()));
                log.info("Initialized persistence manager: " + persistMgr.getClass().getName());
                Repair repair = new Repair();
                {
                    BundleReader bundleReader = new BundleReader(persistMgr, false, repair);
                    log.info("Traversing bundles");
                    Iterable<NodeDescription> iterable = Coroutine.<NodeDescription>toIterable(bundleReader, progress);
                    //traverse.checkVersionBundles(iterable);
                    clean &= traverse.checkBundles(iterable, repair);
                    if (fix) {
                        repair.perform(bundleReader);
                    }
                }
                if (fix) {
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
                }
                {
                    ReferencesReader referenceReader = new ReferencesReader(persistMgr);
                    log.info("Traversing references");
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
            log.error("An exception occurred while trying to check the repository. ", ex);
            return false;
        } catch (Exception ex) {
            log.error("An exception occurred while trying to check the repository. ", ex);
            return false;
        } finally {
            if (persistMgr != null) {
                Access.close(persistMgr);
            }
        }
        return clean;
    }

    static enum SanityCheckerMode {
        TRUE("true"),
        FALSE("false"),
        CORRUPTMIXINPROPCLEAR("corruptMixinPropClear"),
        CORRUPTMIXINPROPDROP("corruptMixinPropDrop"),
        CORRUPTMIXINSET("corruptMixinSetClear"),
        FIXMIXINSET("fixMixinSetTo"),
        FIXMIXINSETFROMPROP("fixMixinSetFromProp"),
        FIXCHILDRENOFMIXINSET("fixChildrenOfMixinSetTo"),
        FIXCHILDRENOFMIXINSETFROMPROP("fixChildrenOfMixinSetFromProp"),
        UNLISTCHILDREN("unlistChildren"),
        DUMP("dump");
        private String name;

        SanityCheckerMode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public boolean checkRepository(String[] arguments) {
        if (arguments.length == 0) {
            return true;
        }
        log.info("Checking repository with arguments: {}", Arrays.toString(arguments));
        SanityCheckerMode mode = null;
        for (SanityCheckerMode candidateMode : SanityCheckerMode.values()) {
            if (candidateMode.getName().equals(arguments[0])) {
                mode = candidateMode;
            }
        }
        if (mode == null) {
            return false;
        }
        Set<String> checkerUUIDs = null;
        Set<Name> checkerNames = null;
        log.info("Running checker with mode: {}", mode);
        switch (mode) {
            case TRUE:
                return check(true);
            case FALSE:
                return check(false);
            case CORRUPTMIXINPROPCLEAR:
            case CORRUPTMIXINPROPDROP:
            case CORRUPTMIXINSET:
            case FIXMIXINSETFROMPROP:
            case FIXCHILDRENOFMIXINSET:
            case FIXCHILDRENOFMIXINSETFROMPROP:
            case UNLISTCHILDREN:
            case DUMP:
                if (arguments.length > 1) {
                    checkerUUIDs = new HashSet<String>();
                    checkerUUIDs.addAll(Arrays.asList(Arrays.copyOfRange(arguments, 1, arguments.length)));
                    if ("all".equals(arguments[1])) {
                        checkerUUIDs.clear();
                    }
                }
                break;
            case FIXMIXINSET:
                if (arguments.length > 1) {
                    checkerUUIDs = new LinkedHashSet<String>();
                    checkerUUIDs.add(arguments[1]);
                    NameFactory nameFactory = NameFactoryImpl.getInstance();
                    checkerNames = new HashSet<Name>();
                    for(int i=2; i<arguments.length; i++) {
                        checkerNames.add(nameFactory.create(arguments[i]));
                    }
                }
                break;
        }

        Progress progress = new Progress();
        progress.setLogger(log);
        PersistenceManager persistMgr = null;
        try {
            FileSystem fs = repConfig.getFileSystem();
            NamespaceRegistry nsReg = new NamespaceRegistryImpl(fs);
            for (WorkspaceConfig wspConfig : repConfig.getWorkspaceConfigs()) {
                log.info("Checking workspace with name: '" + wspConfig.getName() +"'");
                PersistenceManagerConfig pmConfig = wspConfig.getPersistenceManagerConfig();
                persistMgr = pmConfig.newInstance(PersistenceManager.class);
                persistMgr.init(new PMContext(
                        new File(repConfig.getHomeDir()), fs,
                        RepositoryImpl.ROOT_NODE_ID,
                        nsReg, null,
                        repConfig.getDataStore()));
                log.info("Initialized persistence manager: " + persistMgr.getClass().getName());
                Repair repair = new Repair();
                {
                    BundleReader bundleReader = new BundleReader(persistMgr, false, repair);
                    log.info("Traversing through bundles");
                    bundleReader.accept(new SanityChecker(repair, mode, checkerUUIDs, checkerNames));
                    repair.perform(bundleReader);
                }
                Access.close(persistMgr);
                persistMgr = null;
            }
            return true;
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName(), ex);
            return false;
        } catch (Exception ex) {
            log.error(ex.getClass().getName(), ex);
            return false;
        } finally {
            if (persistMgr != null) {
                Access.close(persistMgr);
            }
        }
    }

    static class SanityChecker implements Visitor<NodeDescription> {

        SanityCheckerMode mode;
        Repair repair;
        Set<String> repairSet;
        Set<Name> names;

        public SanityChecker(Repair repair, SanityCheckerMode mode, Set<String> repairSet, Set<Name> names) {
            this.repair = repair;
            this.mode = mode;
            this.repairSet = repairSet;
            this.names = names;
        }

        public void visit(NodeDescription nodeDescription) {
            BundleReader.NodeDescriptionImpl node = (BundleReader.NodeDescriptionImpl) nodeDescription;
            boolean skip = true;
            switch (mode) {
                case CORRUPTMIXINPROPCLEAR:
                case CORRUPTMIXINPROPDROP:
                case CORRUPTMIXINSET:
                case FIXMIXINSETFROMPROP:
                case FIXMIXINSET:
                case DUMP:
                    if (repairSet == null || repairSet.contains(node.getNode().toString())) {
                        skip = false;
                    }
                    break;
                case FIXCHILDRENOFMIXINSET:
                case FIXCHILDRENOFMIXINSETFROMPROP:
                case UNLISTCHILDREN:
                    if (repairSet == null || repairSet.contains(node.getParent().toString())) {
                        skip = false;
                    }
                    break;
            }
            if (skip) {
                return;
            }
            switch (mode) {
                case CORRUPTMIXINPROPCLEAR:
                case CORRUPTMIXINPROPDROP:
                case FIXMIXINSETFROMPROP:
                case FIXCHILDRENOFMIXINSETFROMPROP:
                    for (Name name : node.bundle.getPropertyNames()) {
                        if ("mixinTypes".equals(name.getLocalName()) && "http://www.jcp.org/jcr/1.0".equals(name.getNamespaceURI())) {
                            if(repairSet == null) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("{").append(name.getNamespaceURI()).append("}").append(name.getLocalName());
                                for (InternalValue value : node.bundle.getPropertyEntry(name).getValues()) {
                                    try {
                                        Name nameValue = value.getName();
                                        sb.append("{").append(nameValue.getNamespaceURI()).append("}").append(nameValue.getLocalName());
                                    } catch (RepositoryException ex) {
                                        log.error("An exception occurred while trying to print the values. ", ex);
                                    }
                                }
                                System.err.println(node.getNode() + " " + sb.toString());
                            }
                            if (repairSet != null) {
                                switch (mode) {
                                    case CORRUPTMIXINPROPCLEAR:
                                        repair.setProperty(Repair.RepairStatus.PENDING, nodeDescription.getNode(), name, new InternalValue[0]);
                                        break;
                                    case CORRUPTMIXINPROPDROP:
                                        repair.setProperty(Repair.RepairStatus.PENDING, nodeDescription.getNode(), name, null);
                                        break;
                                    case FIXMIXINSETFROMPROP:
                                    case FIXCHILDRENOFMIXINSETFROMPROP:
                                        try {
                                            Set<Name> newMixinNames = new HashSet<Name>();
                                            for(InternalValue nameValue : node.bundle.getPropertyEntry(name).getValues()) {
                                                newMixinNames.add(nameValue.getName());
                                            }
                                            repair.setMixins(Repair.RepairStatus.PENDING, nodeDescription.getNode(), newMixinNames);
                                        } catch(RepositoryException ex) {
                                            ex.printStackTrace();
                                        }
                                        break;
                                }
                            }
                        }
                    }
                    break;
                case UNLISTCHILDREN:
                    if (repairSet != null) {
                        for (Iterator<NodePropBundle.ChildNodeEntry> iter = node.bundle.getChildNodeEntries().iterator(); iter.hasNext();) {
                            NodePropBundle.ChildNodeEntry childEntry = iter.next();
                            repair.unlistChild(Repair.RepairStatus.PENDING, node.getNode(), DatabaseDelegate.create(childEntry.getId()));
                        }
                    }
                    break;
                case DUMP:
                    if (repairSet != null) {
                        StringBuffer sb = new StringBuffer();
                        sb.append("node\t").append(node.getNode()).append("\n");
                        sb.append(" parent\t").append(node.getParent()).append("\n");
                        sb.append(" modcount\t").append(node.bundle.getModCount()).append("\n");
                        sb.append(" isreferenceable\t").append(node.bundle.isReferenceable()).append("\n");
                        sb.append(" nodetype\t").append(node.bundle.getNodeTypeName()).append("\n");
                        sb.append(" mixintypes\n");
                        for (Name name : node.bundle.getMixinTypeNames()) {
                            sb.append("    ").append(name.toString()).append("\n");
                        }
                        sb.append(" children\n");
                        for (ChildNodeEntry entry : node.bundle.getChildNodeEntries()) {
                            sb.append("    ").append(entry.getName().toString()).append(":").append(entry.getId()).append("\n");
                        }
                        sb.append("  properties\n");
                        for (Name name : node.bundle.getPropertyNames()) {
                            NodePropBundle.PropertyEntry propertyEntry = node.bundle.getPropertyEntry(name);
                            sb.append("    ").append("type : ").append(PropertyType.nameFromValue(propertyEntry.getType())).append(", name : ").append(name.toString()).append("\n");
                            for (InternalValue value : propertyEntry.getValues()) {
                                sb.append("      ").append(value.toString());
                                sb.append("\n");
                            }
                        }
                        System.err.println(sb.toString());
                    } else {
                        System.err.println(node.getNode());
                    }
                    break;
                case CORRUPTMIXINSET:
                    System.err.println(node.getNode());
                    repair.setMixins(Repair.RepairStatus.PENDING, nodeDescription.getNode(), Collections.EMPTY_SET);
                    break;
                case FIXCHILDRENOFMIXINSET:
                case FIXMIXINSET:
                    System.err.println(node.getNode());
                    repair.setMixins(Repair.RepairStatus.PENDING, nodeDescription.getNode(), names);
                    break;
            }
        }
    }
}
