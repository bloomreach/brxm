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

jQuery.noConflict();
(function($) {

    $.namespace('Hippo.PageComposer.UI');

    if (!Hippo.PageComposer.UI.Factory) {
        var Factory = function() {
            this.objects = {};
            this.registry = {};
        };

        Factory.prototype = {
            createOrRetrieve : function(element) {
                var verified = this.verify(element);
                return this.objects[verified.id] || this._create(verified, true);
            },

            _create : function(data, verify) {
                var die = Hippo.PageComposer.Main.die;
                if (typeof this.registry[data.xtype] === 'undefined') {
                    die('No implementation found for xtype=' + data.xtype);
                }
                var c = new this.registry[data.xtype](data.id, data.element);
                if(verify) {
                    if (!c instanceof data.base) {
                        Hippo.PageComposer.Main.die('Instance with id ' + data.id + ' should be a subclass of ' + data.base);
                    }
                }
                this.objects[c.id] = c;
                return c;
            },

            verify : function(element) {
                var die = Hippo.PageComposer.Main.die;

                if (typeof element === 'undefined' || element === null) {
                    die("element is undefined or null");
                }

                var el = $(element);
                var id = el.attr('hst:id');
                if (typeof id === 'undefined') {
                    die('Attribute hst:id not found on element');
                }

                if (!element.id) {
                    if(Hippo.PageComposer.Main.isDebug()) {
                        console.warn('No @id found on element, using value of hst:id instead.');
                    }
                    element.id = id;
                }

                var type = el.attr('hst:type');
                if (typeof type === 'undefined') {
                    die('Attribute hst:type not found');
                }

                var base = Hippo.PageComposer.UI.Widget;
                if (type === HST.CONTAINER) {
                    base = Hippo.PageComposer.UI.Container.Base;
                } else if (type === HST.CONTAINERITEM) {
                    base = Hippo.PageComposer.UI.ContainerItem.Base;
                }

                //Not very sexy this..
                var xtype = el.attr('hst:xtype');
                if (typeof xtype === 'undefined' || xtype == null || xtype == '') {
                    if (type === HST.CONTAINER) {
                        xtype = 'Hippo.PageComposer.UI.Container.Base';
                    } else if (type === HST.CONTAINERITEM) {
                        xtype = 'Hippo.PageComposer.UI.ContainerItem.Base';
                    }
                }

                return {
                    id: id,
                    type: type,
                    xtype : xtype,
                    element: element,
                    base: base
                };
            },

            getById : function(id) {
                var o = this.objects[id];
                if (typeof o === 'undefined') {
                    return null;
                }
                return o;
            },

            deleteObjectRef : function(ref) {
                if (typeof this.objects[ref] !== 'undefined') {
                    delete this.objects[ref];
                }
            },

            register : function(key, value) {
                this.registry[key] = value;
            },

            isContainer : function(element) {
                var die = Hippo.PageComposer.Main.die;
                var type = $(element).attr('hst:type');
                if (typeof type === 'undefined') {
                    die('Attribute hst:type not found');
                }
                return type === HST.CONTAINER;
            }

        };

        Hippo.PageComposer.UI.Factory = new Factory();
    }
})(jQuery);
