/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
Hippo.ChannelManager.TemplateComposer.DragDropOne = (function() {
    "use strict";

    var pageContext;

    return {

        setPageContext: function(context) {
            pageContext = context;
        },

        init: function(c) {
            c.onRender = c.onRender.createSequence(this.onRender);
        },

        onRender: function() {
            var self, iframe, iframePosition, iframeToolbarHeight;

            self = this;
            iframe = Ext.getCmp('pageEditorIFrame');

            this.boxes = [];
            this.nodeOverRecord = null;

            this.dragZone = new Ext.grid.GridDragZone(this, {
                containerScroll: true,
                ddGroup: 'templatecomposer',

                onInitDrag: function() {
                    iframePosition = iframe.getPosition();
                    iframeToolbarHeight = iframe.getTopToolbar().getHeight();

                    iframe.hostToIFrame.publish('highlight');
                    iframe.hostToIFrame.publish('enablemouseevents');

                    pageContext.stores.pageModel.each(function(record) {
                        var type, id, el, box;
                        type = record.get('type');
                        if (record.get('type') === HST.CONTAINER) {
                            id = record.get('id');
                            el = iframe.getElement(id + '-overlay');
                            if (el !== null && !iframe.getElement(id).getAttribute(HST.ATTR.HST_CONTAINER_DISABLED)) {
                                box = Ext.Element.fly(el).getBox();
                                self.boxes.push({record: record, box: box});
                            }
                        }
                    });
                },

                onEndDrag: function() {
                    self.boxes = [];
                    iframe.hostToIFrame.publish('disablemouseevents');
                    iframe.hostToIFrame.publish('unhighlight');
                }
            });

            this.dropZone = new Ext.dd.DropZone(iframe.getEl(), {
                ddGroup: 'templatecomposer',

                //If the mouse is over a grid row, return that node. This is
                //provided as the "target" parameter in all "onNodeXXXX" node event handling functions
                getTargetFromEvent: function(e) {
                    return e.getTarget();
                },

                //While over a target node, return the default drop allowed class which
                //places a "tick" icon into the drag proxy.
                onNodeOver: function(target, dd, e) {
                    var eventXY, curX, curY, i, len, item, box;

                    eventXY = e.xy;
                    curX = eventXY[0] - iframePosition[0];
                    curY = eventXY[1] - iframePosition[1] - iframeToolbarHeight;

                    for (i = 0, len = self.boxes.length; i < len; i++) {
                        item = self.boxes[i];
                        box = item.box;
                        if (curX >= box.x && curX <= box.right && curY >= box.y && curY <= box.bottom) {
                            self.nodeOverRecord = item.record;
                            return Ext.dd.DropZone.prototype.dropAllowed;
                        }
                    }
                    self.nodeOverRecord = null;
                    return Ext.dd.DropZone.prototype.dropNotAllowed;
                },

                //On node drop we can interrogate the target to find the underlying
                //application object that is the real target of the dragged data.
                //In this case, it is a Record in the GridPanel's Store.
                //We can use the data set up by the DragZone's getDragData method to read
                //any data we decided to attach in the DragZone's getDragData method.
                onNodeDrop: function() {
                    if (self.nodeOverRecord !== null) {
                        var pageContainer, selections, pmRecord, parentId, parentLastModifiedTimestamp, pmStore, offset, at, i, len, record, newRecord;
                        pageContainer = pageContext.getPageContainer();

                        selections = self.getSelectionModel().getSelections();

                        pmRecord = self.nodeOverRecord;
                        parentId = pmRecord.get('id');
                        parentLastModifiedTimestamp = pmRecord.get('lastModifiedTimestamp');
                        pmStore = pmRecord.store;


                        offset = pmRecord.data.children.length + 1;
                        at = pmStore.indexOf(pmRecord) + offset;
                        for (i = 0, len = selections.length; i < len; i++) {
                            record = selections[i];
                            newRecord = {
                                parentId: parentId,
                                //we set the id of new types to the id of their prototype, this allows use
                                //to change the rest-api url for the create method, which should contain this
                                //id
                                id: record.get('id'),
                                name: null,
                                type: HST.CONTAINERITEM,
                                template: record.get('template'),
                                componentClassName: record.get('componentClassName'),
                                xtype: record.get('xtype'),
                                isRoot: false,
                                children: [],
                                lastModifiedTimestamp: parentLastModifiedTimestamp
                            };
                            pmStore.on('write', pageContainer.refreshIframe, pageContainer, {single: true});
                            pmStore.insert(at + i, new pmStore.recordType(newRecord));
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    };

}());
