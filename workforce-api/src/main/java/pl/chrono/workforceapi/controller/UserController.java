package pl.chrono.workforceapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.usermanager.api.I_UserService;
import pl.chrono.usermanager.dto.request.SetActive;
import pl.chrono.usermanager.dto.request.SetEmploymentEndDate;
import pl.chrono.usermanager.dto.request.SetEmploymentStartDate;
import pl.chrono.usermanager.dto.request.SetManagerRequest;
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
  @PreAuthorize("hasAnyRole('ADMIN','HR')")
  public ResponseEntity<List<UserDto>> getAllManagers(){

    List<UserDto> managers = userService.getAllManagers();

    return ResponseEntity.ok(managers);
  }

  @GetMapping("/employees")
  @PreAuthorize("hasAnyRole('ADMIN','HR')")
  public ResponseEntity<List<UserDto>> getAllEmployees(){

    List<UserDto> employees = userService.getAllEmployees();

    return ResponseEntity.ok(employees);
  }


  @GetMapping("/admins")
  @PreAuthorize("hasAnyRole('ADMIN','HR')")
  public ResponseEntity<List<UserDto>> getAllAdmins(){

    List<UserDto> admins = userService.getAllAdmins();

    return ResponseEntity.ok(admins);
  }


  @GetMapping("/hrs")
  @PreAuthorize("hasAnyRole('ADMIN','HR')")
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

  @GetMapping("/users")
  @PreAuthorize("hasAnyRole('ADMIN','HR')")
  public ResponseEntity<List<UserDto>> getAllUsers(){

    List<UserDto> users = userService.getAllUsers();

    return ResponseEntity.ok(users);
  }

  @PostMapping("/set-manager")
  @PreAuthorize("hasAnyRole('ADMIN','HR')")
  public ResponseEntity<Void> getUser(@RequestBody SetManagerRequest request){
    userService.setManager(request.email(), request.managerEmail());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/set-start-date")
  @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
  public ResponseEntity<Void> setStartDate(@RequestBody SetEmploymentStartDate request){
    userService.setEmploymentStartDate(request.email(), request.date());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/set-end-date")
  @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
  public ResponseEntity<Void> setEndDate(@RequestBody SetEmploymentEndDate request){
    userService.setEmploymentEndDate(request.email(), request.date());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/set-active")
  @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
  public ResponseEntity<Void> setActive(@RequestBody SetActive request){
    userService.setIsActive(request.email(), request.active());
    return ResponseEntity.ok().build();
  }
}
