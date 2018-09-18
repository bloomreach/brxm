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

class EditComponentMainCtrl {
  constructor(
    $log,
    $q,
    CmsService,
    ComponentEditor,
    EditComponentService,
    HippoIframeService,
    PageStructureService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.CmsService = CmsService;
    this.ComponentEditor = ComponentEditor;
    this.EditComponentService = EditComponentService;
    this.HippoIframeService = HippoIframeService;
    this.PageStructureService = PageStructureService;
  }

  discard() {
    console.log('TODO: implement EditComponentMainCtrl.discard');
  }

  save() {
    this.HippoIframeService.reload();
    this.CmsService.reportUsageStatistic('CMSChannelsSaveComponent');
  }

  deleteComponent() {
    console.log('TODO: implement EditComponentMainCtrl.deleteComponent');
  }

  isSaveAllowed() {
    return false;
  }

  uiCanExit() {
    return this._confirmExit()
      .then(() => this.ComponentEditor.close())
      .catch((e) => {
        if (e) {
          this.$log.error('An error occurred while closing the ComponentEditor ->', e);
        } else {
          // the user has cancelled the confirmation dialog
        }
        return this.$q.reject();
      });
  }

  _confirmExit() {
    const message = this.EditComponentService.getTransitionMessageKey();
    return this.ComponentEditor.confirmSaveOrDiscardChanges(message)
      .then(() => this.PageStructureService.renderComponent(this.ComponentEditor.component.id));
  }
}

export default EditComponentMainCtrl;
