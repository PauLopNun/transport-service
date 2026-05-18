FROM 822414985516.dkr.ecr.eu-west-1.amazonaws.com/transport-service:java21-base
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
