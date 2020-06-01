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

class DocumentLocationFieldController {
  constructor(
    $element,
    ChannelService,
    CmsService,
    FeedbackService,
    PickerService,
    Step1Service,
  ) {
    'ngInject';

    this.$element = $element;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.FeedbackService = FeedbackService;
    this.PickerService = PickerService;
    this.Step1Service = Step1Service;
  }

  $onInit() {
    if (this.rootPath && !this.rootPath.startsWith('/')) {
      throw new Error(`The rootPath option can only be an absolute path: ${this.rootPath}`);
    }

    if (this.defaultPath && this.defaultPath.startsWith('/')) {
      throw new Error(`The defaultPath option can only be a relative path: ${this.defaultPath}`);
    }

    if (!this.rootPath && this.defaultPath) {
      this.rootPath = this.ChannelService.getChannel().contentRoot;
    }

    this.initialPickerPath = this.rootPath || this.ChannelService.getChannel().contentRoot;

    this.pickerPath = '/';
    this.pickerConfig = {
      configuration: 'cms-pickers/documents-folders-only',
      rootPath: this.initialPickerPath,
      selectableNodeTypes: ['hippostd:folder'],
    };

    if (this.rootPath) {
      const path = this.rootPath + (this.defaultPath ? `/${this.defaultPath}` : '');
      this.setPath(path);
    }
  }

  setPath(path) {
    this.Step1Service.getFolders(path)
      .then(folders => this.onLoadFolders(folders));
  }

  /**
   * Store the path and locale of the last folder and calculate the corresponding pathLabel,
   * defaultPath and pickerPath.
   * @param folders The array of folders returned by the backend
   */
  onLoadFolders(folders = []) {
    if (folders.length > 0) {
      this.folders = folders;

      const lastFolder = folders[folders.length - 1];
      this.locale = lastFolder.locale;
      this.path = lastFolder.path;
      this.pathLabel = `/${folders.map(f => f.displayName).join('/')}`;
      this.defaultPath = lastFolder.path.substring(this.rootPath.length + 1);

      const existing = folders.filter(folder => folder.exists);
      this.pickerPath = existing.length === 0 ? '/' : existing[existing.length - 1].path;
    }
  }

  openPicker() {
    this.PickerService.pickPath(this.pickerConfig, this.pickerPath)
      .then(({ path }) => this.onPathPicked(path))
      .catch(() => this.onPathCanceled());
    this.CmsService.reportUsageStatistic('DocumentLocationPicker (create content panel)');
  }

  onPathPicked(path) {
    if (!path.startsWith('/')) {
      path = `/${path}`;
    }
    if (!path.startsWith(this.initialPickerPath)) {
      this.FeedbackService.showError('ERROR_DOCUMENT_LOCATION_NOT_ALLOWED', { root: this.initialPickerPath, path });
    } else {
      if (!this.rootPath) {
        this.rootPath = path;
      }
      this.setPath(path);
    }
  }

  onPathCanceled() {
    // focus this field again so keypresses will reach Angular Material instead of the parent window
    this.$element.find('.input-overlay').focus();
  }
}

export default DocumentLocationFieldController;
