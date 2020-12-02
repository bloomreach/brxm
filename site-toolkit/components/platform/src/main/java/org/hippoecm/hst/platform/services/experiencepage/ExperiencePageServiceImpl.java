/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.services.experiencepage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.configuration.experiencepage.ExperiencePageLoadingException;
import org.hippoecm.hst.configuration.experiencepage.ExperiencePageService;
import org.hippoecm.hst.platform.configuration.cache.HstNodeImpl;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.platform.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.platform.utils.UUIDUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_FROZENPRIMARYTYPE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.XPAGE_PROPERTY_PAGEREF;
import static org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type.CONTAINER_COMPONENT;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;

public class ExperiencePageServiceImpl implements ExperiencePageService {

    private final static Logger log = LoggerFactory.getLogger(ExperiencePageServiceImpl.class);

    private final static String ROOT_EXPERIENCE_PAGES_NAME = "hst:experiencePage";

    public void init() {
        HippoServiceRegistry.register(this, ExperiencePageService.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregister(this, ExperiencePageService.class);
    }

    @Override
    public HstComponentConfiguration loadExperiencePage(final Node hstPage, final HstSite hstSite,
                                                        final ClassLoader websiteClassLoader) {

        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            // set the classloader of the platform webapp to load the model
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

            // Note that the 'hstPage' node can be a frozen node from version history for Experience Pages!!
            final HstNodeImpl hstPageNode = getHstNode(hstPage);

            final String pageref = hstPageNode.getValueProvider().getString(XPAGE_PROPERTY_PAGEREF);

            if (StringUtils.isBlank(pageref)) {
                throw new ExperiencePageLoadingException(String.format("Cannot load XPage since '%s' misses property value " +
                        "for '%s'", hstPageNode.getValueProvider().getPath(), XPAGE_PROPERTY_PAGEREF));
            }

            final HstComponentsConfigurationService componentsConfiguration = (HstComponentsConfigurationService) hstSite.getComponentsConfiguration();
            final HstComponentConfigurationService xPageLayout =
                    (HstComponentConfigurationService) componentsConfiguration.getXPages().get(pageref);

            if (xPageLayout == null) {
                throw new ExperiencePageLoadingException(String.format("Cannot load XPage '%s' because XPage " +
                                "'%s' not found below at '%s'", hstPageNode.getValueProvider().getPath(),
                        pageref, hstSite.getConfigurationPath() + "/" + NODENAME_HST_PAGES));
            }

            final HstComponentConfigurationService copy = xPageLayout.copy(hstPageNode.getValueProvider().getIdentifier(),
                    hstPageNode.getValueProvider().getPath(), false);


            // the root configuration prefix is *JUST* the path to the hstPage itself (hstPage is typically a
            // node of type hst:component below a document variant
            final String rootConfigurationPrefix = hstPageNode.getValueProvider().getPath();


            final Map<String, HstNode> xPageDocChildNodes = hstPageNode.getChildren();

            // get the hst containers of an XPage below a document variant in isolation!
            // the 'keys' are the node names of 'containers' below the XPage document variant which MAP to the
            // hippo:identifier in the hst:containercomponent in the XPage hst configuration!
            // final Map<String, HstComponentConfigurationService> containerConfigurations = containers.entrySet().stream()
            //
            // since there *could* be an HST container below hst:xpage which is only present in the XPage Doc and has been
            // added explicitly, we assume that these HST containers never has a nodename which is an identifier, hence
            // we also validate that the nodename is an identifier: if it isn't, then it will be added to the XPage
            // component configuration later below

            final Map<String, HstComponentConfigurationService> documentXPageLayoutContainers = xPageDocChildNodes.entrySet().stream()
                    .filter(entry -> {
                        final boolean isContainer = NODETYPE_HST_CONTAINERCOMPONENT.equals(entry.getValue().getNodeTypeName());
                        if (!isContainer) {
                            // Only loading now containers which are for an XPage Layout container
                            return false;
                        }
                        String nodeName = entry.getKey();
                        if (UUIDUtils.isValidUUID(nodeName)) {
                            return true;
                        }
                        // not a container for an XPage Layout container since those have a nodename which matched a UUID
                        // since must match hippo:identifier property from XPage Layout container
                       return false;
                    })
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry ->
                            new HstComponentConfigurationService(entry.getValue(), null, ROOT_EXPERIENCE_PAGES_NAME, Collections.emptyMap(), rootConfigurationPrefix)));


            // mark the container items as experience page components *except* the containers which are of type
            // hst:containercomponentreference : those are never containers which are part of the XPage Layout since
            // their actual storage is below hst:workspace/hst:contaiers
            documentXPageLayoutContainers.values().forEach(config -> config.flattened()
                    .map(c -> (HstComponentConfigurationService) c)
                    .filter(c -> !c.isContainerComponentReference())
                    .forEach(c -> c.setExperiencePageComponent(true)));

