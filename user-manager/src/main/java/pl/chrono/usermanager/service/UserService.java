package pl.chrono.usermanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.usermanager.api.I_UserService;
import pl.chrono.usermanager.dto.response.UserDto;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements I_UserService {

  private final I_UserData userData;

  @Override
  @Transactional(readOnly = true)
  public List<UserDto> getSubordinatesForManager(Integer managerID){
    return userData.getSubordinatesForManager(managerID)
        .stream()
        .map(user -> new UserDto(user.getUserId(), user.getName(), user.getSurname() ,user.getEmail()))
        .collect(Collectors.toList());
  }

}
