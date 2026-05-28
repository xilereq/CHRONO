package pl.chrono.reportmanager.generator;

public interface I_CsvGenerator<T> {
  byte[] generateCsv(T reportData);

  default String formatSeconds(Long totalSeconds) {
    if (totalSeconds == null) return "0:00";
    long absSeconds = Math.abs(totalSeconds);
    long hours = absSeconds / 3600;
    long minutes = absSeconds % 3600 / 60;
    String sign = totalSeconds < 0 ? "-" : "";
    return String.format("%s%d:%02d", sign, hours, minutes);
  }
}
