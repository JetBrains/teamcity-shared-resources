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

  /**
   * Name of the lock
   */
  private final String myName;

  /**
   * Type of the lock
   */
  private final LockType myType;

  public Lock(@NotNull String name, @NotNull LockType type) {
    myName = name;
    myType = type;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public LockType getType() {
    return myType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Lock)) return false;
    Lock lock = (Lock) o;
    return myName.equals(lock.myName) && myType == lock.myType;
  }

  @Override
  public int hashCode() {
    int result = myName.hashCode();
    result = 31 * result + myType.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Lock {" +
            "name ='" + myName + '\'' +
            ", type ='" + myType.getName() + '\'' +
            '}';
  }
}
