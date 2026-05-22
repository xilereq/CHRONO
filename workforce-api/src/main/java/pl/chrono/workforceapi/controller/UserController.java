package pl.chrono.workforceapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.usermanager.api.I_UserService;
import pl.chrono.usermanager.dto.response.UserDto;
import pl.chrono.usermanager.service.AuthService;
import java.util.List;

@RestController
@RequestMapping("api/users")
public class UserController extends BaseController{

  private final I_UserService userService;

  public UserController(AuthService authService, I_UserService userService){
    super(authService);
    this.userService = userService;
  }

  @GetMapping("/subordinates")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<List<UserDto>> getMySubordinates(@RequestHeader("Authorization") String token){

    Integer managerID = extractId(token);

    List<UserDto> subordinates = userService.getSubordinatesForManager(managerID);

    return ResponseEntity.ok(subordinates);
  }
}
