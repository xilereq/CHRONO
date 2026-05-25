package pl.chrono.workforceapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.usermanager.api.I_UserService;
import pl.chrono.usermanager.dto.response.UserDto;
import pl.chrono.usermanager.service.AuthService;
import java.util.List;
import java.util.Map;

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

  @GetMapping("/managers")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<List<UserDto>> getAllManagers(){

    List<UserDto> managers = userService.getAllManagers();

    return ResponseEntity.ok(managers);
  }

  @GetMapping("/employees")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<List<UserDto>> getAllEmployees(){

    List<UserDto> employees = userService.getAllEmployees();

    return ResponseEntity.ok(employees);
  }


  @GetMapping("/admins")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<List<UserDto>> getAllAdmins(){

    List<UserDto> admins = userService.getAllAdmins();

    return ResponseEntity.ok(admins);
  }


  @GetMapping("/hrs")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<List<UserDto>> getAllHrs(){

    List<UserDto> hrs = userService.getAllHrs();

    return ResponseEntity.ok(hrs);
  }

  @GetMapping("my-role")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, String>> getMyRole(Authentication authentication){
    String role = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .findFirst()
        .orElse("ROLE_USER");

    return ResponseEntity.ok(Map.of("role", role));
  }

}
