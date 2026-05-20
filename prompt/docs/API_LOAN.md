# 대출 API 명세서

---

## 1. 플랫폼 API (Platform → Frontend)

Base URL: `/loan`

---

### 1-1. 대출 심사 서류 조회
`GET /loan/review/documents`

**Request**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| - | - | 없음 |

**Response**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| documents | Array | 심사 서류 목록 |
| documents[].documentType | String | 서류 타입 |
| documents[].documentName | String | 서류명 |
| documents[].documentUrl | String | 서류 URL (웹뷰 로드용, CDN) |
| documents[].isMandatory | Boolean | 필수 여부 |

**Success**
| code | message | 상황 |
|------|---------|------|
| 2004 | 대출 심사 서류 조회가 완료되었습니다. | 정상 처리 |

**Error**
| code | message | 상황 |
|------|---------|------|
| 4000 | 필수 항목이 누락되었습니다 | 필수 파라미터 누락 |
| 5300 | 서류 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | DB 조회 실패 |
| 5900 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 알 수 없는 오류 |

---

### 1-2. 대출 심사 요청
`POST /loan/evaluation`

**Request**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| customerName | String | 고객명 |
| customerRrnPrefix | String | 주민등록번호 앞 7자리 (1회용 AES 암호화) |
| customerPhone | String | 연락처 |
| depositBankCode | String | 입금 은행 코드 (예: "020") |
| depositAccountNo | String | 입금 계좌번호 (1회용 AES 암호화) |
| documents | Array | 심사 서류 목록 |
| documents[].documentType | String | 서류 타입 (CREDIT_INFO_CONSENT / PERSONAL_INFO_CONSENT / EMPLOYMENT_CERT / INCOME_CERT) |
| documents[].signedContent | String | 작성된 서류 내용 (base64 인코딩) |
| documents[].agreedAt | String | 작성/서명 시각 (ISO-8601) |

**Response**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| applicationId | String | 심사 접수 고유 ID (상태 조회 시 사용) |
| receivedAt | String | 접수 일시 (ISO-8601) |

**Success**
| code | message | 상황 |
|------|---------|------|
| 2004 | 대출 심사 요청이 완료되었습니다. | 정상 처리 |

**Error**
| code | message | 상황 |
|------|---------|------|
| 4000 | 필수 항목이 누락되었습니다 | 필수 파라미터 누락 |
| 4001 | 대출 희망 금액을 확인해주세요 | requestedAmount ≤ 0 |
| 4002 | 소득 정보를 확인해주세요 | incomeInfo ≤ 0 |
| 4004 | 파일과 약관 수가 일치하지 않습니다 | 필수 서류 수 불일치 |
| 4110 | 고객 정보를 확인해주세요 | 주민번호 유효성 실패 |
| 4120 | 동의서 파일을 확인해주세요 | 파일 누락/손상 |
| 4124 | 필수 약관에 동의해주세요 | 필수 약관 미동의 |
| 5200 | 신용 평가 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | CSS 외부 연동 실패 |
| 5201 | 신용 평가 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요 | CSS 타임아웃 |
| 5300 | 심사 결과 산출 중 오류가 발생했습니다. 고객센터에 문의해주세요 | 한도/금리 산출 실패 |
| 5900 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 알 수 없는 오류 |

---

### 1-3. 대출 심사 상태 조회 (Polling)
`GET /loan/evaluation/{applicationId}/status`

**Request**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| applicationId | String | 심사 접수 고유 ID (Path Variable) |

