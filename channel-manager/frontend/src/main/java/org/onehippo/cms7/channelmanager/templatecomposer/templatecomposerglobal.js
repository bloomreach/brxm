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

    if (window.Hippo === undefined) {
        window.Hippo = {};
    }

    Hippo.namespace = function(description) {
        var scope, parts, i, len;

        scope = window;
        parts = description.split('.');

        for (i = 0, len = parts.length; i < len; i++) {
            if (scope[parts[i]] === undefined) {
                scope[parts[i]] = {};
            }
            scope = scope[parts[i]];
        }
    };

    HST = {
        COMPONENT       : 'COMPONENT',
        CONTAINER       : 'CONTAINER_COMPONENT',
        CONTAINERITEM   : 'CONTAINER_ITEM_COMPONENT',
        CMSLINK : 'cmslink',

        ATTR : {
            ID : 'uuid',
            TYPE : 'type',
            XTYPE : 'xtype',
            INHERITED : 'inherited',
            URL : 'url',
            REF_NS : 'refNS',
            VARIANT : 'hst_internal_variant'
        },

        CLASS : {
            CONTAINER : 'hst-container',
            ITEM : 'hst-container-item',
            EDITLINK : 'hst-cmseditlink'
        },

        DIR : {
            VERTICAL    : 1,
            HORIZONTAL  : 2
        }
    };

}());

