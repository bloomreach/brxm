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
 * @requires hippoajax, hashmap, hippodom, layoutmanager, container
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
            this.editors = new YAHOO.hippo.HashMap();
            this.activeEditors = new YAHOO.hippo.HashMap();
            this.newEditors = new YAHOO.hippo.HashMap();
            
            this.initialized = false;
            this.registered = false;
            this.sizeState =  null;
        };

        YAHOO.hippo.EditorManagerImpl.prototype = {

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
                 // Internal method that is used to load Xinha plugins et al
                YAHOO.hippo.HippoAjax.loadJavascript(editorUrl + 'XinhaLoader.js', function() {
                    this.initialized = true; 
                }, this);

                //Save open editors when a WicketAjax callback is executed
                var me = this;
                Wicket.Ajax.registerPreCallHandler(function() { 
                    me.saveEditors(); 
                });
                
            },
            
            /**
             * Register a XinhaTextEditor. This method is called on dom load.
             * TODO: implement configurable editor instantiation
             */
            register : function(cfg) {
                var editor = null;
                if(this.editors.containsKey(cfg.name)) {
                    editor = this.editors.remove(cfg.name);
                    editor.reset(cfg);
                } else {
                    editor = new YAHOO.hippo.XinhaEditor(cfg);
                }
                this.newEditors.put(cfg.name, editor);
                
                if(!this.registered) {
                    var deltaProvider = null; //alternative element for width/height delta
                    
                    //Start at the editor container element and traverse up to the nearest form.
                    //During the traversal monitor for a parent element with classname 'column-wrapper', 
                    //if found save the current element as an alternative deltaProvider
                    var form = Dom.getAncestorBy(Dom.get(cfg.name), function(element) {
                        if(element.tagName.toLowerCase() === 'form') {
                            return true;
                        }
                        if(Dom.hasClass(element.parentNode, 'column-wrapper')) {
                            deltaProvider = element;
                        }
                        return false;
                    });
                    
                    if(form == null) {
                        return;
                    }
                    
                    //register the form as a resize listene
                    var me = this;
                    YAHOO.hippo.LayoutManager.registerResizeListener(form, this, function(unitSize) {
                        var sizes = deltaProvider == null ? {width: unitSize.wrap.w, height: unitSize.wrap.h} : Dom.getRegion(deltaProvider);
                        if(me.sizeState == null) {
                            me.sizeState = {w: sizes.width, h: sizes.height};
                        } else {
                            var deltaW = sizes.width  - me.sizeState.w;
                            var deltaH = sizes.height - me.sizeState.h;
                            me.resize(deltaW, deltaH);
                            me.sizeState = {w: sizes.width, h: sizes.height};
                        }
                    }, true);
                    YAHOO.hippo.HippoAjax.registerDestroyFunction(form, function() {
                        YAHOO.hippo.LayoutManager.unregisterResizeListener(form, me);
                        me.registered = false;
                        //a new form is loaded so clear editors map
                        me.editors.forEach(me, function(k, v){
                            v.destroy();
                        });
                        me.editors.clear();
                    }, this);
                    this.registered = true;
                }
            },

            /**
             * Called upon window.load (maybe LayoutManager.afterRender would be better?)
             * Starts rendering of all Xinha on current page.
             */
            render : function() {
                if (!this.initialized) {
                    //XinhaLoader.js hasn't been added to the head section yet, wait a little longer
                    Lang.later (100, this, this.render);
                    return;
                }
                
                this.newEditors.forEach(this, function(name, editor) {
                    this.editors.put(editor.name, editor);
                    editor.render();
                });
                this.newEditors.clear();
            },
            
            resize : function(deltaW, deltaH) {
                this.activeEditors.forEach(this, function(k, v) {
                    v.resize(deltaW, deltaH);
                });
            },
            
            editorLoaded : function(name) {
                var editor = this.editors.get(name);
                editor.onEditorLoaded();

                this.registerCleanup(editor.getContainer(), name);                
                this.activeEditors.put(name, editor);
            },
            
            cleanup : function(name) {
                this.sizeState = null;
                var editor = this.activeEditors.remove(name);
                // TODO: works now??
                //Xinha.collectGarbageForIE(); 
            },

            registerCleanup : function(element, name) {
                //TODO: test
                //Dom.setStyle(element.parentNode, 'display', 'block');
                HippoAjax.registerDestroyFunction(element, this.cleanup, this, name);
            },
            
            saveEditors : function() {
                this.activeEditors.forEach(this, function(k, v) {
                    v.save();
                });
            },

            //TODO: should be something like widget.id
            saveByTextareaId : function(id) {
                var editor = this.getEditorByWidgetId(id);
                if(editor != null) {
                    editor.save();
                }
            },
            
            getEditorByWidgetId : function(id) {
                var values = this.activeEditors.valueSet();
                for ( var i = 0; i < values.length; ++i) {
                    var editor = values[i];
                    if(editor.getWidgetId() === id) {
                        return editor;
                    }
                }
                return null;
            }
        };
        
        YAHOO.hippo.BaseEditor = function(config) {
            if(!Lang.isString(config.name) || Lang.trim(config.name).length === 0) {
                throw new Error("Editor configuration parameter 'name' is undefined or empty");
            }
            this.name = config.name;
            this.config = config;
            this.lastData = null;
            this.container = null;
            this.sizeState = {w: 0, h: 0};
        };
        
        YAHOO.hippo.BaseEditor.prototype = {
            render : function() {
            },
            
            save : function() {
            },
            
            reset : function(config) {
                this.config = config;
                this.container = null;
            },
            
            destroy : function() {
            },
            
            resize : function(deltaW, deltaH) {
            },
            
            setSize : function(w, h) {
                this.sizeState.w = w;
                this.sizeState.h = h;
            },

            isFullScreen : function() {
                return false;
            },
            
            getWidgetId : function() {
                return null;
            },
            
            getContainer : function() {
                if(this.container == null) {
                    this.container = Dom.getAncestorBy(Dom.get(this.name), function(element) {
                        return Dom.hasClass(element, 'hippo-editor-field-subfield');
                    });
                }
                return this.container;
            }
            
        };

        YAHOO.hippo.XinhaEditor = function(config) {
            YAHOO.hippo.XinhaEditor.superclass.constructor.apply(this, arguments); 

            this.createStarted = false;
            this.pluginsLoaded = false;
            this.xinha = null;
            this.tooltip = null;
        };

        YAHOO.extend(YAHOO.hippo.XinhaEditor, YAHOO.hippo.BaseEditor, {
            
            render : function() {
                var container = this.getContainer();
                Dom.addClass(container, 'rte-preview-style');

                var containerHeight = this.calculateHeight(); 
                Dom.setStyle(container, 'height', containerHeight + 'px');
                
                var clickable = Dom.get(this.name);
                var clickableMargin = YAHOO.hippo.Dom.getMargin(clickable);
                var clickableHeight = containerHeight - clickableMargin.h;
                Dom.setStyle(clickable, 'height', clickableHeight + 'px');
                
                if(this.config.started) {
                    if(this.tooltip != null) {
                        this.hideTooltip();
                        this.createEditor();
                    } else {
                        Lang.later (300, this, this.createEditor);
                    }
                } else {
                    this.createTooltip("Click to edit");
                }
                
            },
            
            createEditor : function() {
                this.createStarted = true;

                var me = this;
                if (!Xinha.loadPlugins(this.config.plugins, function() { me.createEditor(); })) {
                    //Plugins not loaded yet, createAndRender will be recalled
                    return;
                }
                this.pluginsLoaded = true;

                //Don't use Xinha's default initSize method
                Xinha.prototype.initSize = function() { /* Nothing */ }

                //Fix for https://issues.onehippo.com/browse/HREPTWO-3990
                //IE7 can't handle innerHTML without rewriting relative links to absolute links.
                Xinha.prototype.setHTML = function(html) {
                    if ( !this.config.fullPage ) {
                        if(Xinha.is_ie && Xinha.ie_version == 7) {
                            try {
                                var reac = this.editorIsActivated();
                                if (reac) {
                                  this.deactivateEditor();
                                }
                                var html_re = /<html>((.|\n)*?)<\/html>/i;
                                html = html.replace(html_re, "$1");
                                this._doc.open("text/html","replace");
                                this._doc.write(html);
                                this._doc.close();
                                if (reac) {
                                  this.activateEditor();
                                }
                                this.setEditorEvents();
                          }catch(e){}
                        } else {
                            this._doc.body.innerHTML = html;
                        }
                    } else {
                        this.setFullHTML(html);
                    }
                    this._textArea.value = html;
                };
              
                //Xinha registers a resize event handler on the window.. not configurable so hack it out! And send patch to Xinha
                var func = Xinha.addDom0Event;
                Xinha.addDom0Event = function(el, ev, fn) {
                    if(el === window && ev === 'resize') {
                        return;
                    }
                    func.call(Xinha, el, ev, fn);
                }

                var xinhaConfig = new Xinha.Config();

                //Set Xinha built-in options
                xinhaConfig.getHtmlMethod = this.config.getHtmlMethod;
                xinhaConfig.convertUrlsToLinks = this.config.convertUrlsToLinks;
                xinhaConfig.flowToolbars = this.config.flowToolbars;
                xinhaConfig.killWordOnPaste = this.config.killWordOnPaste;
                xinhaConfig.showLoading = this.config.showLoading;
                xinhaConfig.statusBar = this.config.statusBar;

                //Set formatting options
                if(!Lang.isUndefined(this.config.formatBlock)) {
                    xinhaConfig.formatblock = this.config.formatBlock;
                }

                if (!Lang.isUndefined(this.config.styleSheets)
                        && this.config.styleSheets.length > 0) {
                    //load xinha stylesheets
                    xinhaConfig.pageStyleSheets = [this.config.styleSheets.length];
                    for ( var i = 0; i < this.config.styleSheets.length; i++) {
                        var ss = this.config.styleSheets[i];
                        if (!ss.indexOf("/") == 0 && !ss.indexOf("http://") == 0) {
                            ss = _editor_url + ss;
                        }
                        xinhaConfig.pageStyleSheets[i] = ss;
                    }
                }
                

                //make editors 
                var textarea = this.config.textarea;
                var xinha = Xinha.makeEditors([textarea], xinhaConfig, this.config.plugins)[textarea];
                var add = function(_base, _new) {
                    for ( var i = 0; i < _new.length; i++) {
                        _base[_new[i].key] = _new[i].value;
                    }
                    return _base;
                }
                
                //concatenate default properties with configured properties
                xinha.config = add(xinha.config, this.config.properties);
                
                if(this.config.toolbars.length == 0) {
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
                    xinha.config.toolbar = [ this.config.toolbars ];
                }

                for ( var i = 0; i < this.config.pluginProperties.length; i++) {
                    var pp = this.config.pluginProperties[i];
                    xinha.config[pp.name] = add(xinha.config[pp.name], pp.values);
                }

                this.xinha = xinha;
                xinha_editors[this.name] = xinha;
                
                var _name = this.name;
                this.xinha._onGenerate = function() {
                    YAHOO.hippo.EditorManager.editorLoaded(_name);
                }
                Xinha.startEditors([this.xinha]);
            },
            
            setSize : function(w, h) {
                this.xinha.sizeEditor(w + 'px', h + 'px', true, true);
                YAHOO.hippo.XinhaEditor.superclass.setSize.call(this, w, h);
            },
            
            /**
             * FIXME: Xinha looses the caret in FF on resize
             */
            resize : function(deltaW, deltaH) {
                //TODO: implement reasonable deltaH handling
                deltaH = 0;
                    
                var newWidth = this.sizeState.w + deltaW;
                var newHeight = this.sizeState.h + deltaH;
                //check if there is a Xinha instance in fullscreen
                if(this.isFullScreen()) {
                    var t = this.xinha.plugins["FullscreenCompatible"];
                    t.instance.editor._fullscreenCompatible(true);
                    t.instance.editor.originalSizes.x = newWidth;
                    t.instance.editor.originalSizes.y = newHeight;
                    this.sizeState.w = newWidth;
                    this.sizeState.h = newHeight;
                } else {
                    this.setSize(newWidth, newHeight);
                }
            },
            
            getWidgetId : function() {
                return this.config.textarea;  
            },
            
            isFullScreen : function() {
                var t = this.xinha.plugins["FullscreenCompatible"];
                return Lang.isObject(t) && Lang.isBoolean(t.instance.editor._isFullScreen) && t.instance.editor._isFullScreen;
            },
            
            calculateHeight : function() {
                var minHeight = 175; //below this threshold Xinha will take too much height and bleed out out the parent element
                for(var i=0; i<this.config.pluginProperties.length; ++i) {
                    var x = this.config.pluginProperties[i];
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
                
                y = Math.round(y);
                
                var containerMargin = YAHOO.hippo.Dom.getMargin(this.getContainer());
                return y - containerMargin.h;
            },
            
            createTooltip : function(defaultText) {
                var context = this.getContainer();
                var cc = Dom.getElementsByClassName ('rte-preview-area', 'div', context);
                if(Lang.isArray(cc) && cc.length > 0) {
                    context = cc[0];
                }
                
                this.tooltip = new YAHOO.widget.Tooltip('tt' + this.name, { 
                    context: context,
                    hidedelay: 10,
                    showdelay: 500,
                    text: this.getProperty("previewTooltipText", defaultText)
                });
            },
            
            hideTooltip : function() {
                if(this.tooltip != null) {
                    if(!Lang.isNull(this.tooltip.showProcId)) {
                        clearTimeout(this.tooltip.showProcId);
                        this.tooltip.showProcId = null;
                    }
                    if(!Lang.isNull(this.tooltip.hideProcId)) {
                        clearTimeout(this.tooltip.hideProcId);
                        this.tooltip.hideProcId = null;
                    }
                    this.tooltip.hide();
                }
            },
            
            destroyTooltip : function() {
                if(this.tooltip != null) {
                    this.hideTooltip();
                    this.tooltip.destroy();
                    this.tooltip = null;
                }
            },
            
            getProperty : function(key, defaultValue) {
                for(var i=0; i<this.config.properties.length; ++i) {
                    if(this.config.properties[i].key === key) {
                        return this.config.properties[i].value;
                    }
                }
                return Lang.isUndefined(defaultValue) ? null : defaultValue; 
            },
                    
            /**
             * Function called by Xinha after it finished creating the editor.
             * At this point we can take some final steps:
             * - set the editor's size according to the space available
             * - remove the 'preview ' styling from the container
             * - register this editor's cleanup function
             * - initialize the lastData property with the initial contents of this editor
             * - do some silly IE stuff
             * - store the editor in the activeEditors map
             * And finally give it focus. This last step is a bit blunt and should be 
             * implemented better.
             */

            onEditorLoaded : function() {
                this.destroyTooltip();
                
                var c = this.getContainer();
                if(c == null) {
                    return;
                }
                
                var w,h;
                if(Dom.hasClass(c, 'rte-preview-style')) {
                    Dom.removeClass(c, 'rte-preview-style');

                    var pr = Dom.getRegion(c);
                    var marges = YAHOO.hippo.Dom.getMargin(c);
                    w = pr.width - marges.w;
                    h = pr.height - marges.h;
                } else {
                    var dim = this.getDimensions(c, this.config)
                    w = dim.w;
                    h = dim.h;
                }
                this.setSize(w, h);
                
                this.lastData = this.xinha.getInnerHTML();
                
                //Workaround for http://issues.onehippo.com/browse/HREPTWO-2960
                //Test if IE8, than set&remove focus on Xinha to prevent UI lockup
                if(YAHOO.env.ua.ie >= 8) {
                  this.xinha.activateEditor();
                  this.xinha.focusEditor();
                  this.xinha.deactivateEditor();
                }
                if(this.config.focus) {
                    this.xinha.activateEditor();
                    this.xinha.focusEditor();
                }
            },
            
            save : function() {
                if(this.xinha.plugins['AutoSave']) {
                    try {
                        var data = this.xinha.getInnerHTML();
                        if(data != this.lastData) {
                            this.xinha.plugins['AutoSave'].instance.save();
                            this.lastData = data;
                        }
                    } catch(e) { }
                }
            },
            
            destroy : function() {
                this.destroyTooltip();
            }

            
        });
    })();

    YAHOO.hippo.EditorManager = new YAHOO.hippo.EditorManagerImpl();
    YAHOO.register("editormanager", YAHOO.hippo.EditorManager, {
        version : "2.7.0", build : "1799"
    });
}