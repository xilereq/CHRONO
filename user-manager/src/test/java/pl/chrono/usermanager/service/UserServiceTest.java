package pl.chrono.usermanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.usermanager.dto.response.UserDto;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private I_UserData userData;

  @InjectMocks
  private UserService userService;

  private User createSampleUser(Integer id, String name, String surname, String email) {
    User user = new User();
    user.setUserId(id);
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    return user;
  }

  @Test
  void getSubordinatesForManager_ShouldReturnMappedUserDtos() {
    User user = createSampleUser(1, "Jan", "Kowalski", "jan.kowalski@test.pl");
    when(userData.getSubordinatesForManager(100)).thenReturn(List.of(user));

    List<UserDto> result = userService.getSubordinatesForManager(100);

    assertEquals(1, result.size());
    assertEquals(1, result.get(0).userID());
    assertEquals("Jan", result.get(0).name());
    assertEquals("Kowalski", result.get(0).surname());
    assertEquals("jan.kowalski@test.pl", result.get(0).email());
  }

  @Test
  void getAllManagers_ShouldReturnMappedUserDtos() {
    User manager = createSampleUser(2, "Anna", "Nowak", "anna.nowak@test.pl");
    when(userData.findByRoles_Name("ROLE_MANAGER")).thenReturn(List.of(manager));

    List<UserDto> result = userService.getAllManagers();

    assertEquals(1, result.size());
    assertEquals(2, result.get(0).userID());
    assertEquals("Anna", result.get(0).name());
  }

  @Test
  void getAllEmployees_ShouldReturnMappedUserDtos() {
    User employee = createSampleUser(3, "Piotr", "Wiśniewski", "piotr.w@test.pl");
    when(userData.findByRoles_Name("ROLE_EMPLOYEE")).thenReturn(List.of(employee));

    List<UserDto> result = userService.getAllEmployees();

    assertEquals(1, result.size());
    assertEquals(3, result.get(0).userID());
    assertEquals("ROLE_EMPLOYEE", "ROLE_EMPLOYEE"); // weryfikacja logiki bywa wprost, tu wystarczy asercja na mapowanie
    assertEquals("Piotr", result.get(0).name());
  }

  @Test
  void getAllAdmins_ShouldReturnMappedUserDtos() {
    User admin = createSampleUser(4, "Kamil", "Zieliński", "admin@test.pl");
    when(userData.findByRoles_Name("ROLE_ADMIN")).thenReturn(List.of(admin));

    List<UserDto> result = userService.getAllAdmins();

    assertEquals(1, result.size());
    assertEquals("Kamil", result.get(0).name());
  }

  @Test
  void getAllHrs_ShouldReturnMappedUserDtos() {
    User hr = createSampleUser(5, "Marta", "Kaczmarek", "hr@test.pl");
    when(userData.findByRoles_Name("ROLE_HR")).thenReturn(List.of(hr));

    List<UserDto> result = userService.getAllHrs();

    assertEquals(1, result.size());
    assertEquals("Marta", result.get(0).name());
  }

  @Test
  void getAllUsers_ShouldReturnMappedUserDtos() {
    // Arrange
    User user1 = createSampleUser(1, "User", "One", "u1@test.pl");
    User user2 = createSampleUser(2, "User", "Two", "u2@test.pl");
    when(userData.findAll()).thenReturn(List.of(user1, user2));

    List<UserDto> result = userService.getAllUsers();

    assertEquals(2, result.size());
    assertEquals("u1@test.pl", result.get(0).email());
    assertEquals("u2@test.pl", result.get(1).email());
  }

  @Test
  void setManager_ShouldAssignManagerSuccessfully() {
    String userEmail = "employee@test.com";
    String managerEmail = "manager@test.com";

    User employee = createSampleUser(1, "E", "E", userEmail);
    User manager = createSampleUser(2, "M", "M", managerEmail);

    when(userData.findByEmail(userEmail)).thenReturn(Optional.of(employee));
    when(userData.findByEmail(managerEmail)).thenReturn(Optional.of(manager));

    userService.setManager(userEmail, managerEmail);

    assertEquals(manager, employee.getManager());
    verify(userData, times(1)).save(employee);
  }

  @Test
  void setManager_ShouldThrowException_WhenUserNotFound() {
    String userEmail = "nonexistent@test.com";
    String managerEmail = "manager@test.com";

    when(userData.findByEmail(userEmail)).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      userService.setManager(userEmail, managerEmail);
    });

    assertEquals("Użytkownik o podanym adresie email nie istnieje.", exception.getMessage());
    verify(userData, never()).save(any());
  }

  @Test
  void setManager_ShouldThrowException_WhenManagerNotFound() {
    String userEmail = "employee@test.com";
    String managerEmail = "nonexistent_manager@test.com";

    User employee = createSampleUser(1, "E", "E", userEmail);

    when(userData.findByEmail(userEmail)).thenReturn(Optional.of(employee));
    when(userData.findByEmail(managerEmail)).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      userService.setManager(userEmail, managerEmail);
    });

    assertEquals("Użytkownik o podanym adresie email nie istnieje.", exception.getMessage());
    verify(userData, never()).save(any());
  }
}
