/*
 *  Copyright 2008 Hippo.
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

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;

public interface IEditorManager extends IClusterable {

    String EDITOR_ID = "editor.id";

    <T> IEditor<T> getEditor(IModel<T> model);

    <T> IEditor<T> openEditor(IModel<T> model) throws ServiceException;

    <T> IEditor<T> openPreview(IModel<T> model) throws ServiceException;

}
