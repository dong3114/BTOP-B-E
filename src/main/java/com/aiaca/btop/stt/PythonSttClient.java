package com.aiaca.btop.stt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Component
public class PythonSttClient implements SttService {

  private final WebClient webClient;
  private final String sttUrl;
  private final ObjectMapper om = new ObjectMapper();

  private Listener listener;
  private final ByteArrayOutputStream pcmBuf = new ByteArrayOutputStream(1 << 20); // 1MB

  public PythonSttClient(WebClient.Builder builder, @Value("${stt.python.url}") String sttUrl) {
    this.webClient = builder.build();
    this.sttUrl = sttUrl;
  }

  @Override public void start(Listener listener) {
    this.listener = listener;
  }

  @Override public void feedPcm(byte[] data, int len) {
    if (data == null || len <= 0) return;
    pcmBuf.write(data, 0, len);
    // 🎯 더 이상 더미 partial("...") 보내지 않음
  }

  // (구호환)
  public void sendChunk(byte[] data, int off, int len) {
    if (data == null || len <= 0) return;
    if (off < 0 || len < 0 || off + len > data.length) return;
    pcmBuf.write(data, off, len);
  }
  public void finish() { stopAndFinalize(); }

  @Override
  public void stopAndFinalize() {
    if (listener == null) return;
    try {
      byte[] pcm = pcmBuf.toByteArray();
      if (pcm.length == 0) {
        listener.onError("녹음 데이터가 비어있습니다.", null);
        return;
      }
      byte[] wav = pcm16MonoToWav(pcm, 16000);

      // 멀티파트(파일명 포함)
      ByteArrayResource wavRes = new ByteArrayResource(wav) {
        @Override public String getFilename() { return "audio.wav"; }
      };
      org.springframework.http.client.MultipartBodyBuilder mb = new org.springframework.http.client.MultipartBodyBuilder();
      // 서버마다 파라미터명이 달라서 여러 키로 전송
      mb.part("audio",       wavRes).filename("audio.wav").contentType(MediaType.APPLICATION_OCTET_STREAM);
      mb.part("file",        wavRes).filename("audio.wav").contentType(MediaType.APPLICATION_OCTET_STREAM);
      mb.part("upload_file", wavRes).filename("audio.wav").contentType(MediaType.APPLICATION_OCTET_STREAM);
      // 가능하면 서버가 원문/표준어 둘 다 주도록 힌트
      mb.part("return_both", "true");

      String targetUrl = normalizeEndpoint(sttUrl);

      String body = webClient.post()
          .uri(targetUrl)
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(mb.build()))
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // === 응답 파싱: 원문/표준어를 각자 독립적으로 찾기 ===
      String raw = null, std = null;
      try {
        if (body != null && !body.isBlank()) {
          JsonNode root = om.readTree(body);

          // 원문 후보(사투리/비정규): 서비스별로 쓰일 수 있는 키들을 폭넓게 탐색(깊이 탐색)
          raw = firstNonBlankDeep(root,
              "raw","dialect","original","stt_raw","raw_text","rawText","asr","transcript","text");

          // 표준어 후보(정규화 텍스트)
          std = firstNonBlankDeep(root,
              "standard","normalized","normalized_text","norm","std","standard_text");

          // ⚠️ 절대 표준어를 원문으로 복사하지 않음 (raw가 없으면 빈값으로 둠)
          // 단, 표준어도 없고 텍스트 키만 하나 있을 때는 원문으로 간주할 수 있으나
          // 혼선을 피하려면 그대로 둡니다.
          if (raw != null && std != null) {
            // 동일 문자열이면 그대로 두되, UI에서 두 칸이 같아 보일 수 있음
            // 필요하면 여기서 같을 때 raw=null 처리 가능(선택)
          }
        }
      } catch (Exception ignore) {
        // 비 JSON 응답이면 굳이 원문에 때려넣지 않음
      }

      if (raw == null) raw = "";
      if (std == null) std = "";

      listener.onFinal(raw, std);

    } catch (Exception e) {
      listener.onError("Python STT 호출 실패: " + e.getMessage(), e);
    } finally {
      pcmBuf.reset();
    }
  }

  @Override public void close() throws IOException {
    pcmBuf.reset();
  }

  // ----------------- helpers -----------------

  /** 간단 WAV 래퍼 (PCM 16-bit mono) */
  private static byte[] pcm16MonoToWav(byte[] pcm, int sampleRate) throws IOException {
    int dataLen = pcm.length;
    int byteRate = sampleRate * 2; // mono * 16bit
    ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataLen);

    out.write(new byte[]{'R','I','F','F'});
    writeLE32(out, 36 + dataLen);
    out.write(new byte[]{'W','A','V','E'});

    out.write(new byte[]{'f','m','t',' '});
    writeLE32(out, 16);
    writeLE16(out, 1);        // PCM
    writeLE16(out, 1);        // mono
    writeLE32(out, sampleRate);
    writeLE32(out, byteRate);
    writeLE16(out, (short)2);
    writeLE16(out, (short)16);

    out.write(new byte[]{'d','a','t','a'});
    writeLE32(out, dataLen);
    out.write(pcm);
    return out.toByteArray();
  }

  private static void writeLE16(OutputStream os, int v) throws IOException {
    os.write(v & 0xff);
    os.write((v >>> 8) & 0xff);
  }
  private static void writeLE32(OutputStream os, int v) throws IOException {
    os.write(v & 0xff);
    os.write((v >>> 8) & 0xff);
    os.write((v >>> 16) & 0xff);
    os.write((v >>> 24) & 0xff);
  }

  /** 깊이 우선 탐색으로 첫 번째 non-blank 값을 찾음 */
  private static String firstNonBlankDeep(JsonNode node, String... keys) {
    if (node == null) return null;

    // 1) 현재 객체에서 직접 찾기
    for (String k : keys) {
      JsonNode n = node.get(k);
      if (n != null && !n.isNull()) {
        String v = n.asText();
        if (v != null && !v.isBlank()) return v;
      }
    }

    // 2) 객체/배열이면 하위로 재귀
    if (node.isObject()) {
      var it = node.fields();
      while (it.hasNext()) {
        var e = it.next();
        String v = firstNonBlankDeep(e.getValue(), keys);
        if (v != null && !v.isBlank()) return v;
      }
    } else if (node.isArray()) {
      for (JsonNode n : node) {
        String v = firstNonBlankDeep(n, keys);
        if (v != null && !v.isBlank()) return v;
      }
    }
    return null;
  }

  /** 엔드포인트 보정: /api/stt-normalize 를 기본으로 맞춤 */
  private static String normalizeEndpoint(String url) {
    if (url == null || url.isBlank()) return "http://127.0.0.1:7000/api/stt-normalize";
    String trimmed = url.trim();
    try {
      java.net.URI uri = java.net.URI.create(trimmed);
      String path = uri.getPath();
      if (path == null || path.isBlank() || "/".equals(path)) {
        return uri.resolve("/api/stt-normalize").toString();
      }
      return trimmed.replaceAll("/+$", "");
    } catch (IllegalArgumentException e) {
      String u = trimmed.replaceAll("/+$", "");
      return u.endsWith("/api/stt-normalize") ? u : (u + "/api/stt-normalize");
    }
  }
}
