import { Component, Input } from '@angular/core';

import { UserSettings } from '../../../models/dto';

@Component({
  selector: 'brna-toolbar-drawer',
  templateUrl: './toolbar-drawer.component.html',
  styleUrls: ['./toolbar-drawer.component.scss'],
})
export class ToolbarDrawerComponent  {

  @Input()
  config: UserSettings;

  get userName(): string {
    return this.config.userName;
  }

  get email(): string {
    return this.config.email || '';
  }

  logout(): void {
    console.log(`logging out ${this.userName}`);
  }
}
