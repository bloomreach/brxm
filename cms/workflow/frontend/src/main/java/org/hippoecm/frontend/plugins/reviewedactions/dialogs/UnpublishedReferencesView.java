/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import javax.jcr.Node;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.editor.workflow.dialog.SelectableDocumentsView;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.service.IEditorManager;

public class UnpublishedReferencesView extends SelectableDocumentsView {

    private static final long serialVersionUID = 1L;

    public UnpublishedReferencesView(String id, ISortableDataProvider<Node, String> provider, IEditorManager mgr) {
        super(id, new MessageModel(provider), provider, mgr);
    }

    private static class MessageModel extends LoadableDetachableModel<String> {

        private static final long serialVersionUID = 1L;
        private final ISortableDataProvider<Node, String> provider;

        private MessageModel(final ISortableDataProvider<Node, String> provider) {
            this.provider = provider;
        }

        @Override
        protected String load() {
            if (provider.size() > 1) {
                return createMessage("message", provider.size());
            } else if (provider.size() == 1) {
                return createMessage("message-single");
            } else {
                return createMessage("message-empty");
            }
        }

        protected String createMessage(final String key, final Object... parameters) {
            return new ClassResourceModel(key, UnpublishedReferencesView.class, parameters).getObject();
        }
    }
}
