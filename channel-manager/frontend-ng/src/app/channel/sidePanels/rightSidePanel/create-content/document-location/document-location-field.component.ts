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

import { Component, Input, OnInit, ViewChild } from '@angular/core';
import FeedbackService from '../../../../../services/feedback.service.js';
import ChannelService from '../../../../channel.service.js';
import { CreateContentService } from '../create-content.service';
import { Folder } from '../create-content.types';
import { Observable } from 'rxjs/Observable';

const MAX_DEPTH = 3;

@Component({
  selector: 'hippo-document-location-field',
  templateUrl: 'document-location-field.html'
})
export class DocumentLocationFieldComponent implements OnInit {
  @Input() rootPath: string;
  @Input() defaultPath: string;
  @ViewChild('form') form: HTMLFormElement;

  public rootPathDepth: number;
  public documentLocationLabel: string;
  public documentLocation: string;

  constructor(
    private channelService: ChannelService,
    private createContentService: CreateContentService,
    private feedbackService: FeedbackService,
  ) { }

  ngOnInit() {
    const channel = this.channelService.getChannel();

    if (this.rootPath) {
      if (this.rootPath.endsWith('/')) {
        this.rootPath = this.rootPath.substring(0, this.rootPath.length - 1);
      }
      if (!this.rootPath.startsWith('/')) {
        this.rootPath = channel.contentRoot + '/' + this.rootPath;
      }
    } else {
      this.rootPath = channel.contentRoot;
    }
    this.rootPathDepth = (this.rootPath.match(/\//g) || []).length;

    if (this.defaultPath) {
      if (this.defaultPath.startsWith('/')) {
        throw new Error('The defaultPath option can only be a relative path');
      }
      this.defaultPath = '/' + this.defaultPath;
    } else {
      this.defaultPath = '';
    }

    this.setDocumentLocation(this.rootPath + this.defaultPath);
  }

  private setDocumentLocation(documentLocation: string) {
    this.createContentService
      .getFolders(documentLocation)
      .subscribe(
      (folders) => this.onLoadFolders(folders),
      (error) => this.onError(error, 'Unknown error loading folders')
      );
  }

  private onLoadFolders(folders: Array<Folder>): void {
    if (folders.length === 0) {
      return;
    }

    const defaultPathDepth = folders.length - this.rootPathDepth;
    const start = defaultPathDepth >= MAX_DEPTH ? folders.length - MAX_DEPTH : this.rootPathDepth - 1;

    this.documentLocation = folders[folders.length - 1].path;
    this.documentLocationLabel = folders
      .filter((folder, index) => index >= start)
      .map(folder => folder.displayName)
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
}
