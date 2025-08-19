package com.aiaca.btop.member.domain;

import lombok.*;

@Getter @Setter
@Builder
@AllArgsConstructor
public class MemberInfo {
    private final String memberNo;
    private final String memberId;
    private final String memberPw;
    private final String memberNick;
    private final String memberPhone;
    private final String memberEmail;
    private final char memberGender;
    private final String memberBirth;
    private final String memberRegion;
    private final int roleLevel;
}
