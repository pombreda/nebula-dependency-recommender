package netflix.nebula.dependency.recommender.provider

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class DependencyReportProviderSpec extends Specification {
    @Rule TemporaryFolder projectDir

    def 'dependency locks provide recommendations'() {
        setup:
        def recommender = new DependencyReportProvider()

        def file = projectDir.newFile()
        file << '''

            ------------------------------------------------------------
            Root project
            ------------------------------------------------------------

            archives - Configuration for archive artifacts.
            No dependencies

            compile - Compile classpath for source set 'main'.
            +--- org.codehaus.groovy:groovy-all:2.3.+ -> 2.3.8
            +--- com.fasterxml.jackson.core:jackson-databind:2.4.1.3

            default - Configuration for default artifacts.
            +--- org.codehaus.groovy:groovy-all:2.3.+ -> 2.3.8
            +--- com.fasterxml.jackson.core:jackson-databind:2.4.1.3

            runtime - Runtime classpath for source set 'main'.
            +--- org.codehaus.groovy:groovy-all:2.3.+ -> 2.3.8
            +--- com.fasterxml.jackson.core:jackson-databind:2.4.1.3
            +--- com.fasterxml.jackson.datatype:jackson-datatype-json-org:2.4.1
            |    +--- com.fasterxml.jackson.core:jackson-core:2.4.1 -> 2.4.1.1
            |    +--- com.fasterxml.jackson.core:jackson-databind:2.4.1 -> 2.4.1.3 (*)
            |    +--- org.apache.lucene:lucene-memory:4.9.1 (*)
            +--- io.reactivex:rxjava:latest.release -> 1.0.2
            |    \\--- xml-resolver:xml-resolver:1.2

            testCompile - Compile classpath for source set 'test'.
            +--- org.codehaus.groovy:groovy-all:2.3.+ -> 2.3.8
            +--- com.fasterxml.jackson.core:jackson-databind:2.4.1.3


            testRuntime - Runtime classpath for source set 'test'.
            +--- org.codehaus.groovy:groovy-all:2.3.+ -> 2.3.8
            +--- com.fasterxml.jackson.core:jackson-databind:2.4.1.3

            versionManagement
            No dependencies
        '''.stripIndent()

        when:
        recommender.setFile(file)

        then:
        recommender.getVersion('org.codehaus.groovy', 'groovy-all') == '2.3.8'
        recommender.getVersion('com.fasterxml.jackson.core', 'jackson-databind') == '2.4.1.3'
        recommender.getVersion('com.fasterxml.jackson.datatype', 'jackson-datatype-json-org') == '2.4.1'
        recommender.getVersion('com.fasterxml.jackson.core', 'jackson-core') == '2.4.1.1'
        recommender.getVersion('org.apache.lucene', 'lucene-memory') == '4.9.1'
        recommender.getVersion('io.reactivex', 'rxjava') == '1.0.2'
        recommender.getVersion('xml-resolver', 'xml-resolver') == '1.2'
    }
}
