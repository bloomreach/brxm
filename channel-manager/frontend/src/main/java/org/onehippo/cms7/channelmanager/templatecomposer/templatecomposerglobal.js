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
            HST_LOCKED_BY : 'HST-LockedBy',
            HST_LOCKED_BY_CURRENT_USER : "HST-LockedBy-Current-User",
            HST_LOCKED_ON : "HST-LockedOn",
            HST_LAST_MODIFIED : "HST-LastModified",
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

