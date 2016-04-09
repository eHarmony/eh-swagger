package com.eharmony.services.swagger;

import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.eharmony.services.swagger.DocumentationRepositoryPublisher;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Configuration
public class Swaggerizer {
    private String basePath = null;
    private Boolean enableRepositoryPublish = false;
    private String repositoryHost = null;
    private String category = null;
    private String environment = null;
    private String apiHost = null;
    private String resourcePackage = null;
    private List<ModelConverter> converters = Collections.emptyList();

    @Bean
    public SwaggerResourceServer classpathServer() {
        return new SwaggerResourceServer(basePath);
    }

    /**
     * Provides a default bean config with very little information. Assumes a @SwaggerDefinition class contains the
     * information or the bean is explicitly configured in the spring context.
     */
    @Bean
    public BeanConfig beanConfig() {
        BeanConfig bc = new BeanConfig();
        // Expect title and description to be set in @SwaggerDefinition class

        bc.setResourcePackage(resourcePackage);
        bc.setHost(apiHost);
        bc.setBasePath(basePath);
        bc.setScan(true);

        return bc;
    }

    // Other stuff swagger needs
    @Bean
    public ApiListingResource apilisting() {
        return new ApiListingResource();
    }

    @Bean
    @Scope("singleton")
    public SwaggerSerializers serializer() {
        return new SwaggerSerializers();
    }

    @Bean
    @Scope("singleton")
    public DocumentationRepositoryPublisher documentationRepositoryPublisher(BeanConfig beanConfig) {
        DocumentationRepositoryPublisher publisher = new DocumentationRepositoryPublisher();
        if (enableRepositoryPublish) {
            publisher.publish(beanConfig, category, environment, repositoryHost);
        }
        return publisher;
    }

    @PostConstruct
    public void init() throws Exception {
        ModelConverters modelConverters = ModelConverters.getInstance();
        for(ModelConverter converter : converters) {
            modelConverters.addConverter(converter);
        }
    }

    public void setBasePath(String basePath) {
        if(basePath.startsWith("/")) {
            this.basePath = basePath;
        } else {
            this.basePath = "/" + basePath;
        }
    }

    public void setResourcePackage(String resourcePackage) {
        this.resourcePackage = resourcePackage;
    }

    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }

    public void setConverters(List<ModelConverter> converters) {
        this.converters = converters;
    }

    public void setEnableRepositoryPublish(Boolean enableRepositoryPublish) {
        this.enableRepositoryPublish = enableRepositoryPublish;
    }

    public void setRepositoryHost(String repositoryHost) {
        this.repositoryHost = repositoryHost;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
