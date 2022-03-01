/*
 *  Copyright 2010-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.diff;

import java.util.Objects;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlDiffModel extends LoadableDetachableModel<String> {

    private static final Logger log = LoggerFactory.getLogger(HtmlDiffModel.class);

    private final IModel<String> original;
    private final IModel<String> current;
    private final DiffService diffService;

    /**
     * Create a HtmlDiffModel using the default HTML diff service
     * @see DefaultHtmlDiffService
     * @param original
     * @param current
     */
    public HtmlDiffModel(final IModel<String> original, final IModel<String> current){
        this(original, current, null);
    }

    /**
     * Create the model using a predefined diff service.
     *
     * @see DiffService
     * @param original
     * @param current
     * @param diffService  if it's null, the default HTML diff service is used (see {@link DefaultHtmlDiffService})
     */
    public HtmlDiffModel(final IModel<String> original, final IModel<String> current, final DiffService diffService) {
        this.original = new ValidHtmlModel(original);
        this.current = new ValidHtmlModel(current);
        this.diffService = (diffService == null ? new DefaultHtmlDiffService() : diffService);
    }

    @Override
    protected String load(){
        if (original == null || current == null) {
            return null;
        }

        if (diffService == null) {
            log.warn("Instance of the DiffService not found");
            return null;
        }

        // Set empty default values in order to prevent nullpointer
        return diffService.diff( Objects.toString(original.getObject(), ""), Objects.toString(current.getObject(), ""));
    }

    @Override
    public void detach() {
        if (original != null) {
            original.detach();
        }

        if (current != null) {
            current.detach();
        }

        super.detach();
    }

    /**
     * This model ensures the value is surrounded with <html><body>..value..</body></html>
     * This is required by the DiffHelper.diffHtml method.
     */
    public static class ValidHtmlModel implements IModel<String> {

        IModel<String> wrapped;

        ValidHtmlModel(final IModel<String> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public String getObject() {
            if (wrapped == null) {
                return null;
            }

            String value = wrapped.getObject();
            if (value == null) {
                return null;
            }

            if (!value.trim().startsWith("<html>")) {
                value = "<html><body>" + value + "</body></html>";
            }

            //Replace the _blank so that the document opens in the same window
            return value.trim().replaceAll("<a target=\"_blank\"", "<a ");
        }

        @Override
        public void detach() {
            if (wrapped != null) {
                wrapped.detach();
            }
        }
    }
}
