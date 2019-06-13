import { animate, style, transition, trigger } from '@angular/animations';
import { Component, HostBinding, Input } from '@angular/core';

import { UserSettings } from '../../../models/dto';
import { CommunicationsService } from '../../../services';

@Component({
  selector: 'brna-user-toolbar-drawer',
  templateUrl: './user-toolbar-drawer.component.html',
  styleUrls: ['./user-toolbar-drawer.component.scss'],
  animations: [
    trigger('slideInOut', [
      transition(':enter', [
        style({ transform: 'translateX(-100%)' }),
        animate('300ms ease-in-out', style({ transform: 'translateX(0%)' })),
      ]),
      transition(':leave', [
        animate('300ms ease-in-out', style({ transform: 'translateX(-100%)' })),
      ]),
    ]),
  ],
})
export class UserToolbarDrawerComponent {
  @Input()
  config: UserSettings;

  @HostBinding('@slideInOut')
  animate = true;

  constructor(
    private communicationService: CommunicationsService,
  ) { }

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
    this.communicationService
      .logout()
      .subscribe(results => {
        results
          .filter(e => e instanceof Error)
          .forEach(e => console.error(e));
        window.location.reload();
      });
  }

}
