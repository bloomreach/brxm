Ext.namespace('Hippo.App');

//FIXME: use event wiring instead
var G_canDrag = false;

Hippo.App.PageEditor = Ext.extend(Ext.App, {

    loadMessage: 'Initializing application',

    init : function() {

        if(this.debug) {
            Ext.data.DataProxy.addListener('exception', function(proxy, type, action, options, res, e) {
                if(!res.success && res.message) {
                    this.addAlert(false, "Server-side error occurred while executing action=" + action);
                    console.error('Server side error: ' + res.message);
                }
                else {
                    if (e && typeof console.error == 'function') {
                        console.error(e);
                    } else {
                        console.group("Exception");
                        console.dir(arguments);
                        console.groupEnd();
                    }
                    this.addAlert(false, "Something bad happened while executing " + action);
                }
            }, this);

            Ext.data.DataProxy.addListener('write', function(proxy, action, result, res, rs) {
                this.addAlert(true, 'Action: ' + action + '<br/>Message: ' + res.message);
            }, this);


            Ext.Ajax.timeout = 90000; // this changes the 30 second default to 90 seconds
        }

        this.initStores();
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
                    loadMask: false,
                    defaultSrc: this.iframeUrl,
                    collapsible: false,
                    disableMessaging: false,
                    listeners: {
                        'message': {
                            fn: this.handleFrameMessages,
                            scope:this
                        },
                        'documentloaded' : {
                            fn: this.loadComponentsFromIframe,
                            scope: this
                        },
                        'exception' : {
                            fn: function(e) {
                                console.error(e);
                            },
                            scope: this
                        }
                    }
                }
            ]
        });

        var window1 = new Hippo.ux.window.FloatingWindow({
            title: 'Configuration',
            x:5, y: 10,
            width: 300,
            height:400,
            initRegion: 'right',
            layout: {
                type:'vbox',
                align: 'stretch'
            },
            closable: false,
            constrainHeader: true,
            items: [
                {
                    xtype: 'h_base_grid',
                    flex: 3,
                    id: 'PageModelGrid',
                    title: 'Containers',
                    store: this.pageModelStore,
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
                },
                {
                    xtype: 'h_base_grid',
                    flex:2,
                    title: 'Available container items',
                    store: this.containerItemsStore,
                    cm: new Ext.grid.ColumnModel({
                        columns: [
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
                }
            ]
        });
        this.beforeQuit(window1.close, window1);
        window1.show();

        var window2 = new Hippo.ux.window.FloatingWindow({
            id: 'propertiesWindow',
            title: 'Properties',
            x:5, y: 410,
            width: 300,
            height: 250,
            layout: {
                type:'vbox'
            },
            initRegion: 'right',
            closable: false,
            constrainHeader: true,
            items:[
                {
                    id: 'componentPropertiesPanel',
                    xtype:'h_properties_panel',
                    width: 280
                }
            ]
        });
        this.beforeQuit(window2.close, window2);
    },

    initStores : function() {
        this.pageModelStore = new Ext.data.ArrayStore({
            fields: [
                {name: 'id'},
                {name: 'name'},
                {name: 'type'},
                {name: 'config'}
            ]
        });

        this.containerItemsStore = new Ext.data.ArrayStore({
            fields: [
                {name: 'id'},
                {name: 'path'},
                {name: 'componentClassName'},
                {name: 'template'}
            ]
        });
        var d = [
            ['Label1', 'name1', 'org.onehippo.components.Label', 'label']
        ];
        this.containerItemsStore.loadData(d);
    },

    loadComponentsFromIframe : function(frm) {
        //Tell the Iframe to subscribe itself for attach/detach messages from the parent (this)
        frm.execScript('Hippo.DD.Main.init()', true);

        this.currentIframe = frm;
        var store = this.pageModelStore;

        var models = [Hippo.App.PageModel.Factory.createModel(Ext.getBody(), {isRoot: true, type: 'page'})];
        var count = 0;
        frm.select('div.componentContentWrapper', true).each(function(el, c, idx) {
            var index = models.length - 1;
            while (index > 0 && !Ext.fly(models[index].element).contains(el)) {
                --index;
            }
            var model = Hippo.App.PageModel.Factory.createModel(el.dom, {parent: models[index]});
            models.push(model);
            store.insert(count++, Hippo.App.PageModel.Factory.createRecord(model));
        });

        this.models = models; //TODO: probably not needed

        this.pageModelStore.filterBy(function(record, id) {
            return record.data.type === 'hst:containercomponent' || record.data.type === 'hst:containeritemcomponent';
        });

        //TODO: load pageModel data from server
        this.initPageModelStore();
    },

    initPageModelStore : function() {
        var containers = '';
        Ext.iterate(this.models, function(item){
            if(item.type == 'hst:containercomponent') {
                if(containers.length > 0) {
                    containers += ',';
                }
                containers += item.id;
            }
        });

        var myProxy = Ext.extend(Ext.data.HttpProxy, {
            doRequest : function(action, rs, params, reader, cb, scope, arg) {
                if (action == 'read') {
                    params['containers'] = containers
                }
                myProxy.superclass.doRequest.apply(this, [action, rs, params, reader, cb, scope, arg]);
            }
        });

        var proxy = new myProxy({
            api: {
                read    : 'services/PageModelService/read'
               ,create  : {url: 'services/PageModelService/create', method: 'POST'}  // Server MUST return idProperty of new record
               ,update  : {url: 'services/PageModelService/update', method: 'POST'}
               ,destroy : {url: 'services/PageModelService/destroy', method: 'GET'}
            }
        });

        var writer = new Ext.data.JsonWriter({
            encode: false   // <-- don't return encoded JSON -- causes Ext.Ajax#request to send data using jsonData config rather than HTTP params
        });

        // Typical Store collecting the Proxy, Reader and Writer together.
        var store = new Ext.data.Store({
            id: 'user',
            restful: true,     // <-- This Store is RESTful
            proxy: proxy,
            reader: new Ext.data.JsonReader({
                successProperty: 'success',
                root: 'data',
                messageProperty: 'message',  // <-- New "messageProperty" meta-data
                idProperty: 'id'
            }, Hippo.App.PageModel.ReadRecord),
            writer: writer,
            listeners: {
                write : {
                    fn: function(store, action, result, res, records) {
                        if(action == 'create') {
                            records = Ext.isArray(records) ? records : [records];
                            for(var i=0; i<records.length; i++) {
                                var record = records[i];
                                if (record.get('type') == 'hst:containeritemcomponent') {
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
                            if(!this.isReloading) {
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
                    },
                    scope: this
                },
                remove : {
                    fn : function(store, record, index) {
                        if (store.silentRemove) {
                            return;
                        }

                        if (record.get('type') == 'hst:containercomponent') {
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
                            if(typeof parentRecord !== 'undefined') {
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

        // load the store immeditately
        store.load();

        var cm = new Ext.grid.ColumnModel({
            columns: [
//                            { header: "Id", dataIndex: 'id', id:'id', viewConfig :{width: 40}},
                { header: "Name", dataIndex: 'name', id:'name', viewConfig :{width: 120}},
                { header: "Type", dataIndex: 'type', id:'type'},
                { header: "Template", dataIndex: 'template', id:'template'}
            ],
            defaults: {
                sortable: false,
                menuDisabled: true
            }
        });

        Ext.getCmp('PageModelGrid').reconfigure(store, cm);

        this.pageModelStore = store;
    },

    handleOnClick : function(element) {
        var id = element.getAttribute('hst:id');
        var recordIndex = this.pageModelStore.findExact('id', id);

        if(recordIndex < 0) {
            console.warn('Handling onClick for element[@hst:id=' + id + '] with no record in component store');
            return;
        }

        var sm = Ext.getCmp('PageModelGrid').getSelectionModel();
        if(sm.isSelected(recordIndex)) {
            sm.deselectRow(recordIndex);
        } else {
            sm.selectRow(recordIndex);
        }
    },

    findElement: function(id) {
        for(var i=0; i<this.models.length; i++) {
            if(this.models[i].id === id) {
                return this.models[i].element;
            }
        }
        var frameDoc = Ext.getCmp('Iframe').getFrameDocument();
        var el = frameDoc.getElementById(id);
        return el;
    },

    select : function(model, index, record) {
        this.sendFrameMessage({element: record.data.element}, 'select');
        this.showProperties(record);

        G_canDrag = record.data.type == 'hst:containercomponent';
    },

    deselect : function(model, index, record) {
        this.sendFrameMessage({element: record.data.element}, 'deselect');
        this.hideProperties();

        G_canDrag = false;
    },

    onRearrangeContainer: function(id, children) {
        var recordIndex = this.pageModelStore.findExact('id', id);//should probably do this through the selectionModel
        var record = this.pageModelStore.getAt(recordIndex);
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
        Ext.getCmp('propertiesWindow').show();
    },

    hideProperties : function() {
        Ext.getCmp('componentPropertiesPanel').removeAll();
        Ext.getCmp('propertiesWindow').hide();
    },

    /**
     * ContextMenu provider
     */
    getMenuActions : function(record, selected) {
        var store = this.pageModelStore;
        var actions = [
            new Ext.Action({
                text: 'Delete',
                handler: function() {
                    Ext.Msg.confirm('Confirm delete', 'Are your sure?', function(btn, text) {
                        if (btn == 'yes') {
                            store.remove(record);
                        }
                    });
                },
                scope: this
            })
        ];
        var children = record.get('children');
        if (record.get('type') == 'hst:containercomponent' && children.length > 0) {
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
            }
        } catch(e) {
            console.error(e);
        }
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
                    var containerRecords = Hippo.App.Main.pageModelStore.query('type', /^hst:containercomponent$/).each(function(item, index, length) {
                        var box = Ext.Element.fly(item.get('element')).getBox();
                        self.boxs.push({record: item, box: box});
                        return true;
                    });
                    Ext.ux.ManagedIFrame.Manager.showShims();
                },

                onEndDrag : function() {
                    self.boxs = [];
                    Ext.ux.ManagedIFrame.Manager.hideShims();
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

                    for(var i=0; i<self.boxs.length; i++) {
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
                        for(var i=0; i< selections.length; i++) {
                            var record = selections[i];
                            var cfg = {
                                parentId: parentId,
                                name: null,
                                type: 'hst:containeritemcomponent',
                                template: record.get('template'),
                                componentClassName : record.get('componentClassName') 
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
