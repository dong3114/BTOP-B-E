package com.aiaca.btop.member.service;

import com.aiaca.btop.member.domain.LoginInfo;
import com.aiaca.btop.member.domain.MemberInfo;
import com.aiaca.btop.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{
    private final MemberMapper memberMapper;

    @Override
    public int createMember(MemberInfo memberInfo) {


        return 0;
    }

    @Override
    public int validateLogin(LoginInfo loginInfo) {
        return 0;
    }

    @Override
    public String getMemberNo(String memberId, String memberPw) {
        return "";
    }

    @Override
    public MemberInfo getMemberInfo(String memberNo) {
        return null;
    }

    @Override
    public List<MemberInfo> getAllMemberList() {
        return List.of();
    }
}
