package com.aiaca.btop.member.controller;

import com.aiaca.btop.member.domain.LoginInfo;
import com.aiaca.btop.member.domain.MemberInfo;
import com.aiaca.btop.member.service.MemberService;
import com.aiaca.btop.security.jwt.JwtUtil;
import com.aiaca.btop.security.jwt.dto.JwtTokenDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/member")
public class MemberController {
    private static final Logger log = LoggerFactory.getLogger(MemberController.class);
    final MemberService memberService;
    final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @Validated LoginInfo loginInfo
    ) {
        // 1. 로그인 유효성 검증
        int check = memberService.validateLogin(loginInfo);
        if(check == 0){
            log.warn("아이디가 올바르지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 2. 회원 정보 로드
        String memberNo = memberService.getMemberNo(loginInfo.getMemberId(), loginInfo.getMemberPw());
        MemberInfo member = memberService.getMemberInfo(memberNo);
        // 3. 토큰 정보 로드
        final String token = jwtUtil.generateToken(memberNo, member.getRoleLevel());
        long expires = jwtUtil.extractExpires(token);
        JwtTokenDTO body = new JwtTokenDTO(token, memberNo, member.getRoleLevel(), expires);

        return ResponseEntity.ok(body);
    }

    // 아이디 중복 검사 조회
    @GetMapping("/validate/id")
    public ResponseEntity<?> validateId(@RequestParam String memberId) {
        boolean available = memberService.validateId(memberId) == 0;
        return ResponseEntity.ok(Map.of("available", available));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated @RequestBody MemberInfo memberInfo) {
        MemberInfo saved = memberService.register(memberInfo);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


}
