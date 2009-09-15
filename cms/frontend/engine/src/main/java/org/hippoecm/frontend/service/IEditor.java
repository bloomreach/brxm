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

/**
 * Interface that represents an editor for a particular document.
 * Can be used by e.g. workflow plugins to change the visual representation of a
 * "document".  This can be achieved by using the edit mode or setting a different
 * (node) model.
 */
public interface IEditor extends IClusterable {
    final static String SVN_ID = "$Id$";

    enum Mode {
        VIEW, EDIT
    }

    Mode getMode();

    void setMode(Mode mode) throws EditorException;

    /**
     * Requests that the editor be closed.
     * @throws EditorException when the editor is in a state where it cannot be closed.
     */
    void close() throws EditorException;

    /**
     * The model that can be used to identify the editor.  For publishable documents,
     * this is the parent handle.
     */
    IModel getModel();

}
