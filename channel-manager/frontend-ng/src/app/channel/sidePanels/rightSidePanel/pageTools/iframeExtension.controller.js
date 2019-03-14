/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class IframeExtensionCtrl {
  constructor(
    $element,
    $log,
    $rootScope,
    ChannelService,
    ConfigService,
    DomService,
    ExtensionService,
    HippoIframeService,
    OpenUIService,
    PathService,
  ) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.$rootScope = $rootScope;
    this.ChannelService = ChannelService;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.ExtensionService = ExtensionService;
    this.HippoIframeService = HippoIframeService;
    this.OpenUIService = OpenUIService;
    this.PathService = PathService;
  }

  $onInit() {
    this._initExtension();

    this._unsubscribeFromPublish = this.$rootScope.$on(
      'channel:changes:publish',
      () => this.child.emitEvent('channel.changes.publish'),
    );
    this._unsubscribeFromDiscard = this.$rootScope.$on(
      'channel:changes:discard',
      () => this.child.emitEvent('channel.changes.discard'),
    );
  }

  $onDestroy() {
    if (this._unsubscribeFromPublish) {
      this._unsubscribeFromPublish();
    }
    if (this._unsubscribeFromDiscard) {
      this._unsubscribeFromDiscard();
    }
  }

  _initExtension() {
    this.extension = this.ExtensionService.getExtension(this.extensionId);

    this.OpenUIService.connect({
      url: this.ExtensionService.getExtensionUrl(this.extension),
      appendTo: this.$element[0],
      methods: {
        getProperties: () => this._getProperties(),
        getPage: () => this.context,
        refreshChannel: () => this.ChannelService.reload(),
        refreshPage: () => this.HippoIframeService.reload(),
      },
    })
      .then((child) => { this.child = child; })
      .catch((error) => {
        this.$log.warn(`Extension '${this.extension.displayName}' failed to connect with the client library.`, error);
      });
  }

  _getProperties() {
    return {
      baseUrl: this.ConfigService.getCmsOrigin() + this.ConfigService.getCmsContextPath(),
      extension: {
        config: this.extension.config,
      },
      locale: this.ConfigService.locale,
      timeZone: this.ConfigService.timeZone,
      user: {
        id: this.ConfigService.cmsUser,
        firstName: this.ConfigService.cmsUserFirstName,
        lastName: this.ConfigService.cmsUserLastName,
        displayName: this.ConfigService.cmsUserDisplayName,
      },
      version: this.ConfigService.cmsVersion,
    };
  }

  $onChanges(params) {
    const changedContext = params.context;

    if (changedContext) {
      // copy the context so any changes to it won't affect the parent version
      this.context = angular.copy(changedContext.currentValue);

      if (this.child) {
        this.child.emitEvent('channel.page.navigate', this.context);
      }
    }
  }
}

export default IframeExtensionCtrl;