**Response**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| applicationId | String | 심사 접수 고유 ID |
| evaluationStatus | String | 현재 심사 상태 (PENDING / APPROVED / REJECTED / FAILED) |
| requestedAt | String | 심사 요청 일시 (ISO-8601) |
| completedAt | String | 심사 완료 일시 (ISO-8601, 완료 전 null) |
| evaluationId | String | 심사 결과 고유 ID (완료 후 발급, 미완료 시 null) |
| approvedLimit | Int | 승인 한도 (승인 시, 그 외 null) |
| interestRate | Number | 적용 금리 (승인 시, 그 외 null) |
| rejectionCode | String | 거절 사유 코드 (거절 시, 그 외 null) |
| rejectionMessage | String | 거절 사유 메시지 (거절 시, 그 외 null) |
| availableProducts | Array | 추천 상품 목록 (승인 시, 그 외 null) |
| availableProducts[].loanProductCode | String | 상품 코드 |
| availableProducts[].loanProductName | String | 상품명 |
| availableProducts[].minAmount | Int | 최소 대출 금액 |
| availableProducts[].maxAmount | Int | 최대 대출 금액 |
| availableProducts[].interestRate | Number | 상품별 적용 금리 (%) |
| availableProducts[].loanPeriodMonths | Int | 대출 기간 (개월) |

**Success**
| code | message | 상황 |
|------|---------|------|
| 2004 | 대출 심사 상태 조회가 완료되었습니다. | 정상 처리 |

**Error**
| code | message | 상황 |
|------|---------|------|
| 4000 | 필수 항목이 누락되었습니다 | 필수 파라미터 누락 |
| 4400 | 유효하지 않은 심사 접수 건입니다 | applicationId 없음 |
| 5300 | 심사 상태 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | DB 조회 실패 |
| 5900 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 알 수 없는 오류 |

---

### 1-4. 대출 계약 서류 조회
`GET /loan/contract/documents/{loanProductCode}/{evaluationId}`

**Request**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| loanProductCode | String | 대출 상품 코드 (Path Variable) |
| evaluationId | String | 심사 결과 고유 ID (Path Variable) |

**Response**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| loanProductCode | String | 대출 상품 코드 |
| loanProductName | String | 대출 상품명 |
| approvedLimit | Int | 승인된 대출 한도 |
| documentUrl | String | 계약서 URL (웹뷰 로드용, CDN) |
| documents | Array | 계약 서류 목록 |
| documents[].documentType | String | 서류 타입 |
| documents[].documentName | String | 서류명 |
| documents[].documentUrl | String | 서류 URL (CDN) |
| documents[].isMandatory | Boolean | 필수 동의 여부 |

**Success**
| code | message | 상황 |
|------|---------|------|
| 2004 | 대출 계약 서류 조회가 완료되었습니다. | 정상 처리 |

**Error**
| code | message | 상황 |
|------|---------|------|
| 4000 | 필수 항목이 누락되었습니다 | 필수 파라미터 누락 |
| 4003 | 유효하지 않은 대출 상품입니다 | loanProductCode 없음/만료 |
| 4400 | 유효하지 않은 심사 결과입니다 | evaluationId 없음/만료 |
| 4401 | 대출 심사가 승인되지 않았습니다 | APPROVED 아닌 건 접근 |
| 5300 | 서류 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | DB 조회 실패 |
| 5900 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 알 수 없는 오류 |

---

### 1-5. 대출 실행
`POST /loan/contract/execution`

**Request**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| evaluationId | String | 심사 결과 고유 ID |
| depositAccountNo | String | 대출금 입금받을 본인 계좌 (1회용 AES 암호화) |
| accountPassword | String | 계좌 비밀번호 (디지털 봉투 - AES+RSA 암호화) |
| executeAmount | Int | 실제 실행 요청 금액 (한도 내) |
| repaymentPeriod | Int | 상환 기간 (개월, 12 / 24 / 36) |

**Response**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| loanId | String | 신규 생성된 대출 원장 고유 ID |
| borrowerName | String | 신청인명 |
| depositTransactionId | String | 입금 처리된 거래 고유 ID |
| loanBalance | Int | 대출 실행 후 대출 잔액 |
| executeAmount | Int | 실제 실행된 금액 |
| interestRate | Number | 최종 적용 금리 |
| repaymentStartDate | String | 상환 시작일 (ISO-8601) |
| maturityDate | String | 만기일 (ISO-8601) |

**Success**
| code | message | 상황 |
|------|---------|------|
| 2004 | 대출 체결이 완료되었습니다. | 정상 처리 |

