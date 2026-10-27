package pl.chrono.reportmanager.dto;

public enum ReportFormat {
  CSV("csv", "text/csv; charset=UTF-8"),
  PDF("pdf", "application/pdf"),
  XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final String extension;
  private final String mediaType;

  ReportFormat(String extension, String mediaType) {
    this.extension = extension;
    this.mediaType = mediaType;
  }

  public String getExtension() {
    return extension;
  }

  public String getMediaType() {
    return mediaType;
  }

  public String fileName(String baseName) {
    return baseName + "." + extension;
  }
}
