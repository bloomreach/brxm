import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ClientApp } from '../../models/client-app.model';
import { ClientAppService } from '../../services';

import { ClientAppComponent } from './client-app.component';

describe('ClientAppComponent', () => {
  let component: ClientAppComponent;
  let fixture: ComponentFixture<ClientAppComponent>;

  const clientAppService = jasmine.createSpyObj(['addConnection']);

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: ClientAppService, useValue: clientAppService }],
      declarations: [ClientAppComponent],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ClientAppComponent);
    component = fixture.componentInstance;
    component.app = new ClientApp('http://mytesturl.com');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