            // into the 'copy' of the xpage, now glue the 'containerConfigurations' from the XPage document variant:
            // it is however not a full replacement since all configuration from the 'hst config containers' should be
            // kept EXCEPT the canonical stored path, identifier, and the lastModified since this obviously is not part
            // of the layout but of the experience page document container

            // first collect and do not manipulate in stream since then ConcurrentModificationException can happen
            // because component items get replaced
            final List<HstComponentConfiguration> flattened = copy.flattened().collect(Collectors.toList());

            final List<String> mergedDocumentContainers = new ArrayList<>();

            for (HstComponentConfiguration c : flattened) {

                final HstComponentConfigurationService pageLayoutContainer = (HstComponentConfigurationService) c;
                if (!pageLayoutContainer.isXpageLayoutComponent()) {
                    // If the container is a NON-EXPLICIT-XPAGE-LAYOUT container, for example inherited 'header' container
                    // it does not need transformation to Experience Page Container : We do not TRANSFORM such components
                    // Note that in the future we might want to support it: then for example a shared header container
                    // can be hijacked in an XPage document through the hippoIdentifier id
                    continue;
                }

                if (pageLayoutContainer.getComponentType() != CONTAINER_COMPONENT) {
                    continue;
                }

                if (pageLayoutContainer.isContainerComponentReference()) {
                    continue;
                }

                // found a container in the XPage hst configuration : Replace it with the 'container' from the
                // XPAGE document variant. If the XPage document variant DOES not (yet) have the container (since
                // for example later added to the XPage hst config), then add the 'container' empty with the
                // UUID of the hst config XPage container: Then WHEN in the CM a webmaster interacts with that
                // container, the Page Composer code will add the container to the XPage!
                final String hippoIdentifier = pageLayoutContainer.getHippoIdentifier();

                if (hippoIdentifier == null) {
                    // check whether the page layout container was coming from, say, abstractpages or from an
                    // Experience Page
                    log.warn("Experience Page Container component '{}' should have property '{}' but is missing. Remove " +
                            "container from request time XPage configuration", pageLayoutContainer.getCanonicalStoredLocation(),
                            HippoNodeType.HIPPO_IDENTIFIER);
                    ((HstComponentConfigurationService) pageLayoutContainer.getParent()).removeChild(pageLayoutContainer);
                }


                final HstComponentConfigurationService documentContainer = documentXPageLayoutContainers.get(hippoIdentifier);
                if (documentContainer == null) {
                    log.debug("XPage Document container for component '{}' does not exist, use the container " +
                            "from the HST Page layout config so items can be added via CM still", pageLayoutContainer.getCanonicalStoredLocation());

                    pageLayoutContainer.transformUnresolvedXpageLayoutContainer();
                } else {
                    mergedDocumentContainers.add(hippoIdentifier);
                    // Replace the UUID and canonical stored location of the container and add the XPage document
                    // children
                    pageLayoutContainer.transformXpageLayoutContainer(documentContainer);

                    // enhance the not yet enhanced XPage container items

                    componentsConfiguration.populateComponentReferences(documentContainer.getChildren().values(), websiteClassLoader);
                    componentsConfiguration.enhanceComponentTree(documentContainer.getChildren().values(), false);

                }

            }

            if (log.isInfoEnabled()) {
                mergedDocumentContainers.forEach(hippoIdentifier -> documentXPageLayoutContainers.remove(hippoIdentifier));

                // any left documentContainers are container that are not present (any more) in the XPage Layout. Log an
                // info message about these
                documentXPageLayoutContainers.values().forEach(config -> log.info("Document XPage contains container '{}' which " +
                                "is not represented by any hippo:identifier in XPage Layout '{}' and will as a result be ignored",
                        config.getCanonicalStoredLocation(), xPageLayout.getCanonicalStoredLocation()));
            }

