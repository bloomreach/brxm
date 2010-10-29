/*
 *  Copyright 2010 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

$.namespace('Hippo.PageComposer');

( function() {

    var Main = function() {
        this.debug = false;
    };

    Main.prototype = {

        init: function(debug) {
            this.debug = debug;

            var manager = new Hippo.PageComposer.UI.Manager();

            //register to listen to iframe-messages
            onhostmessage(function(msg) {
                manager.select(msg.data.element);
                return false;
            }, this, false, 'select');

            onhostmessage(function(msg) {
                manager.deselect(msg.data.element);
                return false;
            }, this, false, 'deselect');

            onhostmessage(function(msg) {
                manager.add(msg.data.element, msg.data.parentId);
                return false;
            }, this, false, 'add');

            onhostmessage(function(msg) {
                manager.remove(msg.data.element);
                return false;
            }, this, false, 'remove');

            this.manager = manager;
        },

        add: function(element, parentId) {
            this.manager.add(element, parentId);
        },

        remove : function(element) {
            this.manager.remove(element);
        },

        select: function(element) {
            this.manager.select(element);
        },

        deselect : function(element) {
            this.manager.deselect(element);
        },

        isDebug: function() {
            return this.debug;
        },

        die: function(msg) {
            if(Hippo.PageComposer.Main.isDebug()) {//global reference for scope simplicity
                console.error(msg);
            } else {
                throw new Error(msg);
            }
        }
    };

    Hippo.PageComposer.Main = new Main();

})();