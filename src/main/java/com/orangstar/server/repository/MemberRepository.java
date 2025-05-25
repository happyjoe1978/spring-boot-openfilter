package com.orangstar.server.repository;

import com.orangstar.server.entity.Member;
import com.orangstar.server.openfilter.OFRepository;
import com.orangstar.server.openfilter.OpenFilterRepository;

@OFRepository
public interface MemberRepository extends OpenFilterRepository<Member, String> {

}
