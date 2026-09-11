# S21 最小版仿真镜像：宿主机 Maven 构建产物 COPY 进 JRE 基础镜像
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/inspection-simulator.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
