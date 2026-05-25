package pl.chrono.usermanager.api;

import pl.chrono.usermanager.dto.response.UserDto;
import java.util.List;

public interface I_UserService {

  List<UserDto> getSubordinatesForManager(Integer managerId);

  List<UserDto> getAllManagers();

  List<UserDto> getAllEmployees();

  List<UserDto> getAllAdmins();

  List<UserDto> getAllHrs();
}
