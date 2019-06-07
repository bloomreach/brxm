import { Component, Input } from '@angular/core';

import { UserSettings } from '../../../models/dto';
import { CommunicationsService } from '../../../services';

@Component({
  selector: 'brna-toolbar-drawer',
  templateUrl: './toolbar-drawer.component.html',
  styleUrls: ['./toolbar-drawer.component.scss'],
})
export class ToolbarDrawerComponent  {

  @Input()
  config: UserSettings;

  constructor(
    private communicationService: CommunicationsService,
  ) {}

  get userName(): string {
    return this.config.userName;
  }

  get email(): string {
    return this.config.email || '';
  }

  get loginUrl(): string {
    return window.location.href;
  }

  logout(): void {
    this.communicationService.logout().subscribe(() => {
      console.log('all apps logged out');
      window.location.reload();
    });
  }

}
