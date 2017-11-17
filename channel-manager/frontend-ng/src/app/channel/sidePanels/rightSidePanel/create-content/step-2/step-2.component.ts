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

import { Component, OnInit, EventEmitter, Output, ViewChild, HostListener, ElementRef } from '@angular/core';
import { MdDialog, MdDialogRef } from '@angular/material';
import './step-2.scss';
import { TranslateService } from '@ngx-translate/core';

import { CreateContentService } from '../create-content.service';
import DialogService from '../../../../../services/dialog.service';
import ContentService from '../../../../../services/content.service';
import FieldService from '../../fields/field.service';
import { NameUrlFieldsComponent } from '../name-url-fields/name-url-fields.component';
import { NameUrlFieldsDialogComponent } from './name-url-fields-dialog/name-url-fields-dialog';
import { Document } from '../create-content.types';

@Component({
  selector: 'hippo-create-content-step-2',
  templateUrl: './step-2.html',
  entryComponents: [NameUrlFieldsDialogComponent]
})
export class CreateContentStep2Component implements OnInit {
  doc: Document;
  docType: any;
  editing: boolean;
  loading: boolean;
  isFullWidth: boolean;
  feedback: any;
  title = 'Create new content';
  defaultTitle = 'Create new content';
  documentId: string;
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
              private translate: TranslateService,
              private dialog: MdDialog) {}

  ngOnInit() {
    this.loadNewDocument();
    this.resetBeforeStateChange();
  }

  loadNewDocument() {
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

  private openEditNameUrlDialog(): MdDialogRef<NameUrlFieldsDialogComponent> {
    return this.dialog.open(NameUrlFieldsDialogComponent, {
      height: '280px',
      width: '600px',
      data: {
        title: this.translate.instant('CHANGE_DOCUMENT_NAME'),
        name: this.doc.displayName,
        url: '',
      }
    });
  }

  private submitEditNameUrl(nameUrlObj) {
    this.doc.displayName = nameUrlObj.name;
  }

  editNameUrl() {
    this.openEditNameUrlDialog().afterClosed().subscribe(
      nameUrlObj => {
        if (nameUrlObj) {
          this.submitEditNameUrl(nameUrlObj);
        }
      }
    );
  }

  private onLoadSuccess(doc, docType) {
    this.doc = doc;
    this.docType = docType;

    this.title = this.translate.instant('CREATE_NEW_DOCUMENT_TYPE', { documentType: docType.displayName });
  }

  setFullWidth(state) {
    this.isFullWidth = state;
    this.onFullWidth.emit(state);
  }

  saveDocument() {
    this.onSave.emit({ mode: 'edit', options: this.documentId });
  }

  isDocumentDirty() {
    return (this.doc && this.doc.info && this.doc.info.dirty);
  }

  close() {
    return this.discardAndClose()
      .then(() => {
        this.resetState();
        this.onClose.emit();
      });
  }

  discardAndClose() {
    return this.confirmDiscardChanges()
      .then(() => this.createContentService.deleteDraft(this.documentId));
  }

  private confirmDiscardChanges() {
    const messageParams = {
      documentName: this.doc.displayName,
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
