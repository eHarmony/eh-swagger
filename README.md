# Swagger for REST Services

Project for streamlining use of [Swagger](http://swagger.io/) to document REST services, with these goals:

1. Simplify configuration for each service.
2. Activate self-contained UI endpoint for each service.
3. Provide common capabilities for eHarmony services.

## Configuration

Three steps:

1. Add this to your pom:
    ```xml
    <!-- Swagger -->
    <dependency>
        <groupId>com.eharmony.services</groupId>
        <artifactId>swagger</artifactId>
        <version>${eh.swagger.version}</version>
    </dependency>         

    <dependency>    <!-- Ensures compatible version for swagger -->
         <groupId>org.reflections</groupId>
         <artifactId>reflections</artifactId>
         <version>0.9.9</version>
    </dependency>
    ```
2. Add this to your application-context.xml, using your appropriate contextPath or path to your api and the packages containing your resources:
    ```xml
    <!-- Swagger -->
    <bean class="com.eharmony.services.swagger.Swaggerizer">
        <property name="basePath" value="BASE_PATH_TO_YOUR_API" />
        <property name="resourcePackage" value="com.eharmony.packages.with.your.resources"/>
    </bean>
    ```
    Your packages should include the @SwaggerDefinition class you will be defining.
3. Add a marker (i.e. has no implementation) Java interface to your source code, e.g. for communication service it would be CommunicationServiceSwaggerConfig.java. 
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

Add these properties to your swagger bean:
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
    
Create the following Chef properties:

```yaml
environment: <np,lt,prod, int>
swagger_repository_service: <The host of the central swagger repository>
swagger_ui_host: <Your environments VIP host>
```   

The swagger spec will be posted to the documentation server when the service starts up. 

## Add custom converters for your swagger spec

Add the following to your Swaggerizer bean config, see `eh-swagger-extensions` for existing converters:

```xml
<bean class="com.eharmony.services.swagger.Swaggerizer">
    <property name="converters">
        <list>
            <bean class="GoogleProtoBufSwaggerConverter"/>
        </list>
    </property>
```