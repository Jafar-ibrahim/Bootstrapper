package org.example.bootstrapper.Controller;

import org.example.bootstrapper.services.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/bootstrapper")
public class WorkerController {

    private final WorkerService workerService;

    @Autowired
    public WorkerController(WorkerService workerService){
        this.workerService = workerService;
    }

    @GetMapping("/getWorker/{username}")
    public String getWorkerForUser(@PathVariable String username) {
        String workerPort = workerService.getWorkerForUser(username);
        return Objects.requireNonNullElse(workerPort, "could not find worker port for user: " + username);
    }
}
