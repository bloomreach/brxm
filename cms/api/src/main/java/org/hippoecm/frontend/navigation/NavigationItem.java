/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend.navigation;


import java.io.Serializable;
import java.util.StringJoiner;

public class NavigationItem implements Serializable {

    private String id;
    private String displayName;
    private String appIframeUrl;
    private String appPath;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getAppIframeUrl() {
        return appIframeUrl;
    }

    public void setAppIframeUrl(final String appIframeUrl) {
        this.appIframeUrl = appIframeUrl;
    }

    public String getAppPath() {
        return appPath;
    }

    public void setAppPath(final String appPath) {
        this.appPath = appPath;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NavigationItem.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("displayName='" + displayName + "'")
                .add("appIframeUrl='" + appIframeUrl + "'")
                .add("appPath='" + appPath + "'")
                .toString();
    }
}
