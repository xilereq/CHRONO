package pl.chrono.dbmanager.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.dbmanager.model.Policy;
import pl.chrono.dbmanager.repository.PolicyRepository;

@RestController
@RequestMapping("/api/policy")
public class PolicyController extends BaseController<Policy, Integer> {

    private final PolicyRepository policyRepository;

    public PolicyController(PolicyRepository policyRepository) {
        super(policyRepository);
        this.policyRepository = policyRepository;
    }
}