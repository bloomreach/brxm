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

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import org.apache.jackrabbit.core.RepositoryImpl;

public class Traverse {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    protected final Set<UUID> all = createSetAllNodes();
    protected final Set<UUID> version = createSetAllNodes();
    protected final Bag<UUID, UUID> childParentRelation = createChildParentBag();
    protected final Bag<UUID, UUID> parentChildRelation = createParentChildBag();
    protected final Bag<UUID, UUID> sourceTargetRelation = createSourceTargetBag();

    protected Bag<UUID, UUID> createChildParentBag() {
        return new BagImpl<UUID, UUID>(new TreeMap<UUID, Collection<UUID>>(), TreeSet.class);
    }

    protected Bag<UUID, UUID> createParentChildBag() {
        return new BagImpl<UUID, UUID>(new TreeMap<UUID, Collection<UUID>>(), TreeSet.class);
    }

    protected Bag<UUID, UUID> createSourceTargetBag() {
        return new BagImpl<UUID, UUID>(new TreeMap<UUID, Collection<UUID>>(), TreeSet.class);
    }

    protected Bag<UUID, UUID> createSourceTargetCopyBag(Bag<UUID, UUID> original) {
        if (original == null) {
            return new BagImpl<UUID, UUID>(new TreeMap<UUID, Collection<UUID>>(), TreeSet.class);
        } else {
            return new BagImpl<UUID, UUID>(new TreeMap<UUID, Collection<UUID>>(), TreeSet.class, original);
        }
    }

    protected Set<UUID> createSetAllNodes() {
        return new TreeSet<UUID>();
    }

    protected Set<UUID> createSetUnvisitedNodes() {
        return new TreeSet<UUID>();
    }

    protected Set<UUID> createSetVisitedNodes() {
        return new TreeSet<UUID>();
    }

    boolean check() {
        for (Map.Entry<UUID, UUID> entry : parentChildRelation) {
            UUID parent = entry.getKey();
            UUID child = entry.getValue();
            if (!childParentRelation.contains(child, parent)) {
                return false;
            }
        }
        for (Map.Entry<UUID, UUID> entry : childParentRelation) {
            UUID child = entry.getKey();
            UUID parent = entry.getValue();
            if (!parentChildRelation.contains(parent, child)) {
                return false;
            }
        }
        return true;
    }

    Iterable<UUID> checkIndices(Iterable<NodeIndexed> allIterable) {
        int count = 0;
        int nonexistentCount = 0;
        int misparentedCount = 0;

        Set<UUID> unvisited = new TreeSet<UUID>();
        unvisited.addAll(all);
        Set<UUID> corrupted = new TreeSet<UUID>();
        for (NodeIndexed item : allIterable) {
            UUID current = item.getNode();
            UUID parent = item.getParent();
            ++count;
            boolean needsReindex = false;
            if (!all.contains(current)) {
                ++nonexistentCount;
                Checker.log.warn("  nonexistent node " + current + " parented by " + parent);
                needsReindex = true;
            } else {
                if (!parentChildRelation.contains(parent, current)) {
                    needsReindex = true;
                    Checker.log.warn("  inconsitent node " + current + " not below parent " + parent);
                }
                if (!childParentRelation.contains(current, parent)) {
                    needsReindex = true;
                    Checker.log.warn("  inconsitent node " + current + " does not have correct parent " + parent);
                }
                if (needsReindex) {
                    ++misparentedCount;
                }
            }
            unvisited.remove(current);
            if (needsReindex) {
                corrupted.add(current);
            }
        }
        if (nonexistentCount > 0) {
            Checker.log.warn("FOUND " + nonexistentCount + " INDEXED NON EXISTENT NODES");
        } else {
            Checker.log.info("FOUND " + nonexistentCount + " INDEXED NON EXISTENT NODES");
        }
        if (misparentedCount > 0) {
            Checker.log.warn("FOUND " + misparentedCount + " INDEXED NODES WITH WRONG PARENT");
        } else {
            Checker.log.info("FOUND " + misparentedCount + " INDEXED NODES WITH WRONG PARENT");
        }
        if (unvisited.size() > 0) {
            Checker.log.warn("FOUND " + unvisited.size() + " UNINDEXED NODES ");
        } else {
            Checker.log.info("FOUND " + unvisited.size() + " UNINDEXED NODES ");
        }
        /*for (UUID current : unvisited) {
        corrupted.add(current);
        Checker.log.warn("  node " + current + " not indexed");
        }*/
        return corrupted;
    }

