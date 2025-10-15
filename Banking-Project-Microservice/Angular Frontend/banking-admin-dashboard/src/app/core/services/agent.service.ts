import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export type AgentMode = 'OFF' | 'DRY_RUN' | 'AUTO';

export interface AgentStatus {
  enabled: boolean;
  mode: AgentMode;
  workflows: {
    kyc: boolean;
    loans: boolean;
    salary: boolean;
    cards: boolean;
    selfService: boolean;
  };
  pollingEnabled: boolean;
  pollingIntervalMs: number;
  lastRunAt: string | null;
  queues: Record<string, number>;
}

export interface ToggleRequest {
  enabled?: boolean;
  mode?: AgentMode;
  workflows?: Partial<AgentStatus['workflows']>;
  pollingEnabled?: boolean;
  pollingIntervalMs?: number;
}

@Injectable({ providedIn: 'root' })
export class AgentService {
  private readonly base = `${environment.orchestratorUrl}/agent`;

  constructor(private http: HttpClient) {}

  getStatus(): Observable<AgentStatus> {
    return this.http.get<AgentStatus>(`${this.base}/status`);
  }

  toggle(body: ToggleRequest): Observable<AgentStatus> {
    return this.http.put<AgentStatus>(`${this.base}/toggle`, body);
  }

  runNow(): Observable<string> {
    return this.http.post(`${this.base}/run-now`, {}, { responseType: 'text' });
  }
}
