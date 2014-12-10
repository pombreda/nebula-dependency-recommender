package netflix.nebula.dependency.recommender.provider

import groovy.json.JsonSlurper
import org.gradle.api.Project

class DependencyReportProvider extends FileBasedRecommendationProvider {
    Map<String, String> recommendations

    DependencyReportProvider() {}

    DependencyReportProvider(Project project) { super(project, "txt", "dependencies") }

    @Override
    String getVersion(String org, String name) throws Exception {
        if(!recommendations) {
            recommendations = [:]

            def inRuntime = false

            for(String line: input.readLines()) {
                if(line.startsWith('runtime - ')) {
                    inRuntime = true
                    continue
                }
                if(line.isEmpty()) {
                    inRuntime = false
                    continue
                }

                if(inRuntime) {
                    def m = line =~ /[^=]+---\s([^:]+:[^:]+):([^\s]*\s->\s)?([^\n\s]+)/
                    recommendations[m[0][1]] = m[0][3]
                }
            }
        }
        recommendations[org + ':' + name]
    }
}
