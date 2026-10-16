package pl.chrono.timemanager.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.chrono.timemanager.dto.response.ExternalHolidayDto;
import pl.chrono.timemanager.exception.HolidayException;
import pl.chrono.timemanager.ports.I_ExternalHolidayProvider;

@Component
public class NagerHolidayAdapter implements I_ExternalHolidayProvider {
  private static final String API_URL = "https://date.nager.at/api/v3/PublicHolidays/%d/%s";
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public NagerHolidayAdapter() {
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  @Override
  public List<ExternalHolidayDto> fetchHolidays(int year, String countryCode) {
    String url = String.format(API_URL, year, countryCode);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 404) {
        throw new HolidayException.InvalidCountryCode(countryCode);
      }
      if (response.statusCode() == 400) {
        throw new HolidayException.InvalidYear(year);
      }
      if (response.statusCode() != 200) {
        throw new HolidayException.ExternalApiError("HTTP status " + response.statusCode());
      }

      List<NagerHolidayItem> items = objectMapper.readValue(
          response.body(), new TypeReference<List<NagerHolidayItem>>() {});

      return items.stream()
          .filter(NagerHolidayItem::global)
          .map(item -> new ExternalHolidayDto(
              item.localName(),
              item.name(),
              item.date(),
              false
          ))
          .toList();
    } catch (IOException e) {
      throw new HolidayException.ExternalApiError(e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new HolidayException.ExternalApiError("Request interrupted");
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record NagerHolidayItem(
      LocalDate date,
      String localName,
      String name,
      boolean global
  ) {
  }
}
