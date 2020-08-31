/*
 *  Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.channelmanager;

public interface ChannelManagerConstants {

    String HST_TYPE = "HST-Type";
    String HST_LABEL = "HST-Label";
    String HST_XTYPE = "HST-XType";
    String HST_PATH_INFO = "HST-Path-Info";
    String HST_CHANNEL_ID = "HST-Channel-Id";
    String HST_CONTEXT_PATH = "HST-Context-Path";
    String HST_MOUNT_ID = "HST-Mount-Id";
    String HST_SITE_ID = "HST-Site-Id";
    String HST_PAGE_ID = "HST-Page-Id";
    String HST_UNPUBLISHED_VARIANT_ID = "HST-Unpublished-Variant-Id";
    String HST_IS_PRIMARYDOCUMENT_VERSION_HISTORY = "HST-Is-PrimaryDocument-Version-History";
    String HST_BRANCH_ID = "HST-Branch-Id";
    String HST_EXPERIENCE_PAGE = "HST-Experience-Page";
    String HST_EXPERIENCE_PAGE_COMPONENT = "HST-Experience-Page-Component";
    String HST_SITEMAP_ID = "HST-Sitemap-Id";
    String HST_SITEMAPITEM_ID = "HST-SitemapItem-Id";

    /**
     * @deprecated since 14.x Experience Pages, do not use any more
     */
    @Deprecated
    String HST_PAGE_EDITABLE = "HST-Page-Editable";

    // marker boolean to indicate that the container(item) is an editable XPage component
    String HST_XPAGE_EDITABLE = "HST-XPage-Editable";

    String HST_RENDER_VARIANT = "HST-Render-Variant";
    String HST_SITE_HAS_PREVIEW_CONFIG = "HST-Site-HasPreviewConfig";
    String HST_END_MARKER = "HST-End";
    String HST_COMPONENT_EDITABLE = "HST-Component-Editable";

    // below used outside HST
    String HST_PAGE_REQUEST_VARIANTS = "HST-Page-Request-Variants";
    String HST_LOCKED_BY = "HST-LockedBy";
    String HST_LOCKED_BY_CURRENT_USER = "HST-LockedBy-Current-User";
    String HST_LOCKED_ON = "HST-LockedOn";
    String HST_LAST_MODIFIED = "HST-LastModified";

    // use for marking hst components which are / can be shared
    String HST_SHARED = "HST-Shared";
    String HST_TYPE_MANAGE_CONTENT_LINK = "MANAGE_CONTENT_LINK";
    String HST_TYPE_PAGE_META_DATA = "PAGE-META-DATA";
    String MANAGE_CONTENT_DEFAULT_PATH = "defaultPath";
    String MANAGE_CONTENT_PARAMETER_NAME = "parameterName";
    String MANAGE_CONTENT_PARAMETER_VALUE = "parameterValue";
    String MANAGE_CONTENT_PARAMETER_VALUE_IS_RELATIVE_PATH = "parameterValueIsRelativePath";
    String MANAGE_CONTENT_PICKER_CONFIGURATION = "pickerConfiguration";
    String MANAGE_CONTENT_PICKER_INITIAL_PATH = "pickerInitialPath";
    String MANAGE_CONTENT_PICKER_REMEMBERS_LAST_VISITED = "pickerRemembersLastVisited";
    String MANAGE_CONTENT_PICKER_ROOT_PATH = "pickerRootPath";
    String MANAGE_CONTENT_PICKER_SELECTABLE_NODE_TYPES = "pickerSelectableNodeTypes";
    String MANAGE_CONTENT_ROOT_PATH = "rootPath";
    String MANAGE_CONTENT_DOCUMENT_TEMPLATE_QUERY = "documentTemplateQuery";
    String MANAGE_CONTENT_FOLDER_TEMPLATE_QUERY = "folderTemplateQuery";
    String MANAGE_CONTENT_UUID = "uuid";
}
