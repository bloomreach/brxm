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
 * @description
 * <p>
 * Todo
 * </p>
 * @namespace YAHOO.hippo
 * @requires hashmap
 * @module editormanager
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.EditorManager) {
    ( function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.EditorManagerImpl = function() {
            this.init();
        };

        YAHOO.hippo.EditorManagerImpl.prototype = {
            
            defaultTimeout: 2000,
            editors : new YAHOO.hippo.HashMap(),
            
            init : function() {
                var me = this;
                Wicket.Ajax.registerPreCallHandler(function() { me.saveEditors(); });
            },
            
            register : function(comp, timeoutLen) {
                this.clear();

                var _id = comp.getId();
                if(!this.editors.containsKey(_id)) {
                    var len = timeoutLen != undefined ? timeoutLen : this.defaultTimeout;
                    var obj = {
                        component: comp, 
                        data: comp.getContents(), 
                        waiting: false, 
                        timeoutLength: len, 
                        timeoutId: null
                    };
                    this.editors.put(_id, obj);
                }
            },
            
            check : function(id) {
                try {
                    var c = this.editors.get(id);
                    
                    if(c.waiting)
                        this.reset(c);
                    
                    var me  = this; 
                    var func = function() { me.handle(c); }
                    c.timeoutId = window.setTimeout(func, c.timeoutLength);
                    c.waiting = true;
                } catch(e) { 
                    //ignore 
                }
            },
            
            saveEditors : function() {
                var values = this.editors.valueSet();
                for(var i=0; i<values.length; i++) {
                    this.handle(values[i]);
                }
            },

            handle : function(c) {
                if(c.waiting) {
                    this.reset(c);
                }
                if(Dom.get(c.component.getId() == null)){
                    return;
                }
                var contents = c.component.getContents(); 
                if(c.data != contents) {
                    c.data = contents;
                    c.component.save();
                }
            },
            
            reset : function(c) {
                if (c.timeoutId != null) {
                    window.clearTimeout(c.timeoutId);
                    c.timeoutId = null;
                }
                c.waiting = false;
            },

            resetById : function(id) {
                try {
                    this.reset(this.editors.get(id));
                } catch(e) {
                    //do nothing
                }
            },
            
            clear : function() {
                this.editors.forEach(this, function(k, v){
                    if(Dom.get(k) == null) {
                        this.editors.remove(k);
                    }; 
                });
            }
            
        };
    })();

    YAHOO.hippo.EditorManager = new YAHOO.hippo.EditorManagerImpl();
    YAHOO.register("editormanager", YAHOO.hippo.EditorManager, {
        version :"2.6.0",
        build :"1321"
    });
}