//JJ
package com.aiaca.btop.ws;

import com.aiaca.btop.stt.SttService;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class SttWsHandler extends BinaryWebSocketHandler {

  private static class Pipe {
    Process ffmpeg; OutputStream ffIn; InputStream ffOut;
    ExecutorService es = Executors.newFixedThreadPool(2);
    AtomicLong pcmBytes = new AtomicLong(0);
  }

  private final ObjectFactory<SttService> sttFactory;
  private final ConcurrentMap<String, Pipe> pipes = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, SttService> stts = new ConcurrentHashMap<>();

  // DI: WebSocketConfig에서 주입
  public SttWsHandler(ObjectFactory<SttService> sttFactory) {
    this.sttFactory = sttFactory;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    try {
      // 1) ffmpeg 프로세스 시작 (webm/opus -> 16k mono s16le)
      ProcessBuilder pb = new ProcessBuilder(
        "ffmpeg","-hide_banner","-loglevel","error",
        "-i","pipe:0","-ac","1","-ar","16000","-f","s16le","pipe:1"
      );
      Process proc = pb.start();
      Pipe p = new Pipe();
      p.ffmpeg = proc; p.ffIn = proc.getOutputStream(); p.ffOut = proc.getInputStream();
      pipes.put(session.getId(), p);

      // 2) 세션별 STT 인스턴스 생성 + 콜백(부분/최종/에러)
      SttService stt = sttFactory.getObject();
      stt.start(new SttService.Listener() {
        @Override public void onPartial(String text) {
          try { session.sendMessage(new TextMessage("{\"partial\":\"" + text + "\"}")); } catch (IOException ignore) {}
        }
        @Override public void onFinal(String text) {
          try { session.sendMessage(new TextMessage("{\"final\":\"" + text + "\"}")); } catch (IOException ignore) {}
        }
        @Override public void onError(String msg, Throwable t) {
          try { session.sendMessage(new TextMessage("{\"error\":\"" + msg.replace("\"","'") + "\"}")); } catch (IOException ignore) {}
        }
      });
      stts.put(session.getId(), stt);

      // 3) ffmpeg stdout(PCM) 읽기 → STT에 공급 + 진행상황 이벤트(데모)
      // PATH: src/main/java/com/aiaca/btop/ws/SttWsHandler.java
// ... p.es.submit(() -> { ... }) 블록 내부, try-with-resources 바로 아래에 finally 추가
    p.es.submit(() -> {
      byte[] buf = new byte[4096];
      try (InputStream is = p.ffOut) {
        int n;
        while ((n = is.read(buf)) != -1 && session.isOpen()) {
          long total = p.pcmBytes.addAndGet(n);
          stt.feedPcm(buf, n);
          if (total % (16000 * 2) < 4096) {
            session.sendMessage(new TextMessage("{\"partial\":\"pcm_bytes=" + total + "\"}"));
          }
        }
      } catch (IOException ignore) {
      } finally {
        // stop 신호 없이 종료된 경우를 대비한 최종화
        try { stt.stopAndFinalize(); } catch (Exception ignore2) {}
      }
    });


      // 4) ffmpeg stderr 로그(디버깅용)
      p.es.submit(() -> {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8))) {
          for (String line; (line = br.readLine()) != null; ) {
            System.err.println("[ffmpeg] " + line);
          }
        } catch (IOException ignore) {}
      });

    } catch (IOException e) {
      try {
        session.sendMessage(new TextMessage("{\"error\":\"ffmpeg failed: " + e.getMessage().replace("\"","'") + "\"}"));
        session.close(CloseStatus.SERVER_ERROR);
      } catch (IOException ignore) {}
    }
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    Pipe p = pipes.get(session.getId()); if (p == null) return;
    try {
      // 브라우저에서 온 webm/opus 바이트를 ffmpeg stdin으로 전달
      ByteBuffer payload = message.getPayload();
      if (payload.hasArray()) p.ffIn.write(payload.array(), payload.position(), payload.remaining());
      else { byte[] b = new byte[payload.remaining()]; payload.get(b); p.ffIn.write(b); }
      p.ffIn.flush();
    } catch (IOException ignore) {}
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) {
    if ("stop".equalsIgnoreCase(message.getPayload())) {
      // STT 최종화 + 종료
      SttService stt = stts.get(session.getId());
      if (stt != null) stt.stopAndFinalize();
      closePipe(session);
      try { session.close(CloseStatus.NORMAL); } catch (IOException ignore) {}
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    closePipe(session);
  }

  private void closePipe(WebSocketSession session) {
    // ffmpeg 파이프 정리
    Pipe p = pipes.remove(session.getId());
    if (p != null) {
      try { p.ffIn.close(); } catch (IOException ignore) {}
      try { p.ffOut.close(); } catch (IOException ignore) {}
      if (p.ffmpeg != null && p.ffmpeg.isAlive()) p.ffmpeg.destroy();
      p.es.shutdownNow();
    }
    // STT 인스턴스 정리
    SttService stt = stts.remove(session.getId());
    if (stt != null) try { stt.close(); } catch (IOException ignore) {}
  }
}
