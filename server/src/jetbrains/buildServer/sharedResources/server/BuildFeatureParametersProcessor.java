package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockParser;
import jetbrains.buildServer.sharedResources.util.FeatureUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.RESOURCE_PARAM_KEY;

/**
 * Created with IntelliJ IDEA.
 * Date: 13.09.12
 * Time: 19:14
 *
 * @author Oleg Rybak
 */
public class BuildFeatureParametersProcessor implements PropertiesProcessor {

  static final String ERROR_EMPTY = "Resource name must not be empty";

  static final String ERROR_NON_UNIQUE = "Non unique resource names found";

  static final String ERROR_WRONG_FORMAT = "Wrong format found at line(s): ";

  private final SharedResourcesPluginConstants c = new SharedResourcesPluginConstants();

  private void checkNotEmpty(@NotNull final Collection<String> strings,
                             @NotNull final Collection<InvalidProperty> res) {
    if (strings.isEmpty()) {
      res.add(new InvalidProperty(RESOURCE_PARAM_KEY, ERROR_EMPTY));
    }
  }

  private void checkValid(@NotNull final Collection<String> strings,
                          @NotNull final Collection<InvalidProperty> res) {
    List<Integer> invalidLines = new ArrayList<Integer>();
    Set<Lock> locks = new HashSet<Lock>();
    int line = 1;
    for (String str: strings) {
      Lock lock = LockParser.parse(str);
      if (lock == null) {
        invalidLines.add(line);
      } else {
        locks.add(lock);
      }
      line++;
    }
    if (!invalidLines.isEmpty()) {
      res.add(new InvalidProperty(RESOURCE_PARAM_KEY, ERROR_WRONG_FORMAT + Arrays.toString(invalidLines.toArray())));
    } else {
      if (locks.size() < strings.size()) {
        res.add(new InvalidProperty(RESOURCE_PARAM_KEY, ERROR_NON_UNIQUE));
      }
    }
  }

  public Collection<InvalidProperty> process(Map<String, String> properties) {
    final Collection<InvalidProperty> result = new ArrayList<InvalidProperty>();
    if (properties != null) {
      final Collection<String> resourceNames = FeatureUtil.toCollection(properties.get(RESOURCE_PARAM_KEY));
      checkNotEmpty(resourceNames, result);
      if (result.isEmpty()) {
        checkValid(resourceNames, result);
      }
      properties.put(RESOURCE_PARAM_KEY, FeatureUtil.fromCollection(resourceNames));
    }
    return result;
  }
}
