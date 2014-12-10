package netflix.nebula.dependency.recommender.provider;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FileBasedRecommendationProvider extends AbstractRecommendationProvider {
    protected Project project;
    protected String defaultClassifier;
    protected String defaultModuleExtension;

    /**
     * We only want to open input streams if a version is actually asked for
     */
    public static interface InputStreamProvider {
        InputStream getInputStream() throws Exception;
    }

    protected InputStreamProvider inputProvider = new InputStreamProvider() {
        @Override
        public InputStream getInputStream() throws Exception {
            throw new InvalidUserDataException("No recommender input source has been defined");
        }
    };

    protected FileBasedRecommendationProvider() { /* for mocks */ }

    public FileBasedRecommendationProvider(Project project) {
        this.project = project;
    }

    public FileBasedRecommendationProvider(Project project, String defaultModuleExtension) {
        this(project);
        this.defaultModuleExtension = defaultModuleExtension;
    }

    public FileBasedRecommendationProvider(Project project, String defaultModuleExtension, String defaultClassifier) {
        this(project, defaultModuleExtension);
        this.defaultClassifier = defaultClassifier;
    }

    protected InputStream getInput() {
        try {
            return inputProvider.getInputStream();
        } catch (Exception e) {
            throw new InvalidUserDataException("Unable to open recommender input source", e);
        }
    }

    public InputStreamProvider setFile(final File f) {
        inputProvider = new InputStreamProvider() {
            @Override
            public InputStream getInputStream() throws Exception {
                return new FileInputStream(f);
            }
        };
        return inputProvider;
    }

    public InputStreamProvider setInputStream(final InputStream in) {
        inputProvider = new InputStreamProvider() {
            @Override
            public InputStream getInputStream() {
                return in;
            }
        };
        return inputProvider;
    }

    public InputStreamProvider setUri(final URI uri) {
        inputProvider = new InputStreamProvider() {
            @Override
            public InputStream getInputStream() throws Exception {
                return uri.toURL().openStream();
            }
        };
        return inputProvider;
    }

    public InputStreamProvider setUri(String uri) {
        return setUri(URI.create(uri));
    }

    public InputStreamProvider setUrl(final URL url) {
        inputProvider = new InputStreamProvider() {
            @Override
            public InputStream getInputStream() throws Exception {
                return url.openStream();
            }
        };
        return inputProvider;
    }

    public InputStreamProvider setUrl(final String url) {
        inputProvider = new InputStreamProvider() {
            @Override
            public InputStream getInputStream() throws Exception {
                return new URL(url).openStream();
            }
        };
        return inputProvider;
    }

    private Pattern moduleStringPattern = Pattern.compile("([^:]+:[^:]+:[^:@]+)(:([^@]+))?(@(.+))?");

    public InputStreamProvider setModule(Object dependencyNotation) {
        final Object enhancedDependencyNotation;

        if(defaultModuleExtension != null && dependencyNotation instanceof String) {
            Matcher matcher = moduleStringPattern.matcher((String) dependencyNotation);
            if(matcher.matches()) {
                String moduleString = matcher.group(1);

                if(matcher.group(3) != null)
                    moduleString += ":" + matcher.group(3);
                else if(defaultClassifier != null)
                    moduleString += ":" + defaultClassifier;

                if(matcher.group(5) != null)
                    moduleString += "@" + matcher.group(5);
                else if(defaultModuleExtension != null)
                    moduleString += "@" + defaultModuleExtension;

                enhancedDependencyNotation = moduleString;
            }
            else {
                // the module string format looks incorrect, just try to roll with it...
                enhancedDependencyNotation = dependencyNotation;
            }
        }
        else if(defaultModuleExtension != null && dependencyNotation != null
                && Map.class.isAssignableFrom(dependencyNotation.getClass())) {
            Map dependencyNotationMap = (Map) dependencyNotation;

            // don't overwrite an extension provided by the user
            if(!dependencyNotationMap.containsKey("ext"))
                dependencyNotationMap.put("ext", defaultModuleExtension);
            if(!dependencyNotationMap.containsKey("classifier"))
                dependencyNotationMap.put("classifier", defaultClassifier);

            enhancedDependencyNotation  = dependencyNotationMap;
        }
        else
            enhancedDependencyNotation = dependencyNotation;

        inputProvider = new InputStreamProvider() {
            @Override
            public InputStream getInputStream() throws Exception {
                // create a temporary configuration to resolve the file
                Configuration conf = project.getConfigurations().detachedConfiguration(
                        project.getDependencies().create(enhancedDependencyNotation));
                return new FileInputStream(conf.resolve().iterator().next());
            }
        };
        return inputProvider;
    }
}