**Error**
| code | message | 상황 |
|------|---------|------|
| 4000 | 필수 항목이 누락되었습니다 | 필수 파라미터 누락 |
| 4001 | 대출 희망 금액을 확인해주세요 | executeAmount ≤ 0 |
| 4110 | 유효하지 않은 계좌입니다 | 계좌 없음/유효하지 않음 |
| 4400 | 유효하지 않은 심사 결과입니다 | evaluationId 없음/만료 |
| 4402 | 이미 실행된 대출입니다 | 중복 실행 요청 |
| 4403 | 실행 금액이 승인 한도를 초과했습니다 | executeAmount > approvedLimit |
| 4404 | 대출 실행일로부터 60일 이내에는 고위험 상품 가입이 제한됩니다 | 불완전판매 방지 |
| 5900 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 알 수 없는 오류 |

---

## 2. 은행 코어 API (Bank Core ← Platform)

Base URL: `/bank/loan`

> 플랫폼은 Zero-Knowledge Pass-through 방식으로 암호문 원본을 그대로 전달.
> 모든 요청에 JWS 전자서명 부착, mTLS로 통신.

---

### 2-1. 대출 심사 서류 조회
`GET /bank/loan/evaluation/terms`

**Request**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| loanProductCode | String | 대출 상품 코드 (Query Parameter) |
| loanBankCode | String | 대출 은행 코드 (예: "020") |

**Response**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| loanProductCode | String | 대출 상품 코드 |
| loanProductName | String | 대출 상품명 |
| documents | Array | 심사 서류 목록 |
| documents[].documentType | String | 서류 타입 |
| documents[].documentName | String | 서류명 |
| documents[].documentUrl | String | 서류 URL (CDN) |
| documents[].isMandatory | Boolean | 필수 여부 |

**Success**
| code | message | 상황 |
|------|---------|------|
| 2004 | 대출 심사 서류 조회가 완료되었습니다. | 정상 처리 |

**Error**
| code | message | 상황 |
|------|---------|------|
| 4000 | 필수 항목이 누락되었습니다 | 필수 파라미터 누락 |
| 4200 | 인증 정보를 확인해주세요 | JWT/mTLS 없음/만료 |
| 4003 | 유효하지 않은 대출 상품입니다 | loanProductCode 없음/만료 |
| 5300 | 서류 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | DB 조회 실패 |
| 5900 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 알 수 없는 오류 |

---

### 2-2. 대출 심사 요청
`POST /bank/loan/evaluation`

**Request**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| customerName | String | 고객명 |
| customerRrnPrefix | String | 주민등록번호 앞 7자리 (AES 암호화) |
| customerPhone | String | 연락처 |
| depositBankCode | String | 입금 은행 코드 |
| depositAccountNo | String | 입금 계좌번호 (AES 암호화) |
| loanBankCode | String | 대출 은행 코드 |
| documents | Array | 심사 서류 목록 |
| documents[].documentType | String | 서류 타입 (CREDIT_INFO_CONSENT / PERSONAL_INFO_CONSENT / EMPLOYMENT_CERT / INCOME_CERT) |
| documents[].signedContent | String | 작성된 서류 내용 (base64) |
| documents[].agreedAt | String | 서명 시각 (ISO-8601) |

**Response**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| isApproved | Boolean | 심사 승인 여부 |
| approvedLimit | Int | 최종 산출된 대출 한도 (거절 시 null) |
| interestRate | Number | 적용 금리 % (거절 시 null) |
| evaluationId | String | 심사 결과 고유 ID |

**Success**
| code | message | 상황 |
|------|---------|------|
| 2004 | 대출 심사가 완료되었습니다. | 승인 |
| 2004 | 대출 심사 결과 승인이 거절되었습니다. | 거절 |

