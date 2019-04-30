/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation.api;

import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Node;

/**
 * The context in which a field is validated.
 */
public interface FieldContext {

    /**
     * @return the JCR name of the field (e.g. "myproject:startDate").
     */
    String getJcrName();

    /**
     * @return the JCR type of the field (e.g. "Date")
     */
    String getJcrType();

    /**
     * @return the type of the field. Can be the same as {@link #getJcrType} (e.g. for "String"),
     * but can also be different if the item is a pseudo-type (e.g. "CalendarDate", which has JCR type "Date").
     */
    String getType();

    /**
     * @return the locale of the current CMS user.
     */
    Locale getLocale();

    /**
     * @return the timezone of the current CMS user.
     */
    TimeZone getTimeZone();

    /**
     * @return the parent node of the field.
     */
    Node getParentNode();

    /**
     * @return the document node (variant)
     */
    Node getDocumentNode();
}
