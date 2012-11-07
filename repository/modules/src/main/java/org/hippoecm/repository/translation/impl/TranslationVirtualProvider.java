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
package org.hippoecm.repository.translation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.Context;
import org.hippoecm.repository.FacetedNavigationEngine.Query;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.DataProviderContext;
import org.hippoecm.repository.dataprovider.HippoVirtualProvider;
import org.hippoecm.repository.dataprovider.IFilterNodeId;
import org.hippoecm.repository.dataprovider.MirrorNodeId;
import org.hippoecm.repository.dataprovider.StateProviderContext;
import org.hippoecm.repository.dataprovider.ViewNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationVirtualProvider extends HippoVirtualProvider {

    private static final Logger log = LoggerFactory.getLogger(TranslationVirtualProvider.class);
    private static final Logger translationsSizeLog = LoggerFactory.getLogger(TranslationVirtualProvider.class.getName() + "/TranslationsSize");

    private static final String HIPPOTRANSLATION_NAMESPACE = "http://www.onehippo.org/jcr/hippotranslation/nt/1.0";
    private static final int MAX_TRANSLATIONS = 100;

    private HippoVirtualProvider subNodesProvider;
    private FacetedNavigationEngine<Query, Context> facetedEngine;
    private FacetedNavigationEngine.Context facetedContext;

    private Name handleName;
    private Name idName;
    private Name localeName;
    private Name docbaseName;

    @Override
    protected void initialize() throws RepositoryException {
        NameFactory nf = NameFactoryImpl.getInstance();
        register(nf.create(HIPPOTRANSLATION_NAMESPACE, "translations"), null);
        handleName = resolveName(HippoNodeType.NT_HANDLE);
        idName = nf.create(HIPPOTRANSLATION_NAMESPACE, "id");
        localeName = nf.create(HIPPOTRANSLATION_NAMESPACE, "locale");
        docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
    }

    @Override
    public void initialize(DataProviderContext stateMgr) throws RepositoryException {
        super.initialize(stateMgr);
        this.facetedEngine = stateMgr.getFacetedEngine();
        this.facetedContext = stateMgr.getFacetedContext();
    }

    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {

        if (subNodesProvider == null) {
            subNodesProvider = getDataProviderContext().lookupProvider(resolveName(HippoNodeType.NT_MIRROR));
            if (subNodesProvider == null) {
                return super.populate(context, state);
            }
        }

        final NodeId parentId = state.getParentId();

        final NodeId docId = getCanonicalId(parentId);
        if (docId == null) {
            return super.populate(context, state);
        }

        final String id = getTranslationId(docId);
        if (id == null) {
            return super.populate(context, state);
        }

        boolean singledView = false;
        LinkedHashMap<Name, String> view = null;
        LinkedHashMap<Name, String> order = null;

        FacetedNavigationEngine.Result facetedResult = facetedEngine.query(
                "//element(*,hippotranslation:translated)[@hippotranslation:id='" + id + "']", facetedContext);
        if (facetedResult.length() > 0) { // NPE if we don't check

            if (parentId instanceof IFilterNodeId) {
                IFilterNodeId filterNodeId = (IFilterNodeId) parentId;
                if (filterNodeId.getView() != null) {
                    view = new LinkedHashMap<Name, String>(filterNodeId.getView());
                }
                if (filterNodeId.getOrder() != null) {
                    order = new LinkedHashMap<Name, String>(filterNodeId.getOrder());
                }
                singledView = filterNodeId.isSingledView();
            }

            ArrayList<ViewNodeId.Child> viewNodesOrdered = new ArrayList<ViewNodeId.Child>();
            for (NodeId t9nDocId : facetedResult) {
                if (t9nDocId == null) {
                    continue;
                }

                String[] languages = getProperty(t9nDocId, localeName, null);
                if (languages == null || languages.length != 1) {
                    continue;
                }
                Name name = resolveName(languages[0]);

                ViewNodeId ard = new ViewNodeId(subNodesProvider, state.getNodeId(), null, t9nDocId, context, name, view,
                        order, singledView);
                viewNodesOrdered.add(ard.new Child(name, ard));
            }
            ViewNodeId.Child[] childrenArray = viewNodesOrdered.toArray(new ViewNodeId.Child[viewNodesOrdered.size()]);
            if (order != null) {
                Arrays.sort(childrenArray);
            }

            Set<NodeId> handles = new TreeSet<NodeId>();
            for (ViewNodeId.Child child : childrenArray) {
                NodeId t9nDocId = child.getValue().getCanonicalId();

                NodeState t9nDocState = getCanonicalNodeState(t9nDocId);
                if (t9nDocState == null) {
                    continue;
                }

                NodeId t9nParentId = t9nDocState.getParentId();
                if (t9nParentId == null) {
                    continue;
                }

                NodeState t9nParentState = getNodeState(t9nParentId, context);
                if (t9nParentState.getNodeTypeName().equals(handleName)) {
                    if ((singledView && handles.contains(t9nParentId))
                            || (view != null && !match(view, t9nDocId))) {
                        continue;
                    }
                    handles.add(t9nParentState.getNodeId());
                }

                state.addChildNodeEntry(child.getValue().name, child.getValue());
            }

            final int numberOfTranslations = state.getChildNodeEntries().size();
            if (numberOfTranslations > MAX_TRANSLATIONS) {
                if (translationsSizeLog.isWarnEnabled()) {
                    translationsSizeLog.warn("The translations node {} has more than {} translations. " +
                            "This usually indicates a workflow misconfiguration.", state.getNodeId(), numberOfTranslations);
                }
            }

        }

        return state;
    }

    private String getTranslationId(final NodeId docId) throws RepositoryException {
        final String id;
        String[] ids = getProperty(docId, idName, null);
        if (ids == null || ids.length == 0) {
            id = null;
        } else {
            id = ids[0];
        }
        return id;
    }

    private NodeId getCanonicalId(NodeId docId) throws RepositoryException {
        if (docId instanceof MirrorNodeId) {
            docId = ((MirrorNodeId) docId).getCanonicalId();
        } else {
            String[] docbase = getProperty(docId, docbaseName, null);
            if (docbase != null && docbase.length > 0) {
                try {
                    docId = new NodeId(UUID.fromString(docbase[0]));
                } catch (IllegalArgumentException e) {
                    log.warn("invalid docbase '" + docbase[0] + "' because not a valid UUID ");
                    docId = null;
                }
            }
        }
        return docId;
    }

    // FIXME: copied from ViewVirtualProvider
    protected boolean match(Map<Name, String> view, NodeId candidate) throws RepositoryException {
        for (Map.Entry<Name, String> entry : view.entrySet()) {
            Name facet = entry.getKey();
            String value = entry.getValue();
            String[] matching = getProperty(candidate, facet, null);
            if (matching != null && matching.length > 0) {
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

}
