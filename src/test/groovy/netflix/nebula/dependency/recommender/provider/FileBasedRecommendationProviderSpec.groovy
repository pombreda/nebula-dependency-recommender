package netflix.nebula.dependency.recommender.provider

import org.gradle.api.InvalidUserDataException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class FileBasedRecommendationProviderSpec extends Specification {
    @Rule TemporaryFolder projectDir

    static def recommenderProvider = [:] as FileBasedRecommendationProvider

    @Unroll
    def '#name results in an input stream'(String name, Closure setInput, Closure fromFile) {
        setup:
        def goodFile = projectDir.newFile(); goodFile << 'test'
        def badFile = new File('doesnotexist')

        when:
        setInput(fromFile.call(goodFile))

        then:
        recommenderProvider.input.text == 'test'

        when:
        setInput(fromFile.call(badFile))
        recommenderProvider.input.text

        then:
        Exception e = thrown()
        e instanceof InvalidUserDataException || e instanceof FileNotFoundException

        where:
        name                |   setInput                            |   fromFile
        'setFile'           |   recommenderProvider.&setFile        |   { File f -> f }
        'setUrl(URL)'       |   recommenderProvider.&setUrl         |   { File f -> f.toURI().toURL() }
        'setUrl(String)'    |   recommenderProvider.&setUrl         |   { File f -> f.toURI().toURL().toString() }
        'setUri(URI)'       |   recommenderProvider.&setUri         |   { File f -> f.toURI() }
        'setUri(String)'    |   recommenderProvider.&setUri         |   { File f -> f.toURI().toString() }
        'setInputStream'    |   recommenderProvider.&setInputStream |   { File f -> new FileInputStream(f) }
    }

    def 'module definition results in an input stream'() {
        setup:
        def recommender = [:] as FileBasedRecommendationProvider

        def project = ProjectBuilder.builder().build();
        project.apply plugin: 'java'
        project.apply plugin: 'nebula-dependency-recommender'

        def repo = projectDir.newFolder('repo')

        def sample = new File(repo, 'sample/recommender/1.0')
        sample.mkdirs()

        def sampleFile = new File(sample, 'recommender-1.0.txt')
        sampleFile << 'test'

        recommender.project = project

        when: // maven based repository
        project.repositories { maven { url repo } }
        recommender.setModule('sample:recommender:1.0@txt')

        then:
        recommender.input.text == 'test'

        when: // ivy based repository
        project.repositories.clear()
        project.repositories { ivy { url repo } }
        recommender.setModule('sample:recommender:1.0@txt')

        then:
        recommender.input.text == 'test'
    }

    @Unroll
    def 'default module classifiers can be provided for module definitions: #module'() {
        setup:
        def recommender = [:] as FileBasedRecommendationProvider

        def project = ProjectBuilder.builder().build();
        project.apply plugin: 'java'
        project.apply plugin: 'nebula-dependency-recommender'

        def repo = projectDir.newFolder('repo2')

        def sample = new File(repo, 'sample/recommender/1.0')
        sample.mkdirs()

        def sampleFile = new File(sample, 'recommender-1.0-class.txt')
        sampleFile << 'test'

        def sampleFile2 = new File(sample, 'recommender-1.0-other.txt')
        sampleFile2 << 'testother'

        recommender.defaultModuleExtension = 'txt'
        recommender.defaultClassifier = 'class'
        recommender.project = project

        when:
        project.repositories { maven { url repo } }

        then:
        recommender.setModule(module)
        recommender.input.text == text

        where:
        module                                                                        |  text
        'sample:recommender:1.0'                                                      |  'test'
        'sample:recommender:1.0@txt'                                                  |  'test'
        [group: 'sample', name: 'recommender', version: '1.0']                        |  'test'
        [group: 'sample', name: 'recommender', version: '1.0', classifier: 'class']   |  'test'
        'sample:recommender:1.0:other'                                                |  'testother'
        [group: 'sample', name: 'recommender', version: '1.0', classifier: 'other']   |  'testother'
    }

    @Unroll
    def 'default module extensions can be provided for module definitions: #module'() {
        setup:
        def recommender = [:] as FileBasedRecommendationProvider

        def project = ProjectBuilder.builder().build();
        project.apply plugin: 'java'
        project.apply plugin: 'nebula-dependency-recommender'

        def repo = projectDir.newFolder('repo3')

        def sample = new File(repo, 'sample/recommender/1.0')
        sample.mkdirs()

        def sampleFile = new File(sample, 'recommender-1.0.txt')
        sampleFile << 'test'

        def sampleFile2 = new File(sample, 'recommender-1.0.other')
        sampleFile2 << 'testother'

        recommender.defaultModuleExtension = 'txt'
        recommender.project = project

        when:
        project.repositories { maven { url repo } }

        then:
        recommender.setModule(module)
        recommender.input.text == text

        where:
        module                                                               |  text
        'sample:recommender:1.0'                                             |  'test'
        'sample:recommender:1.0@txt'                                         |  'test'
        [group: 'sample', name: 'recommender', version: '1.0']               |  'test'
        [group: 'sample', name: 'recommender', version: '1.0', ext: 'txt']   |  'test'
        'sample:recommender:1.0@other'                                       |  'testother'
        [group: 'sample', name: 'recommender', version: '1.0', ext: 'other'] |  'testother'
    }
}