**Error**
| code | message | 상황 |
|------|---------|------|
| 4000 | 필수 항목이 누락되었습니다 | 필수 파라미터 누락 |
| 4110 | 고객 정보를 확인해주세요 | 주민번호 유효성 실패 |
| 4120 | 동의서 파일을 확인해주세요 | 파일 누락/손상 |
| 4121 | 지원하지 않는 파일 형식입니다 | PDF 외 파일 |
| 4122 | 파일 크기가 초과되었습니다 | 용량 초과 |
| 4123 | 유효하지 않은 약관입니다 | termId 없음/만료 |
| 4124 | 필수 약관에 동의해주세요 | 필수 약관 미동의 |
| 4200 | 인증 정보를 확인해주세요 | JWT/mTLS 없음/만료 |
| 5200 | 신용 평가 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | CSS 외부 연동 실패 |
| 5201 | 신용 평가 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요 | CSS 타임아웃 |
| 5300 | 심사 결과 산출 중 오류가 발생했습니다. 고객센터에 문의해주세요 | 한도/금리 산출 실패 |
| 5900 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 알 수 없는 오류 |

---

### 2-3. 대출 심사 상태 조회 (Polling)
`GET /bank/loan/evaluation/{evaluationId}/status`

**Request**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| evaluationId | String | 심사 접수 고유 ID (Path Variable) |

**Response**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| evaluationId | String | 심사 접수 고유 ID |
| evaluationStatus | String | 현재 심사 상태 (PENDING / APPROVED / REJECTED / FAILED) |
| requestedAt | String | 심사 요청 일시 (ISO-8601) |
| completedAt | String | 심사 완료 일시 (ISO-8601, 완료 전 null) |
| approvedLimit | Int | 승인 한도 (승인 시, 그 외 null) |
| interestRate | Number | 적용 금리 (승인 시, 그 외 null) |
| rejectionCode | String | 거절 사유 코드 (거절 시, 그 외 null) |
| rejectionMessage | String | 거절 사유 메시지 (거절 시, 그 외 null) |
| availableProducts | Array | 추천 상품 목록 (승인 시, 그 외 null) |
| availableProducts[].loanProductCode | String | 상품 코드 |
| availableProducts[].loanProductName | String | 상품명 |
| availableProducts[].minAmount | Int | 최소 대출 금액 |
| availableProducts[].maxAmount | Int | 최대 대출 금액 |
| availableProducts[].interestRate | Number | 상품별 적용 금리 (%) |
| availableProducts[].loanPeriodMonths | Int | 대출 기간 (개월) |

**Success**
| code | message | 상황 |
|------|---------|------|
| 2004 | 심사 상태 조회가 완료되었습니다. | 정상 처리 |

**Error**
| code | message | 상황 |
|------|---------|------|
| 4000 | 필수 항목이 누락되었습니다 | 필수 파라미터 누락 |
| 4200 | 인증 정보를 확인해주세요 | JWT/mTLS 없음/만료 |
| 4400 | 유효하지 않은 심사 접수 건입니다 | evaluationId 없음 |
| 5300 | 심사 상태 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | DB 조회 실패 |
| 5900 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 알 수 없는 오류 |

---

### 2-4. 대출 계약 서류 조회
`GET /bank/loan/contract/terms/{loanProductCode}/{evaluationId}`

**Request**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| loanProductCode | String | 대출 상품 코드 (Path Variable) |
| evaluationId | String | 심사 결과 고유 ID (Path Variable) |

**Response**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| loanProductCode | String | 대출 상품 코드 |
| loanProductName | String | 대출 상품명 |
| approvedLimit | Int | 승인된 대출 한도 |
| documents | Array | 계약 서류 목록 |
| documents[].documentType | String | 서류 타입 |
| documents[].documentName | String | 서류명 |
| documents[].documentUrl | String | 서류 URL (CDN) |
| documents[].isMandatory | Boolean | 필수 동의 여부 |

**Success**
| code | message | 상황 |
|------|---------|------|
| 2004 | 대출 계약 서류 조회가 완료되었습니다. | 정상 처리 |

**Error**
| code | message | 상황 |
|------|---------|------|
| 4000 | 필수 항목이 누락되었습니다 | 필수 파라미터 누락 |
| 4200 | 인증 정보를 확인해주세요 | JWT/mTLS 없음/만료 |
| 4003 | 유효하지 않은 대출 상품입니다 | loanProductCode 없음/만료 |
| 4400 | 유효하지 않은 심사 결과입니다 | evaluationId 없음/만료 |
| 4401 | 대출 심사가 승인되지 않았습니다 | APPROVED 아닌 건 접근 |
| 5300 | 서류 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | DB 조회 실패 |
| 5900 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 알 수 없는 오류 |

