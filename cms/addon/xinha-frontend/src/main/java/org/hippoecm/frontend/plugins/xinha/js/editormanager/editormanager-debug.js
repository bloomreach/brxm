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
 * @requires hippoajax, hashmap
 * @module editormanager
 */

/**
 * Xinha globals
 */
var _editor_url = null;
var _editor_lang = null;
var _editor_skin = null;
var xinha_editors = [];

YAHOO.namespace('hippo');

if (!YAHOO.hippo.EditorManager) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang, HippoAjax = YAHOO.hippo.HippoAjax;
        
        /**
         * This xinha plugin is used as an indicator for Xinha's load status
         */
        EditorManagerPlugin._pluginInfo = {
            name :"EditorManagerPlugin",
            version :"1.0",
            developer :"Arthur Bogaart",
            developer_url :"http://www.onehippo.org",
            c_owner :"Arthur Bogaart",
            sponsor :"",
            sponsor_url :"",
            license :""
        }

        function EditorManagerPlugin(editor) {
            this.editor = editor;
        }
        
        EditorManagerPlugin.prototype.onGenerateOnce = function() {
            YAHOO.hippo.EditorManager.registerEditor(this.editor._textArea.getAttribute("id"), this.editor._framework.table);
        }

        /**
         * The editor-manager controls the life-cycle of Xinha editors.
         * Optionally, Xinha instances can be cached in the browser DOM, turned off by default.
         */
        YAHOO.hippo.EditorManagerImpl = function() {
            //this._init();
        };

        YAHOO.hippo.EditorManagerImpl.prototype = {

            defaultTimeout : 2000,
            editors : new YAHOO.hippo.HashMap(),
            activeEditors : new YAHOO.hippo.HashMap(),
            initialized : false,
            usePool : false,
            pool: null,

//            _init : function() {
//                //Save open editors when a WicketAjax callback is executed
//                var me = this;
//                Wicket.Ajax.registerPreCallHandler(function() {
//                    me.saveEditors();
//                });
//            },

            init : function(editorUrl, editorLang, editorSkin) {
                if (this.initialized) {
                    return;
                }

                // set Xinha globals
                _editor_url = editorUrl;
                _editor_lang = editorLang;
                _editor_skin = editorSkin;
                
                //and load XinhaLoader.js
                var me = this;
                this._loadback(editorUrl + 'XinhaLoader.js', function() { 
                    me.initialized = true; 
                });

                //Save open editors when a WicketAjax callback is executed
                Wicket.Ajax.registerPreCallHandler(function() { 
                    me.saveEditors(); 
                });
            },
            
            _loadback : function(Url, Callback, Scope, Bonus) {
                var agt       = navigator.userAgent.toLowerCase();
                var is_ie    = ((agt.indexOf("msie") != -1) && (agt.indexOf("opera") == -1));
                var T = !is_ie ? "onload" : 'onreadystatechange';
                var S = document.createElement("script");
                S.type = "text/javascript";
                S.src = Url;
                if ( Callback ) {
                  S[T] = function() {      
                    if ( is_ie && ( ! ( /loaded|complete/.test(window.event.srcElement.readyState) ) ) ){
                      return;
                    }
                    Callback.call(Scope ? Scope : this, Bonus);
                    S[T] = null;
                  };
                }
                document.getElementsByTagName("head")[0].appendChild(S);
            },

            register : function(cfg) {
                if (!this.initialized) {
                    //XinhaLoader.js hasn't been added to the head section yet, wait a little longer
                    var me = this;
                    var f = function() {
                        me.register(cfg);
                    }
                    window.setTimeout(f, 200);
                    return;
                }
                
                if(this.usePool && this.editors.containsKey(cfg.name)) {
                    var st = new Date();
                    //update properties
                    var editor = this.editors.get(cfg.name);
                    editor.xinha.config = this._appendProperties(editor.xinha.config, cfg.properties);
                    for ( var i = 0; i < cfg.pluginProperties.length; i++) {
                        var pp = cfg.pluginProperties[i];
                        //console.log('Updating plugin properties to xinha[' + editor.name + ']plugin[' + pp.name + '] ');//+ Lang.dump(pp.values));
                        editor.xinha.config[pp.name] = this._appendProperties(editor.xinha.config[pp.name], pp.values);
                    }

                    this.log('Xinha[' + cfg.name + '] - Update properties took ' + (new Date().getTime() - st.getTime() ) + 'ms');
                    
                    this.render(cfg.name);
                    return;
                }
                
                //create new xinha editor
                var editor = {
                    createStarted : false,
                    pluginsLoaded : false,
                    xinhaAvailable : false,
                    name : cfg.name,
                    config : cfg,
                    xinha : null,
                    lastData: null
                };
    
                this.editors.put(editor.name, editor);
    
                if(this.usePool && this.pool == null) {
                    //create a pool element in the document body
                    var pool = document.createElement('div');
                    pool.id = 'poolid';
                    Dom.setStyle(pool, 'display', 'none');
                    Dom.setStyle(pool, 'position', 'absolute');
                    Dom.setXY(pool, [-4000, -4000]);
                    this.pool = document.body.appendChild(pool);
                }
                
                this.createAndRender(editor);
            },

            createAndRender : function(editor) {
                if(!editor.createStarted) {
                    editor.timer = new Date();
                }
                editor.createStarted = true;

                var me = this;
                var f = function() {
                    me.createAndRender(editor);
                }

                if (!Xinha.loadPlugins(editor.config.plugins, f)) {
                    //Plugins not loaded yet, createAndRender will be recalled
                    return;
                }
                //console.log('Plugins loaded for ' + editor.name + ' - ' + Lang.dump(editor.config.plugins));
                editor.pluginsLoaded = true;

                var xinhaConfig = new Xinha.Config();
                if (!Lang.isUndefined(editor.config.styleSheets)
                        && editor.config.styleSheets.length > 0) {
                    //load xinha stylesheets
                    xinhaConfig.pageStyleSheets = [editor.config.styleSheets.length];
                    for ( var i = 0; i < editor.config.styleSheets.length; i++) {
                        var ss = editor.config.styleSheets[i];
                        if (!ss.indexOf("/") == 0 && !ss.indexOf("http://") == 0) {
                            ss = _editor_url + ss;
                        }
                        xinhaConfig.pageStyleSheets[i] = ss;
                    }
                }
                
                //Set formatting options
                if(!Lang.isUndefined(editor.config.formatBlock)) {
                    xinhaConfig.formatblock = editor.config.formatBlock;
                }
                
                //var editors = Xinha.makeEditors([ editor.name ], xinhaConfig, editor.config.plugins);
                //make editors 
                var xinha = Xinha.makeEditors([ editor.name ], xinhaConfig, editor.config.plugins)[editor.name];
                
                //Register EditorManagerPlugin
                xinha.registerPlugin(EditorManagerPlugin);
                
                //concatenate default properties with configured properties
                xinha.config = this._appendProperties(xinha.config, editor.config.properties);
                //console.log('Adding properties to xinha[' + editor.name + ']');// + Lang.dump(editor.config.properties));
                
                if(editor.config.toolbars.length == 0) {
                    //Load toolbar with all Xinha default buttons
                    //remove button popupeditor
                    outerLoop:
                    for(var i=0; i<xinha.config.toolbar.length; i++) {
                        for(var j=0; j<xinha.config.toolbar[i].length; j++) {
                            if(xinha.config.toolbar[i][j] == 'popupeditor') {
                                xinha.config.toolbar[i].splice(j, 1);//remove element from array
                                break outerLoop;      
                            }
                        }
                    }
                } else {
                    xinha.config.toolbar = [ editor.config.toolbars ];
                }

                for ( var i = 0; i < editor.config.pluginProperties.length; i++) {
                    var pp = editor.config.pluginProperties[i];
                    //console.log('Adding properties to xinha[' + editor.name + ']plugin[' + pp.name + '] ');//+ Lang.dump(pp.values));
                    xinha.config[pp.name] = this._appendProperties(
                            xinha.config[pp.name], pp.values);
                }

                //xinha.registerPlugins(editor.config.plugins); //moved this to Xinha.makeEditors arguments
                editor.xinha = xinha;
                xinha_editors[editor.name] = xinha;
                editor.xinhaAvailable = true;
                
                var time = new Date().getTime() - editor.timer.getTime();
                this.log('Xinha[' + editor.name + '] - Create took ' + time + 'ms');

                this.render(editor.name);
            },

            render : function(name) {
                if(this.activeEditors.containsKey(name)) {
                    this.log('ALERT_ALERT_ALERT: render called on already active editor[' + name + '] - should not happen')
                    return;
                }
                
                var editor = this.editors.get(name);
                if (!editor.xinhaAvailable) {
                    if (!editor.createStarted) {
                        this.createAndRender(editor);
                    }
                    return;
                }
                var st = new Date();
                
                if(!this.usePool) {
                    Xinha.startEditors([ editor.xinha ]);
                    editor._timer = st;
                } else {

                    var id = 'POOLID-' + editor.name;
                    var el = Dom.getElementBy(function(node) {
                        return node.id == id;
                    }, 'div', this.pool);
                    
                    if(el.length == 0) {
                        Xinha.startEditors([ editor.xinha ]);
                        editor._timer = st;
                    } else {
                        var textarea = Dom.get(name);
                        var textareaName = textarea.name;
                        var value = textarea.value;
    
                        var parent = new YAHOO.util.Element(textarea.parentNode);
                        parent.removeChild(textarea);
                        parent.appendChild(Dom.getFirstChild(el));
                        this.pool.removeChild(el);
                        
                        editor.xinha._framework.ed_cell.replaceChild(textarea, editor.xinha._textArea);
                        editor.xinha._textArea = textarea;
                        
                        editor.xinha._textArea.value = value;
                        editor.xinha.initIframe();
    
                        editor.xinha.setEditorContent(value);
    
                        editor.xinha._textArea.id = name
                        editor.xinha._textArea.name = textareaName;
                        
                        this.registerCleanup(editor.xinha._framework.table, name);
                        
    //                    editor.xinha.updateToolbar();
    //                    for ( var i in editor.xinha.plugins ) {
    //                      var plugin = editor.xinha.plugins[i].instance;
    //                      Xinha.refreshPlugin(plugin);
    //                    }
    //                    editor.xinha.setEditorEvents();
                        
                        editor.xinha.deactivateEditor();
                        editor.lastData = editor.xinha.getInnerHTML();
                    }                    
                }
                
                this.log('Xinha[' + name + '] - Render took ' + (new Date().getTime() - st.getTime()) + 'ms')
                this.activeEditors.put(editor.name, editor);
            },
            
            cleanup : function(name) {
                var st = new Date();
                var editor = this.activeEditors.remove(name);
                if(this.usePool) {
                    this.saveInPool(editor);
                }
                //Xinha.collectGarbageForIE(); //TODO: doesn't work like this
                this.log('Xinha[' + name + '] - Cleanup took ' + (new Date().getTime() - st.getTime()) + 'ms')
            },
            
            saveInPool : function(editor) {
                editor.xinha._textArea.id = null;
                editor.xinha._textArea.name = null;
                var poolEl = document.createElement('div');
                poolEl.id = 'POOLID-' + editor.name;
                Dom.setStyle(poolEl, 'display', 'none');
                poolEl.appendChild(editor.xinha._framework.table);
                this.pool.appendChild(poolEl);
            },

            /**
             * Workaround! Using the EditorManagerPlugin as an indicator of Xinha's load status.
             * Better would be a Xinha LoadSucces callback function but it doesn't exist.
             */
            registerEditor : function(xId, xTable) {
                this.registerCleanup(xTable, xId);
                var editor = this.editors.get(xId);
                editor.lastData = editor.xinha.getInnerHTML();
                
              //Workaround for http://issues.onehippo.com/browse/HREPTWO-2960
              //Test if IE8, than set&remove focus on Xinha to prevent UI lockup
              if(YAHOO.env.ua.ie >= 8) {
                  editor.xinha.activateEditor();
                  editor.xinha.focusEditor();
                  editor.xinha.deactivateEditor();
              }

                this.log('Xinhap[' + xId + '] - Render took ' + (new Date().getTime() - editor._timer.getTime()) + 'ms');
            },
            
            registerCleanup : function(element, name) {
                Dom.setStyle(element.parentNode, 'display', 'block');
                HippoAjax.registerDestroyFunction(element, this.cleanup, this, name);
            },
            
            saveEditors : function() {
                var keys = this.activeEditors.keySet();
                for ( var i = 0; i < keys.length; i++) {
                    this.saveEditor(keys[i]);
                }
            },

            saveEditor : function(name) {
                var editor = this.editors.get(name);
                if(editor != null && editor.xinha.plugins['AutoSave']) {
                    try {
                        var data = editor.xinha.getInnerHTML();
                        if(data != editor.lastData) {
                            var st = new Date();
                            
                            editor.xinha.plugins['AutoSave'].instance.save();
                            editor.lastData = data;

                            this.log('Xinha[' + editor.name + '] - Save took ' + (new Date().getTime() - st.getTime()) + 'ms');
                        }
                    } catch(e) {
                        YAHOO.log('Error retrieving innerHTML from xinha, skipping save', 'error', 'EditorManager');
                    }
                }
            },

            clear : function() {
                this.editors.forEach(this, function(k, v) {
                    if (Dom.get(k) == null) {
                        var ed = this.editors.remove(k);
                        // TODO: cleanup xinha
                        // ed.clear();
                        this.log('Removed ' + k);
                    }
                });
            },

            _appendProperties : function(base, properties) {
                for ( var i = 0; i < properties.length; i++) {
                    base[properties[i].key] = properties[i].value;
                }
                return base;
            },

            log : function(message) {
                YAHOO.log(message, "info", "EditorManager");           
            }
        };
        
    })();

    YAHOO.hippo.EditorManager = new YAHOO.hippo.EditorManagerImpl();
    YAHOO.register("editormanager", YAHOO.hippo.EditorManager, {
        version : "2.7.0", build : "1799"
    });
}