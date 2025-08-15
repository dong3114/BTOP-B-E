// PATH: BTOP-B-E/src/main/java/com/aiaca/btop/ws/SttWsHandler.java
package com.aiaca.btop.ws;

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
  private final ConcurrentMap<String, Pipe> pipes = new ConcurrentHashMap<>();

  @Override public void afterConnectionEstablished(WebSocketSession session) {
    try {
      ProcessBuilder pb = new ProcessBuilder(
        "ffmpeg","-hide_banner","-loglevel","error",
        "-i","pipe:0","-ac","1","-ar","16000","-f","s16le","pipe:1"
      );
      Process proc = pb.start();
      Pipe p = new Pipe(); p.ffmpeg=proc; p.ffIn=proc.getOutputStream(); p.ffOut=proc.getInputStream();
      pipes.put(session.getId(), p);

      p.es.submit(() -> {  // stdout(PCM) 읽기 → 진행상황 전송(데모)
        byte[] buf = new byte[4096];
        try (InputStream is = p.ffOut) {
          int n; while ((n = is.read(buf)) != -1 && session.isOpen()) {
            long total = p.pcmBytes.addAndGet(n);
            if (total % (16000*2) < 4096)
              session.sendMessage(new TextMessage("{\"partial\":\"pcm_bytes=" + total + "\"}"));
          }
        } catch (IOException ignore) {}
      });
      p.es.submit(() -> {  // stderr 로그
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8))) {
          for (String line; (line = br.readLine()) != null; ) System.err.println("[ffmpeg] " + line);
        } catch (IOException ignore) {}
      });
    } catch (IOException e) {
      try {
        session.sendMessage(new TextMessage("{\"error\":\"ffmpeg failed: " + e.getMessage().replace("\"","'") + "\"}"));
        session.close(CloseStatus.SERVER_ERROR);
      } catch (IOException ignore) {}
    }
  }

  @Override protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    Pipe p = pipes.get(session.getId()); if (p == null) return;
    try {
      ByteBuffer payload = message.getPayload();
      if (payload.hasArray()) p.ffIn.write(payload.array(), payload.position(), payload.remaining());
      else { byte[] b = new byte[payload.remaining()]; payload.get(b); p.ffIn.write(b); }
      p.ffIn.flush();
    } catch (IOException ignore) {}
  }

  @Override public void handleTextMessage(WebSocketSession session, TextMessage message) {
    if ("stop".equalsIgnoreCase(message.getPayload())) {
      closePipe(session);
      try { session.close(CloseStatus.NORMAL); } catch (IOException ignore) {}
    }
  }

  @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { closePipe(session); }

  private void closePipe(WebSocketSession session) {
    Pipe p = pipes.remove(session.getId()); if (p == null) return;
    try { p.ffIn.close(); } catch (IOException ignore) {}
    try { p.ffOut.close(); } catch (IOException ignore) {}
    if (p.ffmpeg != null && p.ffmpeg.isAlive()) p.ffmpeg.destroy();
    p.es.shutdownNow();
  }
}
