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
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSelectionList } from '@angular/material/list';

import { Ng1TargetingService, NG1_TARGETING_SERVICE } from '../../../services/ng1/targeting.ng1service';
import { Persona } from '../../models/persona.model';

@Component({
  selector: 'em-segments-dialog',
  templateUrl: './segments-dialog.component.html',
  styleUrls: ['./segments-dialog.component.scss'],
})
export class SegmentsDialogComponent implements OnInit {
  personas?: Persona[];

  @ViewChild('personasList')
  selectionList?: MatSelectionList;

  constructor(
      @Inject(NG1_TARGETING_SERVICE) private readonly targetingService: Ng1TargetingService,
      @Inject(MAT_DIALOG_DATA) public data: any,
      private readonly dialogRef: MatDialogRef<SegmentsDialogComponent>,
  ) { }

  async ngOnInit(): Promise<void> {
    const response = await this.targetingService.getPersonas();
    this.personas = response.data.items
      .filter(persona => !this.data.selectedPersonaIds.includes(persona.id));
  }

  selectPersona(): void {
    const selectedPersona = this.selectionList?.selectedOptions.selected[0].value;
    this.dialogRef.close(selectedPersona);
  }

  hasSelectedPersona(): boolean {
    return this.selectionList?.selectedOptions.hasValue() === true;
  }
}
