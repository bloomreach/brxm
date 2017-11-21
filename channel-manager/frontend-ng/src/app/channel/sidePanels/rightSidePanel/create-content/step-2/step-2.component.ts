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

import { Component, OnInit, EventEmitter, Output, ViewChild, HostListener, ElementRef, Input } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material';
import { TranslateService } from '@ngx-translate/core';
import './step-2.scss';

import ContentService from '../../../../../services/content.service';
import { CreateContentService } from '../create-content.service';
import DialogService from '../../../../../services/dialog.service';
import FieldService from '../../fields/field.service';
import { NameUrlFieldsComponent } from '../name-url-fields/name-url-fields.component';
import { NameUrlFieldsDialogComponent } from './name-url-fields-dialog/name-url-fields-dialog';
import { Document, DocumentTypeInfo } from '../create-content.types';
import FeedbackService from '../../../../../services/feedback.service.js';

@Component({
  selector: 'hippo-create-content-step-2',
  templateUrl: './step-2.html',
  entryComponents: [NameUrlFieldsDialogComponent]
})
export class CreateContentStep2Component implements OnInit {
  private documentUrl: string;
  private documentLocale: string;
  doc: Document;
  docType: any;
  editing: boolean;
  loading: boolean;
  isFullWidth: boolean;
  feedback: any;
  title = 'Create new content';
  defaultTitle = 'Create new content';
  documentId: string;
  @Input() options: any;
  @Output() onSave: EventEmitter<any> = new EventEmitter();
  @Output() onClose: EventEmitter<any> = new EventEmitter();
  @Output() onBeforeStateChange: EventEmitter<any> = new EventEmitter();
  @Output() onFullWidth: EventEmitter<any> = new EventEmitter();
  @ViewChild(NameUrlFieldsComponent) nameUrlFields: NameUrlFieldsComponent;
  @ViewChild('step2') step2: ElementRef;

  // Prevent the default closing action bound to the escape key by Angular Material.
  // We should show the "unsaved changes" dialog first.
  @HostListener('keypress', ['$event']) closeOnEsc(e) {
    if (e.which === 27) {
      e.stopImmediatePropagation();
      this.close();
    }
  }

  constructor(private createContentService: CreateContentService,
              private contentService: ContentService,
              private fieldService: FieldService,
              private dialogService: DialogService,
              private feedbackService: FeedbackService,
              private translate: TranslateService,
              public dialog: MatDialog) {}

  ngOnInit() {
    this.loadNewDocument();
    this.resetBeforeStateChange();
  }

  loadNewDocument(): Promise<Document> {
    const doc = this.createContentService.getDocument();
    this.documentId = doc.id;
    this.fieldService.setDocumentId(doc.id);
    this.loading = true;
    return this.contentService.getDocumentType(doc.info.type.id)
      .then(docType => {
        this.onLoadSuccess(doc, docType);
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
  }

  openEditNameUrlDialog() {
    const dialog = this.dialog.open(NameUrlFieldsDialogComponent, {
      height: '280px',
      width: '600px',
      data: {
        title: this.translate.instant('CHANGE_DOCUMENT_NAME'),
        name: this.doc.displayName,
        url: this.documentUrl,
        locale: this.documentLocale
      }
    });
    dialog.afterClosed().subscribe((result: { name: string, url: string }) => this.onEditNameUrlDialogClose(result));
    return dialog;
  }

  private async onEditNameUrlDialogClose(data: { name: string, url: string }) {
    try {
      const result = await this.createContentService.setDraftNameUrl(this.doc.id, data);
      this.doc.displayName = result.displayName;
      this.documentUrl = result.urlName;
    } catch (error) {
        const errorKey = this.translate.instant(`ERROR_${error.data.reason}`)
        this.feedbackService.showError(errorKey, error.data.params);
    }
  }

  private onLoadSuccess(doc: Document, docTypeInfo: DocumentTypeInfo) {
    this.doc = doc;
    this.docType = docTypeInfo;
    this.title = this.translate.instant('CREATE_NEW_DOCUMENT_TYPE', { documentType: docTypeInfo.displayName });

    this.doc.displayName = this.options.name;
    this.documentUrl = this.options.url;
    this.documentLocale = this.options.locale;
  }

  setFullWidth(state: boolean) {
    this.isFullWidth = state;
    this.onFullWidth.emit(state);
  }

  saveDocument() {
    this.contentService.saveDraft(this.doc)
      .then(() => {
        this.onBeforeStateChange.emit(() => Promise.resolve());
        this.onSave.emit(this.doc.id);
      });
  }

  isDocumentDirty(): boolean {
    return (this.doc && this.doc.info && this.doc.info.dirty);
  }

  close(): Promise<void> {
    return this.discardAndClose()
      .then(() => {
        this.resetState();
        this.onClose.emit();
      });
  }

  discardAndClose(): Promise<void> {
    return this.confirmDiscardChanges()
      .then(() => {
        // TODO: Delete document
      });
  }

  private confirmDiscardChanges(): Promise<void> {
    const messageParams = {
      // documentName: this.doc.displayName,
    };

    const confirm = this.dialogService.confirm()
      .title(this.translate.instant('DISCARD_DOCUMENT', messageParams))
      .textContent(this.translate.instant('CONFIRM_DISCARD_NEW_DOCUMENT', messageParams))
      .ok(this.translate.instant('DISCARD'))
      .cancel(this.translate.instant('CANCEL'));

    return this.dialogService.show(confirm);
  }

  private resetState() {
    delete this.doc;
    delete this.documentId;
    delete this.docType;
    delete this.feedback;
    this.title = this.defaultTitle;
    this.onBeforeStateChange.emit(() => Promise.resolve());
  }

  private resetBeforeStateChange() {
    this.onBeforeStateChange.emit(() => this.discardAndClose());
  }
}
