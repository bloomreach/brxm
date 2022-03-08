/*
 *  Copyright 2022 Bloomreach (http://www.bloomreach.com)
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

import org.apache.wicket.IPageManagerProvider;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.page.IPageManager;
import org.apache.wicket.page.PageManager;
import org.apache.wicket.pageStore.IPageStore;
import org.apache.wicket.pageStore.InSessionPageStore;
import org.apache.wicket.pageStore.RequestPageStore;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.session.PluginUserSession;

/**
 * <p>
 * {@link IPageManagerProvider} that provides a page manager that does not serialize pages (but drops them from the
 * page page store instead ) and uses only {@value #NUMBER_OF_PAGES_IN_THE_CMS} pages.
 * </p>
 *
 * <p></p>Wicket uses a {@link IPageStore} to store and retrieve versions of pages to enable the back and forward button in
 * the browser.</p>
 * <p>In the CMS we don't have that functionality, but instead the page versions are used to show the login screen or
 * the application. See {@link PluginUserSession} and {@link org.hippoecm.frontend.PluginPage}.</p>
 *
 * <p>There is no need to serialize and deserialize <emp>pages</emp>. If somehow the
 * session is serialized, the pages will be dropped from the store, see {@link InSessionPageStore}</p>
 */
public class AmnesicPageManagerProvider implements IPageManagerProvider {

    private static final int NUMBER_OF_PAGES_IN_THE_CMS = 2;

    @Override
    public IPageManager get() {
        return new PageManager(new RequestPageStore(new InSessionPageStore(NUMBER_OF_PAGES_IN_THE_CMS)));
    }
}
