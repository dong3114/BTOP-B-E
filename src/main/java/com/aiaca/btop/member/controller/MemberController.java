package com.aiaca.btop.member.controller;

import com.aiaca.btop.member.domain.LoginInfo;
import com.aiaca.btop.member.domain.MemberInfo;
import com.aiaca.btop.member.service.MemberService;
import com.aiaca.btop.security.jwt.JwtUtil;
import com.aiaca.btop.security.jwt.dto.JwtTokenDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/member/")
public class MemberController {
    final MemberService memberService;
    final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @Validated LoginInfo loginInfo
    ) {
        // 1. 로그인 유효성 검증
        int check = memberService.validateLogin(loginInfo);
        if(check == 0) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        // 2. 회원 정보 로드
        String memberNo = memberService.getMemberNo(loginInfo.getMemberId(), loginInfo.getMemberPw());
        MemberInfo member = memberService.getMemberInfo(memberNo);
        // 3. 토큰 정보 로드
        final String token = jwtUtil.generateToken(memberNo, member.getRoleLevel());
        long expires = jwtUtil.extractExpires(token);
        JwtTokenDTO body = new JwtTokenDTO(token, memberNo, member.getRoleLevel(), expires);

        return ResponseEntity.ok(body);
    }

}
