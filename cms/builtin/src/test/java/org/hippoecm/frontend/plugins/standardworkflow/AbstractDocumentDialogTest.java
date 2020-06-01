/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standardworkflow;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;

public class AbstractDocumentDialogTest extends AbstractWicketDialogTest {

    private static final String URL_INPUT = "name-url:url";
    private static final String NAME_INPUT = "name-url:name";
    private static final String URL_ACTION_LABEL = "name-url:uriAction:uriActionLabel";
    private static final String WICKET_PATH_ENABLE_URIINPUT_LINK = "dialog:content:form:name-url:uriAction";

    @SuppressWarnings("unchecked")
    protected FormComponent<String> getNameField() {
        return (FormComponent<String>) formTester.getForm().get(NAME_INPUT);
    }

    @SuppressWarnings("unchecked")
    protected FormComponent<String> getUrlField() {
        return (FormComponent<String>) formTester.getForm().get(URL_INPUT);
    }

    protected Label getUrlActionLabel() {
        return (Label) formTester.getForm().get(URL_ACTION_LABEL);
    }

    protected void clickUrlActionLink() {
        tester.clickLink(WICKET_PATH_ENABLE_URIINPUT_LINK);
    }

    protected void setUrl(final String url) {
        formTester.setValue(URL_INPUT, url);
    }

    protected void setName(final String name) {
        formTester.setValue(NAME_INPUT, name);
    }
}
