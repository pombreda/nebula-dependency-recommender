package netflix.nebula.dependency.recommender.provider;

import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.mvn3.org.apache.maven.model.Dependency;
import org.gradle.mvn3.org.apache.maven.model.Model;
import org.gradle.mvn3.org.apache.maven.model.building.*;
import org.gradle.mvn3.org.apache.maven.model.interpolation.StringSearchModelInterpolator;
import org.gradle.mvn3.org.apache.maven.model.path.DefaultPathTranslator;
import org.gradle.mvn3.org.apache.maven.model.path.DefaultUrlNormalizer;
import org.gradle.mvn3.org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.gradle.mvn3.org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.gradle.mvn3.org.codehaus.plexus.interpolation.ValueSource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MavenBomRecommendationProvider extends FileBasedRecommendationProvider {
    private Map<String, String> recommendations;

    public MavenBomRecommendationProvider(Project project) {
        super(project, "pom");
    }

    @Override
    public String getVersion(String org, String name) throws Exception {
        if(recommendations == null) {
            recommendations = new HashMap<>();
            DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
            request.setModelSource(new StringModelSource(IOUtils.toString(getInput(), "UTF-8")));

            DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
            modelBuilder.setModelInterpolator(new ProjectPropertiesModelInterpolator(project));

            ModelBuildingResult result = modelBuilder.build(request);
            for (Dependency d : result.getEffectiveModel().getDependencyManagement().getDependencies()) {
                recommendations.put(d.getGroupId() + ":" + d.getArtifactId(), d.getVersion());
            }
        }
        return recommendations.get(org + ":" + name);
    }

    private static class ProjectPropertiesModelInterpolator extends StringSearchModelInterpolator {
        private final Project project;

        ProjectPropertiesModelInterpolator(Project project) {
            this.project = project;
            setUrlNormalizer(new DefaultUrlNormalizer());
            setPathTranslator(new DefaultPathTranslator());
        }

        public List<ValueSource> createValueSources(Model model, File projectDir, ModelBuildingRequest request, ModelProblemCollector collector) {
            List<ValueSource> sources = new ArrayList<>();
            sources.add(new MapBasedValueSource(project.getProperties()));
            sources.add(new PropertiesBasedValueSource(System.getProperties()));
            sources.addAll(super.createValueSources(model, projectDir, request, collector));
            return sources;
        }
    }
}
