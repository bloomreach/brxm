/*
 * Copyright 2019-2023 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Field for editing HTML with CKEditor.
 */
public interface HtmlField {

    String HTMLPROCESSOR_ID = "htmlprocessor.id";

    /**
     * @return the CKEditor JSON configuration for the HTML field.
     */
    ObjectNode getConfig();

}
