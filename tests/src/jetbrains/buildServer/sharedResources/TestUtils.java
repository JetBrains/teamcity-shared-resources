package jetbrains.buildServer.sharedResources;

import jetbrains.buildServer.sharedResources.model.LockType;

import java.util.Random;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Date: 02.10.12
 * Time: 15:56
 *
 * @author Oleg Rybak
 */
public class TestUtils {

  /** For random size of collections */
  private static final Random R = new Random();

  /** Maximum size of collection */
  public static final int RANDOM_UPPER_BOUNDARY = 20;

  /** Random provider for other tests */
  public static int generateBoundedRandomInt() {
    return generateBoundedRandomInt(RANDOM_UPPER_BOUNDARY);
  }

  /** Random provider for other tests */
  public static int generateBoundedRandomInt(int max) {
    return 1 + R.nextInt(max);
  }

  /**
   * Generates lock representation as it is in build parameters
   * @return lock representation as a parameter
   */
  public static String generateLockAsParam(String name, LockType type) {
    return SharedResourcesPluginConstants.LOCK_PREFIX + type.getName() + "." + name;
  }

  /**
   * Generates random configuration parameter
   * @return random configuration parameter as String
   */
  public static String generateRandomConfigurationParam() {
    return "teamcity." + UUID.randomUUID().toString();
  }

  /**
   * Generates serialized version of a lock that has random type as it is stored in feature parameter
   * @return serialized lock
   */
  public static String generateSerializedLock() {
    final LockType[] allTypes = LockType.values();
    return generateSerializedLock(allTypes[generateBoundedRandomInt(1000) % allTypes.length]);
  }

  /**
   * Generates serialized version of a lock with specified type as it is stored in feature parameter
   * @param type type of generated lock
   * @return serialized lock
   */
  public static String generateSerializedLock(LockType type) {
    return generateBoundedRandomInt(100000) + " " + type;
  }



  /**
   * Generates random name that consists of letters, numbers and underscores
   * @return random name
   */
  public static String generateRandomName() {
    return "my_name_" + generateBoundedRandomInt(100000);
  }
}
