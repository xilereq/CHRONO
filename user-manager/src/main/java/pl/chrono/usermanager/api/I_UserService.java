package pl.chrono.usermanager.api;

import pl.chrono.usermanager.dto.response.UserDto;
import java.util.List;

public interface I_UserService {

  List<UserDto> getSubordinatesForManager(Integer managerId);
}
