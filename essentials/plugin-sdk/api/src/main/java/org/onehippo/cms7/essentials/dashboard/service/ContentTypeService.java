/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.service;

import java.util.List;
import java.util.function.Predicate;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.ContentType;

/**
 * ContentTypeService provides access to the project's content types.
 *
 * It produces instances of the serializable ContentType, and Essentials-adjusted variation of the repository's
 * ContentType. In order to use this service, @Inject it into your REST resource or Instruction.
 */
public interface ContentTypeService {

    /**
     * Specialized version of fetchContentTypes.
     *
     * @param context access to the project
     * @param filter  custom filtering of content types, may be null
     * @return        Filtered and sorted list of ContentTypes
     */
    List<ContentType> fetchContentTypesFromOwnNamespace(PluginContext context, Predicate<ContentType> filter);

    /**
     * Fetch content types from the repository's ContentTypeService.
     *
     * @param context access to the project
     * @param filter  custom filtering of content types, may be null
     * @param ownNamespaceOnly additional filtering flag to only allow content types of the project's namespace
     * @return        Filtered and sorted list of ContentTypes
     */
    List<ContentType> fetchContentTypes(PluginContext context, Predicate<ContentType> filter, boolean ownNamespaceOnly);

    /**
     * Translate a JCR type name into the JCR base path for the corresponding content type's definition
     *
     * @param jcrType type as used at the JCR API, e.g. myhippoproject:newsdocument
     * @return        absolute path to content type definition base node, e.g. /hippo:namespaces/myhippoproject/newsdocument
     */
    String jcrBasePathForContentType(String jcrType);
}
