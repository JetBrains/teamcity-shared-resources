package jetbrains.buildServer.sharedResources;

import jetbrains.buildServer.sharedResources.model.LockType;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Date: 02.10.12
 * Time: 15:56
 *
 * @author Oleg Rybak
 */
public class TestUtils {

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
}
