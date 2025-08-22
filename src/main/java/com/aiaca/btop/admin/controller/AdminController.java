package com.aiaca.btop.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiaca.btop.member.controller.MemberController;
import com.aiaca.btop.member.domain.LoginInfo;
import com.aiaca.btop.member.domain.MemberInfo;
import com.aiaca.btop.member.service.MemberService;
import com.aiaca.btop.security.jwt.JwtUtil;
import com.aiaca.btop.security.jwt.dto.JwtTokenDTO;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {
	
	private static final Logger log = LoggerFactory.getLogger(MemberController.class);
    final MemberService memberService;
    final JwtUtil jwtUtil;
	
    @PostMapping("")
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
        // 회원 권한 검증
        if(!member.getMemberNo().equals("2")) {
        	log.warn("권한이 없는 아이디 입니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 3. 토큰 정보 로드
        final String token = jwtUtil.generateToken(memberNo, member.getRoleLevel());
        long expires = jwtUtil.extractExpires(token);
        JwtTokenDTO body = new JwtTokenDTO(token, memberNo, member.getRoleLevel(), expires);

        return ResponseEntity.ok(body);
    }

}
