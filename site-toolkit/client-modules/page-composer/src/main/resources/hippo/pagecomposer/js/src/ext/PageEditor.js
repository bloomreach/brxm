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

Ext.namespace('Hippo.App');

Hippo.App.PageEditor = Ext.extend(Ext.App, {

    loadMessage: 'Initializing application',

    init : function() {
        if (this.debug) {
            Ext.Ajax.timeout = 90000; // this changes the 30 second default to 90 seconds
        }

        this.ids = {
            page    : null,
            toolkit : null,
            site    : null
        };

        this.stores = {
            toolkit : null,
            pageModel : null
        };

        this.pageModelFacade = null;

        this.initUI();
    },

    initUI : function() {

        var viewport = new Ext.Viewport({
            layout: 'fit',
            title: 'Hippo PageEditor',
            renderTo: Ext.getBody(),
            items :   [
                {
                    id: 'Iframe',
                    xtype: 'iframepanel',
//                    loadMask: true,
                    defaultSrc: this.iframeUrl,
                    collapsible: false,
                    disableMessaging: false,
                    tbar: [
                        {
                            text: 'Page Composer',
                            iconCls: 'title-button',
                            id: 'pageComposerButton',
                            listeners: {
                                'click': {
                                    fn: this.toggleConfigWindow,
                                    scope: this
                                }
                            }
                        },
                        '->',
                        {
                            text: 'Refresh',
                            id: 'refreshButton',
                            listeners: {
                                'click' : {
                                    fn: this.refreshIframe,
                                    scope: this
                                }
                            }
                        },
                        {
                            text: 'Logout',
                            id: 'logoutButton',
                            listeners: {
                                'click' : {
                                    fn: this.doLogout,
                                    scope: this
                                }
                            }
                        }
                    ],
                    listeners: {
                        'message': {
                            fn: this.handleFrameMessages,
                            scope:this
                        },
                        'documentloaded' : {
                            fn: function(frm) {
                                //Only called after the first load. Every refresh within a running iframe result in
                                //a domready event only
                                if (Ext.isSafari || Ext.isChrome) {
                                    this.onIframeDOMReady(frm);
                                }
                            },
                            scope: this
                        },
                        'domready' : {
                            fn: function(frm) {
                                //Safari && Chrome report a DOM ready event, but js is not yet fully loaded, resulting
                                //in 'undefined' errors.
                                if (Ext.isGecko) {
                                    this.onIframeDOMReady(frm);
                                }
                            },
                            scope: this
                        },
                        'exception' : {
                            fn: function(frm, e) {
                                console.error(e); //ignore for now..
                            },
                            scope: this
                        }
                    }
                }
            ]
        });

    },

    toggleConfigWindow: function () {
        if (!this.mainWindow.isVisible()) {
            this.mainWindow.show('pageComposerButton');
        } else {
            this.mainWindow.hide('pageComposerButton');
        }
    },

    refreshIframe : function() {
        var iframe = Ext.getCmp('Iframe');
        iframe.setSrc(iframe.getFrameDocument().location.href); //following links in the iframe doesn't set iframe.src..
    },

    doLogout : function() {
        Ext.Ajax.request({
            url: 'services-new/' + this.ids.site + './logout',
            success: function () {
                location.reload(true);
            }
        });
    },

    onIframeDOMReady : function(frm) {
        //send init call to iframe app
        frm.execScript('Hippo.PageComposer.Main.init(' + this.debug + ')', true);
    },

    onIframeAppLoaded : function(data) {
        var siteId = data.siteIdentifier;
        var toolkitId = data.toolkitIdentifier;
        var pageId = data.rootComponentIdentifier;

        if (toolkitId != this.ids.toolkit) {
            this.stores.toolkit = this.createToolkitStore(toolkitId);
            this.stores.toolkit.load();
        }

        if (pageId != this.ids.page) {
            this.stores.pageModel = this.createPageModelStore(pageId);
            this.stores.pageModel.load();
        } else {
            this.shareData();
        }

        if (!this.mainWindow) {
            var win = this.mainWindow = this.createMainWindow();
            this.beforeQuit(win.close, win);
            win.show();
        } else {
            if (toolkitId != this.ids.toolkit) {
                var grid = Ext.getCmp('ToolkitGrid');
                grid.reconfigure(this.stores.toolkit, grid.getColumnModel());
            }
            if (pageId != this.ids.page) {
                var grid = Ext.getCmp('PageModelGrid');
                grid.reconfigure(this.stores.pageModel, grid.getColumnModel());
            }
        }

        this.ids.page = pageId;
        this.ids.toolkit = toolkitId;
        this.ids.site = siteId;
    },

    createToolkitStore : function(toolkitId) {
        return new Hippo.App.ToolkitStore({
            toolkitId : toolkitId
        });
    },

    createPageModelStore : function(pageId) {
        return new Hippo.App.PageModelStore({
            rootComponentIdentifier: this.rootComponentIdentifier,
            pageId: pageId,

            listeners: {
                write : {
                    fn: function(store, action, result, res, records) {
                        if (action == 'create') {
                            records = Ext.isArray(records) ? records : [records];
                            for (var i = 0; i < records.length; i++) {
                                var record = records[i];
                                if (record.get('type') == HST.CONTAINERITEM) {
                                    //add element to the iframe DOM
                                    this.sendFrameMessage({parentId: record.get('parentId'), element: record.get('element')}, 'add');

                                    //add id to parent children map
                                    var parentId = record.get('parentId');
                                    var parentIndex = store.findExact('id', parentId);
                                    var parentRecord = store.getAt(parentIndex);
                                    var children = parentRecord.get('children');
                                    children.push(record.get('id'));
                                    parentRecord.set('children', children);
                                }
                            }
                        } else if (action == 'update') {
                            if (!this.isReloading) {
                                store.reload();
                                this.isReloading = true;
                            }
                        }
                    },
                    scope: this
                },
                load :{
                    fn : function(store, records, options) {
                        this.isReloading = false;
                        this.shareData();
                    },
                    scope: this
                },
                remove : {
                    fn : function(store, record, index) {

                        if (record.get('type') == HST.CONTAINER) {
                            //remove all children as well
                            Ext.each(record.get('children'), function(id) {
                                var childIndex = store.findExact('id', id);
                                if (childIndex > -1) {
                                    store.removeAt(childIndex);
                                }
                            });
                        } else {
                            //containerItem: unregister from parent
                            var parentRecord = store.getAt(store.findExact('id', record.get('parentId')));
                            if (typeof parentRecord !== 'undefined') {
                                var children = parentRecord.get('children');
                                children.remove(record.get('id'));
                                parentRecord.set('children', children);
                            }
                        }
                        var grid = Ext.getCmp('PageModelGrid');
                        if (grid.getSelectionModel().getSelected() == record) {
                            this.deselect(null, null, record);
                        }
                        this.sendFrameMessage({element: record.data.element}, 'remove');
                    },
                    scope : this
                }
            }
        });
    },

    createMainWindow : function() {
        var window1 = new Hippo.ux.window.FloatingWindow({
            title: 'Configuration',
            x:10, y: 35,
            width: 310,
            height: 650,
            initRegion: 'right',
            layout: 'border',
            closable: true,
            constrainHeader: true,
            closeAction: 'hide',
            items: [
                {
                    region: 'north',
                    split:true,
                    layout: 'accordion',
                    height: 300,
                    items:[
                        {
                            xtype: 'h_base_grid',
                            flex:2,
                            id: 'ToolkitGrid',
                            title: 'Toolkit',
                            store: this.stores.toolkit,
                            cm: new Ext.grid.ColumnModel({
                                columns: [
                                    { header: "Name", dataIndex: 'name', id:'name', viewConfig :{width: 40}},
                                    { header: "Id", dataIndex: 'id', id:'id', viewConfig :{width: 40}},
                                    { header: "Path", dataIndex: 'path', id:'path', viewConfig :{width: 120}}
                                ],
                                defaults: {
                                    sortable: true,
                                    menuDisabled: true
                                }
                            }),
                            plugins: [
                                Hippo.App.DragDropOne
                            ]
                        },
                        {
                            xtype: 'h_base_grid',
                            flex: 3,
                            id: 'PageModelGrid',
                            title: 'Containers',
                            store: this.stores.pageModel,
                            sm: new Ext.grid.RowSelectionModel({
                                singleSelect: true,
                                listeners: {
                                    rowselect: {
                                        fn: this.select,
                                        scope: this
                                    },
                                    rowdeselect: {
                                        fn: this.deselect,
                                        scope: this
                                    }
                                }
                            }),
                            cm : new Ext.grid.ColumnModel({
                                columns: [
                                    { header: "Name", dataIndex: 'name', id:'name', viewConfig :{width: 120}},
                                    { header: "Type", dataIndex: 'type', id:'type'},
                                    { header: "Template", dataIndex: 'template', id:'template'}
                                ],
                                defaults: {
                                    sortable: false,
                                    menuDisabled: true
                                }
                            }),
                            menuProvider: this
                        }
                    ]
                },
                {
                    id: 'componentPropertiesPanel',
                    xtype:'h_properties_panel',
                    region: 'center',
                    split: true
                }
            ]
        });
        return window1;
    },

    shareData : function() {
        var self = this;
        var facade = function() {
        };
        facade.prototype = {
            getName : function(id) {
                var idx = self.stores.pageModel.findExact('id', id);
                if (idx == -1) {
                    return null;
                }
                var record = self.stores.pageModel.getAt(idx);
                return record.get('name');
            }
        };

        if (this.pageModelFacade == null) {
            this.pageModelFacade = new facade();
        }
        this.sendFrameMessage(this.pageModelFacade, 'sharedata');
    },

    handleOnClick : function(element) {
        var id = element.getAttribute('hst:id');
        var recordIndex = this.stores.pageModel.findExact('id', id);

        if (recordIndex < 0) {
            console.warn('Handling onClick for element[@hst:id=' + id + '] with no record in component store');
            return;
        }

        var sm = Ext.getCmp('PageModelGrid').getSelectionModel();
        if (sm.isSelected(recordIndex)) {
            sm.deselectRow(recordIndex);
        } else {
            sm.selectRow(recordIndex);
        }
    },

    findElement: function(id) {
        var frameDoc = Ext.getCmp('Iframe').getFrameDocument();
        var el = frameDoc.getElementById(id);
        return el;
    },

    select : function(model, index, record) {
        this.sendFrameMessage({element: record.data.element}, 'select');
        if (record.get('type') === HST.CONTAINERITEM) {
            this.showProperties(record);
        }
    },

    deselect : function(model, index, record) {
        this.sendFrameMessage({element: record.data.element}, 'deselect');
        this.hideProperties();
    },

    onRearrangeContainer: function(id, children) {
        var recordIndex = this.stores.pageModel.findExact('id', id);//should probably do this through the selectionModel
        var record = this.stores.pageModel.getAt(recordIndex);
        record.set('children', children);
        record.commit();
    },

    handleReceivedItem : function(containerId, element) {
        //we reload for now so no action here, children value update of containers will take care of it
    },

    sendFrameMessage : function(data, name) {
        Ext.getCmp('Iframe').getFrame().sendMessage(data, name);
    },

    showProperties : function(record) {
        Ext.getCmp('componentPropertiesPanel').reload(record.get('id'), record.get('name'), record.get('path'));
    },

    hideProperties : function() {
        Ext.getCmp('componentPropertiesPanel').removeAll();
    },

    /**
     * ContextMenu provider
     */
    getMenuActions : function(record, selected) {
        var actions = [];
        var store = this.stores.pageModel;
        var type = record.get('type');
        if (type == HST.CONTAINERITEM) {
            actions.push(new Ext.Action({
                text: 'Delete',
                handler: function() {
                    this.removeByRecord(record)
                },
                scope: this
            }));
        }
        var children = record.get('children');
        if (type == HST.CONTAINER && children.length > 0) {
            actions.push(new Ext.Action({
                text: 'Delete items',
                handler: function() {
                    var msg = 'You are about to remove ' + children.length + ' items, are your sure?';
                    Ext.Msg.confirm('Confirm delete', msg, function(btn, text) {
                        if (btn == 'yes') {
                            var r = [children.length];
                            Ext.each(children, function(c) {
                                r.push(store.getAt(store.findExact('id', c)));
                            });
                            //it seems that calling store.remove(r) will end up re-calling the destroy api call for
                            //all previous items in r.. maybe a bug, for now do a loop
                            Ext.each(r, store.remove, store);
                            //store.remove(r);
                        }
                    });
                },
                scope: this
            }));
        }
        return actions;
    },

    removeByRecord: function(record) {
        var store = this.stores.pageModel;
        Ext.Msg.confirm('Confirm delete', 'Are your sure you want to delete ' + record.get('name') + '?', function(btn, text) {
            if (btn == 'yes') {
                store.remove(record);
            }
        });
    },

    removeByElement : function(element) {
        var store = this.stores.pageModel;
        var index = store.findExact('id', Ext.fly(element).getAttribute('hst:id'));
        this.removeByRecord(store.getAt(index))
    },

    /**
     * It's not possible to register message:afterselect style listeners..
     * This should work and I'm probably doing something stupid, but I could not
     * get it to work.. So do like this instead.....
     */
    handleFrameMessages : function(frm, msg) {
        try {
            if (msg.tag == 'rearrange') {
                this.onRearrangeContainer(msg.data.id, msg.data.children);
            } else if (msg.tag == 'onclick') {
                this.handleOnClick(msg.data.element);
            } else if (msg.tag == 'receiveditem') {
                this.handleReceivedItem(msg.data.id, msg.data.element);
            } else if (msg.tag == 'remove') {
                this.removeByElement(msg.data.element);
            } else if (msg.tag == 'onappload') {
                this.onIframeAppLoaded(msg.data);
            } else if (msg.tag == 'refresh') {
                this.refreshIframe();
            }
        } catch(e) {
            console.error(e);
        }
    }

});

