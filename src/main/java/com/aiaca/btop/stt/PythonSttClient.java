//JJ
package com.aiaca.btop.stt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PythonSttClient {
  private final WebClient web;
  private final ObjectMapper mapper = new ObjectMapper();
  private final String url;

  public PythonSttClient(WebClient.Builder builder, @Value("${stt.python.url}") String url) {
    this.web = builder.build();
    this.url = url;
  }

  public String normalizeSegment(byte[] wavBytes) {
    try {
      ByteArrayResource res = new ByteArrayResource(wavBytes) {
        @Override public String getFilename() { return "seg.wav"; }
      };
      LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
      parts.add("file", res);
      parts.add("engine", "whisper-1");
      parts.add("llm", "gpt-5-nano");
      parts.add("keep_style", "true");
      parts.add("language_hint", "ko");
      parts.add("temperature", "0.0");

      String json = web.post()
        .uri(url)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .retrieve()
        .bodyToMono(String.class)
        .block();

      JsonNode n = mapper.readTree(json);
      // 표준어 우선
      if (n.hasNonNull("standard")) return n.get("standard").asText();
      if (n.hasNonNull("dialect"))  return n.get("dialect").asText();
      return "";
    } catch (Exception e) {
      System.err.println("[PY-STT] error " + e.getMessage());
      return "";
    }
  }
}
