import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AgentService, AgentStatus, AgentMode, ToggleRequest } from '../../services/agent.service';

@Component({
  selector: 'app-agent-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './agent-panel.component.html',
  styleUrls: ['./agent-panel.component.css']
})
export class AgentPanelComponent implements OnInit {

  loading = false;
  saving = false;
  running = false;

  status: AgentStatus | null = null;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  // Local form state helpers
  availableModes: AgentMode[] = ['OFF', 'DRY_RUN', 'AUTO'];
  desiredPollingMs: number | null = null;

  constructor(private agent: AgentService) {}

  ngOnInit(): void {
    this.refreshStatus();
  }

  refreshStatus(): void {
    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;

    this.agent.getStatus().subscribe({
      next: (resp) => {
        this.status = resp;
        this.desiredPollingMs = resp.pollingIntervalMs;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = this.extractError(err, 'Failed to load agent status');
      }
    });
  }

  setEnabled(enabled: boolean): void {
    if (!this.status) { return; }
    this.persistToggle({ enabled });
  }

  setMode(mode: AgentMode): void {
    if (!this.status) { return; }
    this.persistToggle({ mode });
  }

  setWorkflow(name: keyof AgentStatus['workflows'], value: boolean): void {
    if (!this.status) { return; }
    const workflows: ToggleRequest['workflows'] = { [name]: value };
    this.persistToggle({ workflows });
  }

  setPollingEnabled(value: boolean): void {
    if (!this.status) { return; }
    this.persistToggle({ pollingEnabled: value });
  }

  applyPollingInterval(): void {
    if (!this.status) { return; }
    const ms = Number(this.desiredPollingMs ?? 0);
    if (!Number.isFinite(ms) || ms <= 0) {
      this.errorMessage = 'Polling interval must be a positive number (ms).';
      return;
    }
    this.persistToggle({ pollingIntervalMs: ms });
  }

  runNow(): void {
    this.running = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.agent.runNow().subscribe({
      next: () => {
        this.running = false;
        this.successMessage = 'Agent run triggered';
        // Refresh after short delay to show updated lastRunAt
        setTimeout(() => this.refreshStatus(), 1000);
      },
      error: (err) => {
        this.running = false;
        this.errorMessage = this.extractError(err, 'Failed to trigger agent run');
      }
    });
  }

  clearMessages(): void {
    this.errorMessage = null;
    this.successMessage = null;
  }

  private persistToggle(body: ToggleRequest): void {
    this.saving = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.agent.toggle(body).subscribe({
      next: (resp) => {
        this.status = resp;
        this.desiredPollingMs = resp.pollingIntervalMs;
        this.saving = false;
        this.successMessage = 'Agent settings updated';
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = this.extractError(err, 'Failed to update agent settings');
      }
    });
  }

  private extractError(err: any, fallback: string): string {
    if (err && err.error) {
      if (typeof err.error === 'string') return err.error;
      if (err.error.message) return err.error.message;
    }
    if (err && err.message) return err.message;
    return fallback;
  }

  // Safe helpers for template
  get lastRunAt(): string {
    return this.status?.lastRunAt || '—';
  }

  queueKeys(): string[] {
    if (!this.status?.queues) return [];
    return Object.keys(this.status.queues);
  }

  queueValue(key: string): number {
    if (!this.status?.queues) return 0;
    return this.status.queues[key] ?? 0;
  }
}
