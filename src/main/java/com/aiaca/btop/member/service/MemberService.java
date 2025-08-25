package com.aiaca.btop.member.service;

import com.aiaca.btop.member.domain.LoginInfo;
import com.aiaca.btop.member.domain.MemberInfo;

import java.util.List;

public interface MemberService {
    // 1. 회원가입 관련
    MemberInfo register(MemberInfo memberInfo);
    int validateId(String memberId);
    int validateNick(String memberNick);

    // 2. 로그인 관련
    int validateLogin(LoginInfo loginInfo);
    String getMemberNo(String memberId, String memberPw);

    // 3. 회원 정보 조회
    List<MemberInfo> getAllMemberList();
    MemberInfo getMemberInfo(String memberNo);


}
