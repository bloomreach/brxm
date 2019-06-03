import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class OverlayService {
  private isOverlayVisible = new BehaviorSubject<boolean>(false);

  get visible$(): Observable<boolean> {
    return this.isOverlayVisible.asObservable();
  }

  enable(): void {
    this.isOverlayVisible.next(true);
  }

  disable(): void {
    this.isOverlayVisible.next(false);
  }
}
