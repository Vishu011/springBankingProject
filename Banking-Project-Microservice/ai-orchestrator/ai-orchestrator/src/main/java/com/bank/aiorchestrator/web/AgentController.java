package com.bank.aiorchestrator.web;

import com.bank.aiorchestrator.model.AgentMode;
import com.bank.aiorchestrator.service.AgentStateService;
import com.bank.aiorchestrator.web.dto.AgentStatusResponse;
import com.bank.aiorchestrator.web.dto.ToggleRequest;
import com.bank.aiorchestrator.service.OrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentStateService state;
    private final OrchestratorService orchestrator;

    public AgentController(AgentStateService state, OrchestratorService orchestrator) {
        this.state = state;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/status")
    public ResponseEntity<AgentStatusResponse> status() {
        AgentStatusResponse resp = new AgentStatusResponse();
        resp.setEnabled(state.isEnabled());
        resp.setMode(state.getMode() == null ? AgentMode.OFF : state.getMode());

        AgentStatusResponse.Workflows wf = new AgentStatusResponse.Workflows();
        wf.setKyc(state.kycEnabled());
        wf.setLoans(state.loansEnabled());
        wf.setSalary(state.salaryEnabled());
        wf.setCards(state.cardsEnabled());
        wf.setSelfService(state.selfServiceEnabled());
        resp.setWorkflows(wf);

        resp.setPollingEnabled(state.isPollingEnabled());
        resp.setPollingIntervalMs(state.getPollingIntervalMs());
        resp.setLastRunAt(orchestrator.getLastRunAtIso());
        resp.setQueues(orchestrator.getCurrentQueuesSnapshot());

        return ResponseEntity.ok(resp);
    }

    @PutMapping("/toggle")
    public ResponseEntity<AgentStatusResponse> toggle(@RequestBody ToggleRequest req) {
        if (req.getEnabled() != null) state.setEnabled(req.getEnabled());
        if (req.getMode() != null) state.setMode(req.getMode());

        if (req.getWorkflows() != null) {
            if (req.getWorkflows().getKyc() != null) state.setKyc(req.getWorkflows().getKyc());
            if (req.getWorkflows().getLoans() != null) state.setLoans(req.getWorkflows().getLoans());
            if (req.getWorkflows().getSalary() != null) state.setSalary(req.getWorkflows().getSalary());
            if (req.getWorkflows().getCards() != null) state.setCards(req.getWorkflows().getCards());
            if (req.getWorkflows().getSelfService() != null) state.setSelfService(req.getWorkflows().getSelfService());
        }

        if (req.getPollingEnabled() != null) state.setPollingEnabled(req.getPollingEnabled());
        if (req.getPollingIntervalMs() != null && req.getPollingIntervalMs() > 0) {
            state.setPollingIntervalMs(req.getPollingIntervalMs());
        }

        return status();
    }

    @PostMapping("/run-now")
    public ResponseEntity<String> runNow() {
        orchestrator.runNow();
        return ResponseEntity.accepted().body("Agent run triggered");
    }
}
