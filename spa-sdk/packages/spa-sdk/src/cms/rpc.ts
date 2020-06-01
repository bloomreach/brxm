/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { EmitterMixin, Emitter } from '../emitter';

type Callable<T = any, U extends unknown[] = any[]> = (...args: U) => T;

const TYPE_EVENT = 'brxm:event';
const TYPE_RESPONSE = 'brxm:response';
const TYPE_REQUEST = 'brxm:request';
const STATE_FULFILLED = 'fulfilled';
const STATE_REJECTED = 'rejected';

export interface Event {
  type: typeof TYPE_EVENT;
  event: string;
  payload?: any;
}

export interface Request {
  type: typeof TYPE_REQUEST;
  id: string;
  command: string;
  payload: any[];
}

export interface Response {
  type: typeof TYPE_RESPONSE;
  id: string;
  state: typeof STATE_FULFILLED | typeof STATE_REJECTED;
  result: any;
}

export type Message = Event | Request | Response;
export type Procedures = Record<string, Callable>;
export type Events = Record<string, any>;

export interface RpcClient<TProcedures extends Procedures, TEvents extends Events> extends Emitter<TEvents> {
  call<K extends keyof TProcedures & string>(command: K, ...params: Parameters<TProcedures[K]>):
    Promise<ReturnType<TProcedures[K]>>;
}

export interface RpcServer<TProcedures extends Procedures, TEvents extends Events> {
  register<K extends keyof TProcedures & string>(
    command: K,
    callback: Callable<Promise<ReturnType<TProcedures[K]>>, Parameters<TProcedures[K]>>,
  ): void;
  trigger<K extends keyof TEvents>(event: K, payload: TEvents[K]): void;
}

class Dummy {}

export abstract class Rpc<
    TRemoteProcedures extends Procedures,
    TRemoteEvents extends Events,
    TProcedures extends Procedures,
    TEvents extends Events,
  >
  extends EmitterMixin<typeof Dummy, Events>(Dummy)
  implements RpcClient<TRemoteProcedures, TRemoteEvents>, RpcServer<TProcedures, TEvents>
{
  private calls = new Map<string, [Callable, Callable]>();
  private callbacks = new Map<keyof TProcedures, Callable<Promise<any>, any>>();

  private generateId() {
    let id: string;
    do {
      id = `${Math.random()}`.slice(2);
    } while (this.calls.has(id));

    return id;
  }

  call<K extends keyof TRemoteProcedures & string>(command: K, ...payload: Parameters<TRemoteProcedures[K]>) {
    return new Promise<ReturnType<TRemoteProcedures[K]>>((resolve, reject) => {
      const id = this.generateId();

      this.calls.set(id, [resolve, reject]);
      this.send({ id, command, payload, type: TYPE_REQUEST });
    });
  }

  register<K extends keyof TProcedures & string>(
    command: K,
    callback: Callable<Promise<ReturnType<TProcedures[K]>>, Parameters<TProcedures[K]>>,
  ) {
    this.callbacks.set(command, callback);
  }

  trigger<K extends keyof TEvents>(event: K & string, payload: TEvents[K]) {
    this.send({ event, payload, type: TYPE_EVENT });
  }

  protected process(message: Message) {
    switch (message?.type) {
      case TYPE_EVENT:
        this.processEvent(message);
        break;
      case TYPE_RESPONSE:
        this.processResponse(message);
        break;
      case TYPE_REQUEST:
        this.processRequest(message);
        break;
    }
  }

  private processEvent(event: Event) {
    this.emit(event.event, event.payload);
  }

  private processResponse(response: Response) {
    if (!this.calls.has(response.id)) {
      return;
    }

    const [resolve, reject] = this.calls.get(response.id)!;
    this.calls.delete(response.id);

    if (response.state === STATE_REJECTED) {
      return void reject(response.result);
    }

    resolve(response.result);
  }

  private async processRequest(request: Request) {
    const callback = this.callbacks.get(request.command);

    if (!callback) {
      return;
    }

    try {
      return this.send({
        type: TYPE_RESPONSE,
        id: request.id,
        state: STATE_FULFILLED,
        result: await callback(...request.payload),
      });
    } catch (result) {
      return this.send({
        result,
        type: TYPE_RESPONSE,
        id: request.id,
        state: STATE_REJECTED,
      });
    }
  }

  protected abstract send(message: Message): void;
}
