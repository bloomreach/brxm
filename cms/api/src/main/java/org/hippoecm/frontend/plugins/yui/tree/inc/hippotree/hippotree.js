/*
 * Copyright 2008-2013 Hippo
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
 * @module hippotree
 * @class a YAHOO.widget.TreeView extension
 * @requires treeview, get
 * @extends YAHOO.widget.TreeView
 * @constructor
 * @param {String}
 *            id the id of the linked element
 * @param {String}
 *            config
 */
YAHOO.namespace("hippo");

(function() {
    var Dom = YAHOO.util.Dom;

    YAHOO.hippo.HippoTree = function(id, config) {
        var args = [ 2 ],
            rootEl = Dom.getElementsByClassName('hippo-yui-tree');
        if (rootEl !== null && rootEl !== undefined && rootEl.length > 0) {
            args[0] = rootEl[0];
        } else {
            args[0] = id;
        }
        args[1] = [ config.treeData ];

        this.callbackUrl = config.callbackUrl;
        this.callbackMethod = config.callbackFunction;

        YAHOO.hippo.HippoTree.superclass.constructor.apply(this, args);
        this.initConfig(config);
    };

    YAHOO.extend(YAHOO.hippo.HippoTree, YAHOO.widget.TreeView, {

        initConfig : function(config) {
            var me, dblClick, click;
            me = this;
            if (config.registerOnDoubleclick) {
                dblClick = function(args) {
                    var uuid = args.node.data.uuid,
                        url = me.callbackUrl + '&action=dblClick&UUID=' + encodeURIComponent(uuid);
                    me.callbackMethod(url);
                };
                this.subscribe("dblClickEvent", dblClick);
            }
            if (config.registerOnclick) {
                click = function(args) {
                    var uuid = args.node.data.uuid,
                        url = me.callbackUrl + '&action=click&UUID=' + encodeURIComponent(uuid);
                    me.callbackMethod(url);
                };
                this.subscribe("clickEvent", click);
            }
        }
    });
}());
