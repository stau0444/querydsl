package com.study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.annotations.QueryProjection;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.dto.MemberDto;
import com.study.querydsl.dto.QMemberDto;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.entity.QTeam;
import com.study.querydsl.entity.Team;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static com.study.querydsl.entity.QMember.member;
import static com.study.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;
    //querydsl을 사용할때는 QueryFactory를 만들어줘야한고 entitymanager를 파라미터로 넣어줘야한다
    //EntityManager가 멀티쓰레드에 맞춰 설계되었기떄문에
    //필드레벨로 가지고와도 상관없다.
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("memberA",10,teamA);
        Member member2 = new Member("memberB",15,teamA);
        Member member3 = new Member("memberC",15,teamB);
        Member member4 = new Member("memberD",45,teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }
    //JPQL로 member1 찾기
    @Test
    public void  startJPQL(){
        Member findMember = em.createQuery("select m from Member m " +
                " where m.username = :username", Member.class)
                .setParameter("username", "memberA")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("memberA");
    }

    //QueryDsl로 member1 찾기
    @Test
    public void startQuerydsl(){
        //찾으려는 엔티티의 Q파일을 가져와야한다.

        //장점
        //파라미터 바인딩을 따로 해주지 않아도 된다.
        //오류가 있을시 실행전 컴파일 단계에서 오류가 터진다.
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("memberA"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("memberA");
    }
    //where 절 .and version
    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("memberA")
                        .and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo(("memberA"));
    }

    //where 절 ()version
    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("memberA"),member.age.eq(10))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo(("memberA"));
    }

    @Test
    public void resultFetch(){
        //리스트
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();
        //단건
        //Member fetchOne = queryFactory
          //      .selectFrom(member)
            //    .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        //여러가지 페이징 정보를 포함한다.
        QueryResults<Member> results = queryFactory.selectFrom(member)
                .fetchResults();
        System.out.println("limit======"+results.getLimit());
        System.out.println("total======="+results.getTotal());
        List<Member> list = results.getResults();
        //카운트 쿼리만 가져옴
        long count = queryFactory.selectFrom(member).fetchCount();
        System.out.println("count========"+count);
    }
    /*
        정렬
        예시) 회원 정렬 순서
        1.회원 나이 내림차순(desc)
        2.회원 이름 올림차순(asc)
        *회원 이름이 없을 경우 마지막에 출력 (nulls last)
     */

    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    public  void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    // select 절에 원하는 데이터를 골라서 받아올경우
    //반환타입이 Tuple이 된다.
    //Tuple은 여러개의 타입이 섞여있을때 사용한다.
    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        //querydsl에서 제공하는 tuple 이기 떄문에 위에
        //select 절에 적혀 있는 그대로 받아 사용할 수 있다
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(55);
        assertThat(tuple.get(member.age.avg())).isEqualTo(13.75);
        assertThat(tuple.get(member.age.max())).isEqualTo(15);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }
    //팀이름과 각팀의 평균 연령
    @Test
    public void group() throws  Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(12.5);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(15);

    }

    //queryDsl 조인

    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("memberA","memberB");
    }

    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("teamA");
    }

    //join on 절 활용
    /*
        예) 회원과 팀을 조인하면서 팀이름이 teamA인 팀만 조인,회원은 모두 조회
        JPQL:select m,t from Member m left join m.team t on t.name ='teamA'
     */
    @Test
    public  void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("Tuple=" +tuple);
        }
    }
    
    //연관관계가 없는 엔티티를 외부조인
    //회원의 이름과 팀이름이 같은 대상 외부조인
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member,team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple= "+ tuple);
        }
    }
    //QueryDsl fetch join

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void  fethchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //프록싱 초기화가 된 객체인지 아닌지를 검증해준다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        System.out.println("loaded=--------------- " + loaded);
        assertThat(loaded).as("페치조인미적용").isFalse();
    }
    @Test
    public void  fetchJoinUse(){
        em.flush();
        em.clear();


        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        //프록싱 초기화가 된 객체인지 아닌지를 검증해준다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).isTrue();
    }
    
    //querydsl 서브쿼리

    @Test
    public  void subQuery(){
        //서브쿼리로 들어가는 쿼리가 밖의 쿼리와 별칭이 겹치면 안되기떄문에
        //서브쿼리로 들어갈 Qtype엔티티를  새로 만들어준다
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(45);
    }

    @Test
    public  void subQueryGoe(){
        //서브쿼리로 들어가는 쿼리가 밖의 쿼리와 별칭이 겹치면 안되기떄문에
        //서브쿼리로 들어갈 Qtype엔티티를  새로 만들어준다
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(45);
    }

    @Test
    public  void subQueryIn(){
        //서브쿼리로 들어가는 쿼리가 밖의 쿼리와 별칭이 겹치면 안되기떄문에
        //서브쿼리로 들어갈 Qtype엔티티를  새로 만들어준다
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                        .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(15,15,45);
    }
    
    
    //select 절의 서브쿼리
    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                ).from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple= " + tuple);
        }
    }

    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 40)).then("21~30")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s=" + s);
        }
    }
    //셀렉트절에 나열되는 것을 프로젝션이라한다.
    @Test
    public void simpleProjection(){
        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s=======" + s);
        }
    }
    //tuple은 리파지토리 계층에서쓰고 밖으로 나갈때는 Dto로 감싸서 내보내는
    //것이좋다
    @Test
    public  void  tupleProjection(){
        List<Tuple> fetch = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : fetch) {
            System.out.println("username="+tuple.get(member.username));
            System.out.println("age= " +tuple.get(member.age));
        }
    }
    
    //JPQL을 통한 DTO 조회(생성자 방식만 지원)
    @Test
    public void findDtoByJPQL(){
        List<MemberDto> resultList = em.createQuery("select new com.study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto="+ memberDto);
        }
    }
    //QueryDsl을 통한 Dto 조회
    //세가지 방법을 지원한다.
        
    //1. setter를 활용한 방법 
    //기본생성자가 필요하다
    @Test
    public void finDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("Dto="+ memberDto);
        }
    }
    //2.필드를 활용한 방법
    @Test
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("Dto="+ memberDto);
        }
    }

    //@QueryProjection을 이용한 Dto조회
    //Dto의 생성자 위에 명시하고 gradle에 querydsl을 컴파일 시켜줘야  한다
    //Dto를 q파일로 만들어 관리하기때문에
    //select절에 타입이 맞지않는다면 컴파일시 오류를 잡을 수 있다
    //q파일로 조회된 dto는 다시 원형 MemberDto로 바뀌어서 리턴된다
    @Test
    public void findByQueryProjection(){
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberdto=" + memberDto);
        }
    }

    //querydsl 동적쿼리 해결 방법
    //1.booleanbuilder

    @Test
    public  void dynamicQuery_Booleanbuilder(){
        String usernameParam = "memberA";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }


        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }
    //2. where문에 다중파라미터 사용하는 방법(훨씬 깔끔하다)
        //메서드를 재활용할 수 있다.
        //쿼리 가독성이 높아진다.
        //메서드간의 조합이 가능하다.
    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "memberA";
        Integer ageParam = 10;
        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond) , ageEq(ageCond))
                //.where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
