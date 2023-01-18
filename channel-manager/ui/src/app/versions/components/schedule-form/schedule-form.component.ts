/*
 * Copyright 2020-2023 Bloomreach
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

import { Component, EventEmitter, Inject, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { UIRouterGlobals } from '@uirouter/core';
import moment, { Moment } from 'moment';
import { interval, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { DateService } from '../../../services/date.service';
import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
import { Version, VersionUpdateBody } from '../../models/version.model';
import { VersionsService } from '../../services/versions.service';

@Component({
  selector: 'em-schedule-form',
  templateUrl: './schedule-form.component.html',
  styleUrls: ['./schedule-form.component.scss'],
})
export class ScheduleFormComponent implements OnInit, OnDestroy {
  private readonly unsubscribe = new Subject();

  @Output()
  cancelForm = new EventEmitter<void>();

  @Input()
  version!: Version;

  scheduleForm = this.fb.group({
    label: [''],
    fromDateTime: ['', Validators.required],
    toDateTime: [''],
  });

  originalFromDateTime?: Moment;
  originalToDateTime?: Moment;

  constructor(
    @Inject(NG1_UI_ROUTER_GLOBALS) private readonly uiRouterGlobals: UIRouterGlobals,
    private readonly versionsService: VersionsService,
    private readonly fb: FormBuilder,
    private readonly dateService: DateService,
  ) { }

  ngOnInit(): void {
    this.originalFromDateTime = moment(this.version.campaign?.from ?? this.dateService.getCurrentDate());
    this.originalToDateTime = this.version.campaign?.to && moment(this.version.campaign?.to);

    this.scheduleForm.patchValue({
      label: this.version.label,
      fromDateTime: this.originalFromDateTime,
      toDateTime: this.originalToDateTime,
    });
    // Ensure mat-errors are shown immediately for toDateTime on first change to fromDateTime
    this.scheduleForm.get('toDateTime')?.markAsTouched();
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  resetFromDate(): void {
    this.scheduleForm.patchValue({
      fromDateTime: moment(this.dateService.getCurrentDate()),
    });
  }

  resetToDate(): void {
    this.scheduleForm.patchValue({
      toDateTime: moment(this.dateService.getCurrentDate()),
    });
  }

  async scheduleCampaign(): Promise<void> {
    const { documentId } = this.uiRouterGlobals.params;
    const formValues = this.scheduleForm.value;
    const body: VersionUpdateBody = {
      label: formValues.label,
    };

    const from = formValues.fromDateTime?.utc().format();
    const to = formValues.toDateTime?.utc().format() || null;

    if (from) {
      body.campaign = {
        from,
      };

      if (to) {
        body.campaign.to = to;
      }
    }

    await this.versionsService.updateVersion(documentId, this.version.jcrUUID, body);
    await this.versionsService.getVersionsInfo(documentId);
  }

  async removeSchedule(): Promise<void> {
    const { documentId } = this.uiRouterGlobals.params;
    const formValues = this.scheduleForm.value;
    const body: VersionUpdateBody = {
      label: formValues.label,
    };

    await this.versionsService.updateVersion(documentId, this.version.jcrUUID, body);
    await this.versionsService.getVersionsInfo(documentId);
  }
}
