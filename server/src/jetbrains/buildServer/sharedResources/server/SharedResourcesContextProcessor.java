package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesContextProcessor implements BuildStartContextProcessor {


  private static final Logger log = Logger.getInstance(BuildStartContextProcessor.class.getName());

  // where is it called?
  @NotNull
  private final SharedResourcesFeatures myFeatures;

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final Resources myResources;

  public SharedResourcesContextProcessor(@NotNull final SharedResourcesFeatures features,
                                         @NotNull final Locks locks,
                                         @NotNull final Resources resources) {
    myFeatures = features;
    myLocks = locks;
    myResources = resources;
  }


  @Override
  public void updateParameters(@NotNull final BuildStartContext context) {
    final SRunningBuild build = context.getBuild();
    final SBuildType myType = build.getBuildType();
    final String projectId = build.getProjectId();
    if (myType != null && projectId != null) {
      if (myFeatures.featuresPresent(myType)) {
        log.info("SRCP :>> features present");
        final Collection<Lock> locks = myLocks.fromBuildParameters(
                ((BuildPromotionEx)build.getBuildPromotion()).getParametersProvider().getAll());
        if (!locks.isEmpty()) {
          log.info("SRCP :>> found some locks");
          // custom resources required by this build
          final Map<String, Resource> myCustomResources = new HashMap<String, Resource>();
          final Map<String, Resource> resourceMap = myResources.asMap(projectId);
          for (Lock lock: locks) {
            final Resource r = resourceMap.get(lock.getName());
            if (ResourceType.CUSTOM.equals(r.getType())) {
              log.info("SRCP :>> custom resource found! [" + r.getName() + "]");
              myCustomResources.put(r.getName(), r);
            }
          }
          if (!myCustomResources.isEmpty()) {
            log.info("SRCP :>> my build contains custom resources. must provide value for param");
            //
            // for each of the resource
            // generate param name
            // BEGIN critical section

            // gather param values
            // determine value to use
            // if no value? RuntimeException???

            // END critical section

            final Collection<? extends  SRunnerContext> contexts =  context.getRunnerContexts();
            for (SRunnerContext c: contexts) {

            }
          }
        } else {
          log.info("SRCP :>> no locks found");
        }
      } else {
        log.info("SRCP :>> no features present");
      }
    }
    // collect locks that are taken
    // determine value of lock to pass
  }

}
