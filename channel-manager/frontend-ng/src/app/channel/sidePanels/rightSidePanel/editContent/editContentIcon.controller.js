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

const PUBLICATION_STATE_ICON_NAMES = {
  new: 'mdi-minus-circle',
  live: 'mdi-check-circle',
  changed: 'mdi-alert',
  unknown: '',
};

const PUBLICATION_STATE_ICON_TOOLTIPS = {
  new: 'DOCUMENT_NEW_TOOLTIP',
  live: 'DOCUMENT_LIVE_TOOLTIP',
  changed: 'DOCUMENT_CHANGED_TOOLTIP',
  unknown: '',
};

class EditContentIconCtrl {
  constructor($q, ContentEditor) {
    'ngInject';

    this.$q = $q;
    this.ContentEditor = ContentEditor;
  }

  getPublicationIconName() {
    return this._getPublicationStateValue(PUBLICATION_STATE_ICON_NAMES, this.ContentEditor.getPublicationState());
  }

  getPublicationIconTooltip() {
    return this._getPublicationStateValue(PUBLICATION_STATE_ICON_TOOLTIPS, this.ContentEditor.getPublicationState());
  }

  _getPublicationStateValue(map, publicationState) {
    return map[publicationState] || map.unknown;
  }
}

export default EditContentIconCtrl;
