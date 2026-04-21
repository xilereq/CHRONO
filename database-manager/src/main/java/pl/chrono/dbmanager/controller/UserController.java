package pl.chrono.dbmanager.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController<User, Integer> {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        super(userRepository);
        this.userRepository = userRepository;
    }
}