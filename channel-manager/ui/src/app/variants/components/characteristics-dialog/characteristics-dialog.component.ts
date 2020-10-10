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

import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSelectionList } from '@angular/material/list';

import { Ng1TargetingService, NG1_TARGETING_SERVICE } from '../../../services/ng1/targeting.ng1service';
import { Characteristic } from '../../models/characteristic.model';

@Component({
  selector: 'em-characteristics-dialog',
  templateUrl: './characteristics-dialog.component.html',
  styleUrls: ['./characteristics-dialog.component.scss'],
})
export class CharacteristicsDialogComponent implements OnInit {
  characteristics?: Characteristic[];

  @ViewChild('characteristicsList')
  selectionList?: MatSelectionList;

  constructor(
      @Inject(NG1_TARGETING_SERVICE) private readonly targetingService: Ng1TargetingService,
      private readonly dialogRef: MatDialogRef<CharacteristicsDialogComponent>,
  ) { }

  async ngOnInit(): Promise<void> {
    const { data } = await this.targetingService.getCharacteristics();
    this.characteristics = data;
  }

  selectCharacteristic(): void {
    const selected = this.selectionList?.selectedOptions.selected[0].value;
    this.dialogRef.close(selected);
  }

  hasSelection(): boolean | undefined {
    return this.selectionList?.selectedOptions.hasValue();
  }
}
