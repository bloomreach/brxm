Ext.namespace('Hippo.ChannelManager');

/*
 --------------------------
 ChannelGridPanel
 --------------------------
 */

Hippo.ChannelManager.ChannelGridPanel = Ext.extend(Ext.grid.GridPanel, {
  constructor: function(config) {
    this.store = config.store;
    this.columns = config.columns;
    Hippo.ChannelManager.ChannelGridPanel.superclass.constructor.call(this, config);
  },

  initComponent: function() {
    var me = this;
    var config = {
      title: 'Manage Channels',
      stripeRows: true,
      tbar: [{
        text: "New Channel",
        handler: me.showChannelWindow,
        scope: me
      }],

      height: 400,
      viewConfig: {
        forceFit: true
      },

      colModel: new Ext.grid.ColumnModel({
        columns: [{
          header: 'Channel Name',
          width: 20,
          scope: me
        }]
      })
    };

    Ext.apply(this, Ext.apply(this.initialConfig, config));

    Hippo.ChannelManager.ChannelGridPanel.superclass.initComponent.apply(this, arguments);

  },


  showChannelWindow: function() {
    console.log("TODO: Show channel window with blueprints ");

  }

});

Ext.reg('Hippo.ChannelManager.ChannelGridPanel', Hippo.ChannelManager.ChannelGridPanel);
