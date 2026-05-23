package pl.chrono.usermanager.dto.response;

public record UserDto(
    Integer userID,
    String name,
    String surname,
    String email
) {}
