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

  private static final Random r = new Random(System.currentTimeMillis());

  /**
   * Generates lock representation as it is in build parameters
   * @return lock representation as a parameter
   */
  public static String generateLockAsParam(LockType type, String name) {
    return SharedResourcesPluginConstants.LOCK_PREFIX + type.getName() + "." + name;
  }

  /**
   * Generates random system parameter
   * @return random system parameter as String
   */
  public static String generateRandomSystemParam() {
    return "system." + UUID.randomUUID().toString();
  }

  /**
   * Generates serialized version of a lock as it is stored in feature parameter
   * @return serialized lock
   */
  public static String generateSerializedLock() {
    return r.nextInt(100000) + " " + (r.nextInt(1000) % 2 == 0 ? "readLock" : "writeLock");
  }
}