Hippo.App.RestStore = Ext.extend(Ext.data.Store, {

    constructor : function(config) {

        var reader = new Ext.data.JsonReader({
            successProperty: 'success',
            root: 'data',
            messageProperty: 'message',
            idProperty: 'id'
        }, config.prototypeRecord);

        var writer = new Ext.data.JsonWriter({
            encode: false   // <-- don't return encoded JSON -- causes Ext.Ajax#request to send data using jsonData config rather than HTTP params
        });

        var cfg = {
            restful: true,
            reader: reader,
            writer: writer
        };

        Ext.apply(this, cfg, config);
        Hippo.App.RestStore.superclass.constructor.call(this, config);
    }
});

Hippo.App.ToolkitStore = Ext.extend(Hippo.App.RestStore, {

    constructor : function(config) {

        var proxy = new Ext.data.HttpProxy({
            api: {
                read     : 'services-new/' + config.toolkitId + './toolkit'
                ,create  : '#'
                ,update  : '#'
                ,destroy : '#'
            }
        });

        var cfg = {
            id: 'ToolkitStore',
            proxy: proxy,
            prototypeRecord : Hippo.App.PageModel.ReadRecord
        };

        Ext.apply(config, cfg);

        Hippo.App.ToolkitStore.superclass.constructor.call(this, config);
    }
});

