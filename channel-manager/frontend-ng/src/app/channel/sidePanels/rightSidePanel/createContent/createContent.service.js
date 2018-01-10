/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

class CreateContentService {
  constructor($state, $transitions, $translate, ContentEditor, RightSidePanelService) {
    'ngInject';

    this.$state = $state;
    this.$translate = $translate;
    this.ContentEditor = ContentEditor;
    this.RightSidePanelService = RightSidePanelService;

    $transitions.onEnter(
      { entering: '**.create-content-step-1' },
      () => this._init(),
    );
  }

  start() {
    this.$state.go('hippo-cm.channel.create-content-step-1');
  }

  stop() {
    this.$state.go('^');
  }

  _init() {
    // TODO: translate title
    this.RightSidePanelService.setTitle('Create new content');
  }
}

export default CreateContentService;
