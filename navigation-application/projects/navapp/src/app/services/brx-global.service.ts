import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class BrxGlobalService {
  constructor() {
    Object.assign(this, (window as any).BRX);
  }
}
