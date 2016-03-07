/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.model;

import org.jetbrains.annotations.NotNull;

/**
 * Named lock implementation
 *
 * @author Oleg Rybak
 */
public class Lock {

  @NotNull
  private static final String NO_VALUE = "";

  /**
   * Name of the lock
   */
  @NotNull
  private final String myName;

  /**
   * Type of the lock
   */
  @NotNull
  private final LockType myType;

  /**
   * Value of the lock. Represents instance of shared resource locked
   */
  @NotNull
  private final String myValue;

  public Lock(@NotNull final String name, @NotNull final LockType type, @NotNull final String value) {
    myName = name;
    myType = type;
    myValue = value;
  }

  public Lock(@NotNull final String name, @NotNull final LockType type) {
    this(name, type, NO_VALUE);
  }


  /**
   * Creates copy of lock with given value
   *
   * @param from lock definition
   * @param value taken value
   * @return copy of combined lock definition and custom value
   */
  public static Lock createFrom(@NotNull final Lock from, @NotNull final String value) {
    return new Lock(from.getName(), from.getType(), value);
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public LockType getType() {
    return myType;
  }

  @NotNull
  public String getValue() {
    return myValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Lock)) return false;
    Lock lock = (Lock) o;
    return myName.equals(lock.myName)
            && myType == lock.myType
            && myValue.equals(lock.myValue);

  }

  @Override
  public int hashCode() {
    int result = myName.hashCode();
    result = 31 * result + myType.hashCode();
    result = 31 * result + myValue.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Lock{" +
            "myName='" + myName + '\'' +
            ", myType=" + myType +
            ", myValue='" + myValue + '\'' +
            '}';
  }
}
