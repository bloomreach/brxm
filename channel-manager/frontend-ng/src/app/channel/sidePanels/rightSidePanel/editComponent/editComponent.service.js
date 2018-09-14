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
    $state,
    $transitions,
    $translate,
    ChannelService,
    CmsService,
    ConfigService,
    ComponentEditor,
    MaskService,
    PageMetaDataService,
    PageStructureService,
    RightSidePanelService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$state = $state;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ComponentEditor = ComponentEditor;
    this.ConfigService = ConfigService;
    this.MaskService = MaskService;
    this.PageMetaDataService = PageMetaDataService;
    this.PageStructureService = PageStructureService;
    this.RightSidePanelService = RightSidePanelService;

    $transitions.onEnter(
      { entering: '**.edit-component' },
      transition => this._loadComponent(transition.params().properties),
    );
    $transitions.onBefore(
      { from: '**.edit-component', to: 'hippo-cm' },
      () => this._onCloseChannel(),
    );

    CmsService.subscribe('kill-editor', (documentId) => {
      this._stopEditingComponent(documentId);
    });

    CmsService.subscribe('hide-component-properties', () => this.MaskService.unmask());
  }

  _stopEditingComponent(componentId) {
    console.log(`TODO: implement EditComponentService._stopEditingComponent -- ${componentId}`);
    if (this.$state.$current.name === 'hippo-cm.channel.edit-component'
    /* && this.ContentEditor.getDocumentId() === documentId */) {
      // this.ContentEditor.kill();
      this.stopEditing();
    }
  }

  startEditing(componentElement) {
    if (!componentElement) {
      this.$log.warn('Problem opening the component properties editor: no component provided.');
      return;
    }

    const channel = this.ChannelService.getChannel();
    const properties = {
      channel: {
        contextPath: channel.contextPath,
        mountId: channel.mountId,
      },
      component: {
        id: componentElement.getId(),
        label: componentElement.getLabel(),
        lastModified: componentElement.getLastModified(),
        variant: componentElement.getRenderVariant(),
      },
      container: {
        isDisabled: componentElement.container.isDisabled(),
        isInherited: componentElement.container.isInherited(),
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
    this.$state.go('^');
  }

  _loadComponent(properties) {
    this._showDefaultTitle();
    this.RightSidePanelService.startLoading();
    this.ComponentEditor.open(properties)
      .then(() => {
        this.componentId = properties.component.id;
        this._showComponentTitle();
        this.RightSidePanelService.stopLoading();
      })
      .catch(e => console.error('Failed to load component', properties, e));
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

  _onCloseChannel() {
    console.log('TODO: implement EditComponentService._onCloseChannel');
    // return this.ContentEditor.confirmSaveOrDiscardChanges('SAVE_CHANGES_ON_CLOSE_CHANNEL')
    //   .then(() => this.ContentEditor.discardChanges())
    //   .then(() => this.ContentEditor.close());
  }
}

export default EditComponentService;
