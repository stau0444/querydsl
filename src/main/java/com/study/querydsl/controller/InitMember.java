package com.study.querydsl.controller;

import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
// 코드 설명
//@Profile 을 하면 애플리케이션이 실행될때 @Profile에 지정된 이름을 찾아
//application.yml이 실행되고 @postConstruct가 실행 되면서
//DB에 샘플데이터를 채워 넣는다.
//postconstructor와 트랜젝션을 분리해줘야 스프링 라이프 사이클을 벗어나지 않는다.
@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private  final InitMemberService initMemberService;

    @PostConstruct
    public void init(){
        initMemberService.init();
    }

    @Component
    static class InitMemberService{

        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init(){
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i<100; i++){
                Team selectedTeam = i%2 ==0 ? teamA:teamB;
                em.persist(new Member("member"+i , i , selectedTeam));
            }
        }

    }
}
