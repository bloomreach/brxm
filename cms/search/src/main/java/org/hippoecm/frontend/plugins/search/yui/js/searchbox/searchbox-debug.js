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

YAHOO.hippo.SearchBox = function(id, config) {
    YAHOO.hippo.SearchBox.superclass.constructor.apply(this, arguments);
};
    
YAHOO.extend(YAHOO.hippo.SearchBox, YAHOO.hippo.HippoAutoComplete, {
    
    initConfig : function(config) {
        YAHOO.hippo.SearchBox.superclass.initConfig.call(this, config);
        this.itemSelectEvent.subscribe(function(sType, aArgs) {
            var data = aArgs[2];
            var url = config.callbackUrl + '&browse=' + encodeURIComponent(data.path);
            config.callbackFunction(url);
            this._clearTextboxValue();
        });
        
        this.suppressInputUpdate = true;
        this.resultTypeList = false; 
    },
    
    formatResult : function(oResultData, sQuery, sResultMatch) {
        return "<span>" + oResultData.label + " (" + oResultData.state + ")<br/>" +  oResultData.excerpt + "</span>";
    },
    
    doBeforeLoadData : function(sQuery, oResponse, oPayload) {
        this.setFooter(oResponse.meta.totalHits + " hit" + (oResponse.meta.totalHits > 1 ? 's' : ''));
        return true;
    }

});
