/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Interface that represents an editor for a particular document.
 * Can be used by e.g. workflow plugins to change the visual representation of a
 * "document".  This can be achieved by using the edit mode or setting a different
 * (node) model.
 */
public interface IEditor<T> extends IClusterable {

    String MODE = "mode";

    enum Mode {
        VIEW, EDIT, COMPARE;

        public static Mode fromConfig(final IPluginConfig config, final Mode defaultValue) {
            return Mode.fromString(config.getString(MODE), defaultValue);
        }

        public static Mode fromString(final String mode) {
            final Mode result = fromStringOrNull(mode);
            if (result == null) {
                throw new IllegalArgumentException("Unknown mode: '" + mode + "'");
            }
            return result;
        }

        public static Mode fromString(final String mode, final Mode defaultValue) {
            final Mode result = fromStringOrNull(mode);
            if (result == null) {
                return defaultValue;
            }
            return result;
        }

        private static Mode fromStringOrNull(final String mode) {
            if ("view".equals(mode)) {
                return VIEW;
            } else if ("edit".equals(mode)) {
                return EDIT;
            } else if ("compare".equals(mode)) {
                return COMPARE;
            }
            return null;
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    Mode getMode();

    void setMode(Mode mode) throws EditorException;

    /**
     * Requests focus on the editor.
     */
    void focus();

    /**
     * Model of the Editor.
     *
     * @return The model that can be used to identify the editor.  For publishable documents,
     * this is the parent handle.
     */
    IModel<T> getModel();

    boolean isModified() throws EditorException;

    boolean isValid() throws EditorException;

    /**
     * The {@link Form} that wraps the editor.
     */
    Form getForm();

    /**
     * Saves the document, and keeps the editor in its current mode (EDIT).
     *
     * @throws EditorException should be thrown in case of the editor is unable to save the document or
     *                         the document is not in Edit mode.
     */
    void save() throws EditorException;

    /**
     * Saves the document, and switches the editor to VIEW mode.
     *
     * @throws EditorException should be thrown in case of the editor is unable to save the document or
     *                         the document is not in Edit mode.
     */
    void done() throws EditorException;

    /**
     * Reverts the current document to last saved state and should keep the editor in its current mode (EDIT).
     *
     * @throws EditorException should be thrown in case of the editor is unable to revert the document or
     *                         the document is not in Edit mode.
     */
    void revert() throws EditorException;

    /**
     * Discards all changes to the document and switches the editor to VIEW mode.
     *
     * @throws EditorException should be thrown in case of the editor is unable to revert the document or
     *                         the document is not in Edit mode.
     */
    void discard() throws EditorException;

    /**
     * Requests that the editor be closed. In case the editor contains modified document,
     * the implementation should ask the user what to be done with the changes to the document.
     *
     * @throws EditorException when the editor is in a state where it cannot be closed.
     */
    void close() throws EditorException;
}
