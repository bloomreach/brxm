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

/*global Xinha, XinhaTools */
/**
 * @description
 * <p>
 * Todo
 * </p>
 * @namespace YAHOO.hippo
 * @requires hippoajax, hashmap, hippodom, layoutmanager, container
 * @module editormanager
 */


YAHOO.namespace('hippo');

if (!YAHOO.hippo.EditorManager) {

    /**
     * Xinha globals
     */
    window._editor_url = null;
    window._editor_lang = null;
    window._editor_skin = null;
    window.xinha_editors = [];

    (function() {
        "use strict";

        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang, HippoAjax = YAHOO.hippo.HippoAjax, HippoDom = YAHOO.hippo.Dom;

        function info(message) {
            YAHOO.log(message, "info", "EditorManager");
        }

        function error(message) {
            YAHOO.log(message, "error", "EditorManager");
        }

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
                window._editor_url = editorUrl;
                window._editor_lang = editorLang;
                window._editor_skin = editorSkin;

                //and load XinhaLoader.js
                // Internal method that is used to load Xinha plugins et al
                YAHOO.hippo.HippoAjax.loadJavascript(editorUrl + 'XinhaLoader.js', function() {
                    this.initialized = true;
                }, this);

                //Save open editors when a WicketAjax callback is executed
                var preCall = function() {
                    this.saveEditors();
                }.bind(this);
                Wicket.Ajax.registerPreCallHandler(preCall);
            },

            /**
             * Register a XinhaTextEditor. This method is called on dom load.
             * TODO: implement configurable editor instantiation
             */
            register : function(cfg) {
                //Start at the editor container element and traverse up to the nearest form.
                //During the traversal monitor for a parent element with classname 'column-wrapper',
                //if found save the current element as an alternative deltaProvider
                var deltaProvider = null,
                    context = null,
                    form,
                    div,
                    new_element;

                form = Dom.getAncestorBy(Dom.get(cfg.name), function(element) {
                    if (element.tagName.toLowerCase() === 'form') {
                        return true;
                    }
                    if (Dom.hasClass(element.parentNode, 'column-wrapper')) {
                        deltaProvider = element;
                    }
                    return false;
                });

                if (form === null) {
                    return;
                }

                if (!this.contexts.containsKey(form.id)) {
                    context = new YAHOO.hippo.EditorContext(form, deltaProvider);
                    this.contexts.put(form.id, context);
                } else {
                    context = this.contexts.get(form.id);
                }
                context.register(cfg);

                if(YAHOO.env.ua.ie > 0 && Lang.isUndefined(this.ieFocusWorkaroundElement)) {
                    div = document.createElement('div');
                    Dom.setStyle(div, 'position', 'absolute');
                    document.body.appendChild(div);
                    Dom.setXY(div, [-2000, -2000]);

                    new_element = document.createElement('input');
                    new_element.type = "text";
                    Dom.generateId(new_element, 'ie-workaround');
                    div.appendChild(new_element);

                    this.ieFocusWorkaroundElement = new_element;
                }

            },

            unregisterContext : function(id) {
                if (this.contexts.containsKey(id)) {
                    this.contexts.remove(id);
                    info('Context ' + id + ' removed');
                }
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
                var cleanupContexts = [], i, removedContext, len;
                this.forEachContext(function(key, context) {
                    if (document.getElementById(key)) {
                        context.render();
                    } else {
                        cleanupContexts.push(key);
                    }
                });

                len=cleanupContexts.length;
                for (i=0; i<len; i++) {
                    removedContext = this.contexts.remove(cleanupContexts[i]);
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
                if (editor !== null) {
                    editor.save(true);
                    info("Saved!");
                }
            },

            getEditorByWidgetId : function(id) {
                var editor = null;
                this.forEachContext(function(key, context) {
                    if (editor === null) {
                        editor = context.getEditorByWidgetId(id);
                    }
                });
                return editor;
            },

            deactivateEditor : function(id) {
                var editor = this.getEditorByWidgetId(id);
                if (editor !== null && editor.context.activeEditors.containsKey(editor.name)) {
                    editor.context.activeEditors.remove(editor.name);
                }
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
                var sizes, deltaW, deltaH;
                if (self.deltaProvider !== null) {
                    sizes = Dom.getRegion(self.deltaProvider);
                    // if deltaProvider exists, but has no region, it is not shown (display:none)
                    if (sizes === false) {
                        return;
                    }
                } else {
                    sizes = {width: unitSize.wrap.w, height: unitSize.wrap.h};
                }
                if (self.sizeState === null) {
                    self.sizeState = {w: sizes.width, h: sizes.height};
                } else {
                    deltaW = sizes.width - self.sizeState.w;
                    deltaH = sizes.height - self.sizeState.h;
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

            unregister: function(name) {
                info('unregister editor ' + name);

                if (this.newEditors.containsKey(name)) {
                    this.newEditors.remove(name);
                }
                if (this.editors.containsKey(name)) {
                    this.editors.remove(name);
                }
                if (this.activeEditors.containsKey(name)) {
                    var editor = this.activeEditors.remove(name);
                }

                //Context can be removed
                if (this.editors.size() === 0) {
                    YAHOO.hippo.EditorManager.unregisterContext(this.form.id);
                }
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
                var values = this.activeEditors.valueSet(), i, editor;
                for (i = 0; i < values.length; ++i) {
                    editor = values[i];
                    if (editor.getWidgetId() === id) {
                        return editor;
                    }
                }
                return null;
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
                var test, root, check;
                if (this.container === null) {
                    test = document.getElementById(this.name);
                    root = Dom.get(test || this.name);
                    check = function(el) {
                        return Dom.hasClass(el, 'hippo-editor-field-subfield');
                    };
                    if(root !== null) {
                        this.container = Dom.getAncestorBy(root, check);
                    }
                }
                return this.container;
            },

            info : function(msg) {
                info('Xinha[' + this.name + '] ' + msg);
            },

            error : function(msg) {
                error('Xinha[' + this.name + '] ' + msg);
            }

        };

        YAHOO.hippo.XinhaEditor = function(config) {
            YAHOO.hippo.XinhaEditor.superclass.constructor.apply(this, arguments);

            this.xinha = null;
            this.tooltip = null;
        };

        YAHOO.extend(YAHOO.hippo.XinhaEditor, YAHOO.hippo.BaseEditor, {

            render : function() {
                var container = this.getContainer();
                if(container === null) {
                    //error('Container element not found for editor ' + this.name);
                    throw new Error('Container element not found for editor ' + this.name);
                }
                YAHOO.hippo.HippoAjax.registerDestroyFunction(container, this.destroy, this);
                Dom.addClass(container, 'rte-preview-style');

                if (this.config.fullscreen && !this.config.started) {
                    this.renderFullscreenPreview(container);
                } else {
                    this.renderPreview(container);
                }

                if (this.config.started) {
                    if (this.tooltip !== null) {
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

            renderPreview : function(container) {
                var containerHeight, clickable, clickableMargin, clickableHeight;

                if (Dom.hasClass(container, 'rte-fullscreen-style')) {
                    // We are starting the editor from fullscreen preview so first restore to normal mode so
                    // Xinha is able to scale itself up&down correctly
                    this.removeFullscreenPreview(container);
                }

                containerHeight = this.config.height > -1 ? this.config.height : this.calculateHeight();
                Dom.setStyle(container, 'height', containerHeight + 'px');

                if (this.config.width > -1) {
                    Dom.setStyle(container, 'width', this.config.width + 'px');
                }

                //FIXME: Xinha doesn't like margins on the container, remove it the ugly way
                Dom.setStyle(container, 'margin-bottom', 0);

                clickable = Dom.get(this.name);
                clickableMargin = HippoDom.getMargin(clickable);
                clickableHeight = containerHeight - clickableMargin.h;
                Dom.setStyle(clickable, 'height', clickableHeight + 'px');
            },

            renderFullscreenPreview : function(container) {
                // Render preview in fullscreen. This state is currently only accessible from Xinha's fullscreen
                // mode, hence we assume all ancestor node are already set to static.
                window.scroll(0, 0);
                Dom.addClass(container, 'rte-fullscreen-style');
                container.style.position = 'absolute';
                container.style.height = '100%';
                container.style.width = '100%';
                container.style.zIndex = 999;

                var cRegion = Dom.getRegion(container), clickable = Dom.get(this.name);
                Dom.setStyle(clickable, 'height', (cRegion.height - HippoDom.getMargin(clickable).h) + 'px');
            },

            removeFullscreenPreview : function(container) {
                Dom.removeClass(container, 'rte-fullscreen-style');
                container.style.position = 'relative';
                container.style.width = '';
                this.removeStaticPosition(container);
            },

            createEditor : function() {
                var me = this, delegate, func, xinhaConfig, textarea, xinha, add, i, j, _name, ss, pp;

                if (!Xinha.loadPlugins(this.config.plugins, function() {
                    me.createEditor();
                })) {
                    //Plugins not loaded yet, createAndRender will be recalled
                    return;
                }

                //Don't use Xinha's default initSize method
                Xinha.prototype.initSize = function() { /* Nothing */
                };

                Xinha._stopEvent = function(ev) {
                    try {
                        if (ev.preventDefault !== undefined) {
                            ev.preventDefault();
                        } else {
                            ev.returnValue = false;
                        }
                        if (ev.stopPropagation !== undefined) {
                            ev.stopPropagation();
                        } else {
                            ev.cancelBubble = true;
                        }
                    } catch(ignore) {
                    }
                };

                //Fix for https://issues.onehippo.com/browse/HREPTWO-3990
                //IE7 can't handle innerHTML without rewriting relative links to absolute links.
                Xinha.prototype.setHTML = function(html) {
                    var editor = this, input, reac, doctype, i, html_re;
                    if (!this.config.fullPage) {
                        if (Xinha.is_ie && (Xinha.ie_version === 7 || Xinha.ie_version === 8)) {
                            input = html;
                            html = '';
                            //try {
                            reac = this.editorIsActivated();
                            if (reac) {
                                this.deactivateEditor();
                            }
                            this._doc.open("text/html", "replace");
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
                            if (editor.config.baseHref !== undefined && editor.config.baseHref !== null) {
                                html += "<base href=\"" + editor.config.baseHref + "\"/>\n";
                            }

                            html += Xinha.addCoreCSS();

                            if (editor.config.pageStyleSheets !== undefined) {
                                for (i = 0; i < editor.config.pageStyleSheets.length; i++) {
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

                            html_re = /<html>((.|\n)*?)<\/html>/i;
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
                if (Xinha.is_ie && Xinha.ie_version === 7) {
                    delegate = Xinha.prototype._editorEvent;
                    Xinha.prototype._editorEvent = function(ev) {
                        try {
                            if (!Lang.isUndefined(ev.type)) {
                                delegate.call(this, ev);
                            }
                        } catch(ignore) {
                        }
                    };
                }

                //Xinha registers a resize event handler on the window.. not configurable so hack it out! And send patch to Xinha
                func = Xinha.addDom0Event;
                Xinha.addDom0Event = function(el, ev, fn) {
                    if (el === window && ev === 'resize') {
                        return;
                    }
                    func.call(Xinha, el, ev, fn);
                };

                xinhaConfig = new Xinha.Config();

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
                    for (i = 0; i < this.config.styleSheets.length; i++) {
                        ss = this.config.styleSheets[i];
                        if (ss.indexOf("/") !== 0 && ss.indexOf("http://") !== 0) {
                            ss = window._editor_url + ss;
                        }
                        xinhaConfig.pageStyleSheets[i] = ss;
                    }
                }


                //make editors
                textarea = this.config.textarea;
                xinha = Xinha.makeEditors([textarea], xinhaConfig, this.config.plugins)[textarea];
                add = function(_base, _new) {
                    var i;

                    _base = _base === undefined || _base === null ? {} : _base;
                    for (i = 0; i < _new.length; i++) {
                        _base[_new[i].key] = _new[i].value;
                    }
                    return _base;
                };

                //concatenate default properties with configured properties
                xinha.config = add(xinha.config, this.config.properties);

                if (this.config.toolbars.length === 0) {
                    //Load toolbar with all Xinha default buttons
                    //remove button popupeditor
                    outerLoop:
                            for (i = 0; i < xinha.config.toolbar.length; i++) {
                                for (j = 0; j < xinha.config.toolbar[i].length; j++) {
                                    if (xinha.config.toolbar[i][j] === 'popupeditor') {
                                        xinha.config.toolbar[i].splice(j, 1);//remove element from array
                                        break outerLoop;
                                    }
                                }
                            }
                } else {
                    xinha.config.toolbar = [ this.config.toolbars ];
                }

                for (i = 0; i < this.config.pluginProperties.length; i++) {
                    pp = this.config.pluginProperties[i];
                    xinha.config[pp.name] = add(xinha.config[pp.name], pp.values);
                }

                this.xinha = xinha;
                window.xinha_editors[this.name] = xinha;

                _name = this.name;
                this.xinha._onGenerate = function() {
                    this.onEditorLoaded();
                    this.context.editorLoaded(_name);

                    if (this.config.fullscreen) {
                        this.xinha._fullscreen(false);
                        window.setTimeout(XinhaTools.proxy(this, function() {
                            this.xinha.focusEditor();
                        }), 1);
                    }

                }.bind(this);

                Dom.setStyle(this.name, 'visibility', 'hidden');
                Xinha.startEditors([this.xinha]);
            },

            setSize : function(w, h) {
                this.xinha.sizeEditor(w + 'px', h + 'px', true, true);
                YAHOO.hippo.XinhaEditor.superclass.setSize.call(this, w, h);
            },

            resize : function(deltaW, deltaH) {
                var newWidth, newHeight, lastRange, fsp, node;

                //TODO: implement reasonable deltaH handling
                deltaH = 0;

                newWidth = this.sizeState.w + deltaW;
                newHeight = this.sizeState.h + deltaH;

                lastRange = null;
                if (Xinha._currentlyActiveEditor === this.xinha) {
                    lastRange = this.xinha.saveSelection();
                }

                //check if there is a Xinha instance in fullscreen
                if (this.isFullScreen()) {
                    fsp = this.getFullscreenPlugin();
                    if(fsp !== null) {
                        fsp.instance.editor._fullscreen(true);
                        this.sizeState.w = newWidth;
                        this.sizeState.h = newHeight;
                    }
                } else {
                    this.setSize(newWidth, newHeight);
                }
                if (lastRange !== null) {
                    if (Xinha.is_gecko) {
                        try {
                            node = YAHOO.util.Selector.query('td.toolbarElement', this._toolbar, true);
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
                return pl !== null && pl.instance.editor._isFullScreen;
            },

            getFullscreenPlugin : function() {
                var i, t, candidates = ['FullscreenCompatible', 'FullScreen'];
                for(i = 0; i < candidates.length; i++) {
                    t = this.xinha.plugins[candidates[i]];
                    if(!Lang.isUndefined(t) && Lang.isObject(t.instance) && Lang.isObject(t.instance.editor)) {
                        return t;
                    }
                }
                return null;
            },

            calculateHeight : function() {
                var i, j, x, vHeight, p, yy, y, containerMargin,
                    minHeight = 175; //below this threshold Xinha will take too much height and bleed out out the parent element
                for (i = 0; i < this.config.pluginProperties.length; ++i) {
                    x = this.config.pluginProperties[i];
                    if (x.name === 'AutoResize') {
                        for (j = 0; j < x.values.length; ++j) {
                            if (x.values[j].key === 'minHeight') {
                                minHeight = x.values[j].value;
                                break;
                            }
                        }
                        break;
                    }
                }
                vHeight = Dom.getViewportHeight();
                p = vHeight / minHeight;
                yy = 0;
                if (p >= 2.2) {
                    yy = (minHeight / 20) * p;
                }

                y = minHeight;
                if (vHeight - vHeight > 0) {  //what should this do?
                    if (y - yy > minHeight) {
                        y -= yy;
                    }
                } else {
                    y += yy;
                }

                y = Math.round(y);

                containerMargin = HippoDom.getMargin(this.getContainer());
                return y - containerMargin.h;
            },

            createTooltip : function(defaultText) {
                var context = this.getContainer(), cc;
                cc = Dom.getElementsByClassName('rte-preview-area', 'div', context);
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
                if (this.tooltip !== null) {
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
                if (this.tooltip !== null) {
                    this.hideTooltip();
                    this.tooltip.destroy();
                    this.tooltip = null;
                }
            },

            getProperty : function(key, defaultValue) {
                var i;
                for (i = 0; i < this.config.properties.length; ++i) {
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
                var c, w, h, pr, marges;

                this.destroyTooltip();

                c = this.getContainer();
                if (c === null) {
                    return;
                }

                if (Dom.hasClass(c, 'rte-preview-style')) {
                    Dom.removeClass(c, 'rte-preview-style');
                }
                pr = Dom.getRegion(c);
                marges = HippoDom.getMargin(c);

                w = this.config.width === -1 ? pr.width - marges.w : this.config.width;
                h = this.config.height === -1 ? pr.height - marges.h : this.config.height;

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
                if (this.xinha.plugins.AutoSave) {
                    try {
                        var data = this.xinha.getInnerHTML(), success, failure;
                        if (data !== this.lastData) {
                            success = function() {
                                info('Content saved.');
                                this.lastData = data;
                            }.bind(this);
                            failure = function() {
                                error('failed to save');
                            }.bind(this);

                            this.xinha.plugins.AutoSave.instance.save(throttled, success, failure);
                        }
                    } catch(e) {
                        error('Error retrieving innerHTML from xinha, skipping save');
                    }
                }
            },

            /**
             * Copied form the sizeDown method of the full-screen module that ships with Xinha.
             */
            removeStaticPosition : function(ancestor) {
                var c, i;

                //remove static position
                while (((ancestor = ancestor.parentNode) !== undefined) && ancestor.style) {
                    ancestor.style.position = ancestor._xinha_fullScreenOldPosition;
                    ancestor._xinha_fullScreenOldPosition = null;

                    if (ancestor.className === 'yui-layout-doc') {
                        for (i = 0; i < ancestor.childNodes.length; i++) {
                            c = ancestor.childNodes[i];
                            c.style.position = c._xinha_fullScreenOldPosition;
                            c._xinha_fullScreenOldPosition = null;
                        }
                    }
                }
            },

            destroy : function() {
                this.destroyTooltip();
                if (this.xinha) {
                    window.xinha_editors.remove(this.name);

                    //If we are using MSIE and this Xinha is active, put focus
                    //in a hidden field that is maintained by the EditorManager
                    //to workaround an issue that caused the UI to lock up
                    if (Xinha.is_ie && Xinha._currentlyActiveEditor &&
                            Xinha._currentlyActiveEditor === this.xinha &&
                            YAHOO.hippo.EditorManager.ieFocusWorkaroundElement) {
                        YAHOO.hippo.EditorManager.ieFocusWorkaroundElement.focus();
                    }
                }
                this.context.unregister(this.name);

            }
        });
    }());

    YAHOO.hippo.EditorManager = new YAHOO.hippo.EditorManagerImpl();
    YAHOO.register("editormanager", YAHOO.hippo.EditorManager, {
        version : "2.7.0", build : "1799"
    });
}
