/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import moment from 'moment';
import { Subject } from 'rxjs';

import { DateService } from '../../../services/date.service';
import { Version } from '../../models/version.model';

@Component({
  selector: 'em-schedule-form',
  templateUrl: './schedule-form.component.html',
  styleUrls: ['./schedule-form.component.scss'],
})
export class ScheduleFormComponent implements OnInit {
  @Output()
  cancelForm = new EventEmitter<void>();

  @Input()
  version!: Version;

  scheduleForm = this.fb.group({
    label: ['', Validators.required],
    fromDateTime: ['', Validators.required],
    toDateTime: [''],
  });

  currentDate?: Date;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dateService: DateService,
  ) { }

  ngOnInit(): void {
    this.currentDate = this.dateService.getCurrentDate();

    this.scheduleForm.patchValue({
      label: this.version.label,
      from: {
        fromDateTime: moment(this.version.campaign?.from ?? this.currentDate),
        toDateTime: this.version.campaign?.to ?? moment(this.version.campaign?.to),
      },
    });
  }

  async scheduleCampaign(): Promise<void> {
    const formValues = this.scheduleForm.value;
    const from = formValues.fromDateTime.utc().format();
    const to = formValues.toDateTime.utc().format();
    const label = formValues.label;

    console.log({
      label,
      from,
      to,
    });
  }
}
