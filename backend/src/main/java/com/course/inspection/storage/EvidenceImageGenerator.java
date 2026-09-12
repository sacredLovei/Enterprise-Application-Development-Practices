package com.course.inspection.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 证据图生成器（S30 说明）：仿真设备不携带真实图像，后端为每条告警生成一张
 * 占位证据图（含告警编号/类型/时间水印）上传 HDFS，以打通"高空发现→证据归档"链路；
 * AWT 不可用时降级为纯文本证据文件（容器环境兜底）。
 */
@Component
public class EvidenceImageGenerator {

    private static final Logger log = LoggerFactory.getLogger(EvidenceImageGenerator.class);

    public record Evidence(byte[] bytes, String ext) {
    }

    public Evidence generate(String alarmId, String alarmType, String description, long occurredTime) {
        try {
            BufferedImage img = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(24, 30, 40));
            g.fillRect(0, 0, 640, 360);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            g.drawString("INSPECTION ALARM EVIDENCE", 30, 60);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
            g.drawString("alarmId: " + alarmId, 30, 120);
            g.drawString("alarmType: " + alarmType, 30, 155);
            g.drawString("occurredTime: " + occurredTime, 30, 190);
            g.drawString(trim(description, 48), 30, 230);
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return new Evidence(out.toByteArray(), "png");
        } catch (Throwable e) {
            log.warn("AWT 不可用，降级为文本证据文件: {}", e.getMessage());
            String text = "alarmId=" + alarmId + "\nalarmType=" + alarmType
                    + "\ndescription=" + description + "\noccurredTime=" + occurredTime + "\n";
            return new Evidence(text.getBytes(StandardCharsets.UTF_8), "txt");
        }
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
