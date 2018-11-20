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
    OverlayService,
    PageMetaDataService,
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
    this.OverlayService = OverlayService;
    this.PageMetaDataService = PageMetaDataService;
    this.RightSidePanelService = RightSidePanelService;

    $transitions.onEnter(
      { entering: '**.edit-component' },
      transition => this._loadComponent(transition.params().properties),
    );

    CmsService.subscribe('hide-component-properties', () => this.MaskService.unmask());
  }

  startEditing(componentElement) {
    if (!componentElement) {
      this.$log.warn('Problem opening the component properties editor: no component provided.');
      return;
    }

    const channel = this.ChannelService.getChannel();
    const properties = {
      channel,
      // TODO: move this logic to ComponentEditorService upon `relevancePresent` flag removal
      component: {
        id: componentElement.getId(),
        label: componentElement.getLabel(),
        lastModified: componentElement.getLastModified(),
        variant: componentElement.getRenderVariant(),
      },
      container: {
        isDisabled: componentElement.container.isDisabled(),
        isInherited: componentElement.container.isInherited(),
        id: componentElement.container.getId(),
      },
      page: this.PageMetaDataService.get(),
    };

    if (this.ConfigService.relevancePresent) {
      this.MaskService.mask();
      this.CmsService.publish('show-component-properties', properties);
    } else {
      this.$state.go('hippo-cm.channel.edit-component', { properties });
    }
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

  _loadComponent(properties) {
    this._showDefaultTitle();
    this.RightSidePanelService.startLoading();
    this.ComponentEditor.open(properties)
      .then(() => {
        this._showComponentTitle();
        this.RightSidePanelService.stopLoading();
        this.OverlayService.sync();
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
