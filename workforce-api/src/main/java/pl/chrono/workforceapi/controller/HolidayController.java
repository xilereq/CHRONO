package pl.chrono.workforceapi.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.timemanager.dto.request.CreateHolidayRequest;
import pl.chrono.timemanager.dto.response.ExternalHolidayDto;
import pl.chrono.timemanager.dto.response.HolidayDto;
import pl.chrono.timemanager.service.HolidayService;
import pl.chrono.usermanager.service.AuthService;

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

  @GetMapping("/external")
  @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
  public ResponseEntity<List<ExternalHolidayDto>> previewExternalHolidays(
      @RequestParam int year,
      @RequestParam(defaultValue = "PL") String countryCode) {
    return ResponseEntity.ok(holidayService.previewExternalHolidays(year, countryCode));
  }

  @PostMapping("/batch")
  @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
  public ResponseEntity<List<HolidayDto>> createHolidaysBatch(
      @Valid @RequestBody List<@Valid CreateHolidayRequest> requests) {
    return ResponseEntity.ok(holidayService.createHolidaysBatch(requests));
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
