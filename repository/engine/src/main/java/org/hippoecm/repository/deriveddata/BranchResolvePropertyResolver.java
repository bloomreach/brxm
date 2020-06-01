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

import org.apache.commons.lang.StringUtils;
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
    private final Optional<Node> ancestorVariantOfModifiedNodeOption;
    private final Optional<Node> ancestorVariantOfAccessedPropertyOption;
    private Property property;

    private BranchResolvePropertyResolver(final PropertyResolver resolver) throws RepositoryException {
        Validate.notNull(resolver);
        this.resolver = resolver;
        resolver.resolve();
        ancestorVariantOfModifiedNodeOption = VariantFinder.find(getModified());
        ancestorVariantOfAccessedPropertyOption = VariantFinder.find(resolver.getProperty());
    }

    public static Property getProperty(final PropertyResolver resolver) throws RepositoryException {
        BranchResolvePropertyResolver branchResolvePropertyResolver = new BranchResolvePropertyResolver(resolver);
        branchResolvePropertyResolver.resolve();
        return branchResolvePropertyResolver.getProperty();
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
        if (resolver.getProperty() == null) {
            this.property = null;
            return;
        }
        final String accessedPropertyPath = getSensibleSameNameSibblingPath((resolver.getProperty()));

        if (!bothModifiedNodeAndAccessedPropertyAreDescendantOfVariant()) {
            this.property = resolver.getProperty();
            log.debug("Both accessed property and derived property aren't descendants of a variant," +
                    "use property:{accessedPropertyPath:{}}.", accessedPropertyPath);
            return;
        }

        if (bothVariantsLackBranchInfo()) {
            this.property = resolver.getProperty();
            log.debug("Both accessed property and derived property aren't descendants of a variant with mixin:{} " +
                    "Use property:{path:{}}.", HippoNodeType.HIPPO_MIXIN_BRANCH_INFO, accessedPropertyPath);
            return;
        }

        log.debug("Both the accessProperty:{accessedProperty:{}} and the modified node:{mofiedNode:{}} are descendants " +
                        "of a variant.",
                accessedPropertyPath, getSensibleSameNameSibblingPath(getModified()));


        if (branchIdsMatch()) {
            this.property = resolver.getProperty();
            log.debug("Branch id's match. Use property:{accessedPropertyPath:{}}.", accessedPropertyPath);
            return;
        }

        if (accessPropertyIsDescendantOfUnpublishedVariant()) {

            ancestorVariantOfAccessedPropertyOption.ifPresent(
                    variant -> log.debug("Ancestor of accessed property:{path:{}} is unpublished variant"
                            , accessedPropertyPath));
            final String state = getState(ancestorVariantOfAccessedPropertyOption.get());
            final String branchId = getBranchId(ancestorVariantOfModifiedNodeOption.get());
            final String versionLabel = getVersionLabel(branchId, state);
            this.property = getPropertyFromVersionHistory(ancestorVariantOfAccessedPropertyOption, versionLabel)
                    .orElseThrow(() -> new RepositoryException(String.format("Could not find node with label %s in version history", versionLabel)));
            log.debug("Use property:{accessedPropertyPath:{}} from version history", accessedPropertyPath);
            return;
        }

        if (accessedPropertyIsDescendantOfPublishedVariant()) {

            ancestorVariantOfAccessedPropertyOption.ifPresent(
                    variant -> log.debug("Ancestor of accessed property:{path:{}} is published variant"
                            , accessedPropertyPath));

            final VariantFinder variantFinder = new VariantFinder(resolver.getProperty());
            final String state = getState(ancestorVariantOfAccessedPropertyOption.get());
            final String branchId = getBranchId(ancestorVariantOfModifiedNodeOption.get());
            final String versionLabel = getVersionLabel(branchId, state);
            this.property = getPropertyFromVersionHistory(variantFinder.findUnpublished(), versionLabel)
                    .orElseThrow(() -> new RepositoryException(String.format("Could not find node with label %s in version history", versionLabel)));
            log.debug("Use property:{accessedPropertyPath:{}} from version history", accessedPropertyPath);
            return;
        }

        throw new RepositoryException(String.format("Could not determine accessedProperty, modfiedNode:{path:%s}" +
                        ", accessedProperty:{path:%s}",
                getSensibleSameNameSibblingPath(getModified()), accessedPropertyPath));
    }

    private String getVersionLabel(final String branchId, final String state) {
        return branchId + "-" + state;
    }

    private boolean accessedPropertyIsDescendantOfPublishedVariant() throws RepositoryException {
        if (ancestorVariantOfAccessedPropertyOption.isPresent()) {
            final Node ancestorVariantOfAccessedProperty = ancestorVariantOfAccessedPropertyOption.get();
            return HippoStdNodeType.PUBLISHED.equals(getState(ancestorVariantOfAccessedProperty));
        }
        return false;
    }

    private boolean accessPropertyIsDescendantOfUnpublishedVariant() throws RepositoryException {
        if (ancestorVariantOfAccessedPropertyOption.isPresent()) {
            final Node ancestorVariantOfAccessedProperty = ancestorVariantOfAccessedPropertyOption.get();
            return HippoStdNodeType.UNPUBLISHED.equals(getState(ancestorVariantOfAccessedProperty));
        }
        return false;
    }

    private boolean bothVariantsLackBranchInfo() throws RepositoryException {

        if (ancestorVariantOfAccessedPropertyOption.isPresent() && ancestorVariantOfModifiedNodeOption.isPresent()) {
            return !isBranchInfo(ancestorVariantOfAccessedPropertyOption.get()) && !isBranchInfo(ancestorVariantOfModifiedNodeOption.get());
        }
        return false;
    }

    private boolean bothModifiedNodeAndAccessedPropertyAreDescendantOfVariant() {
        return Stream.of(ancestorVariantOfModifiedNodeOption, ancestorVariantOfAccessedPropertyOption)
                .allMatch(Optional::isPresent);
    }

    private Optional<Property> getPropertyFromVersionHistory(final Optional<Node> findUnpublished
            , final String versionLabel) throws RepositoryException {

        if (findUnpublished.isPresent()) {
            return Optional.of(getPropertyFromVersionHistory(findUnpublished.get(), versionLabel));
        }

        return Optional.empty();
    }

    private Property getPropertyFromVersionHistory(Node unpublished, String versionLabel) throws RepositoryException {
        String relativePropertyPath = new RelativePathFinder(JcrUtils.getNodePathQuietly(unpublished)
                , resolver.getProperty().getPath()).getRelativePath();
        log.debug("Find property:{path:{}} relative to frozen variant of node:{path:{}}"
                , relativePropertyPath, JcrUtils.getNodePathQuietly(unpublished));

        final Optional<Node> findFrozenVariant = getFrozenVariant(unpublished, versionLabel);
        if (findFrozenVariant.isPresent()) {
            final Node frozenVariant = findFrozenVariant.get();
            if (frozenVariant.hasProperty(relativePropertyPath)) {
                return frozenVariant.getProperty(relativePropertyPath);
            }
        }
        return null;
    }

    private String getState(final Node variant) throws RepositoryException {
        Validate.notNull(variant);
        Validate.notNull(variant);
        Validate.isTrue(variant.isNodeType(NT_DOCUMENT)
                , EXPECTED_NODE_TYPE, variant.getPath(), NT_DOCUMENT);
        Validate.isTrue(variant.isNodeType(HippoStdNodeType.NT_DOCUMENT)
                , EXPECTED_NODE_TYPE, variant.getPath(), HippoStdNodeType.NT_DOCUMENT);
        return JcrUtils.getStringProperty(variant, HippoStdNodeType.HIPPOSTD_STATE, null);
    }


    private boolean branchIdsMatch() throws RepositoryException {
        if (ancestorVariantOfAccessedPropertyOption.isPresent() && ancestorVariantOfModifiedNodeOption.isPresent()) {
            return getBranchId(ancestorVariantOfAccessedPropertyOption.get()).equals(getBranchId(ancestorVariantOfModifiedNodeOption.get()));
        }
        return false;
    }


    private String getBranchId(Node variant) throws RepositoryException {
        Validate.isTrue(variant.isNodeType(NT_DOCUMENT)
                , EXPECTED_NODE_TYPE, variant.getPath(), NT_DOCUMENT);
        return JcrUtils.getStringProperty(variant, HIPPO_PROPERTY_BRANCH_ID, BranchConstants.MASTER_BRANCH_ID);
    }

    private Boolean isBranchInfo(final Node variant) throws RepositoryException {
        Validate.isTrue(variant.isNodeType(HippoNodeType.NT_DOCUMENT));
        return variant.isNodeType(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
    }


    private Optional<Node> getFrozenVariant(final Node accessed, String versionLabel) throws RepositoryException {
        final VersionHistory versionHistory = getVersionHistory(accessed);
        if (versionHistory == null) {
            return Optional.empty();
        }
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
    }

    private String getSensibleSameNameSibblingPath(Node node) throws RepositoryException {
        return replaceNumberByState(VariantFinder.find(node), node.getPath());
    }

    private String replaceNumberByState(final Optional<Node> findVariant, final String path) throws RepositoryException {
        if (findVariant.isPresent()) {
            return path.replaceAll("\\[[1-3]\\]", "[" + getState(findVariant.get()) + "]");
        }
        return StringUtils.EMPTY;
    }

    private String getSensibleSameNameSibblingPath(Property property) throws RepositoryException {
        return replaceNumberByState(VariantFinder.find(property), property.getPath());
    }

    private VersionHistory getVersionHistory(Node accessed) throws RepositoryException {
        final VersionManager versionManager = accessed.getSession().getWorkspace().getVersionManager();
        return versionManager.getVersionHistory(accessed.getPath());
    }
}
