/*
 * Copyright 2007 Hippo
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
 * @description <p>TODO</p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom
 * @module ajaxindicator
 */
 
 //TODO: might register to a custom layout-processing event to extend the loading indication
 //untill after the layout has processed, instead of just the postAjaxEvent
 
(function() {
  var Dom = YAHOO.util.Dom,
  Lang = YAHOO.util.Lang;

  YAHOO.namespace('hippo');
    
  YAHOO.hippo.AjaxIndicator = function(_elId) {
    this.elementId = _elId;
    
    var _this = this;
    Wicket.Ajax.registerPreCallHandler(function() { _this.show() });
    Wicket.Ajax.registerPostCallHandler(function(){ _this.hide() });
  };

  YAHOO.hippo.AjaxIndicator.prototype = {
    elementId: null,
    calls: 0,
    
    getElement: function() {
        YAHOO.log('Trying to find ajax indicator element[' + this.elementId + ']', 'info', 'AjaxIndicator');
        return Dom.get(this.elementId);
    },
    
    show: function() {
        this.calls++;
        this.setCursor(window, 'wait');
        Dom.setStyle(this.getElement(), 'display', 'block');
        YAHOO.log('Show ajax indicator element[' + this.elementId + ']', 'info', 'AjaxIndicator');
    },
    
    hide: function() {
        if(this.calls > 0) {
            this.calls--;
        } 
        if (this.calls == 0) {
            YAHOO.log('Hide ajax indicator element[' + this.elementId + ']', 'info', 'AjaxIndicator');
            this.setCursor(window, 'default');
            Dom.setStyle(this.getElement(),'display', 'none');
        }  
    },
    
    setCursor: function(win, cursor) {
        if (!Lang.isNull(win.document.body)) {
            YAHOO.log('Setting cursor[' + cursor + '] on window[' + win + ']', 'info', 'AjaxIndicator');
            Dom.setStyle(win.document.body, 'cursor', cursor);
        }        
    }
    
  };
})();

YAHOO.register("ajaxindicator", YAHOO.hippo.AjaxIndicator, {version: "2.5.2", build: "1076"});