/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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

class EditComponentService {
  constructor(
    $log,
    $q,
    $state,
    $transitions,
    $translate,
    ChannelService,
    CmsService,
    ComponentEditor,
    ConfigService,
    MaskService,
    PageStructureService,
    ProjectService,
    RightSidePanelService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$state = $state;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ComponentEditor = ComponentEditor;
    this.ConfigService = ConfigService;
    this.MaskService = MaskService;
    this.PageStructureService = PageStructureService;
    this.ProjectService = ProjectService;
    this.RightSidePanelService = RightSidePanelService;

    $transitions.onEnter(
      { entering: '**.edit-component' },
      transition => this._loadComponent(transition.params()),
    );

    CmsService.subscribe('hide-component-properties', () => this.MaskService.unmask());

    ProjectService.beforeChange('editComponent', (projectIdIdentical) => {
      if (!projectIdIdentical) {
        return this.stopEditing();
      }
      return this.$q.resolve();
    });
  }

  startEditing(componentElement, variantId) {
    this.readyForUser = false;
    if (!componentElement) {
      this.$log.warn('Problem opening the component properties editor: no component provided.');
      return;
    }

    this.$state.go('hippo-cm.channel.edit-component', {
      componentId: componentElement.getId(),
      variantId: variantId || componentElement.getRenderVariant(),
    });
  }

  stopEditing() {
    if (this.$state.is('hippo-cm.channel.edit-component')) {
      return this.$state.go('^');
    }
    return this.$q.resolve();
  }

  killEditor() {
    this.ComponentEditor.kill();
    this.stopEditing();
  }

  isReadyForUser() {
    return this.readyForUser;
  }

  _loadComponent({ componentId, variantId }) {
    this._showDefaultTitle();
    this.RightSidePanelService.startLoading();

    return this.ComponentEditor
      .open(componentId, variantId)
      .then(() => {
        this._showComponentTitle();
        this.RightSidePanelService.stopLoading();
        this.readyForUser = true;
      })
      .catch(() => this.stopEditing());
  }

  _showDefaultTitle() {
    this.RightSidePanelService.clearContext();

    const componentLabel = this.$translate.instant('COMPONENT');
    this.RightSidePanelService.setTitle(componentLabel);
  }

  _showComponentTitle() {
    const componentLabel = this.$translate.instant('COMPONENT');
    this.RightSidePanelService.setContext(componentLabel);

    const componentName = this.ComponentEditor.getComponentName();
    this.RightSidePanelService.setTitle(componentName);
  }
}

export default EditComponentService;