    boolean checkReferences(Iterable<NodeReference> references) {
        boolean clean = true;
        Set<UUID> missingSources = new TreeSet<UUID>();
        Set<UUID> missingTargets = new TreeSet<UUID>();
        Bag<UUID, UUID> missingReferences = createSourceTargetBag();
        Bag<UUID, UUID> existingReferences = createSourceTargetCopyBag(sourceTargetRelation);
        int count = 0;
        for (NodeReference item : references) {
            if (!all.contains(item.getSource())) {
                missingSources.add(item.getSource());
            } else if (!all.contains(item.getTarget())) {
                missingTargets.add(item.getTarget());
            } else if (!existingReferences.contains(item.getSource(), item.getTarget())) {
                missingReferences.put(item.getSource(), item.getTarget());
            } else {
                existingReferences.remove(item.getSource(), item.getTarget());
            }
            count++;
            if (count % 1000 == 0) {
                Checker.log.info("Checked " + count + " references...");
            }
        }
        if (count % 1000 != 0) {
            Checker.log.info("Checked " + count + " references...");
        }
        if (missingReferences.size() > 0) {
            clean = false;
            Checker.log.warn("MISSING REFERENCES " + missingReferences.size());
            for(Map.Entry<UUID,UUID> reference : missingReferences) {
                Checker.log.warn("  bad reference from "+reference.getKey()+" to "+reference.getValue());
            }
        } else {
            Checker.log.info("MISSING REFERENCES " + missingSources.size());
        }
        if (missingSources.size() > 0) {
            clean = false;
            Checker.log.warn("MISSING REFERENCE SOURCES " + missingSources.size());
            for(UUID referer : missingSources) {
                Checker.log.warn("  referring "+referer+" no longer present");
            }
        } else {
            Checker.log.info("MISSING REFERENCE SOURCES " + missingSources.size());
        }
        if (missingTargets.size() > 0) {
            clean = false;
            Checker.log.warn("MISSING REFERENCE TARGETS " + missingTargets.size());
            for(UUID referenced : missingTargets) {
                Checker.log.warn("  referenced "+referenced+" no longer present");
            }
        } else {
            Checker.log.info("MISSING REFERENCE TARGETS " + missingTargets.size());
        }
        /*
        if (existingReferences.size() > 0) {
            Checker.log.warn("ORPHANED REFERENCES " + existingReferences.size());
            for(Map.Entry<UUID,UUID> reference : existingReferences) {
                Checker.log.warn("  reference source "+reference.getKey()+" target "+reference.getValue()+" no longer present");
            }
        } else {
            Checker.log.info("ORPHANED REFERENCES " + existingReferences.size());
        }
        */
        return clean;
    }

    boolean checkVersionBundles(Iterable<NodeDescription> allIterable, Repair repair) {
        Checker.log.info("Reading version bundle information");
        int count = 0;
        for (NodeDescription item : allIterable) {
            version.add(item.getNode()); // all.add(item.getNode());
            count++;
            if (count % 1000 == 0) {
                Checker.log.info("Read " + count + " version bundles...");
            }
        }
        if (count % 1000 != 0) {
            Checker.log.info("Read " + count + " version bundles...");
        }
        return true;
    }

