

package jetbrains.buildServer.sharedResources.server.feature;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.sharedResources.server.feature.FeatureParams.LOCKS_FEATURE_PARAM_KEY;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class SharedResourcesFeatureImpl implements SharedResourcesFeature {

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final SBuildFeatureDescriptor myDescriptor;

  @NotNull
  private final Map<String, Lock> myLockedResources;


  SharedResourcesFeatureImpl(@NotNull final Locks locks,
                             @NotNull final SBuildFeatureDescriptor descriptor) {
    myLocks = locks;
    myDescriptor = descriptor;
    myLockedResources = myLocks.fromFeatureParameters(myDescriptor);
  }

  @NotNull
  @Override
  public Map<String, Lock> getLockedResources() {
    return Collections.unmodifiableMap(myLockedResources);
  }

  @Override
  public boolean updateLock(@NotNull final BuildTypeSettings settings,
                            @NotNull final String oldName,
                            @NotNull final String newName) {
    boolean result = false;
    final Lock lock = myLockedResources.remove(oldName);
    if (lock != null) {
      // save its type
      result = true;
      final LockType lockType = lock.getType();
      final String lockValue = lock.getValue();
      // add lock with new resource name and saved type
      myLockedResources.put(newName, new Lock(newName, lockType, lockValue));
      // serialize locks
      final String locksAsString = myLocks.asFeatureParameter(myLockedResources.values());
      // update build feature parameters
      final Map<String, String> newParams = new HashMap<>(myDescriptor.getParameters());
      newParams.put(LOCKS_FEATURE_PARAM_KEY, locksAsString);
      // update build feature
      settings.updateBuildFeature(myDescriptor.getId(), myDescriptor.getType(), newParams);
    }
    return result;
  }

}