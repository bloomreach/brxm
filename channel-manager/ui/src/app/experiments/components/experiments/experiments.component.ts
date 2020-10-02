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

import { Component, Inject } from '@angular/core';

import { Ng1ComponentEditorService, NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { ExperimentsService } from '../../services/experiments.service';

@Component({
  templateUrl: 'experiments.component.html',
  styleUrls: ['experiments.component.scss'],
})
export class ExperimentsComponent {
  private readonly component = this.componentEditorService.getComponent();
  private readonly componentId = this.component.getId();
  readonly experiment$ = this.experimentsService.getExperiment(this.componentId);

  constructor(
    @Inject(NG1_COMPONENT_EDITOR_SERVICE) private readonly componentEditorService: Ng1ComponentEditorService,
    private readonly experimentsService: ExperimentsService,
  ) {}
}
