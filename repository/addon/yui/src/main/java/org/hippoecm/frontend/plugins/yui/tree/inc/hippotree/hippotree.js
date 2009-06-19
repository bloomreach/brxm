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
 * @module hippotree
 * @class a YAHOO.widget.TreeView extension
 * @requires treeview, get
 * @extends YAHOO.widget.TreeView
 * @constructor
 * @param {String} id the id of the linked element
 * @param {String} config
 */
YAHOO.namespace("hippo"); 

( function() {
    var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

    YAHOO.hippo.HippoTree = function(id, config) {
        var args = [2];
        args[0] = id;
        console.dir(config);
        args[1] = [config.treeData]; 
        console.dir(config.treeData);
        
        this.callbackUrl = config.callbackUrl; //used for getting search results, currently uses YUI to do callbacks
        this.callbackMethod = config.callbackFunction; //used for handling clicks
        
        YAHOO.hippo.HippoTree.superclass.constructor.apply(this, args); 
        
        this.initConfig(config);
    };
    
    YAHOO.extend(YAHOO.hippo.HippoTree, YAHOO.widget.TreeView, {
        
        initConfig : function(config) {
            var me  = this;
            var dblClick = function(args) {
                var uuid = args.node.data.uuid;
                var url = me.callbackUrl + '&action=add&key=' + encodeURIComponent(uuid);
                me.callbackMethod(url);
            }
            this.subscribe("dblClickEvent", dblClick);

            var click = function(args) {
                var uuid = args.node.data.uuid;
                var url = me.callbackUrl + '&action=select&key=' + encodeURIComponent(uuid);
                me.callbackMethod(url);
            }
            this.subscribe("clickEvent", click);
        }
        
    });
 })();
