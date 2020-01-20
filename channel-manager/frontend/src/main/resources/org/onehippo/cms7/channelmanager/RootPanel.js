/**
 * Copyright 2011-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
  "use strict";

  Ext.namespace('Hippo.ChannelManager');

  /**
   * @class Hippo.ChannelManager.RootPanel
   * @extends Ext.Panel
   */
  Hippo.ChannelManager.RootPanel = Ext.extend(Ext.Panel, {

    constructor: function (config) {
      this.channelStore = config.channelStore;
      this.blueprintStore = config.blueprintStore;
      this.resources = config.resources;
      this.selectedChannelId = null;
      this.perspectiveId = config.perspectiveId;

      Ext.apply(config, {
        id: 'rootPanel',
        layout: 'card',
        layoutOnCardChange: true,
        deferredRender: true,
        viewConfig: {
          forceFit: true
        },
        border: false
      });

      Hippo.ChannelManager.RootPanel.superclass.constructor.call(this, config);

      this.selectCard(config.activeItem);
    },

    initComponent: function () {
      var self, channelSelectedHandler;

      self = this;
      this.addEvents('navigate-to-channel-overview');

      // recalculate the ExtJs layout when the YUI layout manager fires a resize event
      this.on('afterlayout', function () {
        var yuiLayout = this.getEl().findParent("div.yui-layout-unit");
        YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, function (sizes) {
          self.setSize(sizes.body.w, sizes.body.h);
          self.doLayout();
        }, true);
      }, this, {single: true});

      this.on('afterrender', function () {
        // only show the channel manager breadcrumb when channel manager is active
        Hippo.Events.subscribe('CMSChannels', this._onActivate, this);
        this.on('navigate-to-channel-overview', this._showChannelOverview, this);
      }, this, {single: true});

      // get all child components
      this.win = new Hippo.ChannelManager.NewChannelWindow({
        blueprintStore: self.blueprintStore,
        channelStore: self.channelStore,
        resources: self.resources
      });
      this.formPanel = Ext.getCmp('channel-form-panel');
      this.gridPanel = Ext.getCmp('channel-grid-panel');
      this.channelIconPanel = Ext.getCmp('channelIconPanel');
      this.channelOverviewPanel = Ext.getCmp('channelOverview');

      // register channel creation events
      this.channelOverviewPanel.on('add-channel', function () {
        this.win.show();
      }, this);
      this.formPanel.on('channel-created', function () {
        this.win.hide();
        this.channelStore.reload();
      }, this);

      channelSelectedHandler = function (channelId, record) {
        this.selectedChannelId = channelId;
        // don't activate channel editor when it is already active
        if (this.layout.activeItem === Hippo.ChannelManager.ChannelEditor.Instance) {
          return;
        }
        Hippo.ChannelManager.ChannelEditor.Instance.initChannel(channelId, '', 'master');
        Hippo.ChannelManager.ChannelEditor.Instance.loadChannel();
        self._showChannelEditor();
      };

      this.gridPanel.on('channel-selected', channelSelectedHandler, this);
      this.channelIconPanel.on('channel-selected', channelSelectedHandler, this);

      Hippo.ChannelManager.ChannelEditor.Instance.on('show-channel-overview', this._showChannelOverview, this);

      Hippo.ChannelManager.RootPanel.superclass.initComponent.apply(this, arguments);
    },

    _onActivate: function() {
      if (this._isChannelEditorShown()) {
        this.layout.activeItem.loadChannel();
      }
    },



    selectCard: function (itemId) {

      if ((typeof itemId !== 'undefined') && itemId !== 0) {
        this.layout.setActiveItem(itemId);
      } else {
        this.layout.setActiveItem(0);
      }
    },

    _showChannelOverview: function () {
      this.selectCard(0);
    },

    _showChannelEditor: function () {
      this.layout.setActiveItem(1);
    },

    _isChannelEditorShown: function () {
      return this.layout.activeItem === this.items.get(1);
    }
  });

  Ext.reg('Hippo.ChannelManager.RootPanel', Hippo.ChannelManager.RootPanel);

  Hippo.ChannelManager.NewChannelWindow = Ext.extend(Ext.Window, {

    constructor: function (config) {
      this.blueprintStore = config.blueprintStore;
      this.channelStore = config.channelStore;
      this.resources = config.resources;

      Hippo.ChannelManager.NewChannelWindow.superclass.constructor.call(this, config);
    },

    initComponent: function () {
      var config = {
        title: this.resources['new-channel-blueprint'],
        width: 720,
        height: 450,
        modal: true,
        resizable: false,
        cls: 'br-window',
        closeAction: 'hide',
        layout: 'fit',
        items: [
          {
            id: 'card-container',
            layout: 'card',
            bodyStyle: 'border: 0',
            activeItem: 0,
            layoutConfig: {
              hideMode: 'offsets',
              deferredRender: true,
              layoutOnCardChange: true
            }
          }
        ],
        buttonAlign: 'left',
        buttons: [
          {
            id: 'previousButton',
            cls: 'btn btn-default',
            text: this.resources['new-channel-previous'],
            handler: this.processPreviousStep,
            width: 'auto',
            scope: this,
            hidden: true
          },
          '->',
          {
            id: 'cancelButton',
            cls: 'btn btn-default',
            text: this.resources['new-channel-cancel'],
            scope: this,
            handler: function () {
              this.hide();
            }
          },
          {
            id: 'nextButton',
            cls: 'btn btn-br-primary',
            text: this.resources['new-channel-next'],
            handler: this.processNextStep,
            scope: this
          }
        ]
      };

      Ext.apply(this, Ext.apply(this.initialConfig, config));

      Hippo.ChannelManager.NewChannelWindow.superclass.initComponent.apply(this, arguments);

      this.on('beforeshow', this.resetWizard, this);
      this.on('beforeshow', this.showMask, this);
      this.on('beforehide', this.hideMask, this);

      Ext.getCmp('card-container').add(new Hippo.ChannelManager.BlueprintListPanel({
        id: 'blueprints-panel',
        store: this.blueprintStore,
        resources: this.resources
      }));

      Ext.getCmp('card-container').add(new Hippo.ChannelManager.ChannelFormPanel({
        id: 'channel-form-panel',
        store: this.channelStore,
        resources: this.resources
      }));

    },

    showBlueprintChoice: function () {
      this.setTitle(this.resources['new-channel-blueprint']);
      Ext.getCmp('card-container').layout.setActiveItem('blueprints-panel');
      Ext.getCmp('nextButton').setText(this.resources['new-channel-next']);
      Ext.getCmp('previousButton').hide();
    },

    showChannelForm: function () {
      this.setTitle(this.resources['new-channel-properties']);
      Ext.getCmp('card-container').layout.setActiveItem('channel-form-panel');
      Ext.getCmp('nextButton').setText(this.resources['new-channel-create']);
      Ext.getCmp('previousButton').show();
    },

    resetWizard: function () {
      this.showBlueprintChoice();
    },

    showMask: function () {
      if (Hippo && Hippo.showMask) {
        Hippo.showMask();
      }
    },

    hideMask: function () {
      if (Hippo && Hippo.hideMask) {
        Hippo.hideMask();
      }
    },

    processPreviousStep: function () {
      if (Ext.getCmp('card-container').layout.activeItem.id === 'channel-form-panel') {
        this.showBlueprintChoice();
      }
    },

    processNextStep: function () {
      if (Ext.getCmp('card-container').layout.activeItem.id === 'blueprints-panel') {
        this.showChannelForm();
      } else {
        this.submitChannelForm();
      }
    },

    submitChannelForm: function () {
      var nextButton = Ext.getCmp('nextButton');
      nextButton.disable();
      Ext.getCmp('channel-form-panel').submitForm().when(function () {
        nextButton.enable();
      }).otherwise(function () {
        nextButton.enable();
      });
    }

  });

}());
