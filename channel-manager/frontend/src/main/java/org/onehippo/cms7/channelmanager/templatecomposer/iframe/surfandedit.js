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

    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame.UI');

    if (!Hippo.ChannelManager.TemplateComposer.IFrame.UI.SurfAndEdit) {
        var SurfAndEdit = function() {
        };

        SurfAndEdit.prototype = {

            createLink : function(element) {
                var die = Hippo.ChannelManager.TemplateComposer.IFrame.Main.die;

                var hstContainerMetaData = this.getContainerMetaData(element);
                if (typeof hstContainerMetaData === 'undefined' || hstContainerMetaData === null) {
                    die(this.resources['factory-no-hst-meta-data']);
                }

                var id = hstContainerMetaData[HST.ATTR.ID];
                if (typeof id === 'undefined') {
                    die(this.resources['factory-attribute-not-found'].format(HST.ATTR.ID));
                }

                $(element).text(this.resources['edit-document']);

                element.setAttribute(HST.ATTR.ID, id);
                element.setAttribute("href", "");

                return element;
            },

            getContainerMetaData : function(element) {
                var die = Hippo.ChannelManager.TemplateComposer.IFrame.Main.die;
                try {
                    if (element.className === HST.CLASS.EDITLINK) {
                        var tmpElement = element;
                        while (tmpElement.previousSibling !== null) {
                            tmpElement = tmpElement.previousSibling;
                            var hstMetaData = this.convertToHstMetaData(tmpElement);
                            if (hstMetaData !== null) {
                                return hstMetaData;
                            }
                        }
                    }
                    return null;
                } catch(exception) {
                    die(this.resources['factory-error-parsing-hst-data'].format(tmpElement.data, Hippo.Util.getElementPath(element)) + ' ' + exception);
                }
            },

            convertToHstMetaData : function(element) {
                var die = Hippo.ChannelManager.TemplateComposer.IFrame.Main.die;
                if (element.nodeType !== 8) {
                    return null;
                }
                try {
                    if (!element.data || element.data.length == 0
                            || !element.data.indexOf(HST.ATTR.ID) === -1) {
                        return null;
                    }
                    var commentJsonObject = JSON.parse(element.data);
                    if (typeof commentJsonObject[HST.ATTR.ID] !== 'undefined') {
                        element.parentNode.removeChild(element);
                        return commentJsonObject;
                    }
                } catch(exception) {
                    die(this.resources['factory-error-parsing-hst-data'].format(element.data) +' '+ exception);
                }
                return null;
            },

            setResources: function(resources) {
                this.resources = resources;
            }

        };

        Hippo.ChannelManager.TemplateComposer.IFrame.UI.SurfAndEdit = new SurfAndEdit();
    }
})(jQuery);
