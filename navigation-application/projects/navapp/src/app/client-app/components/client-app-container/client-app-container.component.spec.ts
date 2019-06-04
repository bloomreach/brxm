import { NO_ERRORS_SCHEMA } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ClientApp } from '../../models/client-app.model';
import { ClientAppService } from '../../services';

import { ClientAppContainerComponent } from './client-app-container.component';

fdescribe('ClientAppContainerComponent', () => {
  let component: ClientAppContainerComponent;
  let fixture: ComponentFixture<ClientAppContainerComponent>;

  const testApp = new ClientApp('mytesturl');
  const testApp2 = new ClientApp('mytesturl2');

  const clientAppService = {
    apps$: of([testApp, testApp2]),
    activeAppId$: of('mytesturl'),
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: ClientAppService, useValue: clientAppService }],
      declarations: [ClientAppContainerComponent],
      schemas: [NO_ERRORS_SCHEMA],
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ClientAppContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create client app nodes and pass the app as input', () => {
    const el: HTMLElement = fixture.nativeElement;
    const clientApps = el.querySelectorAll('brna-client-app');
    expect(clientApps.length).toEqual(2);
    expect((clientApps[0] as any).app).toBe(testApp);
  });
});
