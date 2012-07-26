/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.translation.components.document;

import java.util.List;

import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.WicketEventReference;
import org.hippoecm.frontend.translation.components.TestLocaleProvider;
import org.hippoecm.frontend.translation.components.TestStringCodecModel;

public abstract class DocumentTranslationPage extends WebPage {

    public DocumentTranslationPage() {
        add(JavascriptPackageResource.getHeaderContribution(WicketEventReference.INSTANCE));
        add(JavascriptPackageResource.getHeaderContribution(WicketAjaxReference.INSTANCE));

        add(new DocumentTranslationView("grid", getFolderTranslations(), "en", "fr", new TestStringCodecModel(),
                new TestLocaleProvider()));
    }

    abstract protected List<FolderTranslation> getFolderTranslations();
}
