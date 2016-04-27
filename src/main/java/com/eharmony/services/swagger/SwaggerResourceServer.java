package com.eharmony.services.swagger;

import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Exposes the swagger UI at "/swagger-ui". The swagger libraries exposes the json at /swagger.json, and the
 * swagger UI is a set of static resources that are available in various ways in a maven jar; this class is provided
 * for convenience so the swagger UI is available from the service itself, and services have minimal configuration
 * to set up swagger.
 */
@Path("/swagger-ui")
public final class SwaggerResourceServer {
    protected final static Logger LOG = LoggerFactory.getLogger(SwaggerResourceServer.class);
    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private ConcurrentMap<String,byte[]> cache = new ConcurrentHashMap<>(16, 0.9f, 1);
    private String contextPath;
    private String themePath;
    private String settings;
    private static final String SWAGGER_UI_PATH = "classpath*:META-INF/swagger-ui/%s";
    private static final String SWAGGER_THEME_PATH = "theme/%s";
    private static final String SWAGGER_SETTINGS_JS = "$(function () {\n" +
            "window.swaggerSettings = {\n" +
            "validationUrl: '%s'};\n" +
            "});";

    public SwaggerResourceServer() {
        Properties properties = new Properties();

        try {
            properties.load(getClass().getResourceAsStream("/eh-swagger.properties"));
            String validationUrl = properties.getProperty("swagger.validator.url",
                    "http://online.swagger.io/validator/debug");
            String theme = properties.getProperty("swagger.theme", "swagger");
            setTheme(theme);
            setValidationUrl(validationUrl);
        } catch (IOException e) {
            LOG.error("Unable to locate eh-swagger.properties in the classpath");
            throw new RuntimeException("Unable to load eh-swagger without eh-swagger.properties");
        }
    }

    /**
     * Provides initial entry point by redirecting Swagger js with desired target url as parameter.
     */
    @GET
    public Response getBase(@Context HttpServletRequest request) throws IOException, URISyntaxException {
        String cp = contextPath;
        if(StringUtils.isBlank(contextPath)) {
            cp = request.getContextPath();
        }

        String loc = "swagger-ui/index.html?url=" + cp + "/swagger.json";

        String queryString = request.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            loc += "&" + queryString;
        }

        return Response.seeOther(new URI(loc)).build(); 
    }

    @GET
    @Path("/settings.js")
    public Response getSettings() throws IOException, URISyntaxException {
        return Response.ok(settings, "text/javascript").build();
    }

    /**
     * Serves static resources. Would be better to just punt all this to Jetty, but couldn't figure
     * out how to make that happen without mucking with Jetty configurations separately for each service.
     * Doing it here compartmentalizes this from normal service handling flow.
     */
    @GET
    @Path("/{fileName:.*}")
    public Response getStatic(@PathParam("fileName") String file) throws IOException, URISyntaxException {
        int qp = file.indexOf('?');
        file = (qp > 0) ? file.substring(0,qp) : file;
        file = file.contains("theme") ? file.replaceFirst("theme", themePath) : file;
        if (!file.contains("..")) {
            byte[] data = cache.get(file);
            if (data == null) {
                Resource[] rz = resolver.getResources(String.format(SWAGGER_UI_PATH, file));
                if (rz.length > 0) {
                    try (InputStream result = rz[0].getInputStream()) {
                        data = ByteStreams.toByteArray(result);
                        cache.putIfAbsent(file,data);
                    }
                }
            }
            if (data != null) {
                String mimeType = URLConnection.guessContentTypeFromName(file);
                CacheControl cacheControl = new CacheControl();
                cacheControl.setMaxAge(600);
                return Response.ok(data, mimeType).cacheControl(cacheControl).build();
            }
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setTheme(String theme) {
        this.themePath = String.format(SWAGGER_THEME_PATH, theme);
    }
    public void setValidationUrl(String validationUrl) {
        this.settings = String.format(SWAGGER_SETTINGS_JS, validationUrl);
    }
}
