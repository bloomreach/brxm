/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Location, LocationStrategy, PathLocationStrategy } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ClientErrorCodes, SiteId } from '@bloomreach/navapp-communication';

import { AppState } from './services/app-state';
import { ChildApiMethodsService } from './services/child-api-methods.service';
import { CommunicationService } from './services/communication.service';
import { NavigatorService } from './services/navigator.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  providers: [
    Location,
    { provide: LocationStrategy, useClass: PathLocationStrategy },
  ],
})
export class AppComponent implements OnInit {
  parentApiVersion: string;

  constructor(
    private readonly communicationService: CommunicationService,
    private readonly childApiMethodsService: ChildApiMethodsService,
    private readonly navigator: NavigatorService,
    // tslint:disable-next-line:member-access
    public readonly state: AppState,
  ) { }

  get currentLocation(): string {
    return `${location}`;
  }

  get shouldConfirm(): boolean {
    return this.state.shouldAskBeforeNavigation;
  }

  async ngOnInit(): Promise<void> {
    await this.communicationService.connect(this.childApiMethodsService.getMethods());

    this.parentApiVersion = this.communicationService.parentApiVersion;
  }

  onButtonClicked(): void {
    this.state.buttonClickedCounter++;
    this.communicationService.notifyAboutUserActivity();
  }

  toggleOverlay(): void {
    this.communicationService.toggleMask();
  }

  navigateTo(path: string, breadcrumbLabel?: string): void {
    this.communicationService.navigateTo(path, breadcrumbLabel);
  }

  navigateInternally(): void {
    this.navigator.navigate(false);
  }

  navigateInternallyWithReplaceState(): void {
    this.navigator.navigate();
  }

  reloadPage(): void {
    this.navigator.reloadPage();
  }

  sendNotAuthenticatedError(): void {
    this.communicationService.sendError(ClientErrorCodes.NotAuthorizedError, 'Not authenticated');
  }

  showError(message: string): void {
    this.communicationService.sendError(ClientErrorCodes.InternalError, message);
  }

  toggleLogoutErrorState(): void {
    this.state.generateAnErrorUponLogout = !this.state.generateAnErrorUponLogout;
  }

  updateSelectedSite(siteId: SiteId): void {
    this.communicationService.updateSelectedSite(siteId);
  }

  toggleConfirm(): void {
    this.state.shouldAskBeforeNavigation = !this.state.shouldAskBeforeNavigation;
  }
}
