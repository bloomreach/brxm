/*
 * Copyright 2016-2023 Bloomreach
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

package org.onehippo.cms.channelmanager.content.documenttype.field.sort;

import java.util.List;

import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;

/**
 * FieldSorter defines a strategy for sorting the fields of a document or compound.
 */
public interface FieldSorter {
    /**
     * Sort the fields of a content type into a one-dimensional order.
     *
     * @param context   information about the desired content type
     * @return          sorted list of the content type's exposed fields, represented as {@link FieldTypeContext}
     */
    List<FieldTypeContext> sortFields(ContentTypeContext context);
}
