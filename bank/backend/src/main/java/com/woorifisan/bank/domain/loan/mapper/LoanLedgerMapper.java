package com.woorifisan.bank.domain.loan.mapper;

import com.woorifisan.bank.domain.loan.model.LoanLedger;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoanLedgerMapper {

    // 조회
    Optional<LoanLedger> findById(Long id);
    // 대출번호로 조회
    Optional<LoanLedger> findByLoanNo(String loanNo);
    // 특정 고객의 현재 실행 중인 대출 전체 조회
    List<LoanLedger> findActiveByCustomerId(Long customerId);

    // 등록/수정
    // 신청 단계 : 최초 행 생성. status = SUBMITTED
    void insert(LoanLedger loanLedger);
    // 심사 단계 : @Async 심사 완료 후 결과 기록
    void updateReviewResult(LoanLedger loanLedger);
    // 실행 단계 : 대출금 실제 지급 후 실행 정보 기록
    // 상품ID, 실행금액, 금리, 상환방식, 기간, 시작일/만기일, status=ACTIVE 업데이트
    void updateExecution(LoanLedger loanLedger);

    // 중복·방지 체크
    // 동일 고객의 SUBMITTED 상태 건이 이미 존재하는지 확인
    boolean existsPendingByCustomerId(Long customerId);
    // 동일 고객이 동일 상품으로 60일 이내 ACTIVE 대출을 보유 중인지 확인
    boolean existsRecentActiveByCustomerAndProduct(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId
    );
}