            // now merge all the child nodes from the XPage document its hst:xpage which are not the containers from the
            // XPage layout but the static components or containers for the XPage Doc explicitly
            // --- LOAD --- (and MERGE) ALL OTHER COMPONENTS FROM XPAGE DOC
            xPageDocChildNodes.entrySet().stream()
                    .filter(entry -> {
                        final boolean isContainer = NODETYPE_HST_CONTAINERCOMPONENT.equals(entry.getValue().getNodeTypeName());
                        if (!isContainer) {
                            // Only loading now containers which are for an XPage Layout container
                            return true;
                        }
                        String nodeName = entry.getKey();
                        if (UUIDUtils.isValidUUID(nodeName)) {
                            // this is a container for an XPage Layout container and already processed earlier
                            return false;
                        }
                        return true;
                    }).forEach(entry -> {
                        // do not include the parent 'copy' yet since this can incorrectly result in that the xpageDocComponent
                        // is considered to be a child of the XPageLayout storage which is never the case: in copy.addXPageDocChild
                        // we do set the parent afterward to 'copy' which admittedly a bit tricky.
                        final HstComponentConfigurationService xpageDocComponent = new HstComponentConfigurationService(entry.getValue(),
                                null, ROOT_EXPERIENCE_PAGES_NAME, Collections.emptyMap(), rootConfigurationPrefix);

                        List<HstComponentConfiguration> canonicalComponents = xpageDocComponent.flattened().collect(Collectors.toList());

                        componentsConfiguration.populateComponentReferences(canonicalComponents, websiteClassLoader);
                        componentsConfiguration.enhanceComponentTree(canonicalComponents, false);

                        // mark the items stored below the xpage document as experience page component : note that due to
                        // referencing this does not have to hold for every component
                        xpageDocComponent.flattened()
                                .filter(c -> c.getCanonicalStoredLocation().startsWith(hstPageNode.getValueProvider().getCanonicalPath()))
                                .forEach(c -> ((HstComponentConfigurationService) c).setExperiencePageComponent(true));

                        final HstComponentConfiguration childByName = copy.getChildByName(entry.getKey());
                        if (childByName == null) {
                            // no merging needed, the Component is not represented at all in the XPage Layout
                            copy.addXPageDocChild(xpageDocComponent);
                        } else {
                            // finegrained merging where the same properties FROM the hst configuration have PRECEDENCE
                            // over the same properies of the XPage Document: For example if 'header' component is present
                            // from the hst config but also below on XPage doc, then for example the value
                            // hst:parameternames is used from the hst config, even if XPage document its 'header' defines
                            // its own hst:parameternames : in practice, this finegrained merging is never used any more any
                            // way and this is in line with CMS-14104.
                            copy.merge(xpageDocComponent);
                        }
            });
            // --- END --- LOAD (and MERGE) ALL OTHER COMPONENTS FROM XPAGE DOC


            // reset all the reference names to get reference names stable for this specific XPage document (so next
            // request has same stable reference names (namespaces)

            setReferenceNames(Collections.singletonList(copy));

            // make sure that all variants are populated again since variants from the XPage Document container items
            // have to be pushed to the root 'hst component' (allowed since on the 'copy')
            copy.populateVariants();

            // get all the existing mount variants from the hst configuration, and add any new variant from the XPage
            // document to it to have a complete set (note this is not set on the in memory HST Model but just on this
            // XPage configuration instance...also we only set it on the 'root component' since it is really only used
            // from the root component

            // the copy has a MUTABLE list so we can 'just' add any new variants to it
            copy.getMountVariants().addAll(copy.getVariants());

            return copy;
        } catch (ModelLoadingException e) {
            String msg = String.format("Exception trying to load Experience Page for '%s' : %s", hstPage, e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            }
            throw new ExperiencePageLoadingException(msg);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }

    }

    @Override
    public HstComponentConfiguration loadExperiencePageComponentItem(final Node componentItem, final HstSite hstSite,
                                                                     final ClassLoader websiteClassLoader) {
        try {
            if (!componentItem.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT) &&
                    !NODETYPE_HST_CONTAINERITEMCOMPONENT.equals(getStringProperty(componentItem, JCR_FROZENPRIMARYTYPE, null))) {
                throw new IllegalArgumentException(String.format("Only (frozen) nodes of type '%s' are allowed but was of type '%s'",
                        NODETYPE_HST_CONTAINERITEMCOMPONENT, componentItem.getPrimaryNodeType().getName()));
            }
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }

        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            // set the classloader of the platform webapp to load the model
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

            final HstNodeImpl hstNode = getHstNode(componentItem);

            final HstComponentConfigurationService containerItem =
                    new HstComponentConfigurationService(hstNode, null, ROOT_EXPERIENCE_PAGES_NAME, false,
                            Collections.emptyMap(), hstNode.getValueProvider().getPath(), null, true);


            containerItem.setExperiencePageComponent(true);

            final List<HstComponentConfiguration> singletonList = Collections.singletonList(containerItem);
            setReferenceNames(singletonList);


            final HstComponentsConfigurationService componentsConfiguration = (HstComponentsConfigurationService) hstSite.getComponentsConfiguration();

            componentsConfiguration.populateComponentReferences(singletonList, websiteClassLoader);
            componentsConfiguration.enhanceComponentTree(singletonList, false);

            return containerItem;
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    private HstNodeImpl getHstNode(final Node jcrNode) {
        try {

            // The hstPage can be a frozen node and then every property is protected, hence we need to include
            // protected properties (which we don't want for in memory model since takes pointless memory but does not
            // matter for Experience Pages since used only once
            return new HstNodeImpl(jcrNode, null, true);

        } catch (RepositoryException e) {
            throw new ExperiencePageLoadingException("Failed to load HstPage" , e);
        }
    }

    public void setReferenceNames(final Collection<HstComponentConfiguration> components) {
        int counter = 0;
        for (HstComponentConfiguration component : components) {
            ((HstComponentConfigurationService)component).setReferenceName("p" + ++counter);
            setReferenceNames(component.getChildren().values());
        }
    }
}
