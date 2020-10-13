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
import { Characteristic, TargetGroup } from '../../models/characteristic.model';

function compareString(a: string, b: string): -1 | 0 | 1 {
  const strA = a.toUpperCase();
  const strB = b.toUpperCase();
  if (strA < strB) {
    return -1;
  }
  if (strA > strB) {
    return 1;
  }

  return 0;
}

@Component({
  selector: 'em-characteristics-dialog',
  templateUrl: './characteristics-dialog.component.html',
  styleUrls: ['./characteristics-dialog.component.scss'],
})
export class CharacteristicsDialogComponent implements OnInit {
  characteristics?: Characteristic[];

  @ViewChild('characteristicsList')
  characteristicsList?: MatSelectionList;

  @ViewChild('targetGroupsList')
  targetGroupsList?: MatSelectionList;

  constructor(
      @Inject(NG1_TARGETING_SERVICE) private readonly targetingService: Ng1TargetingService,
      private readonly dialogRef: MatDialogRef<CharacteristicsDialogComponent>,
  ) { }

  async ngOnInit(): Promise<void> {
    const { data } = await this.targetingService.getCharacteristics();
    this.characteristics = data;

    this.characteristics.forEach((c: Characteristic) => c.targetGroups.sort((a, b) => compareString(a.name, b.name)));
  }

  addSelection(): void {
    this.dialogRef.close({
      characteristic: this.getSelectedCharacteristic(),
      targetGroup: this.getSelectedTargetGroup(),
     });
  }

  hasSelectedCharacteristic(): boolean {
    return this.characteristicsList?.selectedOptions.hasValue() === true;
  }

  getSelectedCharacteristic(): Characteristic | undefined {
    return this.characteristicsList?.selectedOptions.selected[0].value;
  }

  hasSelectedTargetGroup(): boolean {
    return this.targetGroupsList?.selectedOptions.hasValue() === true;
  }

  getSelectedTargetGroup(): TargetGroup | undefined {
    return this.targetGroupsList?.selectedOptions.selected[0].value;
  }

  getCharacteristicIcon(characteristic: Characteristic): string {
    switch (characteristic.id) {
      case 'city':
      case 'country':
      case 'continent':
        return 'web';
      case 'dayofweek':
        return 'calendar-today';
      case 'documenttypes':
        return 'chart-pie';
      case 'groups':
        return 'account-circle-outline';
      case 'pageviews':
        return 'home-outline';
      case 'referrer':
        return 'exit-to-app';
      case 'returningvisitor':
        return 'face-outline';
      case 'tracking':
        return 'cookie-outline';
      default:
        return '';
    }
  }

  getPropertiesAsString(properties: TargetGroupProperty[]): string {
    return properties.map(p => p.value || p.name).join(', ');
  }
}
