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

import { Ng1TargetingService, NG1_TARGETING_SERVICE } from '../../../services/ng1/targeting.ng1.service';
import { Characteristic, TargetGroup, TargetGroupProperty } from '../../models/characteristic.model';

function defaultTargetGroupSort(a: TargetGroup, b: TargetGroup): number {
  return a.name.localeCompare(b.name, undefined, { sensitivity: 'accent' });
}

function sortableDayOfWeek(targetGroup: TargetGroup): number {
  return targetGroup.properties
    .map((property: TargetGroupProperty) => parseInt(property.name, 10))
    .map((propertyNameAsNumber: number, index: number) => propertyNameAsNumber + (index * 10))
    .reduce((a: number, b: number) => a + b, 0);
}

function lookup(ar: any[], value: string, prop = 'value'): string {
  const target = ar.find(p => p[prop] === value);
  if (!target) {
    throw new Error(`Property "${prop}" with value "${value}" was not found in ${ar}`);
  }
  return target.name;
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
    if (data) {
      this.characteristics = this.parseCharacteristics(data);
    }
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

  private parseCharacteristics(characteristics: Characteristic[]): Characteristic[] {
    if (!characteristics) {
      return [];
    }

    characteristics.forEach((c: Characteristic) => {
      if (c.id === 'dayofweek') {
        this.parseDayOfWeek(c);
      } else if (c.id === 'continent') {
        this.parseContinent(c);
      } else if (c.id === 'documenttypes') {
        this.parseDocumentTypes(c);
      } else if (c.id === 'groups') {
        this.parseGroups(c);
      } else if (c.id === 'referrer') {
        this.parseReferrer(c);
      } else if (c.id === 'returningvisitor') {
        this.parseChoices(c);
      } else if (c.id === 'tracking') {
        this.parseChoices(c);
      }
    });

    return characteristics;
  }

  private parseContinent(characteristic: Characteristic): void {
    const { continentsMap } = this.targetingService.getCharacteristicConfig(characteristic.id);
    this.parseProperties(characteristic, ({ name }) => continentsMap[name]);
  }

  private parseDayOfWeek(characteristic: Characteristic): void {
    const { daysOfWeek } = this.targetingService.getCharacteristicConfig(characteristic.id);
    this.parseProperties(
      characteristic,
      ({ name }) => lookup(daysOfWeek, name, 'index'),
      (a: TargetGroup, b: TargetGroup): number => sortableDayOfWeek(a) - sortableDayOfWeek(b),
    );
  }

  private parseDocumentTypes(characteristic: Characteristic): void {
    const { documentTypes } = this.targetingService.getCharacteristicConfig(characteristic.id);
    this.parseProperties(characteristic, ({ name }) => lookup(documentTypes, name, 'type'));
  }

  private parseGroups(characteristic: Characteristic): void {
    const { groups } = this.targetingService.getCharacteristicConfig(characteristic.id);
    this.parseProperties(characteristic, ({ name }) => lookup(groups, name));
  }

  private parseReferrer(characteristic: Characteristic): void {
    const { renderedValuesPrefix } = this.targetingService.getCharacteristicConfig(characteristic.id);
    this.parseProperties(characteristic, ({ name }) => `${renderedValuesPrefix}${name}`);
  }

  private parseChoices(characteristic: Characteristic): void {
    const { choices } = this.targetingService.getCharacteristicConfig(characteristic.id);
    this.parseProperties(characteristic, ({ name }) => {
      const choice = lookup(choices, name);
      return `${choice.charAt(0).toUpperCase()}${choice.substr(1)}`;
    });
  }

  private parseProperties(
    characteristic: Characteristic,
    valueFn: (property: TargetGroupProperty) => string,
    sortFn = defaultTargetGroupSort,
  ): void {
    characteristic.targetGroups
      .sort(sortFn)
      .flatMap((targetGroup: TargetGroup) => targetGroup.properties)
      .forEach((property: TargetGroupProperty) => {
        property.value = valueFn(property);
      });
  }
}