---

### 2-5. 대출 실행
`POST /bank/loan/execution`

**Request**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| evaluationId | String | 심사 결과 고유 ID |
| depositAccountNo | String | 대출금 입금받을 본인 계좌번호 (AES 암호화) |
| accountPassword | String | 계좌 비밀번호 (디지털 봉투 - AES+RSA 암호화) |
| executeAmount | Int | 실제 실행 요청 금액 (한도 내) |
| repaymentPeriod | Int | 상환 기간 (개월, 12 / 24 / 36) |

**Response**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| loanId | String | 신규 생성된 대출 원장 고유 ID |
| borrowerName | String | 신청인명 |
| depositTransactionId | String | 입금 처리된 거래 고유 ID |
| loanBalance | Int | 대출 실행 후 대출 잔액 |
| executeAmount | Int | 실제 실행된 금액 |
| interestRate | Number | 최종 적용 금리 |
| repaymentStartDate | String | 상환 시작일 (ISO-8601) |
| maturityDate | String | 만기일 (ISO-8601) |

**Success**
| code | message | 상황 |
|------|---------|------|
| 2004 | 대출 체결이 완료되었습니다. | 정상 처리 |

**Error**
| code | message | 상황 |
|------|---------|------|
| 4000 | 필수 항목이 누락되었습니다 | 필수 파라미터 누락 |
| 4001 | 대출 실행 금액을 확인해주세요 | executeAmount ≤ 0 |
| 4022 | 비밀번호가 일치하지 않습니다 | 비밀번호 오류 |
| 4110 | 입금 계좌가 유효하지 않습니다 | 계좌 없음/LOCKED/CLOSED |
| 4200 | 인증 정보를 확인해주세요 | JWT/mTLS 없음/만료 |
| 4400 | 유효하지 않은 심사 결과입니다 | evaluationId 없음/만료 |
| 4401 | 대출 심사가 승인되지 않았습니다 | APPROVED 아닌 건 |
| 4402 | 이미 실행된 대출입니다 | 중복 실행 요청 |
| 4403 | 실행 금액이 승인 한도를 초과했습니다 | executeAmount > approvedLimit |
| 4404 | 대출 실행일로부터 60일 이내에는 고위험 상품 가입이 제한됩니다 | 불완전판매 방지 |
| 5100 | 대출 실행 중 오류가 발생했습니다. 고객센터에 문의해주세요 | 입금 처리 실패 |
| 5300 | 거래 기록 저장에 실패했습니다. 고객센터에 문의해주세요 | DB Insert 실패 |
| 5900 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 알 수 없는 오류 |

---

## 3. 플로우 요약

```
Frontend → Platform Backend → Bank Core

1. GET  /loan/review/documents              → GET  /bank/loan/evaluation/terms
2. POST /loan/evaluation                    → POST /bank/loan/evaluation
3. GET  /loan/evaluation/{id}/status        → GET  /bank/loan/evaluation/{id}/status  (5초 Polling)
4. GET  /loan/contract/documents/{code}/{id} → GET  /bank/loan/contract/terms/{code}/{id}
5. POST /loan/contract/execution            → POST /bank/loan/execution
```

## 4. 보안 참고사항

- 플랫폼은 고객 금융 데이터(계좌번호, 주민번호, 계좌 비밀번호)를 **절대 복호화하지 않음** (Zero-Knowledge)
- 계좌 비밀번호: 프론트에서 1회용 AES 키로 암호화 후 은행 RSA 공개키로 AES 키를 암호화 (디지털 봉투)
- 플랫폼 → 은행 통신: mTLS + JWS 전자서명
- Polling GUID: 동일 `evaluationId`에 대해 Redis TTL 30분으로 GUID 재사용 (LN-{UUID})
