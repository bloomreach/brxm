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

class ListingCtrl {
  selectDocument(document) {
    this.selectedDocument = document;
  }

  _getDocumentStatusIcon(item) {
    const docState = item.state;
    let iconPath;

    switch (docState) {
      case 'new':
        iconPath = '/cms/angular/hippo-cm/images/document-status-new.svg';
        break;
      case 'changed':
        iconPath = '/cms/angular/hippo-cm/images/document-status-changed.svg';
        break;
      case 'live':
        iconPath = '/cms/angular/hippo-cm/images/document-status-live.svg';
        break;
      default:
        iconPath = '/cms/angular/hippo-cm/images/document.svg';
    }

    return iconPath;
  }

  getItemIcon(item) {
    let iconPath;

    if (item.type === 'folder') {
      iconPath = '/cms/angular/hippo-cm/images/folder-closed.svg';
    } else {
      iconPath = this._getDocumentStatusIcon(item);
    }

    return iconPath;
  }
}

export default ListingCtrl;
