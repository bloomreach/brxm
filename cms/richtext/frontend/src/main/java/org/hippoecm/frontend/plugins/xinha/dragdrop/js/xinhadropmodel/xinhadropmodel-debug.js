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
 * @class a YAHOO.hippo.DDBaseDropModel extension
 * @requires dragdropmodel
 * @extends YAHOO.hippo.DDbaseDropModel
 * @constructor
 */
YAHOO.namespace("hippo"); 

( function() {
    "use strict";

    var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

    YAHOO.hippo.DDDropModel = function(id, sGroup, config) { 
        YAHOO.hippo.DDDropModel.superclass.constructor.apply(this, arguments); 
    };
    
    YAHOO.extend(YAHOO.hippo.DDDropModel, YAHOO.hippo.DDBaseDropModel, {
        
        TYPE: "DDDropModel",
        
        /**
         * lookup drop model, getParameters, add to own and return;
         */
        getCallbackParameters : function(dropId) {
            var cp, textAreas, editor, xinha, sel, activeElement;
            
            cp = YAHOO.hippo.DDDropModel.superclass.getCallbackParameters.call(this, dropId);

            textAreas = Dom.getElementsByClassName('xinha_textarea', 'textarea', Dom.get(dropId));
            if(textAreas === null || textAreas[0] === null) {
                return cp;
            }

            editor = YAHOO.hippo.EditorManager.getEditorByWidgetId(textAreas[0].id);
            if(editor !== null) {
                xinha = editor.xinha;
                sel = xinha.getSelection();
    
                cp.put("emptySelection", xinha.selectionEmpty(sel));
                activeElement = xinha.activeElement(sel);
                if(activeElement !== null) {
                    YAHOO.log("Active element: " + activeElement.tagName.toLowerCase(), "info", "DragDropModel");
                    cp.put("activeElement", activeElement.tagName.toLowerCase());
                }
            }
            return cp;
        }       
        
    });
}());
