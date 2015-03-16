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

import java.util.Collections;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.util.HstSiteMapUtils;

public class SiteMapTreePickerRepresentation extends AbstractTreePickerRepresentation {

    public AbstractTreePickerRepresentation representExpandedParentTree(final HstSiteMapItem hstSiteMapItem) {
        // TODO HSTTWO-3225
        AbstractTreePickerRepresentation picker = new SiteMapTreePickerRepresentation();
        // TODO
        picker.setPickerType(PickerType.PAGES.getName());
        return picker;
    }


    public AbstractTreePickerRepresentation represent(final PageComposerContextService pageComposerContextService, final HstSiteMap hstSiteMap) {
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
            setLeaf(false);
            if (isInvisibleItem(child, pageNotFound)) {
                continue;
            }

            AbstractTreePickerRepresentation childRepresentation = new SiteMapTreePickerRepresentation()
                    .represent(pageComposerContextService, child, false);
            getItems().add(childRepresentation);
        }
        Collections.sort(getItems(), comparator);
        return this;
    }


    public AbstractTreePickerRepresentation represent(final PageComposerContextService pageComposerContextService,
                                                      final HstSiteMapItem hstSiteMapItem,
                                                      final boolean loadChildren) {
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
        for (HstSiteMapItem child : hstSiteMapItem.getChildren()) {
            if (!isInvisibleItem(child, pageNotFound)) {
                setExpandable(true);
                if (loadChildren) {
                    AbstractTreePickerRepresentation childRepresentation = new SiteMapTreePickerRepresentation().represent(pageComposerContextService, child, false);
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
