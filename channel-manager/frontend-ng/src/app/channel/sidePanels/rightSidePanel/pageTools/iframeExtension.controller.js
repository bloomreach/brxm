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

export default class IframeExtensionCtrl {
  constructor(
    $element,
    $rootScope,
    ChannelService,
    DomService,
    HippoIframeService,
    OpenUiService,
    PathService,
  ) {
    'ngInject';

    this.$element = $element;
    this.$rootScope = $rootScope;
    this.ChannelService = ChannelService;
    this.DomService = DomService;
    this.HippoIframeService = HippoIframeService;
    this.OpenUiService = OpenUiService;
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

  async _initExtension() {
    this.child = await this.OpenUiService.initialize(this.extensionId, {
      appendTo: this.$element[0],
      methods: {
        getPage: () => this.context,
        refreshChannel: () => this.ChannelService.reload(),
        refreshPage: () => this.HippoIframeService.reload(),
      },
    });
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
