//JJ
package com.aiaca.btop.stt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class PythonSttClient implements SttService {

  private final WebClient webClient;
  private final String sttUrl;

  private Listener listener;
  private final ByteArrayOutputStream pcmBuf = new ByteArrayOutputStream(1 << 20); // 1MB 초기

  // application.properties 에서 stt.python.url 로 주입
  public PythonSttClient(
      WebClient.Builder builder,
      @Value("${stt.python.url}") String sttUrl
  ) {
    this.webClient = builder.build();
    this.sttUrl = sttUrl;
  }

  @Override public void start(Listener listener) {
    this.listener = listener;
    // 스트리밍 중간(부분) 결과는 FastAPI가 WebSocket/스트리밍 엔드포인트가 있어야 가능.
    // 지금은 최종화 시 한 번만 호출한다.
  }

  @Override public void feedPcm(byte[] data, int len) {
    // ffmpeg 가 뽑아주는 16kHz mono s16le PCM을 누적
    if (data == null || len <= 0) return;
    pcmBuf.write(data, 0, len);
    // (선택) 진행 상황을 부분 결과로 흉내내고 싶다면 여기서 bytes 카운트로 onPartial 호출 가능
    if (listener != null) listener.onPartial("...", null); // 표준어는 아직 없으니 null
  }

  // ====== 호환용 래퍼 메서드 (기존 코드가 사용 중인 경우 대비) ======
  /** 기존 코드 호환: 오프셋/길이가 있는 버퍼를 누적 처리 */
  public void sendChunk(byte[] data, int off, int len) {
    if (data == null || len <= 0) return;
    if (off < 0 || len < 0 || off + len > data.length) return;
    byte[] slice = new byte[len];
    System.arraycopy(data, off, slice, 0, len);
    this.feedPcm(slice, len);
  }

  /** 기존 코드 호환: 스트림 종료/최종화 */
  public void finish() {
    this.stopAndFinalize();
  }
  // =============================================================

  @Override public void stopAndFinalize() {
    if (listener == null) return;

    try {
      // 1) 누적 PCM -> WAV 래핑
      byte[] pcm = pcmBuf.toByteArray();
      if (pcm.length == 0) {
        listener.onError("녹음 데이터가 비어있습니다.", null);
        return;
      }
      byte[] wav = pcm16MonoToWav(pcm, 16000);

      // 2) multipart 업로드
      MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
      form.add("audio", new NamedBytes(wav, "audio.wav"));
      // 필요 시 엔진/모델 파라미터도 함께 전송:
      // form.add("engine", "gpt-4o-transcribe");
      // form.add("llm", "gpt-4o-mini");

      String body = webClient.post()
          .uri(sttUrl) // 예: http://127.0.0.1:7000/api/stt-normalize
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(form))
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // 3) 응답 파싱 (유연 처리)
      // 기대 JSON: {"raw":"...","standard":"..."} 또는 {"text":"..."} 등
      String raw = extractJson(body, "raw");
      String std = extractJson(body, "standard");

      if ((raw == null || raw.isBlank()) && (std == null || std.isBlank())) {
        // fallback
        raw = (body == null) ? "" : body;
      }
      if (std == null) std = "";

      listener.onFinal(raw, std);

    } catch (Exception e) {
      if (listener != null) listener.onError("Python STT 호출 실패: " + e.getMessage(), e);
    } finally {
      pcmBuf.reset();
    }
  }

  @Override public void close() throws IOException {
    pcmBuf.reset();
  }

  // --- helpers ---

  /** 간단 WAV 래퍼 (PCM 16-bit mono) */
  private static byte[] pcm16MonoToWav(byte[] pcm, int sampleRate) throws IOException {
    int dataLen = pcm.length;
    int byteRate = sampleRate * 2; // mono * 16bit(2바이트)
    ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataLen);
    // RIFF 헤더
    out.write(new byte[]{'R','I','F','F'});
    writeLE32(out, 36 + dataLen);
    out.write(new byte[]{'W','A','V','E'});
    // fmt chunk
    out.write(new byte[]{'f','m','t',' '});
    writeLE32(out, 16);       // Subchunk1Size
    writeLE16(out, 1);        // PCM
    writeLE16(out, 1);        // mono
    writeLE32(out, sampleRate);
    writeLE32(out, byteRate);
    writeLE16(out, (short)2); // blockAlign
    writeLE16(out, (short)16);// bitsPerSample
    // data chunk
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

  /** 아주 단순한 키 추출 (정식 JSON 파서는 Jackson 써도 됩니다) */
  private static String extractJson(String json, String key) {
    if (json == null) return null;
    String needle = "\"" + key + "\":";
    int idx = json.indexOf(needle);
    if (idx < 0) return null;
    int start = json.indexOf('"', idx + needle.length());
    if (start < 0) return null;
    int end = json.indexOf('"', start + 1);
    if (end < 0) return null;
    return json.substring(start + 1, end);
  }

  /** 멀티파트용 이름 가진 바이트 리소스 */
  static class NamedBytes extends ByteArrayResource {
    private final String filename;
    NamedBytes(byte[] bytes, String filename) { super(bytes); this.filename = filename; }
    @Override public String getFilename() { return filename; }
  }
}