Hippo.App.PageModelStore = Ext.extend(Hippo.App.RestStore, {

    constructor : function(config) {

        var proxy = new Ext.data.HttpProxy({
            api: {
                read     : 'services-new/' + config.pageId + './pagemodel'
                ,create  : {url: 'services/PageModelService/create', method: 'POST'}  // Server MUST return idProperty of new record
                ,update  : {url: 'services/PageModelService/update', method: 'POST'}
                ,destroy : {url: 'services/PageModelService/destroy', method: 'GET'}
            },

            listeners : {
                beforewrite : {
                    fn : function(proxy, action, rs, params) {
                        if (action == 'create') {
                            var prototypeId = rs.get('id');
                            var parentId = rs.get('parentId');
                            proxy.setApi(action, {url: 'services-new/' + parentId + './create/' + prototypeId , method: 'POST'});
                        } else if (action == 'update') {
                            //Ext appends the item ID automatically
                            var id = rs.get('id');
                            proxy.setApi(action, {url: 'services-new/' + id + './update' , method: 'POST'});
                        } else if (action == 'destroy') {
                            //Ext appends the item ID automatically
                            var parentId = rs.get('parentId');
                            proxy.setApi(action, {url: 'services-new/' + parentId + './delete' , method: 'GET'});
                        }
                    }
                }
            }
        });
        var cfg = {
            id: 'PageModelStore',
            proxy: proxy,
            prototypeRecord : Hippo.App.PageModel.ReadRecord
        };

        Ext.apply(config, cfg);

        Hippo.App.PageModelStore.superclass.constructor.call(this, config);
    }
});


