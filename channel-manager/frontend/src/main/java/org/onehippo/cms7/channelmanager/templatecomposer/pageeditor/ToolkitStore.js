
Hippo.ChannelManager.TemplateComposer.ToolkitStore = Ext.extend(Hippo.ChannelManager.TemplateComposer.RestStore, {

    constructor : function(config) {
        var proxy = new Ext.data.HttpProxy({
            api: {
                read     : config.composerRestMountUrl + config.mountId + './toolkit?'+config.ignoreRenderHostParameterName+'=true'
                ,create  : '#'
                ,update  : '#'
                ,destroy : '#'
            }
        });

        var cfg = {
            id: 'ToolkitStore',
            proxy: proxy,
            prototypeRecord :  [
                {name: 'id', mapping: 'id'},
                {name: 'name', mapping: 'name'},
                {name: 'componentClassName', mapping: 'componentClassName'},
                {name: 'template', mapping: 'template'},
                {name: 'xtype', mapping: 'xtype'}
            ]
        };

        Ext.apply(config, cfg);

        Hippo.ChannelManager.TemplateComposer.ToolkitStore.superclass.constructor.call(this, config);
    }
});
