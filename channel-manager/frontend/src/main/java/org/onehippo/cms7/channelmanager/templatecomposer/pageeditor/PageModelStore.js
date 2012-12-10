/*
 *  Copyright 2011 Hippo.
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
(function() {
    "use strict";

    Hippo.ChannelManager.TemplateComposer.PageModelStore = Ext.extend(Hippo.ChannelManager.TemplateComposer.RestStore, {

        constructor: function(config) {
            var composerRestMountUrl, PageModelProxy, cfg;

            composerRestMountUrl = config.composerRestMountUrl;
            PageModelProxy = Ext.extend(Ext.data.HttpProxy, {
                buildUrl: function() {
                    return PageModelProxy.superclass.buildUrl.apply(this, arguments) + '?FORCE_CLIENT_HOST=true';
                }
            });

            cfg = {
                id: 'PageModelStore',
                proxy: new PageModelProxy({
                    api: {
                        read: composerRestMountUrl + '/' + config.mountId + './pagemodel/' + config.pageId + "/", create: '#' // see beforewrite
                        , update: '#', destroy: '#'
                    },

                    listeners: {
                        beforewrite: {
                            fn: function(proxy, action, rs, params) {
                                var prototypeId, parentId, id;
                                if (action === 'create') {
                                    prototypeId = rs.get('id');
                                    parentId = rs.get('parentId');
                                    proxy.setApi(action, {url: composerRestMountUrl + '/' + parentId + './create/' + prototypeId, method: 'POST'});
                                } else if (action === 'update') {
                                    //Ext appends the item ID automatically
                                    id = rs.get('id');
                                    proxy.setApi(action, {url: composerRestMountUrl + '/' + id + './update', method: 'POST'});
                                } else if (action === 'destroy') {
                                    //Ext appends the item ID automatically
                                    parentId = rs.get('parentId');
                                    proxy.setApi(action, {url: composerRestMountUrl + '/' + parentId + './delete', method: 'GET'});
                                }
                            }
                        }
                    }
                }),
                listeners: {
                    write: {
                        fn: function(store, action, result, res, records) {
                            var i, record, parentId, parentIndex, parentRecord, children;
                            if (action === 'create') {
                                records = Ext.isArray(records) ? records : [records];
                                for (i = 0; i < records.length; i++) {
                                    record = records[i];
                                    if (record.get('type') === HST.CONTAINERITEM) {
                                        //add id to parent children map
                                        parentId = record.get('parentId');
                                        parentIndex = store.findExact('id', parentId);
                                        parentRecord = store.getAt(parentIndex);
                                        children = parentRecord.get('children');
                                        if (!children) {
                                            children = [];
                                        }
                                        children.push(record.get('id'));
                                        parentRecord.set('children', children);
                                    }
                                }
                            } else if (action === 'update') {
                                if (!this.isReloading) {
                                    this.isReloading = true;
                                    store.reload();
                                }
                            }
                        },
                        scope: this
                    },
                    remove: {
                        fn: function(store, record, index) {
                            var childIndex, parentRecord, children;
                            if (record.get('type') === HST.CONTAINER) {
                                //remove all children as well
                                Ext.each(record.get('children'), function(id) {
                                    childIndex = store.findExact('id', id);
                                    if (childIndex > -1) {
                                        store.removeAt(childIndex);
                                    }
                                });
                            } else {
                                //containerItem: unregister from parent
                                parentRecord = store.getAt(store.findExact('id', record.get('parentId')));
                                if (typeof parentRecord !== 'undefined') {
                                    children = parentRecord.get('children');
                                    children.remove(record.get('id'));
                                    parentRecord.set('children', children);
                                }
                            }
                        },
                        scope: this
                    },
                    load: {
                        fn: function(store, records, options) {
                            this.isReloading = false;
                        },
                        scope: this
                    }
                },
                prototypeRecord: [
                    {name: 'id', mapping: 'id'},
                    {name: 'name', mapping: 'name'},
                    {name: 'path', mapping: 'path'},
                    {name: 'parentId', mapping: 'parentId'},
                    {name: 'componentClassName', mapping: 'componentClassName'},
                    {name: 'template', mapping: 'template'},
                    {name: 'type', mapping: 'type'},
                    {name: 'xtype', mapping: 'xtype'},
                    {name: 'children', mapping: 'children'}
                ]
            };

            Ext.apply(config, cfg);

            Hippo.ChannelManager.TemplateComposer.PageModelStore.superclass.constructor.call(this, config);
        }
    });

}());