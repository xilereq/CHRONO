package pl.chrono.timemanager.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PolicyMapperTest {

  private final PolicyMapper mapper = Mappers.getMapper(PolicyMapper.class);

  @Test
  void mapWorkingDaysNullListTest() {
    Set<DayOfWeek> result = mapper.mapWorkingDays(null);
    assertThat(result).isEmpty();
  }

  @Test
  void mapWorkingDayEmptyListTest() {
    Set<DayOfWeek> result = mapper.mapWorkingDays(Collections.emptyList());
    assertThat(result).isEmpty();
  }

  @Test
  void mapWorkingDaysValidListTest() {
    List<String> input = Arrays.asList(" monday ", "TUESDAY", "wednesday");

    Set<DayOfWeek> result = mapper.mapWorkingDays(input);

    assertThat(result).hasSize(3)
        .contains(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY);
  }

  @Test
  void mapWorkingDaysExceptionTest() {
    List<String> input = List.of("NOT_A_DAY");

    assertThrows(IllegalArgumentException.class, () -> mapper.mapWorkingDays(input));
  }
}
