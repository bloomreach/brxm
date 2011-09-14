/* global Ext */

/** Stripped Messaging Driver */
/* Messaging Driver for ux.ManagedIFrame
 *******************************************************************************
 * This file is distributed on an AS IS BASIS WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * ***********************************************************************************
 * @version 2.1.2
 *
 * License: ux.ManagedIFrame, ux.ManagedIFramePanel, ux.ManagedIFrameWindow
 * are licensed under the terms of the Open Source GPL 3.0 license:
 * http://www.gnu.org/licenses/gpl.html
 *
 * Commercial use is prohibited without a Commercial Developement License. See
 * http://licensing.theactivegroup.com.
 *
 * Donations are welcomed: http://donate.theactivegroup.com
 *
 */

(function() {

    /**
     * @private, frame messaging interface (for same-domain-policy frames
     *           only)
     */
    // each tag gets a hash queue ($ = no tag ).
    var tagStack = { '$' : [] };
    var isEmpty = function(v, allowBlank) {
        return v === null || v === undefined
                || (!allowBlank ? v === '' : false);
    };
    var apply = function(o, c, defaults) {
        if (defaults) {
            apply(o, defaults);
        }
        if (o && c && typeof c == 'object') {
            for (var p in c) {
                o[p] = c[p];
            }
        }
        return o;
    };

    window.sendMessage = function(message, tag, domain) {
        var fn, result;
        // only raise matching-tag handlers
        var compTag = message.tag || tag || null;
        var mstack = !isEmpty(compTag) ? tagStack[String(compTag).toLowerCase()] || [] : tagStack["$"];

        for (var i = 0, l = mstack.length; i < l; i++) {
            if (fn = mstack[i]) {
                result = fn.apply(fn.__scope, [{
                        type : 'message',
                        data : message,
                        domain : domain || document.domain,
                        origin : location.protocol + '//' + location.hostname,
                        uri : document.documentURI,
                        source : window,
                        tag : tag ? String(tag).toLowerCase() : null
                    }]) === false ? false : result;
                if (fn.__single) {
                    mstack[i] = null;
                }
                if (result === false) {
                    break;
                }
            }
        }

        return result;
    };

    window.onhostmessage = function(fn, scope, single, tag) {
        if (typeof fn == 'function') {
            if (!isEmpty(fn.__index)) {
                throw "onhostmessage: duplicate handler definition"
                        + (tag ? " for tag:" + tag : '');
            }

            var k = isEmpty(tag) ? "$" : tag.toLowerCase();
            tagStack[k] || (tagStack[k] = []);
            apply(fn, {
                __tag : k,
                __single : single || false,
                __scope : scope || window,
                __index : tagStack[k].length
            });
            tagStack[k].push(fn);

        } else {
            throw "onhostmessage: function required";
        }

    };

    window.unhostmessage = function(fn) {
        if (typeof fn == 'function' && typeof fn.__index != 'undefined') {
            var k = fn.__tag || "$";
            tagStack[k][fn.__index] = null;
        }
    };
})();