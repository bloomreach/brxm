/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
/**
 * @description
 * <p>
 * Provides a singleton flash detection helper
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hippoajax, uploader
 * @module flash
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.Flash) {
    (function() {
        YAHOO.hippo.FlashImpl = function() {
            this.config = null;
        };

        YAHOO.hippo.FlashImpl.prototype = {

            register : function(config) {
                this.config = config;
            },

            probe : function() {
                if (this.config !== null && this.config !== undefined) {
                    var playerVersion = YAHOO.deconcept.SWFObjectUtil.getPlayerVersion(),
                        url = this.config.callbackUrl + '&major=' + playerVersion.major + '&minor=' + playerVersion.minor + '&rev=' + playerVersion.rev;
                    this.config.callbackFunction(url);
                }
            }
        };

    }());

   YAHOO.hippo.Flash = new YAHOO.hippo.FlashImpl();

   YAHOO.register("flash", YAHOO.hippo.Flash, {
       version: "2.8.1", build: "19"
   });
}
