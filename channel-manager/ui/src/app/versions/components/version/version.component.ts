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

import { Component, EventEmitter, HostBinding, Inject, Input, OnInit, Output } from '@angular/core';
import { UIRouterGlobals } from '@uirouter/core';

import { Ng1IframeService, NG1_IFRAME_SERVICE } from '../../../services/ng1/iframe.ng1service';
import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
import { Ng1WorkflowService, NG1_WORKFLOW_SERVICE } from '../../../services/ng1/workflow.ng1.service';
import { Version } from '../../models/version.model';
import { VersionsInfo } from '../../models/versions-info.model';
import { VersionsService } from '../../services/versions.service';

@Component({
  selector: 'em-version',
  templateUrl: './version.component.html',
  styleUrls: ['./version.component.scss'],
})
export class VersionComponent {
  @Input()
  version!: Version;

  @Input()
  versionsInfo!: VersionsInfo;

  @HostBinding('class.selected')
  @Input()
  isSelected!: boolean;

  @Input()
  renderPath!: string;

  constructor(
    @Inject(NG1_IFRAME_SERVICE) private readonly ng1IframeService: Ng1IframeService,
    @Inject(NG1_WORKFLOW_SERVICE) private readonly ng1WorkflowService: Ng1WorkflowService,
    @Inject(NG1_UI_ROUTER_GLOBALS) private readonly ng1UiRouterGlobals: UIRouterGlobals,
    private readonly versionsService: VersionsService,
  ) { }

  async restoreVersion(versionUUID: string): Promise<void> {
    const documentId = this.ng1UiRouterGlobals.params.documentId;
    await this.ng1WorkflowService.createWorkflowAction(documentId, {}, 'restore', versionUUID);
    await this.ng1IframeService.load(this.renderPath);
    await this.versionsService.getVersionsInfo(documentId);
  }
}
