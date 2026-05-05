package pl.chrono.workforceapi.controller;

import lombok.RequiredArgsConstructor;
import pl.chrono.usermanager.service.AuthService;

@RequiredArgsConstructor
public abstract class BaseController {
  protected final AuthService authService;

  protected Integer extractId(String header) {
    return authService.extractUserIdFromToken(header.substring(7));
  }
}
