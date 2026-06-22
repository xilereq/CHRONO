package pl.chrono.usermanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.usermanager.api.I_UserService;
import pl.chrono.usermanager.dto.response.UserDto;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements I_UserService {

  private final I_UserData userData;


  @Override
  @Transactional(readOnly = true)
  public List<UserDto> getSubordinatesForManager(Integer managerID) {
    return userData.getSubordinatesForManager(managerID)
        .stream()
        .map(user -> new UserDto(user.getUserId(), user.getName(), user.getSurname(), user.getEmail()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserDto> getAllManagers() {
    return userData.findByRoles_Name("ROLE_MANAGER")
        .stream()
        .map(user -> new UserDto(user.getUserId(), user.getName(), user.getSurname(), user.getEmail()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserDto> getAllEmployees() {
    return userData.findByRoles_Name("ROLE_EMPLOYEE")
        .stream()
        .map(user -> new UserDto(user.getUserId(), user.getName(), user.getSurname(), user.getEmail()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserDto> getAllAdmins() {
    return userData.findByRoles_Name("ROLE_ADMIN")
        .stream()
        .map(user -> new UserDto(user.getUserId(), user.getName(), user.getSurname(), user.getEmail()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserDto> getAllHrs() {
    return userData.findByRoles_Name("ROLE_HR")
        .stream()
        .map(user -> new UserDto(user.getUserId(), user.getName(), user.getSurname(), user.getEmail()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserDto> getAllUsers() {
    return userData.findAll()
        .stream()
        .map(user -> new UserDto(user.getUserId(), user.getName(), user.getSurname(), user.getEmail()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void setManager(String email, String managerEmail) {
    User user = userData.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Użytkownik o podanym adresie email nie istnieje."));
    User manager = userData.findByEmail(managerEmail)
        .orElseThrow(() -> new IllegalArgumentException("Użytkownik o podanym adresie email nie istnieje."));
    user.setManager(manager);

    userData.save(user);
  }

  @Override
  @Transactional
  public void setEmploymentStartDate(String email, LocalDate date){
    User user = userData.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Użytkownik o podanym adresie email nie istnieje."));
    user.setEmploymentStartDate(date);

    userData.save(user);
  }

  @Override
  @Transactional
  public void setEmploymentEndDate(String email, LocalDate date){
    User user = userData.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Użytkownik o podanym adresie email nie istnieje."));
    if(!date.isAfter(user.getEmploymentStartDate())){
      throw new IllegalArgumentException("Data zakończenia zatrudnienia musi być późniejsza niż data rozpoczęcia.");
    }

    user.setEmploymentEndDate(date);
    userData.save(user);
  }

  @Override
  @Transactional
  public void setIsActive(String email, boolean active){
    User user = userData.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Użytkownik o podanym adresie email nie istnieje."));

    user.setActive(active);
    userData.save(user);
  }
}
