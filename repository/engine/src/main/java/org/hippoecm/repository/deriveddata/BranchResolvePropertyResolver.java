/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.repository.deriveddata;

import java.util.Optional;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.commons.lang3.Validate;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.branch.BranchConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;


public class BranchResolvePropertyResolver implements PropertyResolver {

    public static final String EXPECTED_NODE_TYPE = "Expected node:{path:%s} to be of type:%s";
    static final Logger log = LoggerFactory.getLogger(BranchResolvePropertyResolver.class);
    private final PropertyResolver resolver;
    private final Optional<Node> findAncestorVariantOfModifiedNode;
    private final Optional<Node> findAncestorVariantOfAccessedProperty;
    private Property property;

    public static Property getProperty(final PropertyResolver resolver) throws RepositoryException {
        BranchResolvePropertyResolver branchResolvePropertyResolver = new BranchResolvePropertyResolver(resolver);
        branchResolvePropertyResolver.resolve();
        return branchResolvePropertyResolver.getProperty();
    }

    private BranchResolvePropertyResolver(final PropertyResolver resolver) throws RepositoryException {
        Validate.notNull(resolver);
        this.resolver = resolver;
        findAncestorVariantOfModifiedNode = VariantFinder.find(getModified());
        findAncestorVariantOfAccessedProperty = VariantFinder.find(resolver.getProperty());
    }

    @Override
    public Property getProperty() {
        return this.property;
    }

    @Override
    public String getRelativePath() {
        return resolver.getRelativePath();
    }

    @Override
    public Node getModified() {
        return resolver.getModified();
    }

    @Override
    public void resolve() throws RepositoryException {
        resolver.resolve();
        if (resolver.getProperty() == null) {
            this.property = null;
            return;
        }
        final String accessedPropertyPath = resolver.getProperty().getPath();

        if (!bothModifiedNodeAndAccessedPropertyAreDescendantOfVariant()) {
            this.property = resolver.getProperty();
            log.debug("Both accessed property and derived property aren't descendants of a variant," +
                    "use property:{accessedPropertyPath:{}}.", accessedPropertyPath);
            return;
        }

        if (bothVariantsLackBranchInfo()) {
            this.property = resolver.getProperty();
            log.debug("Both accessed property and derived property aren't descendants of a variant with mixin:{} " +
                    "Use property:{accessedPropertyPath:{}}.", accessedPropertyPath);
            return;
        }

        log.debug("Both the accessProperty:{accessedProperty:{}} and the modified node:{mofiedNode:{}} are descendants of a variant.",
                accessedPropertyPath, JcrUtils.getNodePathQuietly(getModified()));

        if (onlyOneOfTheVariantsHasABranchId()) {
            String message = String.format("Only one of the following two variants have the mixin:%s :" +
                            "the variant that is the ancestor of the modified node:{accessedProperty:%s} and " +
                            "the variant that is the ancestor of the accessed property:{accessedProperty:%s}. " +
                            "Please change your derived data configuration so that in all cases both " +
                            "the derived property and the accessed property are descendants of variants that both " +
                            "can be associated with a project or both cannot be associated with a project."
                    , HippoNodeType.HIPPO_MIXIN_BRANCH_INFO
                    , JcrUtils.getNodePathQuietly(getModified())
                    , resolver.getProperty().getPath());
            throw new DerivedDataConfigurationException(message);
        }

        log.debug("Both accessed property and derived property are descendants of a variant with mixin{}",
                HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);

        if (branchIdsMatch()) {
            this.property = resolver.getProperty();
            log.debug("Branch id's match. Use property:{accessedPropertyPath:{}}.", accessedPropertyPath);
            return;
        }

        this.property = getPropertyFromVersionHistory();
        log.debug("Use property:{accessedPropertyPath:{}} from version history", accessedPropertyPath);
    }

    private boolean bothVariantsLackBranchInfo() {
        return !findAncestorVariantOfAccessedProperty.map(this::isBranchInfo).orElse(false) && !findAncestorVariantOfModifiedNode.map(this::isBranchInfo).orElse(false);
    }

    private boolean bothModifiedNodeAndAccessedPropertyAreDescendantOfVariant() {
        return Stream.of(findAncestorVariantOfModifiedNode, findAncestorVariantOfAccessedProperty).allMatch(Optional::isPresent);
    }

    private boolean onlyOneOfTheVariantsHasABranchId() {
        return Stream.of(findAncestorVariantOfAccessedProperty, findAncestorVariantOfModifiedNode)
                .filter(Optional::isPresent)
                .map(this::isBranchInfo)
                .reduce((a, b) -> a ^ b)
                .orElse(false);
    }


