/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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
(function($) {
    "use strict";

    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame.UI');

    var hostToIFrame, iframeToHost, Factory;

    if (!Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory) {

        hostToIFrame = window.parent.Ext.getCmp('pageEditorIFrame').hostToIFrame;
        iframeToHost = window.parent.Ext.getCmp('pageEditorIFrame').iframeToHost;

        $(window).unload(function() {
            hostToIFrame = null;
            iframeToHost = null;
        });

        // TODO refactor, this is a module
        Factory = function() {
            this.objects = {};
            this.registry = {};

            hostToIFrame.subscribe('init', function(data) {
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
                var c;
                if (this.registry[data.xtype] === undefined) {
                    iframeToHost.exception(this.resources['factory-xtype-not-found'].format(data.xtype));
                }
                c = new this.registry[data.xtype](data.id, data.element, this.resources);
                if (!c instanceof data.base) {
                    iframeToHost.exception(this.resources['factory-inheritance-error'].format(data.id, data.base));
                }
                this.objects[c.id] = c;
                return c;
            },

            _enhance : function(element) {
                var hstContainerMetaData, id, type, base, xtype, url, refNS, variant,
                        hstContainerLockedBy, hstContainerLockedByCurrentUser, hstContainerLockedOn, hstContainerLastModified, disabled;

                hstContainerMetaData = this.getContainerMetaData(element);
                if (hstContainerMetaData === null) {
                    if ($.trim($(element).html()) === '') {
                        console.info('Skipping empty element "{0}" with no meta data.'.format(Hippo.Util.getElementPath(element)));
                        return null;
                    } else {
                        console.warn('No hst meta data found for element "{0}".'.format(Hippo.Util.getElementPath(element)));
                        return null;
                    }
                }

                if (hstContainerMetaData === undefined || hstContainerMetaData === null) {
                    iframeToHost.exception(this.resources['factory-no-hst-meta-data'].format(Hippo.Util.getElementPath(element)));
                }

                id = hstContainerMetaData[HST.ATTR.ID];
                if (id === undefined) {
                    iframeToHost.exception(this.resources['factory-attribute-not-found'].format(HST.ATTR.ID, Hippo.Util.getElementPath(element)));
                }

                element.id = id;
                element.setAttribute(HST.ATTR.ID, id);

                type = hstContainerMetaData[HST.ATTR.TYPE];
                if (type === undefined) {
                    iframeToHost.exception(this.resources['factory-attribute-not-found'].format(HST.ATTR.TYPE, Hippo.Util.getElementPath(element)));
                }
                element.setAttribute(HST.ATTR.TYPE,  type);

                base = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Widget;
                if (type === HST.CONTAINER) {
                    base = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base;
                } else if (type === HST.CONTAINERITEM) {
                    base = Hippo.ChannelManager.TemplateComposer.IFrame.UI.ContainerItem.Base;
                }

                //Not very sexy this..
                xtype = hstContainerMetaData[HST.ATTR.XTYPE];
                if (xtype === undefined || xtype === null || xtype === '') {
                    if (type === HST.CONTAINER) {
                        xtype = 'Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base';
                    } else if (type === HST.CONTAINERITEM) {
                        xtype = 'Hippo.ChannelManager.TemplateComposer.IFrame.UI.ContainerItem.Base';
                    }
                }
                element.setAttribute(HST.ATTR.XTYPE, xtype);

                url = hstContainerMetaData[HST.ATTR.URL];
                if (url !== undefined) {
                    element.setAttribute(HST.ATTR.URL, url);
                }

                refNS = hstContainerMetaData[HST.ATTR.REF_NS];
                if (refNS !== undefined) {
                    element.setAttribute(HST.ATTR.REF_NS, refNS);
                }

                if (hstContainerMetaData[HST.ATTR.INHERITED] !== undefined) {
                    element.setAttribute(HST.ATTR.HST_CONTAINER_DISABLED, "true");
                }

                variant = hstContainerMetaData[HST.ATTR.VARIANT];
                if (variant !== undefined) {
                    element.setAttribute(HST.ATTR.VARIANT, variant);
                }

                if (type === HST.CONTAINER || type === HST.CONTAINERITEM) {
                    hstContainerLockedBy = hstContainerMetaData[HST.ATTR.HST_CONTAINER_COMPONENT_LOCKED_BY];
                    if (hstContainerLockedBy !== undefined) {
                        element.setAttribute(HST.ATTR.HST_CONTAINER_COMPONENT_LOCKED_BY, hstContainerLockedBy);
                    }
                    hstContainerLockedByCurrentUser = hstContainerMetaData[HST.ATTR.HST_CONTAINER_COMPONENT_LOCKED_BY_CURRENT_USER];
                    if (hstContainerLockedByCurrentUser !== undefined) {
                        element.setAttribute(HST.ATTR.HST_CONTAINER_COMPONENT_LOCKED_BY_CURRENT_USER, hstContainerLockedByCurrentUser);
                        if (hstContainerLockedBy && hstContainerLockedByCurrentUser === "false") {
                            element.setAttribute(HST.ATTR.HST_CONTAINER_DISABLED, "true");
                        }
                    }
                    hstContainerLockedOn = hstContainerMetaData[HST.ATTR.HST_CONTAINER_COMPONENT_LOCKED_ON];
                    if (hstContainerLockedOn !== undefined) {
                        element.setAttribute(HST.ATTR.HST_CONTAINER_COMPONENT_LOCKED_ON, hstContainerLockedOn);
                    }
                    hstContainerLastModified = hstContainerMetaData[HST.ATTR.HST_CONTAINER_COMPONENT_LAST_MODIFIED];
                    if (hstContainerLastModified !== undefined) {
                        element.setAttribute(HST.ATTR.HST_CONTAINER_COMPONENT_LAST_MODIFIED, hstContainerLastModified);
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
                if (o === undefined) {
                    return null;
                }
                return o;
            },

            deleteObjectRef : function(ref) {
                if (this.objects[ref] !== undefined) {
                    delete this.objects[ref];
                }
            },

            register : function(key, value) {
                this.registry[key] = value;
            },

            getContainerMetaData : function(element) {
                var childNodes, children, i, descendants, j, len, childrenLen, descendantsLen, hstMetaData;
                if ($(element).hasClass(HST.CLASS.ITEM)) {
                    if (element.tagName === 'TR') {
                        children = element.childNodes;
                        childNodes = [];
                        for (i = 0, childrenLen = children.length; i < childrenLen; i++) {
                            if (children[i].tagName === 'TD') {
                                descendants = children[i].childNodes;
                                for (j = 0, descendantsLen = descendants.length; j < descendantsLen; j++) {
                                    childNodes.push(descendants[j]);
                                }
                            }
                        }
                    } else {
                        childNodes = element.childNodes;
                    }
                    for (i = 0, len = childNodes.length; i < len; i++) {
                        try {
                            hstMetaData = this.convertToHstMetaData(childNodes[i]);
                            if (hstMetaData !== null) {
                                return hstMetaData;
                            }
                        } catch (e) {
                            iframeToHost.exception(this.resources['factory-error-parsing-hst-data'].format(childNodes[i].data, Hippo.Util.getElementPath(element)) + ' ' + e);
                        }
                    }
                    // now check the previous siblings of the HstContainerItem : In case of not using the built in
                    // vbox.ftl, table.ftl, ol.ftl, etc, the meta data can be written as a sibling just before the
                    // HstContainerItem
                    return this.findMetaDataOnPreviousSibling(element);
                } else if ($(element).hasClass(HST.CLASS.CONTAINER)) {
                    return this.findMetaDataOnPreviousSibling(element);
                }
                return null;
            },

            findMetaDataOnPreviousSibling : function(element) {
                var tmpElement, hstMetaData;
                tmpElement = element;
                while (tmpElement.previousSibling !== null) {
                    tmpElement = tmpElement.previousSibling;
                    try {
                        hstMetaData = this.convertToHstMetaData(tmpElement);
                        if (hstMetaData !== null) {
                            return hstMetaData;
                        }
                    } catch (ex) {
                        iframeToHost.exception(this.resources['factory-error-parsing-hst-data'].format(tmpElement.data, Hippo.Util.getElementPath(element)) + ' ' + ex);
                    }
                }
                return null;
            },

            convertToHstMetaData : function(element) {
                var commentJsonObject;
                if (element.nodeType !== 8) {
                    return null;
                }
                if (!element.data || element.data.length === 0
                        || element.data.indexOf(HST.ATTR.ID) === -1
                        || element.data.indexOf(HST.ATTR.TYPE) === -1
                        || element.data.indexOf(HST.ATTR.XTYPE) === -1) {
                    return null;
                }
                commentJsonObject = JSON.parse(element.data);
                if (commentJsonObject[HST.ATTR.ID] !== undefined
                    && commentJsonObject[HST.ATTR.TYPE] !== undefined
                    && commentJsonObject[HST.ATTR.XTYPE] !== undefined) {
                    element.parentNode.removeChild(element);
                    return commentJsonObject;
                }
                return null;
            }

        };

        Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory = new Factory();
    }
}(jQuery));
