
Hippo.ChannelManager.TemplateComposer.RestStore = Ext.extend(Ext.data.Store, {

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
        Hippo.ChannelManager.TemplateComposer.RestStore.superclass.constructor.call(this, config);
    }
});
