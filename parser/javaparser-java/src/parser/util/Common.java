// $Id$
// Created by Balint Kiss, 2017

package parser.util;

import org.eclipse.jdt.core.JavaCore;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Common utility and helper methods that can either be generally used or couldn't be categorized.
 */
public final class Common {

  /**
   * Disallow construction.
   */
  private Common() {}

  /**
   * Remove duplicates and return flattened array.
   * This method can accept array with any type.
   *
   * @param type
   * @param arr
   * @param <T>
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] flatten(final Class<T> type, final T[] arr) {
    Set<T> flattened = new HashSet<>();
    Collections.addAll(flattened, arr);
    return flattened.toArray((T[]) Array.newInstance(type, flattened.size()));
  }

  /**
   * Set the JDK source version level for Eclipse JDT options map.
   *
   * @param sourceLevel
   * @param options
   */
  public static void applySourceLevel(final String sourceLevel, Map<?, ?> options) {
    switch (sourceLevel) {    // Switch treats String objects as if we were using equals() method
      case "1.1":
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_1, options);
        break;

      case "1.2":
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_2, options);
        break;

      case "1.3":
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_3, options);
        break;

      case "1.4":
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, options);
        break;

      case "1.5": /* fallthrough */
      case "5":
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_5, options);
        break;

      case "1.6": /* fallthrough */
      case "6":
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
        break;

      case "1.7": /* fallthrough */
      case "7":   /* fallthrough */
      default:
        // Defaulting to 1.7
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
    }
  }
}
