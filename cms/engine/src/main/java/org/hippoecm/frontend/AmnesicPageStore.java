/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend;

import java.io.Serializable;

import org.apache.wicket.page.IManageablePage;
import org.apache.wicket.pageStore.IPageStore;

public class AmnesicPageStore implements IPageStore {

    @Override
    public void destroy() {

    }

    @Override
    public IManageablePage getPage(final String sessionId, final int pageId) {
        return null;
    }

    @Override
    public void removePage(final String sessionId, final int pageId) {

    }

    @Override
    public void storePage(final String sessionId, final IManageablePage page) {

    }

    @Override
    public void unbind(final String sessionId) {

    }

    @Override
    public Serializable prepareForSerialization(final String sessionId, final Object page) {
        return null;
    }

    @Override
    public Object restoreAfterSerialization(final Serializable serializable) {
        return null;
    }

    @Override
    public IManageablePage convertToPage(final Object page) {
        return null;
    }
}