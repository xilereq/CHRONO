package pl.chrono.usermanager.dto.response;

import java.util.List;

public record TeamTreeNodeDto(
    Integer userId,
    String name,
    String surname,
    String email,
    List<String> roles,
    boolean active,
    int directSubordinates,
    int totalSubordinates,
    List<TeamTreeNodeDto> subordinates
) {
}
