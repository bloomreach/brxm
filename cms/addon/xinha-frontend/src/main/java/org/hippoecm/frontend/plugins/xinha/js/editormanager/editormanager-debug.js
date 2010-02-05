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
 * @requires hippoajax, hashmap, hippodom, layoutmanager
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
         * The editor-manager controls the life-cycle of Xinha editors.
         */
        YAHOO.hippo.EditorManagerImpl = function() {
        };

        YAHOO.hippo.EditorManagerImpl.prototype = {

            defaultTimeout  : 2000,
            editors         : new YAHOO.hippo.HashMap(),
            activeEditors   : new YAHOO.hippo.HashMap(),
            initMap         : new YAHOO.hippo.HashMap(),
            initialized     : false,
            resizeRegistered: false,
            sizeState       : null,

            /**
             * Setup Xinha global variables and start loading XinhaLoader.js
             * Also register a pre-callback-handler with WicketAjax that saves
             * Xinha's that are currently in use.
             */
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
            
            /**
             * Internal method that is used to load Xinha plugins et al
             */
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
            
            /**
             * Called upon window.load (maybe LayoutManager.afterRender would be better?)
             * Starts rendering of all Xinha on current page.
             */
            renderAll : function() {
                if (!this.initialized) {
                    //XinhaLoader.js hasn't been added to the head section yet, wait a little longer
                    var me = this;
                    window.setTimeout(function() {
                        me.renderAll();
                    }, 100);
                    return;
                }
                var newEditors = this.initMap.valueSet();
                this.renderPreviews(newEditors);
                
                for(var i=0; i<newEditors.length; ++i) {
                    var editor = newEditors[i];
                    editor.container = this._getBaseContainer(editor.name); //cache it
                    this.editors.put(editor.name, editor);
                    
                    if(editor.config.started) {
                        //start Xinha editor
                        this.createAndRender(editor.name);
                        
                    }
                }
                this.initMap.clear();
            },
            
            /**
             * Register a XinhaTextEditor. This method is called on dom.load.
             */
            register : function(cfg) {
                if(!this.initMap.containsKey(cfg.name)) { 
                    this.initMap.put(cfg.name, {
                        createStarted : false,
                        pluginsLoaded : false,
                        xinhaAvailable : false,
                        name : cfg.name,
                        config : cfg,
                        xinha : null,
                        lastData: null,
                        container: null,
                        sizeState: {w: 0, h: 0},
                        
                        getContainer : function() {
                            if(this.container == null) {
                                var el = Dom.get(this.name);
                                while (el != null && el != document.body) {
                                    if (Dom.hasClass(el, 'hippo-editor-field-subfield')) {
                                        return el;
                                    }
                                    el = el.parentNode;
                                }
                            }
                            return this.container;
                        }
                    });
                } else {
                    this.initMap.get(cfg.name).config = cfg;
                }
                
                if(!this.resizeRegistered) {
                    var me = this;
                    var form = Dom.getAncestorByTagName(cfg.name, 'form');
                    YAHOO.hippo.LayoutManager.registerResizeListener(form, this, function(sizes) {
                        if(me.sizeState == null) {
                            me.sizeState = {w: sizes.wrap.w, h: sizes.wrap.h};
                        } else {
                            var deltaW = sizes.wrap.w - me.sizeState.w;
                            var deltaH = sizes.wrap.h - me.sizeState.h;
                            
                            var editors = me.activeEditors.valueSet();
                            for(var i=0; i<editors.length; ++i) {
                                var editor = editors[i];
                                var t = editor.xinha.plugins["FullscreenCompatible"];
                                if(typeof(t) === 'object' && typeof(t.instance.editor._isFullScreen) === 'boolean' && t.instance.editor._isFullScreen) {
                                    t.instance.editor._fullscreenCompatible(true);
                                    t.instance.editor.originalSizes.w += deltaW;
                                    return;
                                }
                            }
                            me.resize(deltaW, deltaH);
                            me.sizeState = {w: sizes.wrap.w, h: sizes.wrap.h};
                        }
                    }, true);
                    YAHOO.hippo.HippoAjax.registerDestroyFunction(form, function() {
                        YAHOO.hippo.LayoutManager.unregisterResizeListener(form, me);
                        me.resizeRegistered = false;
                    }, this);
                    this.resizeRegistered = true;
                }
            },

            createAndRender : function(name) {
                var editor = this.editors.get(name);
                
                editor.createStarted = true;

                var me = this;
                if (!Xinha.loadPlugins(editor.config.plugins, function() { me.createAndRender(name); })) {
                    //Plugins not loaded yet, createAndRender will be recalled
                    return;
                }
                editor.pluginsLoaded = true;

                Xinha.prototype.initSize = function() {
                    //don't use Xinha default initSize method
                }

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
                
                //make editors 
                var textarea = editor.config.textarea;
                var xinha = Xinha.makeEditors([textarea], xinhaConfig, editor.config.plugins)[textarea];
                
                //concatenate default properties with configured properties
                xinha.config = this._appendProperties(xinha.config, editor.config.properties);
                
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
                
                this.render(editor.name);
            },

            render : function(name) {
                if(this.activeEditors.containsKey(name)) {
                    this.log('Error: render called on already active editor[' + name + '] - should never happen')
                    return;
                }
                
                var editor = this.editors.get(name);
                if (!editor.xinhaAvailable) {
                    if (!editor.createStarted) {
                        this.createAndRender(name);
                    }
                    return;
                }
                
                var me = this;
                //register onload callback
                editor.xinha._onGenerate = function() {
                    me.editorLoaded(name);
                }
                Xinha.startEditors([editor.xinha]);
            },
            
            resize : function(deltaW, deltaH) {
                var editors = this.activeEditors.valueSet();
                for(var i=0; i<editors.length; ++i) {
                    var editor = editors[i];
                    var newWidth = editor.sizeState.w + deltaW;
                    var newHeight = editor.sizeState.h;
                    this.sizeEditor(editor, newWidth, newHeight);
                }
            },
            
            sizeEditor : function(editor, w, h) {
                editor.xinha.sizeEditor(w + 'px', h + 'px', true, true);
                editor.sizeState.w = w;
                editor.sizeState.h = h;
            },
            
            /**
             * Function called by Xinha after it finished creating the editor.
             * At this point we can take some final steps:
             * - set the editor's size according to the space available
             * - remove the 'preview ' styling from the container
             * - register this editor's cleanup function
             * - initialize the lastData property with the initial contents of this editor
             * - do some silly IE stuff
             * - store the editor in our activeEditors map
             * And finally give it focus. This last step is a bit blunt and should be 
             * implemented better.
             */
            editorLoaded : function(name) {
                var w,h;
                var editor = this.editors.get(name);
                if(this.hasPreviewStyles(name)) {
                    this.removePreviewStyles(editor.name);
                    
                    var container = Dom.get(name);
                    
                    //var container = editor.getContainer();
                    var pr = Dom.getRegion(container);
                    var marges = YAHOO.hippo.Dom.getMargin(container);
                    w = pr.width - marges.w;
                    h = pr.height - marges.h;
                } else {
                    var dim = this.getDimensions(container, editor.config)
                    w = dim.w;
                    h = dim.h;
                }
                this.sizeEditor(editor, w, h);
                
                this.registerCleanup(container, name);
                editor.lastData = editor.xinha.getInnerHTML();
                
                //Workaround for http://issues.onehippo.com/browse/HREPTWO-2960
                //Test if IE8, than set&remove focus on Xinha to prevent UI lockup
                if(YAHOO.env.ua.ie >= 8) {
                  editor.xinha.activateEditor();
                  editor.xinha.focusEditor();
                  editor.xinha.deactivateEditor();
                }
                
                this.activeEditors.put(editor.name, editor);
                editor.xinha.focusEditor();

                this.log('Xinha[' + name + '] - editor successfully loaded');
            },
            
            renderPreviews : function(editors) {
                for(var i=0; i<editors.length; ++i) {
                    var editor = editors[i];
                    var config = editor.config;
                    var container = editor.getContainer();
                    
                    var containerMargin = YAHOO.hippo.Dom.getMargin(container);
                    var containerHeight = this.getHeight(container, config) - containerMargin.h; 
                    Dom.setStyle(container, 'height', containerHeight + 'px');
                    Dom.addClass(container, 'rte-preview-style');
                    
                    var clickable = Dom.get(editor.name);
                    var clickableMargin = YAHOO.hippo.Dom.getMargin(clickable);
                    var clickableHeight = containerHeight - clickableMargin.h;
                    Dom.setStyle(clickable, 'height', clickableHeight + 'px')
                }
            },
            
//            /**
//             * Render a preview state of the RTE using same dimensions as edit-mode
//             */
//            renderPreview : function(name) {
//                var editor = this.editors.get(name);
//                var config = editor.config;
//                var container = editor.container;
//                
//                var dim = this.getDimensions(container, config);
//                var marg = YAHOO.hippo.Dom.getMargin(container);
//                var h = dim.h - marg.h;
//                
//                Dom.setStyle(container, 'height', h + 'px');
//                Dom.addClass(container, 'rte-preview-style')
//                
//                dim = this.getDimensions(container, config);
//                var w = dim.w - marg.w;
//
//                //console.log('Calc width for preview: ' + w);
//                
//                Dom.setStyle(container, 'width', w + 'px');
//                //Dom.setStyle(parent, 'height', h + 'px');
//                
//                //Dom.setStyle(el, 'width', (w - marg.w) + 'px');
//                //Dom.setStyle(el, 'height', (h - marg.h) + 'px');
//                
//                var clickable = Dom.get(name);
//                var marg2 = YAHOO.hippo.Dom.getMargin(clickable);
//                h = h-marg2.h;
//                Dom.setStyle(clickable, 'height', h + 'px')
//
//            },
            
            getDimensions : function(el, cfg) {
                var minWidth = 0, minHeight = 0;
                for(var i=0; i<cfg.pluginProperties.length; ++i) {
                    var x = cfg.pluginProperties[i];
                    if(x.name === 'AutoResize') {
                        for(var j=0; j<x.values.length; ++j) {
                            if(x.values[j].key === 'minHeight') {
                                minHeight = x.values[j].value; 
                                break;
                            }
                        }
                        break;
                    }
                }
                
                if(Lang.isUndefined(minWidth) || minWidth <= 0) {
                    minWidth = 150;
                }
                if(Lang.isUndefined(minHeight) || minHeight <= 0) {
                    minHeight= 150;
                }
                var vWidth = Dom.getViewportWidth();
                var vHeight = Dom.getViewportHeight();
                
                var x = Dom.getRegion(el).width;
                if(x < minWidth) {
                    x = minWidth;
                }
                
                var p = vHeight / minHeight;
                var yy = 0;
                if(p >= 2.2) {
                    yy = (minHeight/20)*p;
                }

                y = minHeight;
                if(vHeight - vHeight > 0) {  //what should this do?
                    if(y - yy > minHeight) {
                        y -= yy;
                    }
                } else {
                    y += yy;
                }
                
                var margins = YAHOO.hippo.Dom.getMargin(el);
                var width = x - margins.w;
                var height = Math.round(y) - margins.h;
                return {
                    w: width, 
                    h: height,
                    viewHeight: vHeight,
                    viewWidth: vWidth
                };

            },
            
            getHeight : function(el, cfg) {
                var minHeight = 175; //below this threshold Xinha will take too much height and bleed out out the parent element
                for(var i=0; i<cfg.pluginProperties.length; ++i) {
                    var x = cfg.pluginProperties[i];
                    if(x.name === 'AutoResize') {
                        for(var j=0; j<x.values.length; ++j) {
                            if(x.values[j].key === 'minHeight') {
                                minHeight = x.values[j].value; 
                                break;
                            }
                        }
                        break;
                    }
                }
                var vHeight = Dom.getViewportHeight();
                var p = vHeight / minHeight;
                var yy = 0;
                if(p >= 2.2) {
                    yy = (minHeight/20)*p;
                }

                y = minHeight;
                if(vHeight - vHeight > 0) {  //what should this do?
                    if(y - yy > minHeight) {
                        y -= yy;
                    }
                } else {
                    y += yy;
                }
                return Math.round(y);
            },
            
            addPreviewStyles : function(name) {
                var container = this.editors.get(name).container;
                if(container != null) {
                    Dom.addClass(container, 'rte-preview-style')
                }
            },
            
            hasPreviewStyles : function(name) {
                return Dom.hasClass(this.editors.get(name).container, 'rte-preview-style');
            },
            
            removePreviewStyles : function(name) {
                var container = this.editors.get(name).container;
                if(container != null) {
                    Dom.removeClass(container, 'rte-preview-style');
                }
            },
            
            _getBaseContainer : function(name) {
                var el = Dom.get(name);
                while (el != null && el != document.body) {
                    if (Dom.hasClass(el, 'hippo-editor-field-subfield')) {
                        return el;
                    }
                    el = el.parentNode;
                }
                return null;
            },
            
            cleanup : function(name) {
                var editor = this.activeEditors.remove(name);
                //Xinha.collectGarbageForIE(); //TODO: doesn't work like this
                this.log('Xinha[' + name + '] - Cleanup executed');
            },

            registerCleanup : function(element, name) {
                Dom.setStyle(element.parentNode, 'display', 'block');
                HippoAjax.registerDestroyFunction(element, this.cleanup, this, name);
            },
            
            saveEditors : function() {
                var keys = this.activeEditors.keySet();
                for ( var i = 0; i < keys.length; ++i) {
                    this.save(keys[i]);
                }
            },

            
            saveByTextareaId : function(id) {
                var values = this.activeEditors.valueSet();
                for ( var i = 0; i < values.length; ++i) {
                    var editor = values[i];
                    if(editor.config.textarea === id) {
                        this.save(editor.name);
                        break;
                    }
                }
            },

            save: function(name) {
                var editor = this.editors.get(name);
                if(editor != null && editor.xinha.plugins['AutoSave']) {
                    try {
                        var data = editor.xinha.getInnerHTML();
                        if(data != editor.lastData) {
                            editor.xinha.plugins['AutoSave'].instance.save();
                            editor.lastData = data;

                            this.log('Xinha[' + editor.name + '] Saved.');
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