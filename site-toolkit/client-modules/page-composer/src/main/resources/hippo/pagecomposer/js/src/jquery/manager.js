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

$.namespace('Hippo.PageComposer.UI');

Hippo.PageComposer.UI.Manager = function() {
    this.current = null;
    this.containers = {};

    this.syncRequested = false;

    this.init();
};

Hippo.PageComposer.UI.Manager.prototype = {
    init: function() {

        //do try/catch because else errors will disappear
        try {
            //attach mouseover/mouseclick for components
            var self = this;
            $('div.componentContentWrapper').each(function(index) {
                if(Hippo.PageComposer.UI.Factory.isContainer(this)) {
                    self._createContainer(this);
                }
            });
        } catch(e) {
            console.error(e);
        }
    },

    _createContainer : function(element) {
        var container = Hippo.PageComposer.UI.Factory.createOrRetrieve(element);
        this.containers[container.id] = container;
        var self = this;
        container.syncAll = function() {
            self.syncAll();
        };
        container.render(this);
        container.activate();
    },

    _retrieve : function(element) {
        var factory = Hippo.PageComposer.UI.Factory;
        var data = factory.verify(element);
        var o = factory.getById(data.id);
        if(o == null) {
            Hippo.PageComposer.Main.die('Object with id ' + data.id + ' not found in registry');
        }
        return o;
    },

    //Deprecated
    _createOrRetrieve : function(element) {
        console.log('Deprecated');
        console.trace();
        var die = Hippo.PageComposer.Main.die;

        if (typeof element === 'undefined' || element === null) {
            die("element is undefined or null");
        }

        var el = $(element);
        var id = el.attr('hst:id');
        if (typeof id === 'undefined') {
            die('Attribute hst:id not found');
        }

        if(Hippo.PageComposer.UI.Factory.isContainer(element)) {
            if (typeof this.containers[id] === 'undefined') {
                this._createContainer(element);
            }
            return this.containers[id];
        } else {
            var parentId = $(element).parents('.componentContentWrapper').attr('hst:id');
            var parent = this.containers[parentId];
            if (typeof parent === 'undefined') {
                die('No existing parent container found for item ' + id);
            }
            if(!parent.hasItem(id)) {
                parent.createItem(element);
            }
            return parent.getItem(id);
        }
    },

    select: function(element) {
        if (this.current != null && this.current.element == element) {
            return;
        }

        this.current = this._retrieve(element);
        this.current.select();
    },

    deselect : function(element) {
        if (this.current != null) {
            this.current.deselect();
            this.current = null;
        }
    },

    add: function(element, parentId) {
        if (typeof this.containers[parentId] !== 'undefined') {
            var container = this.containers[parentId];
            container.addAndRefresh(element);
            this.sync();
        }
    },

    remove : function(element) {
        if (!element.hasAttribute('hst:id')) {
            element = $(element).parents('.componentContentWrapper')[0];
        }

        var d = Hippo.PageComposer.UI.Factory.verify(element);
        if (d.type == HST.CONTAINERITEM) {
            var containerId = $(element).parents('.componentContentWrapper').attr('hst:id');
            var container = this.containers[containerId];
            if (typeof container !== 'undefined' && container.removeItemAndRefresh(d.id)) {
                Hippo.PageComposer.UI.Factory.deleteObjectRef(containerId);
            }
        } else if (d.type == HST.CONTAINER) {
            var container = this.containers[d.id];
            if (typeof container !== 'undefined') {
                container.remove();
                delete this.containers[id];
            }
        }
    },
    requestSync : function() {
        this.syncRequested = true;
    },

    sync : function() {
        if(this.syncRequested) {
            $.each(this.containers, function(key, value) {
                value.sync();
            });
        }
        this.syncRequested = false;
    }

};




