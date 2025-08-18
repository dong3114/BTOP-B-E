package com.aiaca.btop.member.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Builder
@AllArgsConstructor
public class LoginInfo {
    private final String memberId;
    private final String memberPw;
}
