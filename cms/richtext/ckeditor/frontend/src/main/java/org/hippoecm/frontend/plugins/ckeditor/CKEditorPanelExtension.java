/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.ckeditor;

import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.IDetachable;

/**
 * Extends a {@link CKEditorPanel} with additional functionality. A CKEditor extension can add configuration properties
 * for a CKEditor instance in {@link #addConfiguration(com.fasterxml.jackson.databind.node.ObjectNode)}, and create one or more Wicket
 * behaviors to render additional Wicket components. The Wicket behaviors will be detached when the
 * {@link CKEditorPanel} to which it is added is detached.
 */
public interface CKEditorPanelExtension extends IDetachable {

    /**
     * Adds configuration to a CKEditor instance.
     * @param editorConfig the configuration for a CKEditor instance
     * @throws IOException
     */
    void addConfiguration(ObjectNode editorConfig) throws IOException;

    /**
     * @return all Wicket behaviors needed by this CKEditor behavior.
     */
    Iterable<Behavior> getBehaviors();

}
