/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.page.IManageablePage;
import org.apache.wicket.pageStore.IPageContext;
import org.apache.wicket.pageStore.IPageStore;

public class AmnesicPageStore implements IPageStore {

    @Override
    public boolean canBeAsynchronous(final IPageContext context) {
        // Suppress Wicket warning
        return true;
    }

    @Override
    public boolean supportsVersioning() {
        return false;
    }

    @Override
    public void addPage(final IPageContext context, final IManageablePage page) {
    }

    @Override
    public void removePage(final IPageContext context, final IManageablePage page) {
    }

    @Override
    public void removeAllPages(final IPageContext context) {
    }

    @Override
    public IManageablePage getPage(final IPageContext context, final int id) {
        return null;
    }

    @Override
    public void destroy() {
    }

}