/*
 *  Copyright 2022 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hippoecm.frontend.service;

import java.net.URI;

/**
 * Represents a Navigation Application menu help link.
 */

public class NavAppHelpLink {
    private final String label;
    private final URI url;
    private final boolean isVisible;

    public NavAppHelpLink(String label, URI url, boolean isVisible) {
        this.label = label;
        this.url = url;
        this.isVisible = isVisible;
    }

    public String getLabel() {
        return this.label;
    }

    public URI getUrl() {
        return this.url;
    }

    public boolean isVisible() {
        return this.isVisible;
    }
}
