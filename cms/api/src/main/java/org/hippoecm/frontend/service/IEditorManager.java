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
package org.hippoecm.frontend.service;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;

public interface IEditorManager extends IClusterable {

    String EDITOR_ID = "editor.id";

    IEditor<Node> getEditor(IModel<Node> model);

    IEditor<Node> openEditor(IModel<Node> model) throws ServiceException;

    IEditor<Node> openPreview(IModel<Node> model) throws ServiceException;

}
