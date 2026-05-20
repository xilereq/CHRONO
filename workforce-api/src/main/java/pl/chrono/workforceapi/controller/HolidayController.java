package pl.chrono.workforceapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.service.HolidayService;
import pl.chrono.usermanager.service.AuthService;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController extends BaseController {

  private final HolidayService holidayService;

  public HolidayController(AuthService authService, HolidayService holidayService) {
    super(authService);
    this.holidayService = holidayService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<HolidayDto> createHoliday(@Valid @RequestBody CreateHolidayRequest request) {
    HolidayDto createdHoliday = holidayService.createHoliday(request);
    return ResponseEntity.ok(createdHoliday);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<List<HolidayDto>> getAllHolidays() {
    return ResponseEntity.ok(holidayService.getAllHolidays());
  }

  @GetMapping("/{holidayId}")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<HolidayDto> getHolidayById(@PathVariable Integer holidayId) {
    return ResponseEntity.ok(holidayService.getHolidayById(holidayId));
  }

  @DeleteMapping("/{holidayId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteHoliday(@PathVariable Integer holidayId) {
    holidayService.deleteHoliday(holidayId);
    return ResponseEntity.noContent().build();
  }
}
