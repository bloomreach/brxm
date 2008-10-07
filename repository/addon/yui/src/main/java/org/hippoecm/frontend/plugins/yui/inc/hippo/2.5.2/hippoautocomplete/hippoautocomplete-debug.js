/*
 * Copyright 2008 Hippo
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
 * @class a YAHOO.widget.Autocomplete extension
 * @requires autocomplete, get
 * @extends YAHOO.widget.AutoComplete
 * @constructor
 * @param {String} id the id of the linked element
 * @param {String} config
 */
YAHOO.namespace("hippo"); 

( function() {
    var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;
  
    YAHOO.hippo.HippoAutoComplete = function(id, config) {
        var args = [3];
        args[0] = id;
        args[1] = config.containerId;
        
        this.callbackUrl = config.callbackUrl; //used for getting search results, currently uses YUI to do callbacks
        this.callbackMethod = config.callbackMethod; //used for handling clicks
        
        var respSchema = {
                resultsList: config.schemaResultList, 
                fields: config.schemaFields,
                metaFields: config.schemaMetaFields
        };
        
        var ds = new YAHOO.util.ScriptNodeDataSource(this.callbackUrl);
        ds.getUtility = YAHOO.util.Get;
        ds.responseType = YAHOO.util.ScriptNodeDataSource.TYPE_JSON;
        ds.responseSchema = respSchema

        args[2] = ds;
        YAHOO.hippo.HippoAutoComplete.superclass.constructor.apply(this, args); 
        
        this.initConfig(config);
    };
    
    YAHOO.extend(YAHOO.hippo.HippoAutoComplete, YAHOO.widget.AutoComplete, {
        
        initConfig : function(config) {
            this.prehighlightClassName = config.prehighlightClassName; 
            this.useShadow = config.useShadow; 
            this.useIFrame = config.useIFrame;
            
            this.maxResultsDisplayed = config.maxResultsDisplayed;
            this.minQueryLength = config.minQueryLength;
            this.queryDelay = config.queryDelay;
    
            this.submitOnlyOnEnter = config.submitOnlyOnEnter;
        },
        
        _isIgnoreKey : function(nKeyCode) {
            if(this.submitOnlyOnEnter) {
                if(nKeyCode == 13)
                    return false;
                return true;
            } else {
                return YAHOO.hippo.HippoAutoComplete.superclass._isIgnoreKey.call(this, nKeyCode);
            }
        },
        
        /**
         * Keep the resultContianer at the same horizontal position as the input box
         */
        doBeforeExpandContainer : function(oTextbox, oContainer, sQuery, aResults) { 
            var pos = Dom.getXY(oTextbox); 
            pos[1] += Dom.get(oTextbox).offsetHeight + 2; 
            Dom.setXY(oContainer,pos); 
            return true; 
        }
        /*,
        _selectItem : function(elListItem) {
            this._bItemSelected = true;
            this._updateValue(elListItem);
            this._sPastSelections = this._elTextbox.value;
            this._clearInterval();
            this.itemSelectEvent.fire(this, elListItem, elListItem._oResultData);
            YAHOO.log("Item selected: " + YAHOO.lang.dump(elListItem._oResultData), "info", this.toString());
            this._toggleContainer(false);
            
            var nodePath = elListItem._oResultData[1]; 
            var url = this.callbackUrl + '&browse=' + encodeURIComponent(nodePath);
            YAHOO.log("Calling url: " + url, "info", this.toString());
            this.callbackMethod(url);
        }
        */
        
    });
 })();
