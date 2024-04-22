package flightapp;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;



public class PasswordUtils {
  /**
   * Generates a cryptographically secure salted password.
   */
  public static byte[] hashPassword(String password) {
    byte[] salt = generateSalt();
    byte[] saltedHash = generateSaltedPassword(password, salt);

    // Combine the salt and the salted hash into a single array that can be written to the database
    byte[] hash = new byte[salt.length + saltedHash.length];
    System.arraycopy(salt, 0, hash, 0, salt.length);
    System.arraycopy(saltedHash, 0, hash, salt.length, saltedHash.length);
    
    return hash;
  }

  /**
   * Verifies whether the plaintext password can be hashed to provided salted hashed password.
   */
  public static boolean plaintextMatchesHash(String plaintext, byte[] saltedHashed) {
 
    byte[] salt = new byte[SALT_LENGTH];
    System.arraycopy(saltedHashed, 0, salt, 0, SALT_LENGTH);

    int hashLen = saltedHashed.length - SALT_LENGTH;
    byte[] hashed = new byte[hashLen];
    System.arraycopy(saltedHashed, SALT_LENGTH, hashed, 0, hashLen);

    return Arrays.equals(hashed, generateSaltedPassword(plaintext, salt));
  }
  
  private static final int HASH_STRENGTH = 65536;
  private static final int KEY_LENGTH = 128;
  private static final int SALT_LENGTH = 16;

  /**
   * Generate a small bit of randomness to serve as a password "salt"
   */
  static byte[] generateSalt() {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[SALT_LENGTH];
    random.nextBytes(salt);
    return salt;
  }

  /**
   * Uses the provided salt to generate a cryptographically-secure hash of the provided password.
   * The resultant byte array should be KEY_LENGTH bytes long.
   */
  static byte[] generateSaltedPassword(String password, byte[] salt)
    throws IllegalStateException {
    // Specify the hash parameters, including the salt
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt,
                                  HASH_STRENGTH, KEY_LENGTH * 8 /* length in bits */);

    // Hash the whole thing
    SecretKeyFactory factory = null;
    byte[] hash = null; 
    try {
      factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      hash = factory.generateSecret(spec).getEncoded();
      return hash;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new IllegalStateException();
    }
  }

}
