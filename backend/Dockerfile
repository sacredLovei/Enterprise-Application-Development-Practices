# S21 最小版后端镜像：宿主机 Maven 构建产物 COPY 进 JRE 基础镜像
# （不在镜像内跑 Maven——规避构建期网络与缓存问题，见 STATE S21 记录）
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/inspection-backend.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
