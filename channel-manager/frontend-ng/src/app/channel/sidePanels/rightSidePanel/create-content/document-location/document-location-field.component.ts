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

import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import FeedbackService from '../../../../../services/feedback.service.js';
import ChannelService from '../../../../channel.service.js';
import CmsService from '../../../../../services/cms.service';
import { CreateContentService } from '../create-content.service';
import { Folder } from '../create-content.types';
import { Channel } from '../../../../../shared/interfaces/channel.types';

@Component({
  selector: 'hippo-document-location-field',
  templateUrl: 'document-location-field.html'
})
export class DocumentLocationFieldComponent implements OnInit {
  private static readonly MAX_DEPTH = 3;

  @Input() rootPath: string;
  @Input() defaultPath: string;
  @Output() changeLocale: EventEmitter<string> = new EventEmitter();
  @ViewChild('form') form: HTMLFormElement;

  public rootPathDepth: number;
  public documentLocationLabel: string;
  public documentLocation: string;

  constructor(
    private channelService: ChannelService,
    private cmsService: CmsService,
    private createContentService: CreateContentService,
    private feedbackService: FeedbackService,
  ) { }

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
  private static parseRootPath (rootPath: string, channelRootPath: string): string {
    if (!rootPath) {
      return channelRootPath;
    }

    if (rootPath.endsWith('/')) {
      rootPath = rootPath.substring(0, rootPath.length - 1);
    }

    if (!rootPath.startsWith('/')) {
      rootPath = channelRootPath + '/' + rootPath;
    }
    return rootPath;
  }

  ngOnInit() {
    if (this.defaultPath && this.defaultPath.startsWith('/')) {
      throw new Error('The defaultPath option can only be a relative path');
    }

    const channel: Channel = this.channelService.getChannel();
    this.rootPath = DocumentLocationFieldComponent.parseRootPath(this.rootPath, channel.contentRoot);
    this.rootPathDepth = (this.rootPath.match(/\//g) || []).length;

    let documentLocationPath = this.rootPath;
    if (this.defaultPath) {
      documentLocationPath += '/' + this.defaultPath;
    }

    this.setDocumentLocation(documentLocationPath);
  }

  private setDocumentLocation(documentLocation: string) {
    this.createContentService
      .getFolders(documentLocation)
      .subscribe(
        (folders) => this.onLoadFolders(folders),
        (error) => this.onError(error, 'Unknown error loading folders')
      );
  }

  /**
   * Store the path of the last folder as documentLocation and calculate the corresponding documentLocationLabel.
   * @param folders The array of folders returned by the backend
   */
  private onLoadFolders(folders: Array<Folder>): void {
    if (folders.length === 0) {
      return;
    }

    const lastFolder = folders[folders.length - 1];
    this.documentLocationLabel = this.calculateDocumentLocationLabel(folders);
    this.documentLocation = lastFolder.path;
    this.changeLocale.emit(lastFolder.locale);
    this.defaultPath = folders
      .filter((folder, index) => index >= this.rootPathDepth)
      .map(folder => folder.name)
      .join('/');
  }

  private onError(error, unknownErrorMessage) {
    if (error.data && error.data.reason) {
      const errorKey = `ERROR_${error.data.reason}`;
      this.feedbackService.showError(errorKey, error.data.params);
    } else {
      console.error(unknownErrorMessage, error);
    }
  }

  /**
   * Calculate the document location label from the given array of folders, using the folder's
   * displayName. It always shows a maximum of three folders in total, and only the last folder
   * of the rootPath if the path after the rootPath is shorter than the maximum.
   */
  private calculateDocumentLocationLabel(folders: Array<Folder>): string {
    const defaultPathDepth = folders.length - this.rootPathDepth;
    const start = defaultPathDepth >= DocumentLocationFieldComponent.MAX_DEPTH ?
      folders.length - DocumentLocationFieldComponent.MAX_DEPTH : this.rootPathDepth - 1;

    return folders
      .filter((folder, index) => index >= start)
      .map(folder => folder.displayName)
      .join('/');
  }
}
