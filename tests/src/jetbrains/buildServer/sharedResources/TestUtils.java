

package jetbrains.buildServer.sharedResources;

import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.server.feature.Locks;

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
  private static int generateBoundedRandomInt(int max) {
    return 1 + R.nextInt(max);
  }

  /**
   * Generates lock representation as it is in build parameters
   * @return lock representation as a parameter
   */
  public static String generateLockAsBuildParam(String name, LockType type) {
    return Locks.LOCK_PREFIX + type.getName() + "." + name;
  }

  /**
   * Generates lock with random name and random type
   * @return randomly generated lock
   */
  public static Lock generateRandomLock() {
    final LockType[] allTypes = LockType.values();
    return new Lock(generateRandomName(), allTypes[generateBoundedRandomInt(1000) % allTypes.length]);
  }

  /**
   * Generates lock with random name, random type and random value
   * @return randomly generated lock with value
   */
  public static Lock generateRandomLockWithValue() {
    final LockType[] allTypes = LockType.values();
    return new Lock(generateRandomName(), allTypes[generateBoundedRandomInt(1000) % allTypes.length], generateRandomName());
  }

  /**
   * Generates random name that consists of letters, numbers and underscores
   * @return random name
   */
  public static String generateRandomName() {
    return UUID.randomUUID().toString();
  }
}