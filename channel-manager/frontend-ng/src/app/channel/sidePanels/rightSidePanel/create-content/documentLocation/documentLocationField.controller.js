/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
  constructor(ChannelService, CreateContentService, FeedbackService) {
    'ngInject';

    this.ChannelService = ChannelService;
    this.CreateContentService = CreateContentService;
    this.FeedbackService = FeedbackService;

    this.MAX_DEPTH = 3;
    this.rootPathDepth = 0;
    this.documentLocationLabel = '';
    this.documentLocation = '';
  }


  /**
   * Parse the rootPath input value;
   * - use channelRootPath if rootPath is empty
   * - use as is if rootPath is absolute
   * - concatenate with channelRootPath if rootPath is relative
   * - make sure it does not end with a slash
   *
   * @param rootPath the component's rootPath
   * @param channelRootPath the channel's rootPath
   */
  static parseRootPath(rootPath, channelRootPath) {
    if (!rootPath) {
      return channelRootPath;
    }

    if (rootPath.endsWith('/')) {
      rootPath = rootPath.substring(0, rootPath.length - 1);
    }

    if (!rootPath.startsWith('/')) {
      rootPath = `${channelRootPath}/${rootPath}`;
    }
    return rootPath;
  }

  $onInit() {
    if (this.defaultPath && this.defaultPath.startsWith('/')) {
      throw new Error('The defaultPath option can only be a relative path');
    }

    const channel = this.ChannelService.getChannel();
    this.rootPath = DocumentLocationFieldController.parseRootPath(this.rootPath, channel.contentRoot);
    this.rootPathDepth = (this.rootPath.match(/\//g) || []).length;

    let documentLocationPath = this.rootPath;
    if (this.defaultPath) {
      documentLocationPath += `/${this.defaultPath}`;
    }

    this.setDocumentLocation(documentLocationPath);
  }

  setDocumentLocation(documentLocation) {
    this.CreateContentService.getFolders(documentLocation).then(
      folders => this.onLoadFolders(folders),
      error => this.onError(error, 'Unknown error loading folders'),
    );
  }

  /**
   * Store the path of the last folder as documentLocation and calculate the corresponding documentLocationLabel.
   * @param folders The array of folders returned by the backend
   */
  onLoadFolders(folders) {
    if (folders.length === 0) {
      return;
    }

    const lastFolder = folders[folders.length - 1];
    this.documentLocationLabel = this.calculateDocumentLocationLabel(folders);
    this.documentLocation = lastFolder.path;
    this.changeLocale({ locale: lastFolder.locale });
    this.defaultPath = folders
      .filter((folder, index) => index >= this.rootPathDepth)
      .map(folder => folder.name)
      .join('/');
  }

  onError(error, unknownErrorMessage) {
    if (error.data && error.data.reason) {
      const errorKey = `ERROR_${error.data.reason}`;
      this.FeedbackService.showError(errorKey, error.data.params);
    } else {
      console.error(unknownErrorMessage, error);
    }
  }

  /**
   * Calculate the document location label from the given array of folders, using the folder's
   * displayName. It always shows a maximum of three folders in total, and only the last folder
   * of the rootPath if the path after the rootPath is shorter than the maximum.
   */
  calculateDocumentLocationLabel(folders) {
    const defaultPathDepth = folders.length - this.rootPathDepth;
    const start = defaultPathDepth >= this.MAX_DEPTH ?
      folders.length - this.MAX_DEPTH : this.rootPathDepth - 1;

    return folders
      .filter((folder, index) => index >= start)
      .map(folder => folder.displayName)
      .join('/');
  }
}

export default DocumentLocationFieldController;
