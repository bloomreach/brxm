/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import { NgModule } from '@angular/core';
import { UpgradeModule  } from '@angular/upgrade/static';

import { ChannelModule } from './channel/channel.module';
import { RightSidePanelModule } from './channel/sidePanels/rightSidePanel/right-side-panel.module';
import { SharedModule } from './shared/shared.module';

import ng1Module from './hippo-cm.ng1.module.js';

@NgModule({
  imports: [
    ChannelModule,
    SharedModule,
    UpgradeModule,
    RightSidePanelModule,
  ]
})
export class AppModule {
  constructor(private upgrade: UpgradeModule) {}

  ngDoBootstrap() {
    this.upgrade.bootstrap(document.body, [ng1Module], { strictDi: true });
  }
}

