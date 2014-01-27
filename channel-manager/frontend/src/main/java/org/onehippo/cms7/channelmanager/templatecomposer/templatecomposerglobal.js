/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

(function() {

    HST = {
        COMPONENT       : 'COMPONENT',
        CONTAINER       : 'CONTAINER_COMPONENT',
        CONTAINERITEM   : 'CONTAINER_ITEM_COMPONENT',
        CMSLINK : 'cmslink',
        MENU : 'menu',

        ATTR : {
            ID : 'uuid',
            TYPE : 'type',
            XTYPE : 'xtype',
            INHERITED : 'inherited',
            HST_CONTAINER_COMPONENT_LOCKED_BY : 'HST-Container-LockedBy',
            HST_CONTAINER_COMPONENT_LOCKED_BY_CURRENT_USER : "HST-Container-LockedBy-Current-User",
            HST_CONTAINER_COMPONENT_LOCKED_ON : "HST-Container-LockedOn",
            HST_CONTAINER_COMPONENT_LAST_MODIFIED : "HST-Container-LastModified",
            HST_CONTAINER_DISABLED : "HST-Container-disabled",
            URL : 'url',
            REF_NS : 'refNS',
            VARIANT : 'hst_internal_variant'
        },

        CLASS : {
            CONTAINER : 'hst-container',
            ITEM : 'hst-container-item',
            EDITLINK : 'hst-cmseditlink',
            EDITMENU : 'hst-cmseditmenu'
        },

        DIR : {
            VERTICAL    : 1,
            HORIZONTAL  : 2
        }
    };

}());

