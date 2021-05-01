package graphql.kickstart.servlet

import spock.lang.Specification

class PartIOExceptionTest extends Specification {

  def "constructs"() {
    when:
    def e = new PartIOException("some message", new IOException())
    then:
    e instanceof RuntimeException
  }
}
