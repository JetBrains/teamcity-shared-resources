/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
@SuppressWarnings("UnusedShould")
public class TestUtils {

  /** For random size of collections */
  private static final Random R = new Random();

  /** Maximum size of collection */
  public static final int RANDOM_UPPER_BOUNDARY = 20;

  /** Random provider for other tests */
  public static int generateBoundedRandomInt(int max) {
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
