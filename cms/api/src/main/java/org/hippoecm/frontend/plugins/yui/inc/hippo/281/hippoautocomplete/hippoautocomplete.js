/*
 * Copyright 2008-2013 Hippo
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
 * @module hippoautocomplete
 * @class a YAHOO.widget.Autocomplete extension
 * @requires autocomplete, get
 * @extends YAHOO.widget.AutoComplete
 * @constructor
 * @param {String} id the id of the linked element
 * @param {String} config
 */
YAHOO.namespace("hippo"); 

( function() {
    var Dom = YAHOO.util.Dom;
  
    YAHOO.hippo.HippoAutoComplete = function(id, config) {
        var args, respSchema, ds;

        args = [3];
        args[0] = id;
        args[1] = config.containerId;
        
        this.callbackUrl = config.callbackUrl; //used for getting search results, currently uses YUI to do callbacks
        this.callbackMethod = config.callbackFunction; //used for handling clicks
        
        respSchema = {
                resultsList: config.schemaResultList, 
                fields: config.schemaFields,
                metaFields: config.schemaMetaFields
        };
        
        ds = new YAHOO.util.ScriptNodeDataSource(this.callbackUrl);
        ds.getUtility = YAHOO.util.Get;
        ds.responseType = YAHOO.util.ScriptNodeDataSource.TYPE_JSON;
        ds.responseSchema = respSchema;

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
            var result;
            if (this.submitOnlyOnEnter) {
                result = (nKeyCode === 13);
            } else {
                result = YAHOO.hippo.HippoAutoComplete.superclass._isIgnoreKey.call(this, nKeyCode);
            }
            return result;
        },
        
        /**
         * Keep the resultContianer at the same horizontal position as the input box
         */
        doBeforeExpandContainer : function(oTextbox, oContainer, sQuery, aResults) { 
            var pos = Dom.getXY(oTextbox); 
            pos[1] += Dom.get(oTextbox).offsetHeight + 2; 
            Dom.setXY(oContainer,pos); 
            return true; 
        },
        
        _onTextboxKeyDown : function(v,oSelf) {
            var nKeyCode = v.keyCode;

            // Clear timeout
            if (oSelf._nTypeAheadDelayID !== -1) {
                clearTimeout(oSelf._nTypeAheadDelayID);
            }
            if (nKeyCode === 27) { //esc
                oSelf._toggleContainer(false);
                oSelf._clearTextboxValue();
            } else {
                return YAHOO.hippo.HippoAutoComplete.superclass._onTextboxKeyDown.call(this, v, oSelf);
            }
        },
        
        _onTextboxBlur : function(v, oSelf) {
            oSelf._clearTextboxValue();
            return YAHOO.hippo.HippoAutoComplete.superclass._onTextboxBlur.call(this, v, oSelf);
        },
        
        _clearTextboxValue : function() {
            this._elTextbox.value = '';
        }
    });
 }());
