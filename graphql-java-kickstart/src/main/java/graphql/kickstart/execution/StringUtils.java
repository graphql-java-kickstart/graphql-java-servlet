package graphql.kickstart.execution;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class StringUtils {

  static boolean isNotEmpty(CharSequence cs) {
    return !isEmpty(cs);
  }

  static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.length() == 0;
  }
}
