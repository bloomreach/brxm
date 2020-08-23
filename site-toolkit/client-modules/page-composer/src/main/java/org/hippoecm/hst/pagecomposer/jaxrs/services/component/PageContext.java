/*
 * Copyright 2020 Bloomreach
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.component;

public class PageContext {

    private boolean homePage;
    private boolean locked;
    private boolean inherited;
    private boolean workspaceConfigured;

    public boolean isHomePage() {
        return homePage;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isInherited() {
        return inherited;
    }

    public boolean isWorkspaceConfigured() {
        return workspaceConfigured;
    }

    PageContext setHomePage(boolean value) {
        homePage = value;
        return this;
    }

    PageContext setLocked(boolean value) {
        locked = value;
        return this;
    }

    PageContext setInherited(boolean value) {
        inherited = value;
        return this;
    }

    PageContext setWorkspaceConfigured(boolean value) {
        workspaceConfigured = value;
        return this;
    }

}
