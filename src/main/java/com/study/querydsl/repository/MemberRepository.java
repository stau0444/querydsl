package com.study.querydsl.repository;

import com.study.querydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member,Long>,MemberRepositoryCustom , QuerydslPredicateExecutor<Member> {
    //메서드 이름으로 쿼리가 만들어지는 방식으로 만들어짐
    List<Member> findByUsername(String username);


}
