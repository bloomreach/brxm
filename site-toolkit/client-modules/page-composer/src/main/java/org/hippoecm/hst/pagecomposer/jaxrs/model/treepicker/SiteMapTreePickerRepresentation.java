/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.util.HstSiteMapUtils;

public class SiteMapTreePickerRepresentation extends AbstractTreePickerRepresentation {


    public static AbstractTreePickerRepresentation representRequestSiteMapItem(final PageComposerContextService pageComposerContextService, final SiteMapHelper siteMapHelper) {
        final HstSiteMapItem siteMapItem = siteMapHelper.getConfigObject(pageComposerContextService.getRequestConfigIdentifier());
        return new SiteMapTreePickerRepresentation().represent(pageComposerContextService, siteMapItem, true, null);
    }

    public static AbstractTreePickerRepresentation representRequestSiteMap(final PageComposerContextService pageComposerContextService) {
        final HstSite site = pageComposerContextService.getEditingPreviewSite();
        final HstSiteMap siteMap = site.getSiteMap();
        return new SiteMapTreePickerRepresentation().represent(pageComposerContextService, siteMap);
    }

    public static AbstractTreePickerRepresentation representExpandedParentTree(final PageComposerContextService pageComposerContextService,
                                                                               final HstSiteMapItem expandToItem) {

        List<HstSiteMapItem> expansionList = new ArrayList<>();
        expansionList.add(expandToItem);

        HstSiteMapItem parent = expandToItem.getParentItem();
        while(parent != null) {
            expansionList.add(0, parent);
            parent = parent.getParentItem();
        }

        return new SiteMapTreePickerRepresentation().represent(pageComposerContextService, expandToItem.getHstSiteMap(), expansionList);
    }



    private AbstractTreePickerRepresentation represent(final PageComposerContextService pageComposerContextService, final HstSiteMap hstSiteMap) {
        return represent(pageComposerContextService, hstSiteMap, null);
    }

    private AbstractTreePickerRepresentation represent(final PageComposerContextService pageComposerContextService,
                                                       final HstSiteMap hstSiteMap,
                                                       final List<HstSiteMapItem> expansionList) {
        if (!(hstSiteMap instanceof CanonicalInfo)) {
            throw new ClientException(String.format("hstSiteMap '%s' expected to be of type '%s'",
                    hstSiteMap, CanonicalInfo.class), ClientError.UNKNOWN);
        }

        setPickerType(PickerType.PAGES.getName());
        setId(((CanonicalInfo) hstSiteMap).getCanonicalIdentifier());
        setType(Type.PAGE.getName());
        setNodeName(hstSiteMap.getSite().getName());
        setDisplayName(hstSiteMap.getSite().getName());
        setNodePath(((CanonicalInfo) hstSiteMap).getCanonicalPath());
        setCollapsed(false);
        setExpandable(true);
        setSelectable(false);

        final String pageNotFound = pageComposerContextService.getEditingMount().getPageNotFound();
        for (HstSiteMapItem child : hstSiteMap.getSiteMapItems()) {
            if (isInvisibleItem(child, pageNotFound)) {
                continue;
            }
            setLeaf(false);
            AbstractTreePickerRepresentation childRepresentation = loadItemRepresentation(pageComposerContextService, expansionList, child);
            getItems().add(childRepresentation);
        }
        Collections.sort(getItems(), comparator);
        return this;
    }

    private AbstractTreePickerRepresentation represent(final PageComposerContextService pageComposerContextService,
                                                       final HstSiteMapItem hstSiteMapItem,
                                                       final boolean loadChildren,
                                                       final List<HstSiteMapItem> expansionList
    ) {
        if (!(hstSiteMapItem instanceof CanonicalInfo)) {
            throw new ClientException(String.format("hstSiteMapItem '%s' expected to be of type '%s'",
                    hstSiteMapItem, CanonicalInfo.class), ClientError.UNKNOWN);
        }
        setPickerType(PickerType.PAGES.getName());
        setId(((CanonicalInfo) hstSiteMapItem).getCanonicalIdentifier());
        setType(Type.PAGE.getName());
        setNodeName(hstSiteMapItem.getValue());
        setDisplayName(hstSiteMapItem.getPageTitle() == null ? hstSiteMapItem.getValue() : hstSiteMapItem.getPageTitle());
        setNodePath(((CanonicalInfo) hstSiteMapItem).getCanonicalPath());

        final String pageNotFound = pageComposerContextService.getEditingMount().getPageNotFound();
        if (loadChildren && !isInvisibleItem(hstSiteMapItem, pageNotFound)) {
            setCollapsed(false);
        }

        if (expansionList != null && expansionList.isEmpty()) {
            setSelected(true);
        }

        for (HstSiteMapItem child : hstSiteMapItem.getChildren()) {
            if (!isInvisibleItem(child, pageNotFound)) {
                setExpandable(true);
                if (loadChildren) {
                    AbstractTreePickerRepresentation childRepresentation = loadItemRepresentation(pageComposerContextService, expansionList, child);
                    getItems().add(childRepresentation);
                } else {
                    break;
                }
            }
        }
        setLeaf(!isExpandable());
        setSelectable(true);
        setPathInfo(HstSiteMapUtils.getPath(hstSiteMapItem, null));
        Collections.sort(getItems(), comparator);
        return this;
    }


    private AbstractTreePickerRepresentation loadItemRepresentation(final PageComposerContextService pageComposerContextService,
                                                                    final List<HstSiteMapItem> expansionList,
                                                                    final HstSiteMapItem item) {
        AbstractTreePickerRepresentation childRepresentation;
        if (expansionList == null) {
            childRepresentation = new SiteMapTreePickerRepresentation()
                    .represent(pageComposerContextService, item, false, null);
        } else {
            if (expansionList.isEmpty()) {
                childRepresentation = new SiteMapTreePickerRepresentation()
                        .represent(pageComposerContextService, item, false, null);
            } else if (expansionList.get(0) == item) {
                final List<HstSiteMapItem> descendantList = new ArrayList<>(expansionList);
                descendantList.remove(0);
                childRepresentation = new SiteMapTreePickerRepresentation()
                        .represent(pageComposerContextService, item, !descendantList.isEmpty(), descendantList);
            } else {
                childRepresentation = new SiteMapTreePickerRepresentation()
                        .represent(pageComposerContextService, item, false, null);
            }
        }

        return childRepresentation;
    }


    private boolean isInvisibleItem(final HstSiteMapItem item, final String pageNotFound) {
        if (!item.isExplicitPath() || item.isContainerResource() || item.isHiddenInChannelManager()) {
            return true;
        }
        if (HstSiteMapUtils.getPath(item, null).equals(pageNotFound)) {
            return true;
        }
        return false;
    }


}
