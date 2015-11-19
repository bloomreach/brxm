/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

public class CopyPageRepresentation {
    /**
     * target mount for the new page
     */
    private String mountId;
    /**
     * targetName of the target to be created copy
     */
    private String targetName;
    /**
     * the parent of the new copied page. If new copied page is a root siteMapItem, the {@code targetSiteMapItemUUID}
     * will be {@code null}
     */
    private String targetSiteMapItemUUID;
    /**
     * the uuid of the page (siteMapItem) that will be copied
     */
    private String siteMapItemUUId;

    public CopyPageRepresentation() {
    }

    public String getMountId() {
        return mountId;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setMountId(final String mountId) {
        this.mountId = mountId;
    }

    public String getTargetName() {
        return targetName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setTargetName(final String targetName) {
        this.targetName = targetName;
    }

    public String getTargetSiteMapItemUUID() {
        return targetSiteMapItemUUID;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setTargetSiteMapItemUUID(final String targetSiteMapItemUUID) {
        this.targetSiteMapItemUUID = targetSiteMapItemUUID;
    }

    public String getSiteMapItemUUId() {
        return siteMapItemUUId;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSiteMapItemUUId(final String siteMapItemUUId) {
        this.siteMapItemUUId = siteMapItemUUId;
    }
}
