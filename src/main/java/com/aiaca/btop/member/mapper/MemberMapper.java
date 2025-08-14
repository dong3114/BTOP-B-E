package com.aiaca.btop.member.mapper;

import com.aiaca.btop.member.domain.MemberInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper {
    void createMember(MemberInfo memberInfo);
    void deleteMember(MemberInfo memberInfo);

}