    private Property getPropertyFromVersionHistory() {
        return findAncestorVariantOfModifiedNode
                .filter(this::isBranchInfo)
                .map(ancestorVariantOfModifiedNode ->
                        findAncestorVariantOfAccessedProperty
                                .filter(this::isBranchInfo)
                                .map(
                                        ancestorVariantOfAccessedProperty -> getPropertyFromVersionHistory(ancestorVariantOfAccessedProperty, ancestorVariantOfModifiedNode)
                                ).orElseThrow(IllegalStateException::new)
                ).orElseThrow(IllegalStateException::new);
    }

    private Property getPropertyFromVersionHistory(Node ancestorOfAccessedProperty, Node ancestorVariantOfModifiedNode) {
        try {
            String relativePropertyPath = new RelativePathFinder(ancestorOfAccessedProperty.getPath(), resolver.getProperty().getPath()).getRelativePath();
            log.debug("Find property:{path:{}} relative to frozen variant of node:{path:{}}", relativePropertyPath, ancestorOfAccessedProperty.getPath());
            return getFrozenVariant(ancestorOfAccessedProperty, getBranchId(ancestorVariantOfModifiedNode), getState(ancestorVariantOfModifiedNode))
                    .filter(frozenVariant -> hasProperty(frozenVariant, relativePropertyPath))
                    .map(frozenVariant -> getProperty(frozenVariant, relativePropertyPath))
                    .orElseThrow(() -> new RepositoryException(String.format("Expected a version for variant:%s.", JcrUtils.getNodePathQuietly(ancestorOfAccessedProperty))));
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private Property getProperty(final Node frozenVariant, final String relativePropertyPath) {
        try {
            return frozenVariant.getProperty(relativePropertyPath);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasProperty(final Node frozenVariant, final String relativePropertyPath) {
        try {
            return frozenVariant.hasProperty(relativePropertyPath);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private String getState(final Node variant) {
        Validate.notNull(variant);
        try {
            Validate.notNull(variant);
            Validate.isTrue(variant.isNodeType(NT_DOCUMENT)
                    , EXPECTED_NODE_TYPE, variant.getPath(), NT_DOCUMENT);
            Validate.isTrue(variant.isNodeType(HippoStdNodeType.NT_DOCUMENT)
                    , EXPECTED_NODE_TYPE, variant.getPath(), HippoStdNodeType.NT_DOCUMENT);
            final String state = JcrUtils.getStringProperty(variant, HippoStdNodeType.HIPPOSTD_STATE, null);
            Validate.notNull(state);
            return state;
        }
        catch (RepositoryException e){
            throw new RuntimeException(e);
        }
    }


    private boolean branchIdsMatch() {
        return findAncestorVariantOfModifiedNode
                .filter(this::isBranchInfo)
                .map(ancestorVariantOfModifiedNode -> findAncestorVariantOfAccessedProperty
                        .filter(this::isBranchInfo)
                        .map(ancestorVariantOfAccessedProperty -> getBranchId(ancestorVariantOfModifiedNode).equals(getBranchId(ancestorVariantOfAccessedProperty)))
                        .orElse(false)
                ).orElse(false);
    }


    private String getBranchId(Node variant) {
        try {
            Validate.isTrue(variant.isNodeType(NT_DOCUMENT)
                    , EXPECTED_NODE_TYPE, variant.getPath(), NT_DOCUMENT);
            Validate.isTrue(variant.isNodeType(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO)
                    , EXPECTED_NODE_TYPE, variant.getPath(), HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
            return JcrUtils.getStringProperty(variant, HIPPO_PROPERTY_BRANCH_ID, BranchConstants.MASTER_BRANCH_ID);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

    }

    private boolean isBranchInfo(final Optional<Node> find) {
        return find.map(this::isBranchInfo).orElse(false);

    }

    private Boolean isBranchInfo(final Node variant) {
        try {
            Validate.isTrue(variant.isNodeType(HippoNodeType.NT_DOCUMENT));
            return variant.isNodeType(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }


    private Optional<Node> getFrozenVariant(final Node accessed, String branchId, String state) {
        try {
            final VersionHistory versionHistory = getVersionHistory(accessed);
            if (versionHistory == null) {
                return Optional.empty();
            }
            final String versionLabel = branchId + "-" + state;
            if (versionHistory.hasVersionLabel(versionLabel)) {
                final Version versionByLabel = versionHistory.getVersionByLabel(versionLabel);
                Node frozenNode = versionByLabel.getFrozenNode();
                if (!(frozenNode instanceof HippoNode)) {
                    // looks odd but depending on version 12 or 13, the version manager is not yet decorated, hence
                    // this explicit refetch of the frozen node via the handle session to make sure to get a HippoNode
                    // decorated variant
                    frozenNode = accessed.getSession().getNode(frozenNode.getPath());
                }
                return Optional.of(frozenNode);
            }
            return Optional.empty();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

    }

    private VersionHistory getVersionHistory(Node accessed) throws RepositoryException {
        final VersionManager versionManager = accessed.getSession().getWorkspace().getVersionManager();
        return versionManager.getVersionHistory(accessed.getPath());
    }





}
