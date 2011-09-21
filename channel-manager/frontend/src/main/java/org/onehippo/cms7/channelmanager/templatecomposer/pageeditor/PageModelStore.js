
Hippo.ChannelManager.TemplateComposer.PageModelStore = Ext.extend(Hippo.ChannelManager.TemplateComposer.RestStore, {

    constructor : function(config) {

        var composerRestMountUrl = config.composerRestMountUrl;
        var ignoreRenderHostParameterName = config.ignoreRenderHostParameterName;

        var PageModelProxy = Ext.extend(Ext.data.HttpProxy, {
            buildUrl : function() {
                 return PageModelProxy.superclass.buildUrl.apply(this, arguments) + '?' + ignoreRenderHostParameterName + '=true';
            }
        });

        var cfg = {
            id: 'PageModelStore',
            proxy: new PageModelProxy({
                api: {
                    read     : composerRestMountUrl + config.mountId + './pagemodel/' + config.pageId + "/"
                    ,create  : '#' // see beforewrite
                    ,update  : '#'
                    ,destroy : '#'
                },

                listeners : {
                    beforeload: {
                        fn: function (store, options) {
                            Hippo.Msg.wait(config.resources['page-model-store-before-load-message']);
                        }
                    },
                    beforewrite : {
                        fn : function(proxy, action, rs, params) {
                            Hippo.Msg.wait(config.resources['page-model-store-before-write-message']);
                            if (action == 'create') {
                                var prototypeId = rs.get('id');
                                var parentId = rs.get('parentId');
                                proxy.setApi(action, {url: composerRestMountUrl + parentId + './create/' + prototypeId, method: 'POST'});
                            } else if (action == 'update') {
                                //Ext appends the item ID automatically
                                var id = rs.get('id');
                                proxy.setApi(action, {url: composerRestMountUrl + id + './update', method: 'POST'});
                            } else if (action == 'destroy') {
                                //Ext appends the item ID automatically
                                var parentId = rs.get('parentId');
                                proxy.setApi(action, {url: composerRestMountUrl + parentId + './delete', method: 'GET'});
                            }
                        }
                    },
                    write :{
                        fn: function(store, action, result, res, rs) {
                            Hippo.Msg.hide();
                            Hippo.ChannelManager.TemplateComposer.Instance.refreshIframe();
                        }
                    },
                    load : {
                        fn: function (store, records, options) {
                            Hippo.Msg.hide();
                        }
                    }
                }
            }),
            prototypeRecord : Hippo.ChannelManager.TemplateComposer.PageModel.ReadRecord
        };

        Ext.apply(config, cfg);

        Hippo.ChannelManager.TemplateComposer.PageModelStore.superclass.constructor.call(this, config);
    }
});
