/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
(function() {

  "use strict";

  Ext.namespace('Hippo.ChannelManager.ChannelEditor');

  /**
   * Retrieves all variants for the component specified by the config property 'componentId'.
   * The REST endpoint for retrieving the global variants is determined by the config property 'variantsUuid'.
   * When 'variantsUuid' is empty or undefined, a single variant with the ID 'hippo-default' will always be returned.
   *
   * The retrieved variants will be cached. Calling 'invalidate()' will invalidate the cache and fire the
   * 'invalidated' event. The next call to 'get()' will then retrieve the component variants again.
   *
   * @type {*}
   */
  Hippo.ChannelManager.ChannelEditor.ComponentVariants = Ext.extend(Ext.util.Observable, {

    variantsFuture: null,

    constructor: function(config) {
      this.variantsUuid = config.variantsUuid;
      this.componentId = config.componentId;
      this.lastModified = config.lastModified;
      this.composerRestMountUrl = config.composerRestMountUrl;
      this.siteContextPath = config.siteContextPath;
      this.locale = config.locale;

      Hippo.ChannelManager.ChannelEditor.ComponentVariants.superclass.constructor.call(this, config);

      this.addEvents('invalidated');
    },

    isMultivariate: function() {
      return !Ext.isEmpty(this.variantsUuid);
    },

    get: function() {
      if (this.variantsFuture === null) {
        this.variantsFuture = this._loadVariants();
      }
      return this.variantsFuture;
    },

    invalidate: function(changedVariantIds, activeVariantId) {
      this.variantsFuture = null;
      this.fireEvent('invalidated', changedVariantIds, activeVariantId);
    },

    cleanup: function() {
      var cleanupFuture = this.isMultivariate() ? this._cleanupVariants() : Hippo.Future.constant();
      return cleanupFuture;
    },

    _loadVariants: function() {
      if (!this.isMultivariate()) {
        return Hippo.Future.constant([{
          id: 'hippo-default',
          name: Hippo.ChannelManager.ChannelEditor.Resources['component-variants-default']
        }]);
      } else {
        var self = this;
        return new Hippo.Future(function(success, fail) {
          if (self.componentId) {
            Ext.Ajax.request({
              url: self.composerRestMountUrl + '/' + self.componentId,
              headers: {
                'Force-Client-Host': 'true',
                'contextPath': self.siteContextPath
              },
              success: function(result) {
                var jsonData = Ext.util.JSON.decode(result.responseText),
                  variantIds = jsonData.data;

                self._loadComponentVariants(variantIds).when(function(variants) {
                  variants.push({
                    id: 'plus',
                    name: Hippo.ChannelManager.ChannelEditor.Resources['component-variants-plus']
                  });
                  success(variants);
                }).otherwise(function(response) {
                  fail(response);
                });
              },
              failure: function(result) {
                fail(result);
              }
            });
          } else {
            success([{
              id: 'hippo-default',
              name: Hippo.ChannelManager.ChannelEditor.Resources['component-variants-default']
            }]);
          }
        });
      }
    },

    _loadComponentVariants: function(variantIds) {
      return new Hippo.Future(function(success, fail) {
        Ext.Ajax.request({
          url: this.composerRestMountUrl + '/' + this.variantsUuid + './componentvariants?locale=' + this.locale,
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Force-Client-Host': 'true',
            'contextPath': this.siteContextPath
          },
          params: Ext.util.JSON.encode(variantIds),
          success: function(result) {
            var jsonData = Ext.util.JSON.decode(result.responseText),
              variants = jsonData.data;
            success(variants);
          },
          failure: function(result) {
            fail(result.response);
          },
          scope: this
        });
      }.createDelegate(this));
    },

    _cleanupVariants: function() {
      return new Hippo.Future(function(success) {
        this.get().when(function(variants) {
          var variantIds = [];
          Ext.each(variants, function(variant) {
            if (variant.id !== 'hippo-default' && variant.id !== 'plus') {
              variantIds.push(variant.id);
            }
          });
          Ext.Ajax.request({
            method: 'POST',
            url: this.composerRestMountUrl + '/' + this.componentId,
            headers: {
              'Force-Client-Host': 'true',
              'Content-Type': 'application/json',
              'lastModifiedTimestamp': this.lastModified,
              'contextPath': this.siteContextPath
            },
            params: Ext.util.JSON.encode(variantIds),
            scope: this,
            success: success,
            failure: success  // ignore failures silently, try cleanup next time
          });
        }.createDelegate(this));
      }.createDelegate(this));
    }
  });

}());
