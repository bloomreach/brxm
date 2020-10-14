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

import { Component, EventEmitter, Inject, OnInit, Output } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';

import { Ng1CmsService, NG1_CMS_SERVICE } from '../../../services/ng1/cms.ng1.service';
import { Ng1ComponentEditorService, NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { Ng1StateService, NG1_STATE_SERVICE } from '../../../services/ng1/state.ng1.service';
import { Characteristic, TargetGroup } from '../../models/characteristic.model';
import { Persona } from '../../models/persona.model';
import { Variant, VariantExpression, VariantExpressionType } from '../../models/variant.model';
import { VariantsService } from '../../services/variants.service';
import { CharacteristicsDialogComponent } from '../characteristics-dialog/characteristics-dialog.component';
import { SegmentsDialogComponent } from '../segments-dialog/segments-dialog.component';

@Component({
  selector: 'em-variants',
  templateUrl: './variants.component.html',
  styleUrls: ['./variants.component.scss'],
})
export class VariantsComponent implements OnInit {
  private readonly component = this.componentEditorService.getComponent();
  private readonly componentId = this.component.getId();

  dirty = false;
  variants: Variant[] = [];
  currentVariant?: Variant;
  variantSelect = new FormControl();

  @Output()
  variantUpdated = new EventEmitter<{ variant: Variant | undefined }>();

  @Output()
  variantInitiated = new EventEmitter<{ variant: Variant | undefined }>();

  constructor(
    @Inject(NG1_COMPONENT_EDITOR_SERVICE) private readonly componentEditorService: Ng1ComponentEditorService,
    @Inject(NG1_STATE_SERVICE) private readonly ng1StateService: Ng1StateService,
    @Inject(NG1_CMS_SERVICE) private readonly cmsService: Ng1CmsService,
    private readonly dialogService: MatDialog,
    private readonly variantsService: VariantsService,
  ) {
  }

  async ngOnInit(): Promise<void> {
    this.variants = await this.variantsService.getVariants(this.componentId);
    this.resetToStateParamsVariant();
  }

  async addVariant(): Promise<void> {
    const formData = this.componentEditorService.propertiesAsFormData();
    const { persona, characteristics } = this.variantsService.extractExpressions(this.variantSelect.value);

    await this.variantsService.addVariant(this.componentId, formData, persona, characteristics);

    const newVariants = await this.variantsService.getVariants(this.componentId);
    const newVariant = newVariants.find(variant => !this.variants?.find(v => v.id === variant.id));

    this.variants = newVariants;

    if (newVariant) {
      return this.selectVariant(newVariant);
    }

    return this.selectVariant(this.variants[0]);
  }

  async deleteVariant(): Promise<void> {
    if (this.currentVariant) {
      await this.variantsService.deleteVariant(this.componentId, this.currentVariant.id);
    }

    return this.selectVariant(this.variants[0]);
  }

  async selectVariant(variant: Variant): Promise<void> {
    // when selecting a variant a user might have changes
    // user might cancel, discard changes or save those changes
    // this visually restores the previous value in the select so it does not switch back and forth
    this.resetToStateParamsVariant();

    return this.ng1StateService.go('hippo-cm.channel.edit-component.properties', {
      componentId: this.componentId,
      variantId: variant.id,
    })
    .catch(error => error /* catching cancel transition error */);
  }

  isDefaultVariant(): boolean {
    return this.currentVariant?.id === 'hippo-default';
  }

  removeExpression(expression: VariantExpression): void {
    if (this.currentVariant?.expressions) {
      this.currentVariant.expressions = this.currentVariant.expressions.filter(exp => exp.id !== expression.id);
    }

    this.onChange();
  }

  async addSegment(): Promise<void> {
    this.cmsService.publish('show-mask');

    const persona = await this.dialogService
      .open(SegmentsDialogComponent, { width: '400px' })
      .afterClosed().toPromise<Persona>();

    this.cmsService.publish('remove-mask');

    if (!persona) {
      return;
    }

    this.currentVariant?.expressions.push({
      id: persona.id,
      name: persona.segmentName,
      type: VariantExpressionType.Persona,
    });

    this.onChange();
  }

  async addCharacteristic(): Promise<void> {
    this.cmsService.publish('show-mask');

    const result = await this.dialogService
      .open(CharacteristicsDialogComponent, { minWidth: 900,  maxWidth: 900 })
      .afterClosed().toPromise();

    this.cmsService.publish('remove-mask');

    if (!result) {
      return;
    }

    const { characteristic, targetGroup } = result;
    this.currentVariant?.expressions.push({
      id: `${characteristic.id}/${targetGroup.id}`,
      name: targetGroup.name,
      type: VariantExpressionType.Rule,
    });

    this.onChange();
  }

  getExpressionTranslation(expression: VariantExpression): string {
    if (expression.type === VariantExpressionType.Persona) {
      return 'EXPRESSION_SEGMENT';
    }

    const [characteristic] = expression.id.split('/');
    return `EXPRESSION_TARGET_GROUP_${characteristic.toUpperCase()}`;
  }

  hasSelectedSegment(): boolean | undefined {
    return this.currentVariant?.expressions
      .some(exp => exp.type === VariantExpressionType.Persona);
  }

  isVariantDirty(variant: Variant): boolean {
    return this.dirty && this.currentVariant?.id === variant.id;
  }

  private onChange(): void {
    this.dirty = true;
    this.variantUpdated.emit({ variant: this.currentVariant });
  }

  private resetToStateParamsVariant(): void {
     this.currentVariant = this.variants?.find(v => v.id === this.ng1StateService.params.variantId);
     this.variantInitiated.emit({ variant: this.currentVariant });
     this.variantSelect.setValue(this.currentVariant);
  }
}
