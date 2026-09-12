package com.course.inspection.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * WebHDFS 客户端（设计报告 5.2.3(1)，D-7）：
 * 两步写入协议——先 CREATE 拿 DataNode 重定向地址，再 PUT 真实数据。
 */
@Component
public class HdfsClient {

    private static final Logger log = LoggerFactory.getLogger(HdfsClient.class);

    private static final String NN = "http://namenode:9870/webhdfs/v1";
    private static final String USER = "root";

    private final RestTemplate restTemplate;

    public HdfsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String upload(String path, byte[] data) {
        String createUrl = UriComponentsBuilder.fromHttpUrl(NN + path)
                .queryParam("op", "CREATE")
                .queryParam("overwrite", "true")
                .queryParam("user.name", USER)
                .build().toUriString();

        // 第一步不带 body：拿到 307 重定向的 DataNode 地址
        ResponseEntity<Void> createResp = restTemplate.exchange(
                createUrl, HttpMethod.PUT, HttpEntity.EMPTY, Void.class);
        URI dataNodeUri = createResp.getHeaders().getLocation();
        if (dataNodeUri == null) {
            throw new IllegalStateException("WebHDFS 未返回 DataNode 重定向地址: " + path);
        }

        // 第二步：向 DataNode 直传数据
        RequestEntity<byte[]> put = RequestEntity
                .put(dataNodeUri)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
        restTemplate.exchange(put, Void.class);

        log.info("HDFS 上传成功 path={} size={}B", path, data.length);
        return path;
    }

    public byte[] download(String path) {
        String url = UriComponentsBuilder.fromHttpUrl(NN + path)
                .queryParam("op", "OPEN")
                .queryParam("user.name", USER)
                .build().toUriString();
        return restTemplate.getForObject(url, byte[].class);
    }

    public boolean exists(String path) {
        String url = UriComponentsBuilder.fromHttpUrl(NN + path)
                .queryParam("op", "GETFILESTATUS")
                .queryParam("user.name", USER)
                .build().toUriString();
        try {
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}
