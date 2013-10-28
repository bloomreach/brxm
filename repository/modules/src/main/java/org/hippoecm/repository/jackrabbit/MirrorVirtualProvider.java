/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.HippoVirtualProvider;
import org.hippoecm.repository.dataprovider.IFilterNodeId;
import org.hippoecm.repository.dataprovider.MirrorNodeId;
import org.hippoecm.repository.dataprovider.StateProviderContext;
import org.hippoecm.repository.dataprovider.ViewNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirrorVirtualProvider extends HippoVirtualProvider
{
    private static final Logger log = LoggerFactory.getLogger(MirrorVirtualProvider.class);

    private Name facetSelectName;
    private Name handleName;
    private Name requestName;
    private Name translationName;
    private Name docbaseName;
    private Name jcrUUIDName;
    private Name jcrMixinTypesName;
    private Name hippoUUIDName;
    private Name documentName;
    private Name hardHandleName;
    private Name softDocumentName;
    private Name softHandleName;
    private Name mixinReferenceableName;
    private Name mixinVersionableName;
    private Set<Name> omittedProperties;

    @Override
    protected void initialize() throws RepositoryException {

        facetSelectName = resolveName(HippoNodeType.NT_FACETSELECT);
        handleName = resolveName(HippoNodeType.NT_HANDLE);
        requestName = resolveName(HippoNodeType.NT_REQUEST);
        translationName = resolveName(HippoNodeType.NT_TRANSLATION);
        docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
        jcrUUIDName = resolveName("jcr:uuid");
        jcrMixinTypesName = resolveName("jcr:mixinTypes");
        hippoUUIDName = resolveName(HippoNodeType.HIPPO_UUID);
        documentName = resolveName(HippoNodeType.NT_DOCUMENT);
        hardHandleName = resolveName(HippoNodeType.NT_HARDHANDLE);
        softDocumentName = resolveName(HippoNodeType.NT_SOFTDOCUMENT);
        softHandleName = resolveName("hipposys:softhandle");
        mixinReferenceableName = resolveName("mix:referenceable");
        mixinVersionableName = resolveName("mix:versionable");

        omittedProperties = new HashSet<Name>();
        omittedProperties.add(resolveName("hippo:related"));
        omittedProperties.add(resolveName("jcr:isCheckedOut"));
        omittedProperties.add(resolveName("jcr:versionHistory"));
        omittedProperties.add(resolveName("jcr:baseVersion"));
        omittedProperties.add(resolveName("jcr:predecessors"));
        omittedProperties.add(resolveName("jcr:mergeFailed"));
        omittedProperties.add(resolveName("jcr:activity"));
        omittedProperties.add(resolveName("jcr:configuration"));

    }

    protected Name getDocbaseName() {
        return docbaseName;
    }

    @Override
    public NodeState populate(StateProviderContext context, HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        NodeState dereference = getNodeState(((MirrorNodeId)nodeId).getCanonicalId(), context);
        if(dereference == null) {
            throw new RepositoryException("Cannot populate top mirror node dereferencing "+((MirrorNodeId)nodeId).getCanonicalId());
        }
        final Name nodeTypeName = dereference.getNodeTypeName();
        final NodeTypeRegistry nodeTypeRegistry = getDataProviderContext().getNodeTypeRegistry();
        final EffectiveNodeType effectiveNodeType = nodeTypeRegistry.getEffectiveNodeType(nodeTypeName);

        NodeState state = createNew(nodeId, nodeTypeName, parentId);
        state.setNodeTypeName(nodeTypeName);

        Set<Name> mixins = new HashSet<Name>(dereference.getMixinTypeNames());
        if(mixins.contains(mixinReferenceableName)) {
            mixins.remove(mixinReferenceableName);
        }
        if(mixins.contains(mixinVersionableName)) {
            mixins.remove(mixinVersionableName);
        }
        if (effectiveNodeType.includesNodeType(documentName)) {
            mixins.add(softDocumentName);
        }
        if(effectiveNodeType.includesNodeType(handleName)) {
            mixins.add(softHandleName);
        }
        state.setMixinTypeNames(mixins);

        for(Iterator iter = dereference.getPropertyNames().iterator(); iter.hasNext(); ) {
            Name propName = (Name) iter.next();
            PropertyId upstreamPropId = new PropertyId(dereference.getNodeId(), propName);
            PropertyState upstreamPropState = getPropertyState(upstreamPropId);
            if(propName.equals(jcrUUIDName)) {
                if(mixins.contains(softDocumentName) || mixins.contains(softHandleName)) {
                    propName = hippoUUIDName;
                } else {
                    continue;
                }
            }
            if(omittedProperties.contains(propName))
                continue;
            state.addPropertyName(propName);
            PropertyState propState = createNew(propName, state.getNodeId());
            propState.setType(upstreamPropState.getType());
            if(propName.equals(jcrMixinTypesName)) {
                // replace the jcr:mixinTypes properties with the possibly changed mixin types
                propState.setValues(InternalValue.create((Name[])state.getMixinTypeNames().toArray(new Name[state.getMixinTypeNames().size()])));
            } else {
                propState.setValues(upstreamPropState.getValues());
            }
            propState.setMultiValued(upstreamPropState.isMultiValued());
        }

        populateChildren(context, nodeId, state, dereference);
        return state;
    }

    protected NodeState populate(StateProviderContext context, MirrorVirtualProvider subProvider, NodeState state, String[] docbase, String[] newFacets, String[] newValues, String[] newModes, boolean newCriteria) throws RepositoryException {
        if (docbase == null || docbase.length == 0) {
            return state;
        }

        if (docbase[0].endsWith("babecafebabe")) {
            // one of the defined (and fixed, so string compare is fine) system areas
            return state;
        }
        HippoVirtualProvider provider = lookupProvider(state.getNodeTypeName());
        if (provider != null) {
            // the provider might be of a Faceted Navigation or FacetSearch node, or any provider that has a hippo:docbase. But, we only want to proceed
            // when the provider is of MirrorVirtualProvider. Therefore this very important check
            if (!(provider instanceof MirrorVirtualProvider)) {
                log.debug("The provider for '{}' is not a mirror kind of provider. '{}' is already mirrored. We skip the derefencing of its docbase!");
                return state;
            }
        }
        NodeState dereference = null;
        try {
            dereference = getNodeState(new NodeId(UUID.fromString(docbase[0])), context);
        } catch (IllegalArgumentException e) {
            log.warn("invalid docbase '" + docbase[0] + "' because not a valid UUID ");
        }
        if (dereference != null) {
            boolean singledView = false;
            LinkedHashMap<Name, String> view = new LinkedHashMap<Name, String>();
            LinkedHashMap<Name, String> order = null;

            if (state.getNodeId() instanceof IFilterNodeId) {
                IFilterNodeId filterNodeId = (IFilterNodeId)state.getNodeId();
                if (filterNodeId.getView() != null) {
                    view.putAll(filterNodeId.getView());
                }
                if (filterNodeId.getOrder() != null) {
                    order = new LinkedHashMap<Name, String>(filterNodeId.getOrder());
                }
                singledView = filterNodeId.isSingledView();
            } else if (state.getParentId() != null && state.getParentId() instanceof IFilterNodeId) {
                // parent state is already virtual, inherit possible filter criteria
                IFilterNodeId filterNodeId = ((IFilterNodeId)state.getParentId());
                if (filterNodeId.getView() != null) {
                    view.putAll(filterNodeId.getView());
                }
                if (filterNodeId.getOrder() != null) {
                    if (order == null) {
                        order = new LinkedHashMap<Name, String>();
                    }
                    order.putAll(filterNodeId.getOrder());
                }
                singledView = filterNodeId.isSingledView();
            }

            if (newCriteria) {
                if (newFacets == null || newValues == null || newModes == null || newFacets.length != newValues.length || newFacets.length != newModes.length) {
                    log.warn("Malformed definition of faceted selection: all must be of same length and must exist. Cannot populate facetselect. Return unpopulated mirror");
                    return state;
                }

                if (dereference.getNodeTypeName().equals(facetSelectName)) {
                    // This means this facetselect directly points to another facetselect. This is not allowed.
                    log.warn("Mirror of facetselect is not allowed to have as docbase the uuid of another mirror or facetselect.");
                    return state;
                }

                for (int i = 0; i < newFacets.length; i++) {
                    if (newModes[i].equalsIgnoreCase("stick") || newModes[i].equalsIgnoreCase("select") || newModes[i].equalsIgnoreCase("single")) {
                        view.put(resolveName(newFacets[i]), newValues[i]);
                        if (newModes[i].equalsIgnoreCase("single")) {
                            singledView = true;
                        }
                    } else if (newModes[i].equalsIgnoreCase("prefer") || newModes[i].equalsIgnoreCase("prefer-single")) {
                        if (order == null) {
                            order = new LinkedHashMap<Name, String>();
                        }
                        order.put(resolveName(newFacets[i]), newValues[i]);
                        if (newModes[i].endsWith("prefer-single")) {
                            singledView = true;
                        }
                    } else if (newModes[i].equalsIgnoreCase("clear")) {
                        view.remove(resolveName(newFacets[i]));
                    }
                }
            }

            ViewNodeId.Child[] childrenArray;
            boolean isHandle = dereference.getNodeTypeName().equals(handleName);
            Name parentName = null;
            if (isHandle) {
                // we only need the parentName when referring to a handle
                parentName = getNodeName(dereference, context);
            }
            if (order != null && isHandle) {
                // since the order is not null, we first have to sort all childs according the order. We only order below a handle
                List<ViewNodeId.Child> children = new ArrayList<ViewNodeId.Child>();
                for (ChildNodeEntry entry : dereference.getChildNodeEntries()) {
                    ViewNodeId childNodeId = subProvider.newViewNodeId(state.getNodeId(), parentName, entry.getId(), context, entry.getName(), view, order, singledView);
                    children.add(childNodeId.new Child(entry.getName()));
                }
                childrenArray = children.toArray(new ViewNodeId.Child[children.size()]);
                if (childrenArray.length > 0) {
                    Arrays.sort(childrenArray, childrenArray[0].getValue().new ChildComparator());
                }
            } else {
                List<ViewNodeId.Child> children = new ArrayList<ViewNodeId.Child>();
                for (ChildNodeEntry entry : dereference.getChildNodeEntries()) {
                    // filtering is only applied on handles
                    if (!isHandle || subProvider.match(view, entry.getId())) {
                        if (isHandle && singledView && (entry.getName().equals(requestName))) {
                            continue;
                        } else {
                            // note that below we also add entries that have a getName() equal to translationName! The translation should never be skipped!
                            // The for loops below will make sure the translation node is added at the last child entries
                            ViewNodeId childNodeId = subProvider.newViewNodeId(state.getNodeId(), parentName, entry.getId(), context, entry.getName(), view, order, singledView);
                            children.add(childNodeId.new Child(entry.getName()));
                        }
                    }
                }
                childrenArray = children.toArray(new ViewNodeId.Child[children.size()]);
                if (isHandle && childrenArray.length > 0) {
                    Arrays.sort(childrenArray, childrenArray[0].getValue().new ChildComparator());
                }
            }
            boolean appendTranslationEntry = false;
            for (int i=0; i<childrenArray.length && (i==0 || !(singledView && isHandle)); i++) {
                if (!childrenArray[i].getKey().equals(requestName) && !childrenArray[i].getKey().equals(translationName)) {
                    if (!childrenArray[i].getValue().getCanonicalId().equals(state.getId())) {
                        if(getCanonicalNodeState(childrenArray[i].getValue().getCanonicalId()) == null) {
                            continue;
                        }
                    }
                    appendTranslationEntry = true;
                    state.addChildNodeEntry(childrenArray[i].getKey(), childrenArray[i].getValue());
                }
            }
            for (final ViewNodeId.Child aChildrenArray : childrenArray) {
                if (aChildrenArray.getKey().equals(requestName) || aChildrenArray.getKey().equals(translationName)) {
                    if (!aChildrenArray.getValue().getCanonicalId().equals(state.getId())) {
                        if (getCanonicalNodeState(aChildrenArray.getValue().getCanonicalId()) == null) {
                            continue;
                        }
                    }
                    if (isHandle && aChildrenArray.getKey().equals(translationName)) {
                        // we only append below a handle the translation node to a handle when there is at least a hippo:document added
                        if (appendTranslationEntry) {
                            state.addChildNodeEntry(aChildrenArray.getKey(), aChildrenArray.getValue());
                        }
                    } else {
                        state.addChildNodeEntry(aChildrenArray.getKey(), aChildrenArray.getValue());
                    }
                }
            }

        }
        return state;
    }

    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {
        String[] docbase = getProperty(state.getNodeId(), docbaseName, null);
        return populate(context, this, state, docbase, null, null, null, false);
    }

    protected boolean match(Map<Name, String> view, NodeId candidate) throws RepositoryException {
        for (Map.Entry<Name, String> entry : view.entrySet()) {
            Name facet = entry.getKey();
            String value = entry.getValue();
            String[] matching = getProperty(candidate, facet, null);
            if (matching != null) {
                if (value != null && !value.equals("") && !value.equals("*")) {
                    int i;
                    for (i = 0; i < matching.length; i++) {
                        if (matching[i].equals(value)) {
                            break;
                        }
                    }
                    if (i == matching.length) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected void populateChildren(StateProviderContext context, NodeId nodeId, NodeState state, NodeState upstream) throws RepositoryException {
        ViewNodeId viewId = (ViewNodeId)nodeId;
        boolean isHandle = state.getNodeTypeName().equals(handleName);
        Name parentName = null;
        if (isHandle) {
            // we only need the parentName when referring to a handle
            parentName = getNodeName(state, context);
        }
        List<ViewNodeId.Child> children = new ArrayList<ViewNodeId.Child>();
        // The translation child will be present as a child when there is a translation child in the upstream and one of the criteria's below is met:
        // 1) viewId.singledView = false
        // 2) viewId.singledView = true AND at least one other child entry is present in the new children Vector
        ViewNodeId translationChildId = null;
        boolean appending = true;
        for (ChildNodeEntry entry : upstream.getChildNodeEntries()) {
            if (getCanonicalNodeState(entry.getId()) == null) {
                continue;
            }
            if (!isHandle || viewId.view == null || match(viewId.view, entry.getId())) {
                /*
                 * below we check on the entry's nodestate wether the node type is hippo:request,
                 * because we do not show these nodes in the facetselects in mode single.
                 * Since match() already populates the nodestates of the child entries, this won't impose
                 * extra performance hit
                 */
                if (viewId.singledView && isHandle) {
                    if (entry.getName().equals(requestName)) {
                        continue;
                    } else if (entry.getName().equals(translationName)) {
                        translationChildId = newViewNodeId(nodeId, parentName, entry.getId(), context, entry.getName(), viewId.view, viewId.order, viewId.singledView);
                    } else if (appending || (parentName != null && parentName.equals(entry.getName()))) {
                        ViewNodeId childNodeId = newViewNodeId(nodeId, parentName, entry.getId(), context, entry.getName(), viewId.view, viewId.order, viewId.singledView);
                        children.add(childNodeId.new Child(entry.getName()));
                        // stop appending after first match because single hippo document view, and not using sorted set
                        // note that we continue the for loop because we might get a translation child entry which needs to be appended to the children
                        if (viewId.order == null) {
                            appending = false;
                        }
                    }
                } else {
                    ViewNodeId childNodeId = newViewNodeId(nodeId, parentName, entry.getId(), context, entry.getName(), viewId.view, viewId.order, viewId.singledView);
                    children.add(childNodeId.new Child(entry.getName()));
                }
            }
        }
        ViewNodeId.Child[] childrenArray = children.toArray(new ViewNodeId.Child[children.size()]);
        if (isHandle && childrenArray.length > 0) {
            Arrays.sort(childrenArray, childrenArray[0].getValue().new ChildComparator());
        }
        for (int i = 0; i < childrenArray.length && (i == 0 || !(viewId.singledView && isHandle)); i++) {
            if(getCanonicalNodeState(childrenArray[i].getValue().getCanonicalId()) == null) {
                continue;
            }
            state.addChildNodeEntry(childrenArray[i].getKey(), childrenArray[i].getValue());
        }
        if(childrenArray.length > 0 && translationChildId != null) {
            // we append the translationChild at the end again
            state.addChildNodeEntry(translationChildId.name, translationChildId);
        }
    }

    ViewNodeId newViewNodeId(NodeId parent, Name parentName, NodeId upstream, StateProviderContext context, Name name, LinkedHashMap<Name, String> view, LinkedHashMap<Name, String> order, boolean singledView) {
        return new ViewNodeId(this, parent, parentName, upstream, context, name, view, order, singledView);
    }

    private Name getNodeName(NodeState state, StateProviderContext context) {
        NodeState parentState = getNodeState(state.getParentId(), context);
        if (parentState != null) {
            ChildNodeEntry cne = parentState.getChildNodeEntry(state.getNodeId());
            if (cne != null) {
                return cne.getName();
            }
        }
        return null;
    }
    
}
