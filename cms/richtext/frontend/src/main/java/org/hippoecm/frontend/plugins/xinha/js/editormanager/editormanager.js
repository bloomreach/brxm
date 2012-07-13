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

"use strict";

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

        var info = function(message) {
            // YAHOO.log(message, "info", "EditorManager");
        };

        var error = function(message) {
            // YAHOO.log(message, "error", "EditorManager");
        };

        /**
         * The editor-manager controls the life-cycle of Xinha editors.
         */
        YAHOO.hippo.EditorManagerImpl = function() {
            this.initialized = false;
            this.contexts = new YAHOO.hippo.HashMap();
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
                //Start at the editor container element and traverse up to the nearest form.
                //During the traversal monitor for a parent element with classname 'column-wrapper',
                //if found save the current element as an alternative deltaProvider
                var deltaProvider = null;
                var form = Dom.getAncestorBy(Dom.get(cfg.name), function(element) {
                    if (element.tagName.toLowerCase() === 'form') {
                        return true;
                    }
                    if (Dom.hasClass(element.parentNode, 'column-wrapper')) {
                        deltaProvider = element;
                    }
                    return false;
                });

                if (form == null) {
                    return;
                }

                var context = null;
                if (!this.contexts.containsKey(form.id)) {
                    context = new YAHOO.hippo.EditorContext(form, deltaProvider);
                    this.contexts.put(form.id, context);
                } else {
                    context = this.contexts.get(form.id);
                }
                context.register(cfg);
            },

            forEachContext: function(cb, obj) {
                this.contexts.forEach(this, function(k, v) {
                    cb.apply(obj || this, [k, v]);
                });
            },

            /**
             * Called upon window.load (maybe LayoutManager.afterRender would be better?)
             * Starts rendering of all Xinha on current page.
             */
            render : function() {
                if (!this.initialized) {
                    //XinhaLoader.js hasn't been added to the head section yet, wait a little longer
                    Lang.later(100, this, this.render);
                    return;
                }
                var cleanupContexts = [];
                this.forEachContext(function(key, context) {
                    if (document.getElementById(key)) {
                        context.render();
                    } else {
                        cleanupContexts.push(key);
                    }
                });

                for (var i=0,len=cleanupContexts.length; i<len; i++) {
                    var removedContext = this.contexts.remove(cleanupContexts[i]);
                    removedContext.destroy();
                }
            },

            saveEditors : function() {
                this.forEachContext(function(key, context) {
                    context.saveEditors();
                });
            },

            //TODO: should be something like widget.id
            saveByTextareaId : function(id) {
                var editor = this.getEditorByWidgetId(id);
                if (editor != null) {
                    editor.save(true);
                    info("Saved!");
                }
            },

            getEditorByWidgetId : function(id) {
                var editor = null;
                this.forEachContext(function(key, context) {
                    if (editor == null) {
                        editor = context.getEditorByWidgetId(id);
                    }
                });
                return editor;
            }

        };

        YAHOO.hippo.EditorContext = function(form, deltaProvider) {
            this.editors = new YAHOO.hippo.HashMap();
            this.activeEditors = new YAHOO.hippo.HashMap();
            this.newEditors = new YAHOO.hippo.HashMap();

            this.sizeState = null;
            this.form = form;
            this.deltaProvider = deltaProvider;

            var self = this;
            //register the form as a resize listener
            YAHOO.hippo.LayoutManager.registerResizeListener(this.form, this, function(unitSize) {
                var sizes;
                if (self.deltaProvider != null) {
                    sizes = Dom.getRegion(self.deltaProvider);
                    // if deltaProvider exists, but has no region, it is not shown (display:none)
                    if (sizes == false) {
                        return;
                    }
                } else {
                    sizes = {width: unitSize.wrap.w, height: unitSize.wrap.h};
                }
                if (self.sizeState == null) {
                    self.sizeState = {w: sizes.width, h: sizes.height};
                } else {
                    var deltaW = sizes.width - self.sizeState.w;
                    var deltaH = sizes.height - self.sizeState.h;
                    self.resize(deltaW, deltaH);
                    self.sizeState = {w: sizes.width, h: sizes.height};
                }
            }, true);
        };

        YAHOO.hippo.EditorContext.prototype = {

            /**
             * Register a XinhaTextEditor. This method is called on dom load.
             * TODO: implement configurable editor instantiation
             */
            register: function(cfg) {
                var editor = null;
                if (this.newEditors.containsKey((cfg.name))) {
                    editor = this.newEditors.remove(cfg.name);
                    editor.reset(cfg);
                } else if (this.editors.containsKey(cfg.name)) {
                    editor = this.editors.remove(cfg.name);
                    editor.reset(cfg);
                } else {
                    editor = new YAHOO.hippo.XinhaEditor(cfg, this);
                }
                this.newEditors.put(cfg.name, editor);
            },

            render: function() {
                this.newEditors.forEach(this, function(name, editor) {
                    if (document.getElementById(editor.name)) {
                        this.editors.put(editor.name, editor);
                        editor.render();
                    }
                });
                this.newEditors.clear();
            },

            resize : function(deltaW, deltaH) {
                this.forEachActiveEditor(function(name, editor) {
                    editor.resize(deltaW, deltaH);
                });
            },

            editorLoaded : function(name) {
                var editor = this.editors.get(name);
                this.activeEditors.put(name, editor);

                info('Editor successfully loaded');
            },

            saveEditors : function() {
                this.forEachActiveEditor(function(name, editor) {
                    editor.save(false);
                });
            },

            getEditorByWidgetId : function(id) {
                var values = this.activeEditors.valueSet();
                for (var i = 0; i < values.length; ++i) {
                    var editor = values[i];
                    if (editor.getWidgetId() === id) {
                        return editor;
                    }
                }
            },

            forEachActiveEditor : function(callback) {
                this.activeEditors.forEach(this, function(name, editor) {
                    if (document.getElementById(name)) {
                        callback(name, editor);
                    }
                });
            },

            destroy : function() {
                this.editors.forEach(this, function(name, editor) {
                    editor.destroy();
                });
            }

        };

        YAHOO.hippo.BaseEditor = function(config, context) {
            if (!Lang.isString(config.name) || Lang.trim(config.name).length === 0) {
                throw new Error("Editor configuration parameter 'name' is undefined or empty");
            }
            this.name = config.name;
            this.config = config;
            this.lastData = null;
            this.container = null;
            this.sizeState = {w: 0, h: 0};
            this.context = context;
        };

        YAHOO.hippo.BaseEditor.prototype = {
            render : function() {
                info('Base class render');
            },

            save : function(throttled) {
                info('Base class save');
            },

            reset : function(config) {
                this.config = config;
                this.container = null;
            },

            destroy : function() {
                info('Base class destroy');
            },

            resize : function(deltaW, deltaH) {
                info("Base class resize");
            },

            setSize : function(w, h) {
                var oW = this.sizeState.w, oH = this.sizeState.h, dW = w - oW, dH = h - oH;
                info('setSize: old[' + oW + ', ' + oH + '] new[' + w + ', ' + h + '] delta[' + dW + ', ' + dH + ']');

                this.sizeState.w = w;
                this.sizeState.h = h;
            },

            isFullScreen : function() {
                return false;
            },

            getWidgetId : function() {
                info("Base class getWidgetId");
                return null;
            },

            getContainer : function() {
                if (this.container == null) {
                    this.container = Dom.getAncestorBy(Dom.get(this.name), function(element) {
                        return Dom.hasClass(element, 'hippo-editor-field-subfield');
                    });
                }
                return this.container;
            },

            info : function(msg) {
                YAHOO.log('Xinha[' + this.name + '] ' + msg, "info", "EditorManager");
                //console.log('Xinha[' + this.name + '] ' + msg);
            },

            error : function(msg) {
                YAHOO.log('Xinha[' + this.name + '] ' + msg, "error", "EditorManager");
                //console.error('Xinha[' + this.name + '] ' + msg);
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
                //FIXME: Xinha doesn't like margins on the container, remove it the ugly way
                Dom.setStyle(container, 'margin-bottom', 0);

                var clickable = Dom.get(this.name);
                var clickableMargin = YAHOO.hippo.Dom.getMargin(clickable);
                var clickableHeight = containerHeight - clickableMargin.h;
                Dom.setStyle(clickable, 'height', clickableHeight + 'px');

                if (this.config.started) {
                    if (this.tooltip != null) {
                        this.hideTooltip();
                        this.createEditor();
                        info('Editor created directly');
                    } else {
                        info('Editor created with a slight delay');
                        Lang.later(300, this, this.createEditor);
                    }
                } else {
                    this.createTooltip("Click to edit");
                }

            },

            createEditor : function() {
                this.createStarted = true;

                var me = this;
                if (!Xinha.loadPlugins(this.config.plugins, function() {
                    me.createEditor();
                })) {
                    //Plugins not loaded yet, createAndRender will be recalled
                    return;
                }
                this.pluginsLoaded = true;

                //Don't use Xinha's default initSize method
                Xinha.prototype.initSize = function() { /* Nothing */
                };

                Xinha._stopEvent = function(ev) {
                    try {
                        if (typeof ev.preventDefault !== 'undefined') {
                            ev.preventDefault();
                        } else {
                            ev.returnValue = false;
                        }
                        if (typeof ev.stopPropagation !== 'undefined') {
                            ev.stopPropagation();
                        } else {
                            ev.cancelBubble = true;
                        }
                    } catch(ignore) {
                    }
                }

                //Fix for https://issues.onehippo.com/browse/HREPTWO-3990
                //IE7 can't handle innerHTML without rewriting relative links to absolute links.
                Xinha.prototype.setHTML = function(html) {
                    var editor = this;
                    if (!this.config.fullPage) {
                        if (Xinha.is_ie && (Xinha.ie_version == 7 || Xinha.ie_version == 8)) {
                            var input = html;
                            html = '';
                            //try {
                            var reac = this.editorIsActivated();
                            if (reac) {
                                this.deactivateEditor();
                            }
                            this._doc.open("text/html", "replace");
                            var doctype;
                            if (editor.config.browserQuirksMode === false) {
                                doctype = '<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">';
                            } else if (editor.config.browserQuirksMode === true) {
                                doctype = '';
                            } else {
                                doctype = Xinha.getDoctype(document);
                            }

                            html += doctype + "\n";
                            html += "<html>\n";
                            html += "<head>\n";
                            html += "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + editor.config.charSet + "\">\n";
                            if (typeof editor.config.baseHref != 'undefined' && editor.config.baseHref !== null) {
                                html += "<base href=\"" + editor.config.baseHref + "\"/>\n";
                            }

                            html += Xinha.addCoreCSS();

                            if (typeof editor.config.pageStyleSheets !== 'undefined') {
                                for (var i = 0; i < editor.config.pageStyleSheets.length; i++) {
                                    if (editor.config.pageStyleSheets[i].length > 0) {
                                        html += "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + editor.config.pageStyleSheets[i] + "\">";
                                    }
                                }
                            }

                            if (editor.config.pageStyle) {
                                html += "<style type=\"text/css\">\n" + editor.config.pageStyle + "\n</style>";
                            }

                            html += "</head>\n";
                            html += "<body" + (editor.config.bodyID ? (" id=\"" + editor.config.bodyID + "\"") : '') + (editor.config.bodyClass ? (" class=\"" + editor.config.bodyClass + "\"") : '') + ">\n";

                            var html_re = /<html>((.|\n)*?)<\/html>/i;
                            html += input.replace(html_re, "$1");
                            html += "</body>\n";
                            html += "</html>";

                            this._doc.write(html);
                            this._doc.close();
                            if (reac) {
                                this.activateEditor();
                            }
                            this.setEditorEvents();
//                            }catch (e) {
//                                alert(e);
//                            }
                        } else {
                            this._doc.body.innerHTML = html;
                        }
                    } else {
                        this.setFullHTML(html);
                    }
                    this._textArea.value = html;
                };

                //Fix for HREPTWO-4060: Xinha undo action broken under Internet Explorer
                //Sometimes an event is passed that will throw an error when ev.type is accessed. Wrap the call with a
                //test for ev.type.
                if (Xinha.is_ie && Xinha.ie_version == 7) {
                    var delegate = Xinha.prototype._editorEvent;
                    Xinha.prototype._editorEvent = function(ev) {
                        try {
                            if (!Lang.isUndefined(ev.type)) {
                                delegate.call(this, ev)
                            }
                        } catch(ignore) {
                        }
                    }
                }

                //Xinha registers a resize event handler on the window.. not configurable so hack it out! And send patch to Xinha
                var func = Xinha.addDom0Event;
                Xinha.addDom0Event = function(el, ev, fn) {
                    if (el === window && ev === 'resize') {
                        return;
                    }
                    func.call(Xinha, el, ev, fn);
                };

                var xinhaConfig = new Xinha.Config();

                //Set Xinha built-in options
                xinhaConfig.getHtmlMethod = this.config.getHtmlMethod;
                xinhaConfig.convertUrlsToLinks = this.config.convertUrlsToLinks;
                xinhaConfig.flowToolbars = this.config.flowToolbars;
                xinhaConfig.killWordOnPaste = this.config.killWordOnPaste;
                xinhaConfig.showLoading = this.config.showLoading;
                xinhaConfig.statusBar = this.config.statusBar;
                xinhaConfig.only7BitPrintablesInURLs = this.config.only7BitPrintablesInURLs;

                //Set formatting options
                if (!Lang.isUndefined(this.config.formatBlock)) {
                    xinhaConfig.formatblock = this.config.formatBlock;
                }

                if (!Lang.isUndefined(this.config.styleSheets)
                        && this.config.styleSheets.length > 0) {
                    //load xinha stylesheets
                    xinhaConfig.pageStyleSheets = [this.config.styleSheets.length];
                    for (var i = 0; i < this.config.styleSheets.length; i++) {
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
                    _base = typeof _base === 'undefined' || _base == null ? {} : _base;
                    for (var i = 0; i < _new.length; i++) {
                        _base[_new[i].key] = _new[i].value;
                    }
                    return _base;
                };

                //concatenate default properties with configured properties
                xinha.config = add(xinha.config, this.config.properties);

                if (this.config.toolbars.length == 0) {
                    //Load toolbar with all Xinha default buttons
                    //remove button popupeditor
                    outerLoop:
                            for (var i = 0; i < xinha.config.toolbar.length; i++) {
                                for (var j = 0; j < xinha.config.toolbar[i].length; j++) {
                                    if (xinha.config.toolbar[i][j] == 'popupeditor') {
                                        xinha.config.toolbar[i].splice(j, 1);//remove element from array
                                        break outerLoop;
                                    }
                                }
                            }
                } else {
                    xinha.config.toolbar = [ this.config.toolbars ];
                }

                for (var i = 0; i < this.config.pluginProperties.length; i++) {
                    var pp = this.config.pluginProperties[i];
                    xinha.config[pp.name] = add(xinha.config[pp.name], pp.values);
                }

                this.xinha = xinha;
                xinha_editors[this.name] = xinha;

                var _name = this.name;
                this.xinha._onGenerate = function() {
                    this.onEditorLoaded();
                    this.context.editorLoaded(_name);
                }.bind(this);

                Dom.setStyle(this.name, 'visibility', 'hidden');
                Xinha.startEditors([this.xinha]);
            },

            setSize : function(w, h) {
                this.xinha.sizeEditor(w + 'px', h + 'px', true, true);
                YAHOO.hippo.XinhaEditor.superclass.setSize.call(this, w, h);
            },

            resize : function(deltaW, deltaH) {
                //TODO: implement reasonable deltaH handling
                deltaH = 0;

                var newWidth = this.sizeState.w + deltaW;
                var newHeight = this.sizeState.h + deltaH;

                var lastRange = null;
                if (Xinha._currentlyActiveEditor === this.xinha) {
                    lastRange = this.xinha.saveSelection();
                }

                //check if there is a Xinha instance in fullscreen
                if (this.isFullScreen()) {
                    var fsp = this.getFullscreenPlugin();
                    if(fsp != null) {
                        fsp.instance.editor._fullscreen(true);
                        this.sizeState.w = newWidth;
                        this.sizeState.h = newHeight;
                    }
                } else {
                    this.setSize(newWidth, newHeight);
                }
                if (lastRange != null) {
                    if (Xinha.is_gecko) {
                        try {
                            var node = YAHOO.util.Selector.query('td.toolbarElement', this._toolbar, true);
                            YAHOO.util.Dom.getFirstChild(node).focus();
                        } catch(e) {
                            //error not important for user
                        }
                    }

                    this.xinha.restoreSelection(lastRange);
                    this.xinha.focusEditor();
                }
            },

            getWidgetId : function() {
                return this.config.textarea;
            },

            /**
            * Check if the deprecated FullscreenCompatible plugin is loaded, otherwise check for the builtin FullScreen
             * module.
            */
            isFullScreen : function() {
                var pl = this.getFullscreenPlugin();
                return pl != null && pl.instance.editor._isFullScreen
            },

            getFullscreenPlugin : function() {
                var candidates = ['FullscreenCompatible', 'FullScreen'];
                for(var i=0; i<candidates.length; i++) {
                    var t = this.xinha.plugins[candidates[i]];
                    if(!Lang.isUndefined(t) && Lang.isObject(t.instance) && Lang.isObject(t.instance.editor)) {
                        return t;
                    }
                }
                return null;
            },

            calculateHeight : function() {
                var minHeight = 175; //below this threshold Xinha will take too much height and bleed out out the parent element
                for (var i = 0; i < this.config.pluginProperties.length; ++i) {
                    var x = this.config.pluginProperties[i];
                    if (x.name === 'AutoResize') {
                        for (var j = 0; j < x.values.length; ++j) {
                            if (x.values[j].key === 'minHeight') {
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
                if (p >= 2.2) {
                    yy = (minHeight / 20) * p;
                }

                var y = minHeight;
                if (vHeight - vHeight > 0) {  //what should this do?
                    if (y - yy > minHeight) {
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
                var cc = Dom.getElementsByClassName('rte-preview-area', 'div', context);
                if (Lang.isArray(cc) && cc.length > 0) {
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
                if (this.tooltip != null) {
                    if (!Lang.isNull(this.tooltip.showProcId)) {
                        clearTimeout(this.tooltip.showProcId);
                        this.tooltip.showProcId = null;
                    }
                    if (!Lang.isNull(this.tooltip.hideProcId)) {
                        clearTimeout(this.tooltip.hideProcId);
                        this.tooltip.hideProcId = null;
                    }
                    this.tooltip.hide();
                }
            },

            destroyTooltip : function() {
                if (this.tooltip != null) {
                    this.hideTooltip();
                    this.tooltip.destroy();
                    this.tooltip = null;
                }
            },

            getProperty : function(key, defaultValue) {
                for (var i = 0; i < this.config.properties.length; ++i) {
                    if (this.config.properties[i].key === key) {
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
                if (c == null) {
                    return;
                }

                var w,h;
                if (Dom.hasClass(c, 'rte-preview-style')) {
                    Dom.removeClass(c, 'rte-preview-style');
                }
                var pr = Dom.getRegion(c);
                var marges = YAHOO.hippo.Dom.getMargin(c);
                w = pr.width - marges.w;
                h = pr.height - marges.h;

                this.setSize(w, h);

                Dom.setStyle(this.name, 'visibility', 'visible');

                //Workaround for http://issues.onehippo.com/browse/HREPTWO-2960
                //Test if IE8, than set&remove focus on Xinha to prevent UI lockup
                if (YAHOO.env.ua.ie >= 8) {
                    this.xinha.activateEditor();
                    this.xinha.focusEditor();
                    this.xinha.deactivateEditor();
                }
                if (this.config.focus) {
                    this.xinha.activateEditor();
                    this.xinha.focusEditor();
                }

                this.lastData = this.xinha.getInnerHTML();
            },

            save : function(throttled) {
                if (this.xinha.plugins['AutoSave']) {
                    try {
                        var data = this.xinha.getInnerHTML();
                        if (data != this.lastData) {
                            this.xinha.plugins['AutoSave'].instance.save(throttled);
                            this.lastData = data;

                            info('Content saved.');
                        }
                    } catch(e) {
                        error('Error retrieving innerHTML from xinha, skipping save');
                    }
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
