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
"use strict";
(function($) {
    var jQuery = $;
    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame.UI');

    if (!Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory) {

        var Main = Hippo.ChannelManager.TemplateComposer.IFrame.Main;

        // TODO refactor, this is a module
        var Factory = function() {
            this.objects = {};
            this.registry = {};

            Main.subscribe('initialize', function(data) {
                this.resources = data.resources;
            }, this);
        };

        Factory.prototype = {
            createOrRetrieve : function(element) {
                if (this.objects[element.id]) {
                    return this.objects[element.id];
                }
                var data = this._enhance(element);
                if (data === null) {
                    return null;
                }
                return this._create(data);
            },

            _create : function(data) {
                var die = Hippo.ChannelManager.TemplateComposer.IFrame.Main.die;
                if (typeof this.registry[data.xtype] === 'undefined') {
                    die(this.resources['factory-xtype-not-found'].format(data.xtype));
                }
                console.log('_create xtype:'+data.xtype+', element: '+Hippo.Util.getElementPath(data.element));
                var c = new this.registry[data.xtype](data.id, data.element, this.resources);
                if (!c instanceof data.base) {
                    Hippo.ChannelManager.TemplateComposer.IFrame.Main.die(this.resources['factory-inheritance-error'].format(data.id, data.base));
                }
                this.objects[c.id] = c;
                return c;
            },

            _enhance : function(element) {
                var die = Hippo.ChannelManager.TemplateComposer.IFrame.Main.die;

                var hstContainerMetaData = this.getContainerMetaData(element);
                if (hstContainerMetaData === null) {
                    if ($.trim($(element).html()) === '') {
                        console.info('Skipping empty element "{0}" with no meta data.'.format(Hippo.Util.getElementPath(element)));
                        return null;
                    } else {
                        console.warn('No hst meta data found for element "{0}".'.format(Hippo.Util.getElementPath(element)));
                        return null;
                    }
                }

                if (typeof hstContainerMetaData === 'undefined' || hstContainerMetaData === null) {
                    die(this.resources['factory-no-hst-meta-data'].format(Hippo.Util.getElementPath(element)));
                }

                var id = hstContainerMetaData[HST.ATTR.ID];
                if (typeof id === 'undefined') {
                    die(this.resources['factory-attribute-not-found'].format(HST.ATTR.ID, Hippo.Util.getElementPath(element)));
                }

                element.id = id;
                element.setAttribute(HST.ATTR.ID, id);

                var type = hstContainerMetaData[HST.ATTR.TYPE];
                if (typeof type === 'undefined') {
                    die(this.resources['factory-attribute-not-found'].format(HST.ATTR.TYPE, Hippo.Util.getElementPath(element)));
                }
                element.setAttribute(HST.ATTR.TYPE,  type);

                var base = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Widget;
                if (type === HST.CONTAINER) {
                    base = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base;
                } else if (type === HST.CONTAINERITEM) {
                    base = Hippo.ChannelManager.TemplateComposer.IFrame.UI.ContainerItem.Base;
                }

                //Not very sexy this..
                var xtype = hstContainerMetaData[HST.ATTR.XTYPE];
                if (typeof xtype === 'undefined' || xtype == null || xtype == '') {
                    if (type === HST.CONTAINER) {
                        xtype = 'Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base';
                    } else if (type === HST.CONTAINERITEM) {
                        xtype = 'Hippo.ChannelManager.TemplateComposer.IFrame.UI.ContainerItem.Base';
                    }
                }
                element.setAttribute(HST.ATTR.XTYPE, xtype);

                var url = hstContainerMetaData[HST.ATTR.URL];
                if (typeof url !== 'undefined') {
                    element.setAttribute(HST.ATTR.URL, url);
                }

                var refNS = hstContainerMetaData[HST.ATTR.REF_NS];
                if (typeof refNS !== 'undefined') {
                    element.setAttribute(HST.ATTR.REF_NS, refNS);
                }

                var inherited = hstContainerMetaData[HST.ATTR.INHERITED];
                if (typeof inherited !== 'undefined') {
                    element.setAttribute(HST.ATTR.INHERITED, inherited);
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

            getContainerMetaData : function(element) {
                var die = Hippo.ChannelManager.TemplateComposer.IFrame.Main.die;
                if (element.className === HST.CLASS.ITEM) {
                    var childNodes;
                    if (element.tagName == 'TR') {
                        var children = element.childNodes;
                        childNodes = [];
                        for (var i = 0; i < children.length; i++) {
                            if (children[i].tagName != 'TD') {
                                continue;
                            }
                            var descendants = children[i].childNodes;
                            for (var j = 0; j < descendants.length; j++) {
                                childNodes.push(descendants[j]);
                            }
                        }
                    } else {
                        childNodes = element.childNodes;
                    }
                    for (var i=0, len=childNodes.length; i<len; i++) {
                        try {
                            var hstMetaData = this.convertToHstMetaData(childNodes[i]);
                            if (hstMetaData !== null) {
                                return hstMetaData;
                            }
                        } catch (exception) {
                            die(this.resources['factory-error-parsing-hst-data'].format(childNodes[i].data, Hippo.Util.getElementPath(element)) + ' ' + exception);
                        }
                    }
                } else if (element.className === HST.CLASS.CONTAINER) {
                    var tmpElement = element;
                    while (tmpElement.previousSibling !== null) {
                        tmpElement = tmpElement.previousSibling;
                        try {
                            var hstMetaData = this.convertToHstMetaData(tmpElement);
                            if (hstMetaData !== null) {
                                return hstMetaData;
                            }
                        } catch (exception) {
                            die(this.resources['factory-error-parsing-hst-data'].format(tmpElement.data, Hippo.Util.getElementPath(element)) + ' ' + exception);
                        }
                    }
                }
                return null;
            },

            convertToHstMetaData : function(element) {
                if (element.nodeType !== 8) {
                    return null;
                }
                if (!element.data || element.data.length == 0
                        || !element.data.indexOf(HST.ATTR.ID) === -1
                        || !element.data.indexOf(HST.ATTR.TYPE) === -1
                        || !element.data.indexOf(HST.ATTR.XTYPE) === -1) {
                    return null;
                }
                var commentJsonObject = JSON.parse(element.data);
                if (typeof commentJsonObject[HST.ATTR.ID] !== 'undefined'
                    && commentJsonObject[HST.ATTR.TYPE] !== 'undefined'
                    && commentJsonObject[HST.ATTR.XTYPE] !== 'undefined') {
                    element.parentNode.removeChild(element);
                    return commentJsonObject;
                }
                return null;
            }

        };

        Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory = new Factory();
    }
})(jQuery);