    boolean checkBundles(Iterable<NodeDescription> allIterable, Repair repair) {
        boolean clean = true;
        Checker.log.info("Reading bundle information");
        int count = 0;
        for (NodeDescription item : allIterable) {
            all.add(item.getNode());
            childParentRelation.put(item.getNode(), item.getParent());
            parentChildRelation.addAll(item.getNode(), item.getChildren());
            sourceTargetRelation.addAll(item.getNode(), item.getReferences());
            count++;
            if (count % 1000 == 0) {
                Checker.log.info("Read " + count + " bundles...");
            }
        }
        if (count % 1000 != 0) {
            Checker.log.info("Read " + count + " bundles...");
        }

        Set<UUID> abandoned = new TreeSet<UUID>();

        Set<UUID> roots = new TreeSet<UUID>();
        {
            Set<UUID> cyclic = new TreeSet<UUID>();
            {
                Set<UUID> orphaned = new TreeSet<UUID>();
                {
                    List<UUID> path = new LinkedList<UUID>();
                    Set<UUID> unvisited = createSetUnvisitedNodes();
                    Set<UUID> visited = createSetVisitedNodes();
                    unvisited.addAll(all);

                    Progress progress = new Progress(unvisited.size());
                    progress.setLogger(Checker.log);
                    for (UUID current = null; !unvisited.isEmpty() || current != null;) {
                        if (current == null) {
                            if (progress.needsUpdate()) {
                                progress.setProgress(unvisited.size());
                            }
                            Iterator<UUID> iter = unvisited.iterator();
                            if (iter.hasNext()) {
                                current = iter.next();
                                iter.remove();
                            }
                        }
                        UUID parent = (childParentRelation.containsKey(current) ? childParentRelation.getFirst(current) : null);
                        if (parent != null && !parent.equals(DatabaseDelegate.nullUUID)) {
                            if (!all.contains(parent)) {
                                orphaned.add(current);
                                visited.add(current);
                                path.clear();
                                current = null;
                                continue;
                            } else {
                                boolean found = false;
                                for (UUID child : parentChildRelation.get(parent)) {
                                    if (child.equals(current)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    abandoned.add(current);
                                }
                            }
                        }
                        if (parent == null || parent.equals(DatabaseDelegate.nullUUID)) {
                            roots.add(current);
                            visited.add(current);
                            path.clear();
                            current = null;
                        } else if (visited.contains(parent)) {
                            if (path.contains(current)) {
                                cyclic.add(current);
                                assert (visited.contains(current));
                                path.clear();
                                current = null;
                            } else {
                                visited.add(current);
                                path.clear();
                                current = null;
                            }
                        } else {
                            assert (unvisited.contains(parent));
                            unvisited.remove(parent);
                            path.add(current);
                            visited.add(current);
                            current = parent;
                        }
                    }
                    progress.close();
                    assert (visited.size() == all.size());
                    assert (unvisited.size() == 0);
                }
                Map<UUID, UUID> missing = new TreeMap<UUID, UUID>();
                Map<UUID, UUID> disconnected = new TreeMap<UUID, UUID>();
                for (Map.Entry<UUID, UUID> entry : parentChildRelation) {
                    UUID parent = entry.getKey();
                    UUID child = entry.getValue();
                    if (childParentRelation.get(child) == null) {
                        missing.put(child, parent);
                    } else if (!childParentRelation.contains(child, parent)) {
                        disconnected.put(child, parent);
                    }
                }
                missing.remove(UUID.fromString(RepositoryImpl.NODETYPES_NODE_ID.toString()));
                missing.remove(UUID.fromString(RepositoryImpl.ACTIVITIES_NODE_ID.toString()));
                missing.remove(UUID.fromString(RepositoryImpl.VERSION_STORAGE_NODE_ID.toString()));
                for (Iterator<UUID> iter=orphaned.iterator(); iter.hasNext(); ) {
                    UUID orphan = iter.next();
                    if (disconnected.containsKey(orphan)) {
                        iter.remove();
                    }
                }
                if(roots.size() != 1) {
                    Checker.log.warn("FOUND " + roots.size() + " ROOTS");
                } else {
                    Checker.log.info("FOUND SINGLE ROOT " + roots.iterator().next().toString());
                }
                if (roots.size() != 1) {
                    clean = false;
                    repair.report(Repair.RepairStatus.FAILURE);
                    for (UUID root: roots) {
                        Checker.log.warn("  Duplicate root "+root.toString());
                    }
                }
                if (cyclic.size() > 0) {
                    Checker.log.warn("FOUND "+cyclic.size() + " CYCLIC PATHS");
                    clean = false;
                    for (UUID cycleStart : cyclic) {
                        Checker.log.warn("Cyclic reference:");
                        List<UUID> cycle = new LinkedList<UUID>();
                        for (UUID current = cycleStart; !cycle.contains(current); current = childParentRelation.getFirst(current)) {
                            Checker.log.warn("  " + current);
                            cycle.add(current);
                        }
                        boolean cycleBroken = false;
                        for (UUID current : cycle) {
                            if (!parentChildRelation.contains(childParentRelation.getFirst(current), current)) {
                                childParentRelation.remove(current);
                                cycleBroken = true;
                                orphaned.add(current);
                            }
                        }
                        if (!cycleBroken) {
                            UUID arbritraty = cycle.iterator().next();
                            childParentRelation.remove(arbritraty);
                        }
                    }
                } else {
                    Checker.log.info("FOUND "+cyclic.size() + " CYCLIC PATHS");
                }
                if (orphaned.size() > 0) {
                    Checker.log.warn("FOUND " + orphaned.size() + " ORPHANED NODES");
                    clean = false;
                    Checker.log.warn("Orphaned reference:");
                    for (UUID orphan : orphaned) {
                        Checker.log.warn("  orphaned node " + orphan);
                        repair.removeNode(Repair.RepairStatus.RECHECK, orphan);
                    }
                    for (UUID orphanedStart : orphaned) {
                        LinkedList<UUID> orphanedPath = new LinkedList<UUID>();
                        for (UUID current = orphanedStart; current != null; current = parentChildRelation.getFirst(current)) {
                            orphanedPath.addFirst(current);
                        }
                        for (UUID current : orphanedPath) {
                            Checker.log.warn("    " + current);
                        }
                    }
                } else {
                    Checker.log.info("FOUND " + orphaned.size() + " ORPHANED NODES");
                }
                if (abandoned.size() > 0) {
                    Checker.log.warn("FOUND " + abandoned.size() + " ABANDONED NODES");
                    clean = false;
                    Checker.log.warn("Abandoned reference:");
                    for (UUID abandon : abandoned) {
                        Checker.log.warn("  abandoned node " + abandon + " by parent " + childParentRelation.getFirst(abandon));
                        repair.removeNode(Repair.RepairStatus.RECHECK, abandon);
                    }
                } else {
                    Checker.log.info("FOUND " + abandoned.size() + " ABANDONED NODES");
                }
                if (missing.size() > 0) {
                    Checker.log.warn("FOUND " + missing.size() + " MISSING CHILD NODES");
                    clean = false;
                    for (Map.Entry<UUID, UUID> entry : missing.entrySet()) {
                        UUID child = entry.getKey();
                        UUID parent = entry.getValue();
                        Checker.log.warn("  node " + parent + " references inexistent child " + child);
                        repair.unlistChild(Repair.RepairStatus.PENDING, parent, child);
                    }
                } else {
                    Checker.log.info("FOUND " + missing.size() + " MISSING CHILD NODES");                    
                }
                if (disconnected.size() > 0) {
                    Checker.log.warn("FOUND " + disconnected.size() + " DISCONNECTED CHILD NODES");
                    for (Map.Entry<UUID, UUID> entry : disconnected.entrySet()) {
                        UUID child = entry.getKey();
                        UUID parent = entry.getValue();
                        UUID childParent = childParentRelation.getFirst(child);
                        if (parentChildRelation.contains(childParent, child)) {
                            // FIXME: there are causes when a parent node can still reference a moved child node.
                            // This is transparent to jackrabbit and causes no errors and is corrected automatically.
                            // but this needs to be sorted out
                            // clean = false;
                            Checker.log.warn("  node " + parent + " references child " + child + " which is correctly located at parent " + childParent);
                            repair.unlistChild(Repair.RepairStatus.PENDING, parent, child);
                        } else if (all.contains(parent)) {
                            clean = false;
                            Checker.log.warn("  node " + parent + " references child " + child + " which is incorrectly located at parent " + childParent);
                            repair.fixParent(Repair.RepairStatus.RECHECK, child, parent);
                        } else {
                            clean = false;
                            Checker.log.warn("  node " + parent + " references child " + child + " which is orphaned located at parent " + childParent);
                        }
                    }
                } else {
                    Checker.log.info("FOUND " + disconnected.size() + " DISCONNECTED CHILD NODES");
                }
            }
        }

        Bag<UUID, UUID> danglingReferences = createSourceTargetCopyBag(null);
        for (Map.Entry<UUID, UUID> reference : sourceTargetRelation) {
            UUID source = reference.getKey();
            UUID target = reference.getValue();
            if (!all.contains(target) && !version.contains(target)) {
                danglingReferences.put(source, target);
            }
        }
        if (danglingReferences.size() > 0) {
            Checker.log.warn("FOUND " + danglingReferences.size() + " DANGLING REFERENCES");
            clean = false;
            for(Map.Entry<UUID,UUID> reference : danglingReferences) {
                UUID source = reference.getKey();
                UUID target = reference.getValue();
                Checker.log.warn("  dangling reference from "+source+" to "+target);
                repair.removeReference(Repair.RepairStatus.PENDING, source, target);
            }
        } else {
            Checker.log.info("FOUND " + danglingReferences.size() + " DANGLING REFERENCES");
        }
        return clean;
    }
}
