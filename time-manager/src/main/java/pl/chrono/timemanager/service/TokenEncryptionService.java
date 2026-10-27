package pl.chrono.timemanager.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.chrono.timemanager.exception.GoogleCalendarException;

@Service
public class TokenEncryptionService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final String KEY_ALGORITHM = "AES";
  private static final int IV_LENGTH_BYTES = 12;
  private static final int KEY_LENGTH_BYTES = 32;
  private static final int TAG_LENGTH_BITS = 128;

  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${chrono.google.token-encryption-key:}")
  private String encryptionKey;

  public String encrypt(String plainText) {
    SecretKey secretKey = resolveSecretKey();
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

      ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
      byteBuffer.put(iv);
      byteBuffer.put(cipherText);
      return Base64.getEncoder().encodeToString(byteBuffer.array());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to encrypt token", e);
    }
  }

  public String decrypt(String encryptedText) {
    SecretKey secretKey = resolveSecretKey();
    try {
      byte[] decoded = Base64.getDecoder().decode(encryptedText);
      if (decoded.length <= IV_LENGTH_BYTES) {
        throw new IllegalArgumentException("Encrypted token payload is too short");
      }

      ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
      byte[] iv = new byte[IV_LENGTH_BYTES];
      byteBuffer.get(iv);
      byte[] cipherText = new byte[byteBuffer.remaining()];
      byteBuffer.get(cipherText);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] plainBytes = cipher.doFinal(cipherText);
      return new String(plainBytes, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException("Failed to decrypt token", e);
    }
  }

  private SecretKey resolveSecretKey() {
    if (encryptionKey == null || encryptionKey.isBlank()) {
      throw new GoogleCalendarException.NotConfigured();
    }
    byte[] decodedKey;
    try {
      decodedKey = Base64.getDecoder().decode(encryptionKey.trim());
    } catch (IllegalArgumentException e) {
      throw new GoogleCalendarException.NotConfigured();
    }
    if (decodedKey.length != KEY_LENGTH_BYTES) {
      throw new GoogleCalendarException.NotConfigured();
    }
    return new SecretKeySpec(decodedKey, KEY_ALGORITHM);
  }
}
