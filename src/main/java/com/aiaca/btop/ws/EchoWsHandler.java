// JJ
package com.aiaca.btop.ws;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class EchoWsHandler extends TextWebSocketHandler {
  @Override public void afterConnectionEstablished(WebSocketSession s) throws Exception {
    s.sendMessage(new TextMessage("connected"));
  }
  @Override protected void handleTextMessage(WebSocketSession s, TextMessage m) throws Exception {
    s.sendMessage(new TextMessage("echo: " + m.getPayload()));
  }
}
