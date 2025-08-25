package com.aiaca.btop.ws;

import com.aiaca.btop.stt.SttService;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SttWsHandler extends BinaryWebSocketHandler {

  /** 세션별 상태/리소스 */
  private static class Pipe {
    // ffmpeg 모드에서만 사용
    Process ffmpeg;
    OutputStream ffIn;
    InputStream ffOut;
    ExecutorService es = Executors.newFixedThreadPool(2);

    // 공통
    Sender sender;
    AtomicLong pcmBytes = new AtomicLong(0);
    volatile long lastStatAt = 0L;

    // 종료/최종처리 제어
    final AtomicBoolean finalized = new AtomicBoolean(false);
    volatile boolean closeAfterFinal = false;

    // 입력 모드
    boolean rawMode = false;   // true면 RAW PCM(바로 feedPcm)
    int rawSampleRate = 16000; // RAW PCM 샘플레이트
  }

  /** 세션별 직렬 송신(컨테이너의 작은 버퍼/동기 전송 한계를 피함) */
  private static class Sender implements Closeable {
    private final WebSocketSession session;
    private final LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();
    private final Thread t;
    private volatile boolean running = true;

    Sender(WebSocketSession s) {
      this.session = s;
      this.t = new Thread(() -> {
        try {
          while (running && session.isOpen()) {
            String msg = q.poll(1, TimeUnit.SECONDS);
            if (msg == null) continue;
            synchronized (session) {
              session.sendMessage(new TextMessage(msg));
            }
          }
        } catch (Throwable ignore) {
        } finally {
          try { if (session.isOpen()) session.close(); } catch (IOException ignore) {}
        }
      }, "ws-sender-" + s.getId());
      this.t.setDaemon(true);
      this.t.start();
    }

    void send(String json) { if (running) q.offer(json); }

    @Override public void close() {
      running = false;
      t.interrupt();
      q.clear();
    }
  }

  private final String ffmpegPath;
  private final ObjectFactory<SttService> sttFactory;
  private final ConcurrentMap<String, Pipe> pipes = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, SttService> stts = new ConcurrentHashMap<>();
  private final ScheduledExecutorService closer = Executors.newScheduledThreadPool(1);

  public SttWsHandler(ObjectFactory<SttService> sttFactory, String ffmpegPath) {
    this.sttFactory = sttFactory;
    this.ffmpegPath = (ffmpegPath == null || ffmpegPath.isBlank()) ? "ffmpeg" : ffmpegPath.trim();
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    Pipe p = new Pipe();
    p.sender = new Sender(session);

    // 쿼리 파라미터로 모드 결정
    Map<String, String> q = parseQuery(session.getUri());
    if (q.containsKey("pcm")) {
      p.rawMode = true;
      try { p.rawSampleRate = Integer.parseInt(q.getOrDefault("pcm", "16000")); } catch (NumberFormatException ignore) {}
    }
    String container = q.getOrDefault("container", "webm"); // webm, ogg

    try {
      // STT 시작
      SttService stt = sttFactory.getObject();
      stt.start(new SttService.Listener() {
        @Override public void onPartial(String raw, String standard) {
          final String std = (standard == null) ? "" : standard;
          enqueue(session, "{\"partial\":{\"stt\":\"" + escape(raw) + "\",\"standard\":\"" + escape(std) + "\"}}");
        }

        @Override public void onFinal(String raw, String standard) {
          enqueue(session, "{\"final\":{\"stt\":\"" + escape(raw) + "\",\"standard\":\"" + escape(standard) + "\"}}");
          // stop이 요청된 이후라면, 최종을 보낸 뒤 약간의 지연을 두고 닫는다.
          Pipe pp = pipes.get(session.getId());
          if (pp != null && pp.closeAfterFinal) {
            closer.schedule(() -> {
              closePipe(session);
              safeClose(session, CloseStatus.NORMAL);
            }, 400, TimeUnit.MILLISECONDS);
          }
        }

        @Override public void onError(String msg, Throwable t) {
          enqueue(session, "{\"error\":\"" + escape(msg) + "\"}");
        }
      });
      stts.put(session.getId(), stt);

      // 입력 모드에 따라 배선
      if (p.rawMode) {
        // RAW PCM: ffmpeg 없이 바로 feedPcm
        pipes.put(session.getId(), p);
        // 진행상황 안내 타이머(선택)
        p.es.submit(() -> {
          try {
            while (!p.finalized.get() && session.isOpen()) {
              long now = System.currentTimeMillis();
              if (now - p.lastStatAt >= 500) {
                p.lastStatAt = now;
                enqueue(session, "{\"partial\":\"pcm_bytes=" + p.pcmBytes.get() + "\"}");
              }
              Thread.sleep(100);
            }
          } catch (InterruptedException ignore) {}
        });
      } else {
        // 컨테이너(webm/ogg) → ffmpeg 디코드 → s16le 16k
        String fmt = container.equalsIgnoreCase("ogg") ? "ogg" : "webm";
        ProcessBuilder pb = new ProcessBuilder(
            ffmpegPath,
            "-nostdin",
            "-hide_banner", "-loglevel", "warning",
            "-fflags", "+genpts+discardcorrupt",
            "-probesize", "1M",
            "-analyzeduration", "0",
            "-f", fmt, "-i", "pipe:0",
            "-af", "aresample=async=1:first_pts=0",
            "-ac", "1", "-ar", "16000",
            "-f", "s16le", "pipe:1"
        );
        Process proc = pb.start();
        p.ffmpeg = proc;
        p.ffIn = proc.getOutputStream();
        p.ffOut = proc.getInputStream();
        pipes.put(session.getId(), p);

        // ffmpeg stdout(PCM) -> STT 공급
        p.es.submit(() -> {
          byte[] buf = new byte[4096];
          try (InputStream is = p.ffOut) {
            int n;
            while ((n = is.read(buf)) != -1 && session.isOpen()) {
              long total = p.pcmBytes.addAndGet(n);
              stt.feedPcm(buf, n);
              long now = System.currentTimeMillis();
              if (now - p.lastStatAt >= 500) {
                p.lastStatAt = now;
                enqueue(session, "{\"partial\":\"pcm_bytes=" + total + "\"}");
              }
            }
          } catch (IOException e) {
            System.err.println("[WS] ffmpeg pipe error: " + e.getMessage());
          } finally {
            if (p.finalized.compareAndSet(false, true)) {
              try { stt.stopAndFinalize(); } catch (Exception ignore) {}
            }
          }
        });

        // ffmpeg stderr 로그
        p.es.submit(() -> {
          try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = br.readLine()) != null; ) {
              System.err.println("[ffmpeg] " + line);
            }
          } catch (IOException ignore) {}
        });
      }

    } catch (IOException e) {
      enqueue(session, "{\"error\":\"ffmpeg failed: " + escape(e.getMessage()) + "\"}");
      safeClose(session, CloseStatus.SERVER_ERROR);
    }
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    Pipe p = pipes.get(session.getId());
    if (p == null) return;
    try {
      ByteBuffer payload = message.getPayload();
      if (p.rawMode) {
        // RAW PCM → 바로 STT
        if (payload.hasArray()) {
          byte[] a = payload.array();
          int pos = payload.position();
          int rem = payload.remaining();
          p.pcmBytes.addAndGet(rem);
          SttService stt = stts.get(session.getId());
          if (stt != null) stt.feedPcm(a, rem); // 배열/오프셋 그대로 가능
        } else {
          byte[] b = new byte[payload.remaining()];
          payload.get(b);
          p.pcmBytes.addAndGet(b.length);
          SttService stt = stts.get(session.getId());
          if (stt != null) stt.feedPcm(b, b.length);
        }
      } else {
        // 컨테이너 → ffmpeg stdin
        if (payload.hasArray()) {
          p.ffIn.write(payload.array(), payload.position(), payload.remaining());
        } else {
          byte[] b = new byte[payload.remaining()];
          payload.get(b);
          p.ffIn.write(b);
        }
        p.ffIn.flush();
      }
    } catch (IOException ignore) {}
  }

  @Override
