package pl.chrono.timemanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.chrono.timemanager.exception.GoogleCalendarException;

class TokenEncryptionServiceTest {

  private static final String VALID_BASE64_KEY = Base64.getEncoder()
      .encodeToString("0123456789abcdef0123456789abcdef".getBytes());

  private TokenEncryptionService tokenEncryptionService;

  @BeforeEach
  void setUp() {
    tokenEncryptionService = new TokenEncryptionService();
  }

  @Test
  void encryptAndDecryptRoundTripTest() {
    ReflectionTestUtils.setField(tokenEncryptionService, "encryptionKey", VALID_BASE64_KEY);
    String originalToken = "1//0gxyz-refresh-token-example";

    String encrypted = tokenEncryptionService.encrypt(originalToken);
    String decrypted = tokenEncryptionService.decrypt(encrypted);

    assertNotNull(encrypted);
    assertNotEquals(originalToken, encrypted);
    assertEquals(originalToken, decrypted);
  }

  @Test
  void encryptWithMissingKeyThrowsNotConfiguredTest() {
    ReflectionTestUtils.setField(tokenEncryptionService, "encryptionKey", "");

    assertThrows(
        GoogleCalendarException.NotConfigured.class,
        () -> tokenEncryptionService.encrypt("token")
    );
  }

  @Test
  void decryptWithNullKeyThrowsNotConfiguredTest() {
    ReflectionTestUtils.setField(tokenEncryptionService, "encryptionKey", null);

    assertThrows(
        GoogleCalendarException.NotConfigured.class,
        () -> tokenEncryptionService.decrypt("encrypted")
    );
  }

  @Test
  void encryptWithInvalidBase64KeyThrowsNotConfiguredTest() {
    ReflectionTestUtils.setField(tokenEncryptionService, "encryptionKey", "%%%not-base64%%%");

    assertThrows(
        GoogleCalendarException.NotConfigured.class,
        () -> tokenEncryptionService.encrypt("token")
    );
  }

  @Test
  void encryptWithWrongLengthKeyThrowsNotConfiguredTest() {
    String shortKey = Base64.getEncoder().encodeToString("short-key".getBytes());
    ReflectionTestUtils.setField(tokenEncryptionService, "encryptionKey", shortKey);

    assertThrows(
        GoogleCalendarException.NotConfigured.class,
        () -> tokenEncryptionService.encrypt("token")
    );
  }
}
