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

import java.io.PrintStream;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public class Traverse {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final Set<UUID> all = createSetAllNodes();
    protected final Set<UUID> version = createSetAllNodes();
    protected final Bag<UUID, UUID> childParentRelation = createChildParentBag();
    protected final Bag<UUID, UUID> parentChildRelation = createParentChildBag();
    protected final Bag<UUID, UUID> sourceTargetRelation = createSourceTargetBag();

    PrintStream progressOut = System.out;
    PrintStream reportOut = System.err;
    PrintStream verboseOut = System.err;

    protected Bag<UUID, UUID> createChildParentBag() {
        return new BagImpl<UUID, UUID>(new TreeMap<UUID, Collection<UUID>>(), TreeSet.class);
    }

    protected Bag<UUID, UUID> createParentChildBag() {
        return new BagImpl<UUID, UUID>(new TreeMap<UUID, Collection<UUID>>(), TreeSet.class);
    }

    protected Bag<UUID, UUID> createSourceTargetBag() {
        return new BagImpl<UUID, UUID>(new TreeMap<UUID, Collection<UUID>>(), TreeSet.class);
    }
    protected Bag<UUID, UUID> createSourceTargetCopyBag(Bag<UUID,UUID> original) {
        if(original == null) {
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
                // System.err.println("  nonexistent node " + current + " parented by " + parent);
                needsReindex = true;
            } else {
                if (!parentChildRelation.contains(parent, current)) {
                    needsReindex = true;
                    //System.err.println("  inconsitent node " + current + " not below parent " + parent);
                }
                if (!childParentRelation.contains(current, parent)) {
                    needsReindex = true;
                    //System.err.println("  inconsitent node " + current + " does not have correct parent " + parent);
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
        System.err.println("FOUND " + nonexistentCount + " INDEXED NON EXISTENT NODES");
        System.err.println("FOUND " + misparentedCount + " INDEXED NODES WITH WRONG PARENT");
        System.err.println("FOUND " + unvisited.size() + " UNINDEXED NODES ");
        /*for (UUID current : unvisited) {
            corrupted.add(current);
            System.err.println("  node " + current + " not indexed");
        }*/
        return corrupted;
    }

    void checkReferences(Iterable<NodeReference> references) {
        Set<UUID> missingSources = new TreeSet<UUID>();
        Set<UUID> missingTargets = new TreeSet<UUID>();
        Bag<UUID, UUID> refs = createSourceTargetCopyBag(sourceTargetRelation);
        for (NodeReference item : references) {
            if (!all.contains(item.getSource())) {
                missingSources.add(item.getSource());
            } else if (!all.contains(item.getTarget())) {
                missingTargets.add(item.getTarget());
            } else if (!refs.contains(item.getSource(), item.getTarget())) {
                //System.err.println("  bad reference from "+item.getSource()+" to "+item.getTarget());
            } else {
                refs.remove(item.getSource(), item.getTarget());
            }
        }
        System.err.println("MISSING REFERENCE SOURCES " + missingSources.size());
        System.err.println("MISSING REFERENCE TARGETS " + missingTargets.size());
        System.err.println("ORPHANED REFERENCES " + refs.size());
    }

    void checkVersionBundles(Iterable<NodeDescription> allIterable) {
        System.err.println("Reading version bundle information");
        for (NodeDescription item : allIterable) {
            version.add(item.getNode()); // all.add(item.getNode());
        }
    }

    void checkBundles(Iterable<NodeDescription> allIterable) {
        System.err.println("Reading bundle information");
        for (NodeDescription item : allIterable) {
            all.add(item.getNode());
            childParentRelation.put(item.getNode(), item.getParent());
            parentChildRelation.addAll(item.getNode(), item.getChildren());
            sourceTargetRelation.addAll(item.getNode(), item.getReferences());
        }

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
                    System.out.println("Traversing up");

                    Progress progress = new Progress(unvisited.size());
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
                        UUID parent = (childParentRelation.containsKey(current) && childParentRelation.containsKey(current) ? childParentRelation.getFirst(current) : null);
                        if (parent != null && !parent.equals(DatabaseDelegate.nullUUID)) {
                            if (!all.contains(parent)) {
                                orphaned.add(current);
                                visited.add(current);
                                path.clear();
                                current = null;
                                continue;
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
                System.err.println("FOUND " + roots.size() + " " + (roots.size() == 1 ? "ROOT" : " ROOTS"));
                /* for (UUID root: roots) {
                    System.err.println("  "+root);
                } */
                System.err.println("FOUND " + cyclic.size() + " CYCLIC REFERENCES");
                for (UUID cycleStart : cyclic) {
                    System.err.println("Cyclic reference:");
                    List<UUID> cycle = new LinkedList<UUID>();
                    for (UUID current = cycleStart; !cycle.contains(current); current = childParentRelation.getFirst(current)) {
                        System.err.println("  " + current);
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
                System.err.println("FOUND " + orphaned.size() + " ORPHANED PATHS");
                if (orphaned.size() > 0) {
                    /*System.err.println("Orphaned reference:");
                    for (UUID orphan : orphaned) {
                        System.err.println("  orphaned node "+ orphan);
                    }*/
                    /*for (UUID orphanedStart : orphaned) {
                        LinkedList<UUID> orphanedPath = new LinkedList<UUID>();
                        for (UUID current = orphanedStart; current != null; current = parentChildRelation.getFirst(current)) {
                            orphanedPath.addFirst(current);
                        }
                        for (UUID current : orphanedPath) {
                            System.err.println("  " + current);
                        }
                    }*/
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
                System.err.println("FOUND " + missing.size() + " MISSING CHILD NODES");
                /*for (Map.Entry<UUID, UUID> entry : missing.entrySet()) {
                    UUID child = entry.getKey();
                    UUID parent = entry.getValue();
                    System.err.println("  node " + parent + " references inexistent child " + child);
                }*/
                System.err.println("FOUND " + disconnected.size() + " DISCONNECTED CHILD NODES");
                for (Map.Entry<UUID, UUID> entry : disconnected.entrySet()) {
                    UUID child = entry.getKey();
                    UUID parent = entry.getValue();
                    UUID childParent = childParentRelation.getFirst(child);
                    /*if (parentChildRelation.contains(childParent, child)) {
                        System.err.println("  node " + parent + " references child " + child + " which is correctly located at parent " + childParent);
                    } else if (all.contains(parent)) {
                        System.err.println("  node " + parent + " references child " + child + " which is incorrectly located at parent " + childParent);
                    } else {
                        System.err.println("  node " + parent + " references child " + child + " which is orphaned located at parent " + childParent);
                    }*/
                }
            }
        }

        Bag<UUID, UUID> danglingReferences = createSourceTargetCopyBag(null);
        for(Map.Entry<UUID,UUID> reference : sourceTargetRelation) {
            UUID source = reference.getKey();
            UUID target = reference.getValue();
            if(!all.contains(target) && !version.contains(target)) {
                danglingReferences.put(source, target);
            }
        }
        System.err.println("FOUND " + danglingReferences.size() + " DANGLING REFERENCES");
        /*for(Map.Entry<UUID,UUID> reference : danglingReferences) {
            UUID source = reference.getKey();
            UUID target = reference.getValue();
            System.err.println("  dangling reference from "+source+" to "+target);
        }*/
    }
}

/*
cafebabe-cafe-babe-cafe-babecafebabe - the root node
deadbeef-cafe-babe-cafe-babecafebabe - /jcr:system
deadbeef-face-babe-cafe-babecafebabe - /jcr:system/jcr:versionStorage
deadbeef-cafe-cafe-cafe-babecafebabe - /jcr:system/jcr:nodeTypes
 */
