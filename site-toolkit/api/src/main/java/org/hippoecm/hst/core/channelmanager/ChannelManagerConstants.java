/*
 *  Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
    String HST_SITEMAP_ID = "HST-Sitemap-Id";
    String HST_SITEMAPITEM_ID = "HST-SitemapItem-Id";
    String HST_PAGE_EDITABLE = "HST-Page-Editable";
    String HST_RENDER_VARIANT = "HST-Render-Variant";
    String HST_SITE_HAS_PREVIEW_CONFIG = "HST-Site-HasPreviewConfig";
    String HST_END_MARKER = "HST-End";

    // below used outside HST
    String HST_PAGE_REQUEST_VARIANTS = "HST-Page-Request-Variants";
    String HST_LOCKED_BY = "HST-LockedBy";
    String HST_LOCKED_BY_CURRENT_USER = "HST-LockedBy-Current-User";
    String HST_LOCKED_ON = "HST-LockedOn";
    String HST_LAST_MODIFIED = "HST-LastModified";
    String HST_INHERITED = "HST-Inherited";
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
    String MANAGE_CONTENT_UUID = "uuid";

    //MANAGE_CONTENT_TEMPLATE_QUERY is deprecated since 12.6, use MANAGE_CONTENT_DOCUMENT_TEMPLATE_QUERY instead
    @Deprecated
    String MANAGE_CONTENT_TEMPLATE_QUERY = "templateQuery";
}
