/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function ($) {
  "use strict";

  Ext.namespace('Hippo.ChannelManager.ChannelEditor');

  Hippo.ChannelManager.ChannelEditor.ComponentPropertiesEditor = Ext.extend(Ext.Panel, {

    componentId: null,
    variant: null,
    componentPropertiesForm: null,
    isReadOnly: false,

    constructor: function (config) {
      Hippo.ChannelManager.ChannelEditor.ComponentPropertiesEditor.superclass.constructor.call(this, Ext.apply(config, {
        bubbleEvents: ['enableDeleteComponent', 'componentChanged']
      }));
      this.componentId = config.componentId;
      this.variant = config.variant;
      this.componentPropertiesForm = config.componentPropertiesForm;
      this.isReadOnly = config.isReadOnly;
      this.componentMessageBus = config.componentMessageBus;

      this.addEvents('enableDeleteComponent', 'componentChanged', 'renderComponent', 'variantsDeleted', 'visibleHeightChanged');
    },

    load: function () {
      return this.componentPropertiesForm.load();
    },

    /**
     * The visible height is the height that should be visible to the user.
     * Subclasses should override this method to calculate their visible height and
     * fire a 'visibleHeightChanged' event whenever their visible height has changed.
     */
    syncVisibleHeight: function () {
      // empty base method
    },

    /**
     * Marks this editor as 'dirty' or not.
     * @param isDirty whether to mark this editor as dirty.
     */
    markDirty: function (isDirty) {
      this.componentPropertiesForm.markDirty(isDirty === undefined ? true : isDirty);
    },

    /**
     * Save the form containing in the editor
     */
    save: function () {
      var def = $.Deferred(),
        form = this.componentPropertiesForm;

      form.submitForm(function (savedVariantId) {
        def.resolve({
          oldId: form.variant.id,
          newId: savedVariantId
        });
      }, function () {
        def.reject(form);
      });
      return def.promise();
    },

    onAfterSave: function() {
      return $.Deferred().resolve().promise();
    },

    getInitialActiveVariantId: function () {
      return $.Deferred().resolve().promise();
    },

    enableDeleteComponent: function (enabled) {
      this.fireEvent('enableDeleteComponent', enabled);
    },

    renderComponent: function() {
      this.fireEvent('renderComponent');
    },

    notifyComponentChanged: function () {
      this.fireEvent('componentChanged', this.componentId);
    }

  });

}(jQuery));
