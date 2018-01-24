/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

const PICKER_CALLBACK_ID = 'document-location-callback-id';
const MAX_DEPTH = 3;

class DocumentLocationFieldController {
  constructor(
    CmsService,
    Step1Service,
    FeedbackService,
  ) {
    'ngInject';

    this.CmsService = CmsService;
    this.Step1Service = Step1Service;
    this.FeedbackService = FeedbackService;

    this.rootPathDepth = 0;
    this.documentLocationLabel = '';
    this.documentLocation = '';
  }

  $onInit() {
    if (!this.rootPath) {
      throw new Error('The rootPath option can not be empty');
    }

    if (!this.rootPath.startsWith('/')) {
      throw new Error(`The rootPath option can only be an absolute path: ${this.rootPath}`);
    }

    if (this.defaultPath && this.defaultPath.startsWith('/')) {
      throw new Error(`The defaultPath option can only be a relative path: ${this.defaultPath}`);
    }
    this.rootPathDepth = (this.rootPath.match(/\//g) || []).length;

    let documentLocationPath = this.rootPath;
    if (this.defaultPath) {
      documentLocationPath += `/${this.defaultPath}`;
    }

    this.pickerConfig = {
      configuration: 'cms-pickers/folders',
      rootPath: this.rootPath,
      selectableNodeTypes: ['hippostd:folder'],
    };

    this.setDocumentLocation(documentLocationPath);
  }

  onPathPicked(callbackId, path) {
    if (callbackId === PICKER_CALLBACK_ID) {
      if (!path.startsWith('/')) {
        path = `/${path}`;
      }
      if (!path.startsWith(this.rootPath)) {
        this.FeedbackService.showError('ERROR_DOCUMENT_LOCATION_NOT_ALLOWED', { root: this.rootPath, path });
      } else {
        this.setDocumentLocation(path);
      }
    }
  }

  setDocumentLocation(documentLocation) {
    this.Step1Service.getFolders(documentLocation)
      .then(folders => this.onLoadFolders(folders));
  }

  /**
   * Store the path of the last folder as documentLocation and calculate the corresponding documentLocationLabel.
   * @param folders The array of folders returned by the backend
   */
  onLoadFolders(folders) {
    if (folders.length > 0) {
      const lastFolder = folders[folders.length - 1];
      this.documentLocationLabel = this.calculateDocumentLocationLabel(folders);
      this.documentLocation = lastFolder.path;
      this.locale = lastFolder.locale;
      this.defaultPath = folders
        .filter((folder, index) => index >= this.rootPathDepth)
        .map(folder => folder.name)
        .join('/');
    }
  }

  openPicker() {
    this.CmsService.subscribeOnce('path-picked', this.onPathPicked, this);
    this.CmsService.publish('show-path-picker', PICKER_CALLBACK_ID, this.documentLocation, this.pickerConfig);
  }

  /**
   * Calculate the document location label from the given array of folders, using the folder's
   * displayName. It always shows a maximum of three folders in total, and only the last folder
   * of the rootPath if the path after the rootPath is shorter than the maximum.
   */
  calculateDocumentLocationLabel(folders) {
    const defaultPathDepth = folders.length - this.rootPathDepth;
    const start = defaultPathDepth >= MAX_DEPTH ?
      folders.length - MAX_DEPTH : this.rootPathDepth - 1;

    return folders
      .filter((folder, index) => index >= start)
      .map(folder => folder.displayName)
      .join('/');
  }
}

export default DocumentLocationFieldController;
