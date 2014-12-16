/*
 * Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @class a YAHOO.hippo.HippoAutocomplete extension
 * @requires hippoautocomplete
 * @extends YAHOO.hippo.HippoAutoComplete
 * @constructor
 * @param {String} id the id of the linked element
 * @param {String} config
 */
YAHOO.namespace("hippo"); 

if (!YAHOO.hippo.TagBox) {
    (function() {
        //var Dom = YAHOO.util.Dom, Lang = YAHOO.lang, HippoAjax = YAHOO.hippo.HippoAjax;

        YAHOO.hippo.TagBox = function(id, config) {
            YAHOO.hippo.TagBox.superclass.constructor.apply(this, arguments);
        };
    
        YAHOO.extend(YAHOO.hippo.TagBox, YAHOO.hippo.HippoAutoComplete, {
        
            initConfig : function(config) {
                YAHOO.hippo.TagBox.superclass.initConfig.call(this, config);
        
                this.itemSelectEvent.subscribe(function(sType, aArgs) {
                    var data = aArgs[2];
                    var url = config.callbackUrl + '&add=' + encodeURIComponent(data.label);
                    var m = aArgs[0];
                    
                    config.callbackFunction(url);
                    //this._clearTextboxValue();
                });
                
                this.suppressInputUpdate = true;
                this.resultTypeList = false; 
            },
            
            formatResult : function(oResultData, sQuery, sResultMatch) {
                return "<span>" + oResultData.label + "</span>";
            },
    
//    doBeforeLoadData : function(sQuery, oResponse, oPayload) {
//        return true;
//    },
//    
            _onTextboxBlur : function(v, oSelf) {
                //oSelf._clearTextboxValue();
                //return YAHOO.hippo.HippoAutoComplete.superclass._onTextboxBlur.call(this, v, oSelf);
            }
        
        });

    })();

//    YAHOO.hippo.TagBox= new YAHOO.hippo.TagBoxImpl();
    YAHOO.register("tagbox", YAHOO.hippo.TagBox, {
        version : "2.6.0",
        build : "1321"
    });

}