public void handleTextMessage(WebSocketSession session, TextMessage message) {
  if ("stop".equalsIgnoreCase(message.getPayload())) {
    Pipe p = pipes.get(session.getId());
    if (p != null) {
      p.closeAfterFinal = true; // onFinal 후 닫기
      enqueue(session, "{\"info\":\"finalizing...\"}"); // 디버깅용
    }
    SttService stt = stts.get(session.getId());
    if (p != null && p.finalized.compareAndSet(false, true)) {
      if (stt != null) try { stt.stopAndFinalize(); } catch (Exception ignore) {}
    }
  }
}

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    closePipe(session);
  }

  private void closePipe(WebSocketSession session) {
    String sid = session.getId();
    Pipe p = pipes.remove(sid);
    if (p != null) {
      try { if (p.ffIn != null) p.ffIn.close(); } catch (IOException ignore) {}
      try { if (p.ffOut != null) p.ffOut.close(); } catch (IOException ignore) {}
      if (p.ffmpeg != null && p.ffmpeg.isAlive()) p.ffmpeg.destroy();
      if (p.sender != null) try { p.sender.close(); } catch (Exception ignore) {}
      p.es.shutdownNow();
    }
    SttService stt = stts.remove(sid);
    if (stt != null) {
      try { stt.close(); } catch (IOException ignore) {}
    }
  }

  private void enqueue(WebSocketSession session, String json) {
    Pipe p = pipes.get(session.getId());
    if (p != null && p.sender != null && session.isOpen()) {
      p.sender.send(json);
    }
  }

  private static void safeClose(WebSocketSession session, CloseStatus status) {
    try { if (session != null && session.isOpen()) session.close(status); } catch (IOException ignore) {}
  }

  /** JSON 안전 이스케이프 */
  private static String escape(String s) {
    if (s == null) return "";
    StringBuilder sb = new StringBuilder(s.length() + 16);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"':  sb.append("\\\""); break;
        case '\\': sb.append("\\\\"); break;
        case '\n': sb.append("\\n");  break;
        case '\r': sb.append("\\r");  break;
        case '\t': sb.append("\\t");  break;
        default:
          if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
          else sb.append(c);
      }
    }
    return sb.toString();
  }

  /** 간단 쿼리 파서 */
  private static Map<String,String> parseQuery(URI uri) {
    Map<String,String> m = new HashMap<>();
    if (uri == null || uri.getQuery() == null) return m;
    String q = uri.getQuery();
    for (String kv : q.split("&")) {
      int i = kv.indexOf('=');
      if (i < 0) m.put(urlDecode(kv), "");
      else m.put(urlDecode(kv.substring(0,i)), urlDecode(kv.substring(i+1)));
    }
    return m;
  }
  private static String urlDecode(String s) {
    try { return URLDecoder.decode(s, "UTF-8"); } catch (Exception e) { return s; }
  }
}
