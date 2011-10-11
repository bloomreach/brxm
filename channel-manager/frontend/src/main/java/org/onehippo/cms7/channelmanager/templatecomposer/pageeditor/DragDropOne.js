
Hippo.ChannelManager.TemplateComposer.DragDropOne = (function() {

    var pageContext;

    return {

        setPageContext: function(context) {
            pageContext = context;
        },

        init: function(c) {
            c.onRender = c.onRender.createSequence(this.onRender);
        },

        onRender: function() {
            var miframePanel = Ext.getCmp('Iframe');
            var miframe = miframePanel.getFrame();

            this.iFramePosition = miframePanel.getPosition();

            this.boxs = [];
            this.nodeOverRecord = null;
            var self = this;

            this.dragZone = new Ext.grid.GridDragZone(this, {
                containerScroll: true,
                ddGroup: 'blabla',

                onInitDrag : function() {
                    var framePanel = Ext.getCmp('Iframe');
                    var frmDoc = framePanel.getFrameDocument();
                    framePanel.getFrame().sendMessage({groups: 'dropzone'}, 'highlight');
                    pageContext.stores.pageModel.each(function(record) {
                        var type = record.get('type');
                        if (record.get('type') === HST.CONTAINER) {
                            var id = record.get('id');
                            var el = frmDoc.getElementById(id + '-overlay');
                            if (el != null && !frmDoc.getElementById(id).getAttribute(HST.ATTR.INHERITED)) {
                                var box = Ext.Element.fly(el).getBox();
                                self.boxs.push({record: record, box: box});
                            }
                        }
                    });
                    Ext.ux.ManagedIFrame.Manager.showShims();
                },

                onEndDrag : function() {
                    self.boxs = [];
                    Ext.ux.ManagedIFrame.Manager.hideShims();
                    Ext.getCmp('Iframe').getFrame().sendMessage({groups: 'dropzone'}, 'unhighlight');
                }
            });

            var containerItemsGrid = this;
            this.dropZone = new Ext.dd.DropZone(miframePanel.body.dom, {
                ddGroup: 'blabla',

                //If the mouse is over a grid row, return that node. This is
                //provided as the "target" parameter in all "onNodeXXXX" node event handling functions
                getTargetFromEvent : function(e) {
                    return e.getTarget();
                },

                //While over a target node, return the default drop allowed class which
                //places a "tick" icon into the drag proxy.
                onNodeOver : function(target, dd, e, data) {
                    var curX = dd.lastPageX + dd.deltaX - self.iFramePosition[0];
                    var curY = dd.lastPageY + dd.deltaY - self.iFramePosition[1];
                    //TODO: implement dynamic fetch of toolbar height to adjust pageY
                    curY -= 27;

                    for (var i = 0; i < self.boxs.length; i++) {
                        var item = self.boxs[i], box = item.box;
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
                onNodeDrop : function(target, dd, e, data) {
                    //                    var rowIndex = this.getView().findRowIndex(target);
                    //                    var r = this.getStore().getAt(rowIndex);
                    //                    Ext.Msg.alert('Drop gesture', 'Dropped Record id ' + data.draggedRecord.id +
                    //                            ' on Record id ' + r.id);
                    if (self.nodeOverRecord !== null) {
                        var pageContainer = pageContext.getPageContainer();

                        var selections = containerItemsGrid.getSelectionModel().getSelections();

                        var pmRecord = self.nodeOverRecord;
                        var parentId = pmRecord.get('id');
                        var pmStore = pmRecord.store;

                        var models = [];
                        var offset = pmRecord.data.children.length + 1;
                        var at = pmStore.indexOf(pmRecord) + offset;
                        for (var i = 0; i < selections.length; i++) {
                            var record = selections[i];
                            var cfg = {
                                parentId: parentId,
                                //we set the id of new types to the id of their prototype, this allows use
                                //to change the rest-api url for the create method, which should contain this
                                //id
                                id : record.get('id'),
                                name: null,
                                type: HST.CONTAINERITEM,
                                template: record.get('template'),
                                componentClassName : record.get('componentClassName'),
                                xtype: record.get('xtype')
                            };
                            var model = Hippo.ChannelManager.TemplateComposer.PageModel.Factory.createModel(null, cfg);
                            models.push(model);
                            // pmStore.on('write', function() { pageContainer.refreshIframe.call(pageContainer); }, this, {single: true});
                            pmStore.insert(at + i, Hippo.ChannelManager.TemplateComposer.PageModel.Factory.createRecord(model));
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    };

})();
