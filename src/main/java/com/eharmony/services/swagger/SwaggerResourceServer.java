package com.eharmony.services.swagger;

import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Exposes the swagger UI at "../swagger-ui". The swagger libraries exposes the json at /swagger.json, and the swagger UI is
 * a set of static resources that are available in various ways in a maven jar; this class is provided for convenience so
 * the swagger UI is available from the service itself, and services have minimal configuration to pull this off.
 * 
 * @author bmccarthy
 *
 */
@Path("/swagger-ui")
public final class SwaggerResourceServer {
    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private ConcurrentMap<String,byte[]> cache = new ConcurrentHashMap<>(16, 0.9f, 1);
    private String contextPath;
    private static final String SWAGGER_UI_PATH = "classpath*:META-INF/swagger-ui/%s";

    public SwaggerResourceServer(String contextPath) {
        this.contextPath = contextPath;
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
    
    /**
     * Serves static resources. Would be better to just punt all this to Jetty, but couldn't figure
     * out how to make that happen without mucking with Jetty configurations separately for each service.
     * Doing it here compartmentalizes this from normal service handling flow. -bmccarthy14Sept15
     */
    @GET
    @Path("/{fileName:.*}")
    public Response getStatic(@PathParam("fileName") String file) throws IOException, URISyntaxException {
        int qp = file.indexOf('?');
        if (qp > 0) file = file.substring(0,qp);
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
}
