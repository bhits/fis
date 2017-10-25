# Short Description

The FHIR Integration Service provides RESTful endpoints to publish and retrieve FHIR resources. 

# Full Description

# Supported Source Code Tags and Current `Dockerfile` Link

[`0.1.0 (latest)`](https://github.com/bhits/fis/releases/tag/0.1.0)

[`Current Dockerfile`](../fis/src/main/docker/Dockerfile)

For more information about this image, the source code, and its history, please see the [GitHub repository](https://github.com/bhits/fis).

# What is the FHIR Integration Service?

The FHIR Integration Service (fis) Spring Boot project provides RESTful endpoints to allow applications to publish and retrieve FHIR resources. 

For more information and related downloads for Consent2Share, please visit [Consent2Share](https://bhits-dev.github.io/consent2share/).

# How to Use This Image

## Start a fis Instance

Be sure to familiarize yourself with the repository's [README.md](https://github.com/bhits/fis) file before starting the instance.

`docker run  --name fis -d bhits/fis:latest <additional program arguments>`

*NOTE: In order for this project to fully function as a microservice in the Consent2Share application, it is required to setup the dependency microservices and the support level infrastructure. Please refer to the Consent2Share Deployment Guide in the corresponding Consent2Share release (see [Consent2Share Releases Page](https://github.com/bhits/consent2share/releases)) for instructions to setup the Consent2Share infrastructure.*
 
## Configure

The Spring profiles `application-default` and `docker` are activated by default when building images.

This project can run with the default configuration which is from three places: `bootstrap.yml`, `application.yml`, and the data which the [`Configuration Server`](https://github.com/bhits/config-server) reads from the `Configuration Data Git Repository`. Both `bootstrap.yml` and `application.yml` files are located in the class path of the running application.

We **recommend** overriding the configuration as needed in the `Configuration Data Git Repository`, which is used by the `Configuration Server`.

Also, [Spring Boot](https://projects.spring.io/spring-boot/) supports other ways to override the default configuration to configure the project for a certain deployment environment. 

The following is an example to override the default database password:

`docker run -d bhits/fis:latest --spring.port=80`

## Environment Variables

When you start the FIS image, you can edit the configuration of the FIS instance by passing one or more environment variables on the command line. 

### JAR_FILE

This environment variable is used to setup which jar file will run. You need to mount the jar file to the root of container.

`docker run --name fis -e JAR_FILE="fis-latest.jar" -v "/path/on/dockerhost/fis-latest.jar:/fis-latest.jar" -d bhits/fis:latest`

### JAVA_OPTS 

This environment variable is used to setup a JVM argument, such as memory configuration.

`docker run --name fis -e "JAVA_OPTS=-Xms512m -Xmx700m -Xss1m" -d bhits/fis:latest`

### DEFAULT_PROGRAM_ARGS 

This environment variable is used to setup an application argument. The default value is: "--spring.profiles.active=application-default, docker".

`docker run --name fis -e DEFAULT_PROGRAM_ARGS="--spring.profiles.active=application-default,ssl,docker" -d bhits/fis:latest`

# Supported Docker Versions

This image is officially supported on Docker version 1.13.0.

Support for older versions (down to 1.6) is provided on a best-effort basis.

Please see the [Docker installation documentation](https://docs.docker.com/engine/installation/) for details on how to upgrade your Docker daemon.

# License

View [license](https://github.com/bhits/fis/blob/master/LICENSE) information for the software contained in this image.

# User Feedback

## Documentation
 
Documentation for this image is stored in the [bhits/fis](https://github.com/bhits/fis) GitHub repository. Be sure to familiarize yourself with the repository's README.md file before attempting a pull request.

## Issues

If you have any problems with or questions about this image, please contact us through a [GitHub issue](https://github.com/bhits/fis/issues).