Hippo.App.DragDropOne = (function() {

    return {

        init: function(c) {
            c.onRender = c.onRender.createSequence(this.onRender);
        },

        onRender: function() {
            var miframePanel = Ext.getCmp('Iframe');
            var miframe = miframePanel.getFrame();

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
                    Hippo.App.Main.stores.pageModel.each(function(record) {
                        var type = record.get('type');
                        if (record.get('type') === HST.CONTAINER) {
                            var id = record.get('id') + '-overlay';
                            var el = frmDoc.getElementById(id);
                            var box = Ext.Element.fly(el).getBox();
                            self.boxs.push({record: record, box: box});
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
                    var curX = dd.lastPageX + dd.deltaX;
                    var curY = dd.lastPageY + dd.deltaY;
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
                    if (self.nodeOverRecord != null) {
                        var selections = containerItemsGrid.getSelectionModel().getSelections();

                        var pmGrid = Ext.getCmp('PageModelGrid');
                        var pmRecord = self.nodeOverRecord;
                        var pmStore = pmGrid.getStore();
                        var parentId = pmRecord.get('id');

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
                            var model = Hippo.App.PageModel.Factory.createModel(null, cfg);
                            models.push(model);
                            pmStore.insert(at + i, Hippo.App.PageModel.Factory.createRecord(model));
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    };
})();
