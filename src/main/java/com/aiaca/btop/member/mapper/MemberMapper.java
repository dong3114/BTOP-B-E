package com.aiaca.btop.member.mapper;

import com.aiaca.btop.member.domain.MemberInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberMapper {
    // 1. 회원가입 관련
    int validateId(@Param("memberId") String memberId);         // ID 유효성 검증
    int validateNick(@Param("memberNick") String memberNick);   // 닉네임 유효성 검증
    int validatePhone(@Param("memberPhone") String memberPhone);// 휴대폰번호 유효성 검증
    void createMember(MemberInfo memberInfo);                   // 회원가입

    // 2. 회원 정보 조회 관련
    List<MemberInfo> getAllMemberList();
    MemberInfo getMemberInfo(@Param("memberNo") String memberNo);

    // 3. 로그인
    String getMemberNo(@Param("memberId") String memberId, @Param("memberPw") String memberPw);
}
