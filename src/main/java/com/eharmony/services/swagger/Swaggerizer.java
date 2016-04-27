package com.eharmony.services.swagger;

import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;

@Service
public class Swaggerizer {
    protected final static Logger LOG = LoggerFactory.getLogger(Swaggerizer.class);

    private String basePath;
    private Boolean enableRepositoryPublish = false;
    private String repositoryHost;
    private String category;
    private String environment;
    private String apiHost;
    private String resourcePackage;
    private String validationUrl;
    private String theme;
    private List<ModelConverter> converters = Collections.emptyList();

    @Inject
    public Swaggerizer(ServletContext servletContext) {
        String contextPath = servletContext.getContextPath();
        LOG.debug("Detected contextPath={}", contextPath);
        if (StringUtils.isNotEmpty(contextPath)) {
            setBasePath(contextPath);
        }

        // Tell Swagger to scan for classes at or below where (the subclass of) this file lives.
        String packageName = getClass().getPackage().getName();
        setResourcePackage(packageName);
    }

    @Bean
    public SwaggerResourceServer classpathServer() {
        SwaggerResourceServer resourceServer = new SwaggerResourceServer();

        if (StringUtils.isNotBlank(basePath)) {
            resourceServer.setContextPath(basePath);
        }
        if (StringUtils.isNotBlank(validationUrl)) {
            resourceServer.setValidationUrl(validationUrl);
        }
        if (StringUtils.isNotBlank(theme)) {
            resourceServer.setTheme(theme);
        }

        return resourceServer;
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

    /**
     * Required by swagger
     */
    @Bean
    public ApiListingResource apilisting() {
        return new ApiListingResource();
    }


    /**
     * Required by swagger
     */
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

    public void setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}
