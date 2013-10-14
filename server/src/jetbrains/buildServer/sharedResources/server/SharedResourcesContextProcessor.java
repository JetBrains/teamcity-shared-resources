package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesContextProcessor implements BuildStartContextProcessor {

  @NotNull
  private static final Logger log = Logger.getInstance(SharedResourcesContextProcessor.class.getName());

  @NotNull
  private final Object o = new Object();

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final Resources myResources;

  @NotNull
  private final LocksStorage myLocksStorage;

  @NotNull
  private final RunningBuildsManager myRunningBuildsManager;

  public SharedResourcesContextProcessor(@NotNull final SharedResourcesFeatures features,
                                         @NotNull final Locks locks,
                                         @NotNull final Resources resources,
                                         @NotNull final LocksStorage locksStorage,
                                         @NotNull final RunningBuildsManager runningBuildsManager) {
    myFeatures = features;
    myLocks = locks;
    myResources = resources;
    myLocksStorage = locksStorage;
    myRunningBuildsManager = runningBuildsManager;
  }


  @Override
  public void updateParameters(@NotNull final BuildStartContext context) {
    final SRunningBuild build = context.getBuild();
    final SBuildType myType = build.getBuildType();
    final String projectId = build.getProjectId();
    if (myType != null && projectId != null) {
      if (myFeatures.featuresPresent(myType)) {
        final Map<String, Lock> locks = myLocks.fromBuildPromotionAsMap(((BuildPromotionEx)build.getBuildPromotion()));
        if (!locks.isEmpty()) {
          processCustomLocks(context, build, projectId, locks);
        }
      }
    }
  }

  private void processCustomLocks(@NotNull final BuildStartContext context,
                                  @NotNull final SRunningBuild build,
                                  @NotNull final String projectId,
                                  @NotNull final Map<String, Lock> locks) {
    final Map<Lock, String> myTakenValues = initTakenValues(locks.values());
    // get custom resources from our locks
    final Map<String, CustomResource> myCustomResources = getCustomResources(projectId, locks);
    synchronized (o) {
      // decide whether we need to resolve values
      if (!myCustomResources.isEmpty()) {
        final Map<String, Set<String>> usedValues = collectTakenValuesFromRuntime(locks);
        for (Map.Entry<String, CustomResource> entry: myCustomResources.entrySet()) {
          if (entry.getValue().isEnabled()) {
            // get value space for current resources
            final List<String> values = new ArrayList<String>(entry.getValue().getValues());
            final String key = entry.getKey();
            // remove used values
            values.removeAll(usedValues.get(key));
            if (!values.isEmpty()) {
              final Lock currentLock = locks.get(key);
              final String paramName = myLocks.asBuildParameter(currentLock);
              String currentValue;
              if (LockType.READ.equals(currentLock.getType())) {
                currentValue = currentLock.getValue().equals("") ? values.iterator().next() : currentLock.getValue();
                // todo: add support of multiple values per lock in custom storage
                myTakenValues.put(currentLock, currentValue);
              } else {
                // we can only have one write lock on each resource at any given time
                // no need to store all values, as lock is exclusive
                currentValue = StringUtil.join(values, ";");
              }
              context.addSharedParameter(paramName, currentValue);
            } else {
              // throw exception?
              log.warn("Unable to assign value to lock [" + key + "] for build with id [" + build.getBuildId() + "]");
            }
          }
        }
      }
      myLocksStorage.store(build, myTakenValues);
    }
  }

  /**
   * Collects acquired values for all locks from runtime
   *
   * @param locks locks required by current build
   * @return map of locks and taken values
   */
  @NotNull
  private Map<String, Set<String>> collectTakenValuesFromRuntime(@NotNull final Map<String, Lock> locks) {
    final List<SRunningBuild> runningBuilds = myRunningBuildsManager.getRunningBuilds();
    final Map<String, Set<String>> usedValues = new HashMap<String, Set<String>>();
    for (String name: locks.keySet()) {
      usedValues.put(name, new HashSet<String>());
    }
    // collect taken values from runtime
    for (SRunningBuild runningBuild: runningBuilds) {
      Map<String, Lock> locksInRunningBuild = myLocksStorage.load(runningBuild);
      for (Lock l: locks.values()) {
        Lock runningLock = locksInRunningBuild.get(l.getName());
        if (runningLock != null) {
          String value = runningLock.getValue();
          if (!"".equals(value)) {
            usedValues.get(l.getName()).add(value);
          }
        }
      }
    }
    return usedValues;
  }

  /**
   * Determines, what locks are acquired on custom resources
   *
   * @param projectId id of current project
   * @param locks locks required
   * @return collection of resources among required locks that are custom
   */
  @NotNull
  private Map<String, CustomResource> getCustomResources(@NotNull final String projectId,
                                                         @NotNull final Map<String, Lock> locks) {
    final Map<String, CustomResource> myCustomResources = new HashMap<String, CustomResource>();
    final Map<String, Resource> resourceMap = myResources.asMap(projectId);
    for (Map.Entry<String, Lock> entry: locks.entrySet()) {
      final Resource r = resourceMap.get(entry.getKey());
      if (ResourceType.CUSTOM.equals(r.getType())) {
        myCustomResources.put(r.getName(), (CustomResource)r);
      }
    }
    return myCustomResources;
  }

  @NotNull
  private Map<Lock, String> initTakenValues(@NotNull final Collection<Lock> myLocks) {
    final Map<Lock, String> result = new HashMap<Lock, String>();
    for (Lock lock: myLocks) {
      result.put(lock, "");
    }
    return result;
  }
}
