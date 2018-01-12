/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.service;

import java.util.List;

import javax.jcr.Session;

import org.onehippo.cms7.essentials.plugin.sdk.model.ContentType;

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
     * @return        Filtered and sorted list of ContentTypes
     */
    List<ContentType> fetchContentTypesFromOwnNamespace();

    /**
     * Fetch content types from the repository's ContentTypeService.
     *
     * @param ownNamespaceOnly additional filtering flag to only allow content types of the project's namespace
     * @return        Filtered and sorted list of ContentTypes
     */
    List<ContentType> fetchContentTypes(boolean ownNamespaceOnly);

    /**
     * Translate a JCR type name into the JCR base path for the corresponding content type's definition
     *
     * @param jcrType type as used at the JCR API, e.g. myhippoproject:newsdocument
     * @return        absolute path to content type definition base node, e.g. /hippo:namespaces/myhippoproject/newsdocument
     */
    String jcrBasePathForContentType(String jcrType);

    /**
     * Extract the prefix from a JCR content type
     */
    String extractPrefix(String jcrContentType);

    /**
     * Extract the short name (the part after the prefix, if any) from a JCR content type
     */
    String extractShortName(String jcrContentType);

    /**
     * Add a 'mixin' type to the specified content type.
     *
     * Adding a mixin to a content type has the effect that future instances of that content type will have the
     * specified mixin. Optionally, you can request that all existing instances of that document type are updated.
     *
     * Upon success, this method does push changes into the JCR session, but not save them to the repository.
     * Upon failure, all pending changes in the session are wiped.
     *
     * @param jcrContentType prefixed JCR name of a content type (document or compound)
     * @param mixinName      prefixed JCR name of the mixin
     * @param session        JCR session to access the repository
     * @param updateExisting when true, also add the mixin to all existing instances (nodes) of the content type
     * @return               true if the content type has the mixin upon returning, false otherwise.
     */
    boolean addMixinToContentType(String jcrContentType, String mixinName, Session session, boolean updateExisting);

    /**
     * Determine the value for the 'wicket.id' property of a new field in the CMS' editor template.
     *
     * The value depends on the content type and represents the positioning of the field in the document editor.
     *
     * @param jcrContentType prefixed JCR name of a content type (document or compound)
     * @return               value to use for 'wicket.id' property of new field
     */
    String determineDefaultFieldPosition(String jcrContentType);
}
