# S21 最小版后端镜像：宿主机 Maven 构建产物 COPY 进 JRE 基础镜像
# （不在镜像内跑 Maven——规避构建期网络与缓存问题，见 STATE S21 记录）
FROM eclipse-temurin:21-jre-alpine
# S63 修复（风险 #38）：安装 CJK 字体——AWT 生成证据图需绘制中文描述，
# 基础镜像仅有 DejaVu（无汉字字形），缺字体时中文被画成占位方块
RUN apk add --no-cache font-noto-cjk
WORKDIR /app
COPY target/inspection-backend.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
