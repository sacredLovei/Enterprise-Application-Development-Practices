package com.course.inspection.config;

import com.course.inspection.common.TopicConst;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * 死信机制（设计报告 5.2.2(3) / FR-2.5）：
 * 手动提交 + 重试 3 次后经 DeadLetterPublishingRecoverer 转 inspection.dlq，不阻塞分区。
 */
@Configuration
public class KafkaDlqConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> dlqProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // 重试 3 次（间隔 2s），仍失败 → 转死信主题 inspection.dlq
        // 注意：不指定 destinationResolver 时默认目标是 "<原主题>.DLT"，必须显式指向口径主题
        var recoverer = new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(
                dlqKafkaTemplate(),
                (cr, e) -> new org.apache.kafka.common.TopicPartition(TopicConst.DLQ, cr.partition()));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(2_000L, 3));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }

    /** 手动提交容器工厂：业务成功才提交 offset（设计报告 5.2.2(3)）。 */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> manualAckFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        // 必须显式注入 ConsumerFactory：自定义工厂会令 Boot 的自动装配退避（否则启动即崩 'consumerFactory' cannot be null）
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }
}
