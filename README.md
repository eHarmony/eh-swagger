# EH Swagger  [![Build Status](https://travis-ci.org/eHarmony/eh-swagger.svg?branch=master)](https://travis-ci.org/eHarmony/eh-swagger)

Project for streamlining use of [Swagger](http://swagger.io/) to document REST services, with these goals:

1. Simplify swagger configuration for each service.
2. Enable a self-contained UI endpoint for each service.
3. Provide common capabilities for eHarmony services (eg. eh-swagger-repository).

## Configuration

### 1. Add this to your pom:

```xml
<!-- Swagger -->
<dependency>
    <groupId>com.eharmony.services</groupId>
    <artifactId>eh-swagger</artifactId>
    <version>${eh.swagger.version}</version>
</dependency>         
```

*Note* If your pom references the bmx library, make sure you're including version 25 or later of that library, which has a fix for avoiding a runtime problem with Swagger.

### 2. Configure the dependency with Spring

There are two options

#### 2a. Use component scan

Add a java file in your source code that is
  * within a spring component-scan directory, and
  * at the root of any service resources
  
The name does no matter elsewhere, though it useful to employ a convention like SERVICESwaggerConfig -- e.g. for communication service it would be _CommunicationServiceSwaggerConfig.java_. The following is an example of the contents of that file:

```java
package com.eharmony.services.communication;

import org.springframework.stereotype.Component;

import com.eharmony.services.swagger.Swaggerizer;

import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@SwaggerDefinition(
        info = @Info(
                title = "Communication Service",
                version = "V1",
                description = "Messages between users in a match"
        ),
        tags = {
                @Tag(name = "IceBreaker", description = "An opening dialog message prompt between users"),
                @Tag(name = "Nudge", description = "A message prompt sent to encourage another user to upload a photo")
        }
)
@Component
public class CommunicationServiceSwaggerConfig extends Swaggerizer {
}
```

Add more (or fewer) @Tag entries as needed to provide visual groupings for your services, then reference these from your @Api annotations (see example below).

#### 2b. XML Configuration

Add this to your application-context.xml, using your appropriate contextPath or path to your api and the packages containing your resources:

```xml
<bean class="com.eharmony.services.swagger.Swaggerizer">
    <property name="basePath" value="BASE_PATH_TO_YOUR_API" />
    <property name="resourcePackage" value="com.eharmony.packages.with.your.resources"/>
</bean>
```

Your packages should include the @SwaggerDefinition class you will be defining.

Add a marker (i.e. has no implementation) Java interface to your source code, e.g. for communication service it would be CommunicationServiceSwaggerConfig.java. 

The following is an example; change the values accordingly for your service:

```java

package com.eharmony.services.communication;

import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@SwaggerDefinition(
        info = @Info(
                title = "Communication Service",
                version = "V1",
                description = "Messages between users in a match"
        ),
        tags = {
                @Tag(name = "icebreaker", description = "An opening dialog message prompt between users"),
                @Tag(name = "nudge", description = "A message prompt sent to encourage another user to upload a photo")
        }
        
)
public interface CommunicationServiceSwaggerConfig {
}

```

Add more (or fewer) @Tag entries as needed to provide visual groupings for your services, then reference these from your @Api annotations (see example below).


## Annotating Services

Swagger reads what it can from method signatures and jersey annnotations, but also provides [additional annotations](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X) to expose more detail. The only required annotation is adding @Api to a class to tell Swagger to look for resources exposed on that class, e.g.: 

```java
@Component @Path("/users/{userId}/matches/{matchId}/nudges")
@Api(value="nudge")
public class UserMatchPhotoNudgeResource {
    // ...
} 
```

To provide information beyond what Swagger can determine on its own, you can (and usually should) on each service method make use of @ApiOperation, @ApiResponse, and @ApiParam(s) as in the following: 

```java
@GET 
@ApiOperation(value="Return all photo nudges for a match")
@ApiResponses(value={@ApiResponse(code=404, message="Invalid match, userId not in match")})
public UserWrapper.WithPhotoNudges getPhotoNudgesForMatch(
        @ApiParam(value="Logged-in user") @PathParam("userId") long userId,
        @ApiParam(value="Target match") @PathParam("matchId") long matchId) {
```

## Accessing the Results

Whatever host/contextPath your service uses, the results will be accessible from a well-known path on top of that. Using communication service on localhost as an example, we would have the following: 

    http://localhost:9357/communication/swagger-ui
    http://localhost:9357/communication/swagger.json
    http://localhost:9357/communication/swagger.yaml
    
The first is for human consumption, the latter two expose the schema for tools that consume those.


## Enabling publishing to repository

To configure the client to post the Swagger documentation to an eh-swagger-repository at startup, add the following properties to your swagger bean:
* enableRepositoryPublish - set to true to turn on publishing
* environment - coming from your Chef properties: lt, np, prod, int
* category - General grouping for your service. Eg. matching, singles, shared
* repositoryHost - host of the central repository
* apiHost - host vip for your service in the specified environment

```xml
<bean class="com.eharmony.services.swagger.Swaggerizer">
    <property name="enableRepositoryPublish" value="true"/>
    <property name="environment" value="${environment}"/>
    <property name="category" value="Matching"/>
    <property name="repositoryHost" value="${swagger.repository.service}"/>
    <property name="apiHost" value="${swagger.ui.host}" />
```    

## Set custom theme and validator

By default, eh-swagger will use the default swagger UI look and feel. There is a customized eHarmony theme that uses eHarmony colors and logo. Themes are configured in src/main/resources/META-INF/swagger-ui/theme.

Add these properties to your Swaggerizer bean:
* theme - default is "swagger", there is also an "eharmony" theme
* validationUrl - url to the validator that swagger uses, default is http://online.swagger.io/validator/debug

```xml
<bean class="com.eharmony.services.swagger.Swaggerizer">
    <property name="theme" value="eharmony"/>
    <property name="validationUrl" value="${swagger.validator.url}"/>
```

## Jersey 2 / Jetty 9

Configuration for jersey 2 is slightly different. 

### User <version>-jersey2 version in the pom

eg:

```xml
<!-- Swagger -->
<dependency>
    <groupId>com.eharmony.services</groupId>
    <artifactId>eh-swagger</artifactId>
    <version>2.0.0-jersey2</version>
</dependency>         
```

### Use component scan for autowiring different Swagger beans

In the application context add the swagger package to your base packages:


```xml             
  <context:annotation-config />
  <context:component-scan base-package="com.eharmony.services.swagger" />
```

### Update web.xml with Swagger providers

The provider packages in the web.xml under the Jersey servlet container need to include swagger:

```xml
    <servlet>
        <servlet-name>jersey</servlet-name>
        <servlet-class>
            org.glassfish.jersey.servlet.ServletContainer</servlet-class>
        <init-param>        
            <param-name>jersey.config.server.provider.packages</param-name>
            <param-value>com.eharmony.services.swagger,io.swagger.jaxrs.listing</param-value>
        </init-param>
    </servlet>
```

### Use properties for theme and validator

For the theme and validator, they are autowired into the Swagger Resource, instead of being manually configured. Set the following properties in your properties file to set them:

```
swagger.validator.url=http://someservice.com/api/validator
swagger.theme=your_theme
```

## Known Issues

### Hibernate

For some versions of hibernate, when starting up a service after adding eh-swagger, you will get the following error:

```
Caused by: javax.validation.ValidationException: Unable to create a Configuration, because no Bean Validation provider could be found. Add a provider like Hibernate Validator (RI) to your classpath.
```

This can be resolved by excluding validation-api from eh-swagger:

```xml
<!-- Swagger -->
<dependency>
    <groupId>com.eharmony.services</groupId>
    <artifactId>eh-swagger</artifactId>
    <version>${eh.swagger.version}</version>
    <exclusions>
        <exclusion>
            <groupId>javax.validation</groupId>
            <artifactId>validation-api</artifactId>
        </exclusion>
    </exclusions>
</dependency>         
```
