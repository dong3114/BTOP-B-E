// JJ
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
    Process ffmpeg;
    OutputStream ffIn;
    InputStream ffOut;
    ExecutorService es = Executors.newFixedThreadPool(2);
    AtomicLong pcmBytes = new AtomicLong(0);
  }

  private final ObjectFactory<SttService> sttFactory;
  private final ConcurrentMap<String, Pipe> pipes = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, SttService> stts = new ConcurrentHashMap<>();

  public SttWsHandler(ObjectFactory<SttService> sttFactory) {
    this.sttFactory = sttFactory;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    try {
      ProcessBuilder pb = new ProcessBuilder(
        "ffmpeg","-hide_banner","-loglevel","error",
        "-i","pipe:0","-ac","1","-ar","16000","-f","s16le","pipe:1"
      );
      Process proc = pb.start();
      Pipe p = new Pipe();
      p.ffmpeg = proc;
      p.ffIn = proc.getOutputStream();
      p.ffOut = proc.getInputStream();
      pipes.put(session.getId(), p);

      // 2-인자 Listener (raw, standard)
      SttService stt = sttFactory.getObject();
      stt.start(new SttService.Listener() {
        @Override public void onPartial(String raw, String standard) {
          String std = (standard == null) ? "" : standard;
          safeSend(session,
            "{\"partial\":{\"stt\":\"" + escape(raw) + "\",\"standard\":\"" + escape(std) + "\"}}"
          );
        }
        @Override public void onFinal(String raw, String standard) {
          safeSend(session,
            "{\"final\":{\"stt\":\"" + escape(raw) + "\",\"standard\":\"" + escape(standard) + "\"}}"
          );
        }
        @Override public void onError(String msg, Throwable t) {
          safeSend(session, "{\"error\":\"" + escape(msg) + "\"}");
        }
      });
      stts.put(session.getId(), stt);

      // ffmpeg stdout(PCM) -> STT 공급
      p.es.submit(() -> {
        byte[] buf = new byte[4096];
        try (InputStream is = p.ffOut) {
          int n;
          while ((n = is.read(buf)) != -1 && session.isOpen()) {
            long total = p.pcmBytes.addAndGet(n);
            stt.feedPcm(buf, n);
            if (total % (16000 * 2) < 4096) {
              safeSend(session, "{\"partial\":\"pcm_bytes=" + total + "\"}");
            }
          }
        } catch (IOException e) {
          System.err.println("[WS] ffmpeg pipe error: " + e.getMessage());
        } finally {
          try { stt.stopAndFinalize(); } catch (Exception ignore) {}
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

    } catch (IOException e) {
      safeSend(session, "{\"error\":\"ffmpeg failed: " + escape(e.getMessage()) + "\"}");
      try { session.close(CloseStatus.SERVER_ERROR); } catch (IOException ignore) {}
    }
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    Pipe p = pipes.get(session.getId()); if (p == null) return;
    try {
      ByteBuffer payload = message.getPayload();
      if (payload.hasArray()) {
        p.ffIn.write(payload.array(), payload.position(), payload.remaining());
      } else {
        byte[] b = new byte[payload.remaining()];
        payload.get(b);
        p.ffIn.write(b);
      }
      p.ffIn.flush();
    } catch (IOException ignore) {}
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) {
    if ("stop".equalsIgnoreCase(message.getPayload())) {
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
    Pipe p = pipes.remove(session.getId());
    if (p != null) {
      try { p.ffIn.close(); } catch (IOException ignore) {}
      try { p.ffOut.close(); } catch (IOException ignore) {}
      if (p.ffmpeg != null && p.ffmpeg.isAlive()) p.ffmpeg.destroy();
      p.es.shutdownNow();
    }
    SttService stt = stts.remove(session.getId());
    if (stt != null) try { stt.close(); } catch (IOException ignore) {}
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

  /** sendMessage 안전 래퍼 */
  private static void safeSend(WebSocketSession session, String json) {
    try {
      if (session != null && session.isOpen()) {
        session.sendMessage(new TextMessage(json));
      }
    } catch (IOException ignore) {}
  }
}
