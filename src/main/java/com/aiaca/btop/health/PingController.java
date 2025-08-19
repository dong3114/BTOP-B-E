//JJ
package com.aiaca.btop.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
  @GetMapping("/api/ping")
  public String ping() { return "pong"; }
}

//백엔드 핑체크 
//콘솔에 curl http://localhost:8090/api/ping 입력 -> 응답 pong 이면 OK