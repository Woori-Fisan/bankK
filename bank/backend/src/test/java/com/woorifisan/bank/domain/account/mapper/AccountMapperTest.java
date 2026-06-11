package com.woorifisan.bank.domain.account.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.woorifisan.bank.domain.account.model.Account;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 실제 DB 또는 설정된 DB 사용
@ActiveProfiles("test") // 테스트 프로파일 사용
@Sql("/sql/test-data.sql") // 테스트 데이터 삽입
class AccountMapperTest {

    @Autowired
    private AccountMapper accountMapper;

    @Test
    @DisplayName("계좌 ID로 계좌 정보를 단건 조회할 수 있다")
    void 계좌_ID_조회_테스트() {
        // given
        // 미리 삽입된 더미 데이터의 ID (1번)를 기준으로 테스트
        Long targetId = 1L;

        // when
        Optional<Account> result = accountMapper.findById(targetId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(targetId);
        System.out.println("계좌번호 해시: " + result.get().getAccountNoHash());
        System.out.println("비밀번호 해시: " + result.get().getPassword());
        System.out.println("현재 잔액: " + result.get().getBalance());
    }
}
