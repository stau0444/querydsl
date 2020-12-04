package com.study.querydsl.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    public void member(){
        Team team = new Team("teamA");
        Team team2 = new Team("teamB");
        em.persist(team);
        em.persist(team2);
        Member member = new Member("memberA",1,team);
        Member member1 = new Member("memberB",1,team2);
        em.persist(member1);
        em.persist(member);

        List<Member> members= em.createQuery("select m from Member m").getResultList();
        for (Member member2 : members) {
            System.out.println("id="+member2.getId());
            System.out.println("name="+member2.getUsername());
            System.out.println("teamname="+member2.getTeam().getName());
        }
    }
    @Test
    public void testEntity(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("memberA",10,teamA);
        Member member2 = new Member("memberB",15,teamA);
        Member member3 = new Member("memberC",15,teamB);
        Member member4 = new Member("memberD",15,teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();

        List<Member> members = em.createQuery("select  m from Member m ", Member.class)
                .getResultList();
        for (Member member : members) {
            System.out.println("member = "+ member);
            System.out.println("member.team = "+ member.getTeam());
        }
    }
}