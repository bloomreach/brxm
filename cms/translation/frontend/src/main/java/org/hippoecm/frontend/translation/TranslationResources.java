/*
 *  Copyright 2010-2023 Bloomreach
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
package org.hippoecm.frontend.translation;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

public final class TranslationResources {

    private static final JavaScriptResourceReference TRANSLATIONS_JS = new JavaScriptResourceReference(TranslationResources.class, "translations.js");
    private static final CssResourceReference COUNTRIES_CSS = new CssResourceReference(TranslationResources.class, "countries.css");

    private TranslationResources() {
    }

    public static IHeaderContributor getTranslationsHeaderContributor() {
        return new IHeaderContributor() {
            @Override
            public void renderHead(final IHeaderResponse response) {
                response.render(JavaScriptHeaderItem.forReference(TRANSLATIONS_JS));
            }
        };
    }

    public static IHeaderContributor getCountriesCss() {
        return new IHeaderContributor() {
            @Override
            public void renderHead(final IHeaderResponse response) {
                response.render(CssHeaderItem.forReference(COUNTRIES_CSS));
            }
        };
    }

}
