

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.sharedResources.model.Lock;
import org.jetbrains.annotations.NotNull;

/**
 * Interface {@code LocksStorage}
 *
 * Contains method definition for storage of taken locks
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface LocksStorage {

  /**
   * Stores taken locks for given build
   *  @param buildPromotion build promotion to store locks for
   * @param takenLocks taken locks for given build with values
   */
  void store(@NotNull final BuildPromotion buildPromotion, @NotNull final Map<Lock, String> takenLocks);

  /**
   * Loads taken locks
   *
   * @param buildPromotion build promotion to load locks for
   * @return collection of taken locks. Values are restored inside locks
   */
  @NotNull
  Map<String, Lock> load(@NotNull final BuildPromotion buildPromotion);

  /**
   * Checks, whether locks has been already stored
   *
   * @param buildPromotion build promotion to check for
   * @return {@code true} if locks has been stored inside build artifact
   * {@code false} otherwise
   */
  boolean locksStored(@NotNull final BuildPromotion buildPromotion);

}