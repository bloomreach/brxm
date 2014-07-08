/*
 * Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
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
 * Provides an interface for components to register on handling WicketAjax 
 * component update
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hashmap
 * @module hippoajax
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.HippoAjax) { // Ensure only one hippo ajax exists
    (function() {
        "use strict";

        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang, wicketProcessComponent, wicketLogError;

        YAHOO.hippo.HippoAjaxImpl = function() {};

        YAHOO.hippo.HippoAjaxImpl.prototype = {
            prefix : 'hippo-destroyable-',
            callbacks : new YAHOO.hippo.HashMap(),
            _scrollbarWidth : null,

            loadJavascript : function(url, callback, scope) {
                var evt = !YAHOO.env.ua.ie ? "onload" : 'onreadystatechange',
                    element = document.createElement("script");
                element.type = "text/javascript";
                element.src = url;
                if (callback) {
                    element[evt] = function() {
                        if (YAHOO.env.ua.ie && (!(/loaded|complete/.test(window.event.srcElement.readyState)))) {
                            return;
                        }
                        callback.call(scope);
                        element[evt] = null;
                    };
                }
                document.getElementsByTagName("head")[0].appendChild(element);
            },

            getScrollbarWidth : function() {
                var inner, outer, w1, w2;

                if (this._scrollbarWidth === null) {
                    inner = document.createElement('p');
                    inner.style.width = "100%";
                    inner.style.height = "200px";

                    outer = document.createElement('div');
                    outer.style.position = "absolute";
                    outer.style.top = "0px";
                    outer.style.left = "0px";
                    outer.style.visibility = "hidden";
                    outer.style.width = "200px";
                    outer.style.height = "150px";
                    outer.style.overflow = "hidden";
                    outer.appendChild(inner);

                    document.body.appendChild(outer);
                    w1 = inner.offsetWidth;
                    outer.style.overflow = 'scroll';
                    w2 = inner.offsetWidth;
                    if (w1 === w2) {
                        w2 = outer.clientWidth;
                    }

                    document.body.removeChild(outer);

                    this._scrollbarWidth = w1 - w2;
                }
                return this._scrollbarWidth;
            },

            getScrollbarHeight : function() {
                //I'm lazy so return scrollbarWidth for now
                return this.getScrollbarWidth();
            },

            registerDestroyFunction : function(el, func, context, args) {
                var id = this.prefix + Dom.generateId();
                el.HippoDestroyID = id;
                if (!Lang.isArray(args)) {
                    args = [args];
                }
                this.callbacks.put(id, {func: func, context: context, args: args});
            },

            callDestroyFunction : function(id) {
                if (this.callbacks.containsKey(id)) {
                    var callback = this.callbacks.remove(id);
                    callback.func.apply(callback.context, callback.args);
                }
            },

            cleanupElement: function(el) {
                var els, i, len;
                //console.time("HippoAjax.processComponent.cleanup");
                els = YAHOO.util.Dom.getElementsBy(function(node) {
                    return !YAHOO.lang.isUndefined(node.HippoDestroyID);
                }, null, el);

                for (i = 0, len = els.length; i < len; i++) {
                    YAHOO.hippo.HippoAjax.callDestroyFunction(els[i].HippoDestroyID);
                }
            }
        };

        YAHOO.hippo.HippoAjax = new YAHOO.hippo.HippoAjaxImpl();

        wicketProcessComponent = Wicket.Ajax.Call.prototype.processComponent;
        Wicket.Ajax.Call.prototype.processComponent = function(context, node) {
            var compId, el;

            compId = node.getAttribute("id");
            el = YAHOO.util.Dom.get(compId);

            if (el !== null && el !== undefined) {
                YAHOO.hippo.HippoAjax.cleanupElement(el);

                //console.time('HippoAjax.processComponent.purgeElement');
                YAHOO.util.Event.purgeElement(el, true);
                //console.timeEnd('HippoAjax.processComponent.purgeElement');
                //console.timeEnd("HippoAjax.processComponent.cleanup");
            }
            wicketProcessComponent.call(this, context, node);
        };

        wicketLogError = Wicket.Log.error;
        Wicket.Log.error = function(msg) {
            if (Lang.isFunction(console.error)) {
                console.error(msg);
            }
            wicketLogError.apply(this, [msg]);
        };
/*
        Wicket.Ajax.Call.prototype.processEvaluation = function(context, node) {
            context.steps.push(function(notify) {
                // get the javascript body
                var text = Wicket._readTextNode(node),
                    encoding = node.getAttribute("encoding"),
                    res;

                // unescape it if necessary
                if (encoding !== null) {
                    text = Wicket.decode(encoding, text);
                }

                // test if the javascript is in form of identifier|code
                // if it is, we allow for letting the javascript decide when the rest of processing will continue
                // by invoking identifier();
                res = text.match(new RegExp("^([a-z|A-Z_][a-z|A-Z|0-9_]*)\\|((.|\\n)*)$"));

                if (res !== null) {

                    text = "var f = function(" + res[1] + ") {" + res[2] + "};";

                    try {
                        // do the evaluation
                        eval.call(window, text + "\n//@ sourceURL=wicket-eval.js");
                        f(notify);
                    } catch (e1) {
                        Wicket.Log.error("Wicket.Ajax.Call.processEvaluation: Exception evaluating javascript: " + e1);
                        Wicket.Log.error("Wicket.Ajax.Call.processEvaluation: Eval value: " + text);
                    }

                } else {
                    // just evaluate the javascript
                    try {
                        // do the evaluation
                        eval.call(window, text + "\n//@ sourceURL=wicket-eval.js");
                    } catch (e2) {
                        Wicket.Log.error("Wicket.Ajax.Call.processEvaluation: Exception evaluating javascript: " + e2.stack);
                        Wicket.Log.error("Wicket.Ajax.Call.processEvaluation: Eval value: " + text);
                    }
                    // continue to next step
                    notify();
                }
            });
        }; */
    }());

    YAHOO.register("hippoajax", YAHOO.hippo.HippoAjax, {
        version: "2.8.1",
        build: "19"
    });
}

var HippoAjax = YAHOO.hippo.HippoAjax;
