package com.aiaca.btop.member.service;

import com.aiaca.btop.member.domain.LoginInfo;
import com.aiaca.btop.member.domain.MemberInfo;
import com.aiaca.btop.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{
    private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);
    private final MemberMapper memberMapper;

    @Override
    @Transactional // RuntimeException/Error 발생 시 자동 롤백
    public MemberInfo register(MemberInfo memberInfo) {
        if (memberMapper.validateId(memberInfo.getMemberId()) > 0) {
            log.error("아이디가 중복입니다.");
            throw new IllegalStateException("아이디가 중복입니다.");
        }
        if (memberMapper.validateNick(memberInfo.getMemberNick()) > 0) {
            log.error("닉네임이 중복입니다.");
            throw new IllegalStateException("닉네임이 중복입니다.");
        }
        if (memberInfo.getMemberPhone() != null && !memberInfo.getMemberPhone().isBlank()) {
            if (memberMapper.validatePhone(memberInfo.getMemberPhone()) > 0) {
                log.error("휴대폰 번호가 중복입니다.");
                throw new IllegalStateException("휴대폰 번호가 중복입니다.");
            }
        }
        memberMapper.createMember(memberInfo);

        return memberInfo;
    }

    @Override
    public int validateLogin(LoginInfo loginInfo) {
        if (loginInfo == null ||
                isBlank(loginInfo.getMemberId()) ||
                isBlank(loginInfo.getMemberPw())) {
            return 0;
        }
        final String memberNo = memberMapper.getMemberNo(
                loginInfo.getMemberId().trim(),
                loginInfo.getMemberPw().trim()
        );
        return (memberNo != null) ? 1 : 0;
    }

    @Override
    public String getMemberNo(String memberId, String memberPw) {
        if (isBlank(memberId) || isBlank(memberPw)) return null;
        return memberMapper.getMemberNo(memberId.trim(), memberPw.trim());
    }

    @Override
    public MemberInfo getMemberInfo(String memberNo) {
        if (isBlank(memberNo)) return null;
        return memberMapper.getMemberInfo(memberNo.trim());
    }

    @Override
    public List<MemberInfo> getAllMemberList() {
        return memberMapper.getAllMemberList();
    }

    @Override
    public int validateId(String memberId) {
        return memberMapper.validateId(memberId);
    }

    @Override
    public int validateNick(String memberNick) { return memberMapper.validateNick(memberNick); }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
