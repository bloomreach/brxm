/*!
 * Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { Component, EventEmitter, HostBinding, Inject, Input, OnChanges, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { UIRouterGlobals } from '@uirouter/core';
import { interval, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { DateService } from '../../../services/date.service';
import { DocumentWorkflowService } from '../../../services/document-workflow.service';
import { Ng1NavappService, NG1_NAVAPP_SERVICE } from '../../../services/ng1/navapp.ng1.service';
import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
import { Version } from '../../models/version.model';
import { VersionsInfo } from '../../models/versions-info.model';
import { VersionsService } from '../../services/versions.service';

@Component({
  selector: 'em-latest-version',
  templateUrl: './latest-version.component.html',
  styleUrls: ['./latest-version.component.scss'],
})
export class LatestVersionComponent implements OnChanges, OnInit, OnDestroy {
  private readonly unsubscribe = new Subject();

  @Input()
  version!: Version;

  @Input()
  versionsInfo!: VersionsInfo;

  @HostBinding('class.selected')
  @Input()
  isSelected!: boolean;

  @Output()
  getVersionsInfo = new EventEmitter<void>();

  @Input()
  actionInProgress!: boolean;
  @Output()
  actionInProgressChange = new EventEmitter<boolean>();

  private readonly documentId = this.ng1UiRouterGlobals.params.documentId;

  isCreateFormVisible = false;

  createForm = this.fb.group({
    label: [''],
  });

  currentDate = this.dateService.getCurrentDate();

  currentUserName?: string;

  constructor(
    @Inject(NG1_UI_ROUTER_GLOBALS) private readonly ng1UiRouterGlobals: UIRouterGlobals,
    private readonly versionsService: VersionsService,
    private readonly documentWorkflowService: DocumentWorkflowService,
    private readonly fb: FormBuilder,
    private readonly dateService: DateService,
    @Inject(NG1_NAVAPP_SERVICE) private readonly navappService: Ng1NavappService,
  ) { }

  async ngOnInit(): Promise<void> {
    const { userName } = await this.navappService.getUserSettings();
    this.currentUserName = userName;

    interval(1000)
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(() => {
      this.currentDate = this.dateService.getCurrentDate();
    });
  }

  ngOnChanges(): void {
    this.isCreateFormVisible = false;
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  async createVersion(): Promise<void> {
    this.actionInProgressChange.emit(true);

    const label = this.createForm.get('label')?.value;
    await this.documentWorkflowService.postAction(this.documentId, ['version'], { label });
    this.versionsService.getVersionsInfo(this.ng1UiRouterGlobals.params.documentId);

    this.actionInProgressChange.emit(false);
  }
}
