/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.mock.core.linking;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Mock implementation of {@link org.hippoecm.hst.core.linking.HstLink}.
 */
public class MockHstLink implements HstLink {

    private String path;
    private boolean notFound;
    private boolean containerResource;
    private Mount mount;
    private HstSiteMapItem hstSiteMapItem;
    private String subPath;
    private boolean representsIndex;

    public MockHstLink() {
        this(null);
    }

    public MockHstLink(String path) {
        setPath(path);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean isContainerResource() {
        return containerResource;
    }

    public void setContainerResource(boolean containerResource) {
        this.containerResource = containerResource;
    }

    public String[] getPathElements() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isNotFound() {
        return notFound;
    }

    public void setNotFound(boolean notFound) {
        this.notFound = notFound;
    }

    public Mount getMount() {
        return mount;
    }
    
    public void setMount(Mount mount) {
        this.mount = mount;
    }

    public HstSiteMapItem getHstSiteMapItem() {
        return hstSiteMapItem;
    }

    public void setHstSiteMapItem(final HstSiteMapItem hstSiteMapItem) {
        this.hstSiteMapItem = hstSiteMapItem;
    }

    public String getSubPath() {
        return subPath;
    }
    
    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }

    @Override
    public boolean representsIndex() {
        return representsIndex;
    }

    public void setRepresentsIndex(final boolean representsIndex) {
        this.representsIndex = representsIndex;
    }

    public String toUrlForm(HstRequestContext requestContext, boolean fullyQualified) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