//        if(usernameCond == null ){
//            return null;
//        }else{
//            return member.username.eq(usernameCond);
//        }

        //간단할 경우 3항연산자 사용가능
        return usernameCond != null ? member.username.eq(usernameCond) : null ;
     }

    private BooleanExpression ageEq(Integer ageCond) {
            return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private  BooleanExpression allEq(String usernameCond,Integer ageCond){
       return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    //벌크 연산 && 배치 쿼리
    //업데이트 쿼리를 DB에 직접 날리기 때문에
    //영속성 컨텍스트에 올라가있는 것과 데이터가 차이가 생긴다.
    //영속성 컨텍스트가 항상 우선권을 갖기 떄문에
    //DB에 쿼리가 날라가서 다시가져왓을때
    //이미 영속성 컨택스트에 값이 있다면
    //DB에 있는 값을 날려버리고
    //영속성 컨텍스트에 있는 것을 남긴다.
    //em.flush, em.clear로 초기화 시켜주는 것이 좋다
    @Test
    public void bulkUpdate(){
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        em.flush();
        em.clear();

        List<Member> result =
                queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : result) {
            System.out.println("member===" + member1);
        }

    }
    @Test
    public void bulkAdd(){
        queryFactory
                .update(member)
                .set(member.age,member.age.add(1))
                //.set(member.age,member.age.multiply(2))
                .execute();
    }

    @Test
    public  void bulkDelete(){
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    //sql function
    @Test
    public  void sqlFunction(){
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0} , {1} , {2})"
                        , member.username, "member", "M"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("result = " + s);
        }
    }

    @Test
    public void sqlFunciton2(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                //.where(member.username.eq(
                  //      Expressions.stringTemplate("function('lower',{0})", member.username)
                .where(member.username.eq(member.username.lower()
        )).fetch();
        for (String s : result) {
            System.out.println("s===" + s);
        }
    }

}

