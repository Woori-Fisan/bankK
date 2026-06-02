-- ==========================================
-- 약관 HTML 본문 현실화 업데이트
-- 준법감시인 심의필 2025-9628 (우리은행 가계대출 상품설명서 기준)
-- ==========================================

-- ==========================================
-- 1. 개인신용정보 수집·이용·제공 동의서
-- ==========================================
UPDATE bank_terms SET terms_content = '<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><style>
body{font-family:-apple-system,BlinkMacSystemFont,"Malgun Gothic","맑은 고딕",sans-serif;padding:24px 28px;font-size:14px;line-height:1.9;color:#1a1a1a}
h1{font-size:18px;font-weight:700;border-bottom:2px solid #1a1a1a;padding-bottom:10px;margin-bottom:20px}
h2{font-size:15px;font-weight:700;margin-top:22px;margin-bottom:8px;color:#1a1a1a}
h3{font-size:14px;font-weight:700;margin-top:16px;margin-bottom:6px}
p{margin:6px 0}
ul,ol{padding-left:24px;margin:6px 0}
li{margin:4px 0}
table{width:100%;border-collapse:collapse;margin:12px 0;font-size:13px}
th,td{border:1px solid #ccc;padding:8px 10px;text-align:left}
th{background:#f5f7fa;font-weight:700}
.notice{background:#fff8e1;border-left:4px solid #f59e0b;padding:12px 16px;margin:16px 0;font-size:13px}
.required{color:#dc2626;font-weight:700}
</style></head><body>
<h1>개인신용정보 수집·이용·제공 동의서</h1>
<p>우리피산 주식회사(이하 "회사")는 「개인정보 보호법」, 「신용정보의 이용 및 보호에 관한 법률」 및 「금융소비자 보호에 관한 법률」에 따라 귀하의 개인신용정보를 아래와 같이 수집·이용·제공하고자 합니다. 내용을 충분히 읽고 동의 여부를 결정하여 주시기 바랍니다.</p>

<div class="notice">
<strong>※ 동의 거부 권리 안내</strong><br>
귀하는 개인신용정보 수집·이용·제공에 대한 동의를 거부할 권리가 있습니다. 다만, 필수 항목에 동의하지 않으실 경우 대출 심사 및 관련 서비스 제공이 불가합니다.
</div>

<h2>제1조 (수집·이용 목적)</h2>
<ul>
<li>대출 신청 접수, 본인 확인 및 심사</li>
<li>대출 한도·금리 산정 및 계약 체결·이행</li>
<li>금융사고 조사·예방 및 부정 이용 방지</li>
<li>법령 및 금융감독 규정 준수</li>
<li>민원 처리 및 분쟁 해결</li>
</ul>

<h2>제2조 (수집·이용 항목)</h2>
<h3>가. 필수 수집 항목</h3>
<table>
<tr><th>구분</th><th>수집 항목</th></tr>
<tr><td>식별정보</td><td>성명, 주민등록번호(또는 외국인등록번호), 주소, 연락처(전화·이메일)</td></tr>
<tr><td>직업·소득정보</td><td>직장명, 직위, 재직기간, 연소득, 소득증빙서류상 소득금액</td></tr>
<tr><td>금융거래정보</td><td>계좌번호, 거래실적, 대출·카드 보유현황, 상환이력</td></tr>
<tr><td>신용정보</td><td>신용평점, 연체 및 채무불이행 이력, 공공기록정보(체납, 파산 등)</td></tr>
</table>
<h3>나. 선택 수집 항목</h3>
<p>우대금리 적용을 위한 급여이체 실적, 자동이체 실적 등 부수거래 정보</p>

<h2>제3조 (개인신용정보 제공 대상 및 목적)</h2>
<table>
<tr><th>제공 대상</th><th>제공 목적</th><th>제공 항목</th></tr>
<tr><td>NICE평가정보(주)</td><td>신용평점 조회 및 심사</td><td>성명, 주민등록번호, 금융거래정보</td></tr>
<tr><td>금융결제원</td><td>계좌 실명 확인</td><td>성명, 계좌번호</td></tr>
<tr><td>금융감독원</td><td>감독·검사 업무</td><td>대출 관련 전반</td></tr>
<tr><td>신용정보협회</td><td>신용정보 관리</td><td>대출 실행·연체 정보</td></tr>
</table>

<h2>제4조 (보유 및 이용 기간)</h2>
<p>수집·이용 목적이 달성될 때까지 보유하며, 관련 법령에서 정한 기간 동안 보존합니다.</p>
<table>
<tr><th>근거 법령</th><th>보존 기간</th></tr>
<tr><td>전자금융거래법</td><td>거래 종료 후 5년</td></tr>
<tr><td>여신전문금융업법</td><td>계약 종료 후 5년</td></tr>
<tr><td>신용정보법</td><td>채권 소멸 후 3~5년</td></tr>
</table>

<h2>제5조 (권리 및 행사 방법)</h2>
<p>귀하는 언제든지 개인신용정보 수집·이용·제공에 대한 동의를 철회하거나, 개인신용정보의 열람·정정·삭제를 요구할 수 있습니다. 단, 관련 법령에 따라 일부 정보는 삭제가 제한될 수 있습니다.</p>
<ul>
<li><strong>문의처</strong>: 우리피산 고객센터 02-2008-5000</li>
<li><strong>온라인</strong>: 인터넷뱅킹 → 개인정보관리</li>
</ul>

<p style="margin-top:28px;padding-top:16px;border-top:1px solid #ddd;color:#555;font-size:13px">
본 동의서의 내용을 충분히 숙지하였으며, 위 개인신용정보의 수집·이용·제공에 동의합니다.<br>
<span class="required">* 필수 동의 항목입니다.</span>
</p>
</body></html>'
WHERE terms_code = 'CREDIT_INFO_AGREE';

-- ==========================================
-- 2. NICE 신용정보 조회 동의서
-- ==========================================
UPDATE bank_terms SET terms_content = '<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><style>
body{font-family:-apple-system,BlinkMacSystemFont,"Malgun Gothic","맑은 고딕",sans-serif;padding:24px 28px;font-size:14px;line-height:1.9;color:#1a1a1a}
h1{font-size:18px;font-weight:700;border-bottom:2px solid #1a1a1a;padding-bottom:10px;margin-bottom:20px}
h2{font-size:15px;font-weight:700;margin-top:22px;margin-bottom:8px}
p{margin:6px 0}
ul{padding-left:24px;margin:6px 0}
li{margin:4px 0}
table{width:100%;border-collapse:collapse;margin:12px 0;font-size:13px}
th,td{border:1px solid #ccc;padding:8px 10px;text-align:left}
th{background:#f5f7fa;font-weight:700}
.notice{background:#f0f9ff;border-left:4px solid #0ea5e9;padding:12px 16px;margin:16px 0;font-size:13px}
.warn{background:#fff8e1;border-left:4px solid #f59e0b;padding:12px 16px;margin:16px 0;font-size:13px}
</style></head><body>
<h1>NICE 신용정보 조회 동의서</h1>
<p>우리피산 주식회사는 대출 심사를 위하여 아래와 같이 NICE평가정보 주식회사에 귀하의 개인신용정보 조회를 의뢰하고자 합니다. 내용을 충분히 읽고 동의 여부를 결정하여 주시기 바랍니다.</p>

<div class="notice">
<strong>※ 신용점수 영향 안내</strong><br>
금융소비자보호에 관한 법률 제10조 및 신용정보법 제36조의2에 따라, 대출 심사 목적의 신용정보 조회는 귀하의 개인신용평점(NICE 기준 1~1,000점)에 영향을 미칠 수 있습니다. 단, 단순 조회에 그치는 경우 영향이 미미할 수 있으며, 실제 대출계약 체결 시 평점이 하락할 수 있습니다.
</div>

<h2>제1조 (신용정보 조회 기관)</h2>
<table>
<tr><th>기관명</th><th>연락처</th><th>홈페이지</th></tr>
<tr><td>NICE평가정보 주식회사</td><td>1600-1522</td><td>www.niceinfo.co.kr</td></tr>
</table>

<h2>제2조 (조회 목적)</h2>
<ul>
<li>대출 신청 심사 및 승인 여부 결정</li>
<li>대출 한도 및 적용 금리 산정</li>
<li>기한의 이익 상실 사유 해당 여부 확인</li>
</ul>

<h2>제3조 (조회 항목)</h2>
<table>
<tr><th>분류</th><th>세부 항목</th></tr>
<tr><td>신용평점</td><td>NICE 개인신용평점(1~1,000점), 신용등급 환산 정보</td></tr>
<tr><td>대출·카드 현황</td><td>금융기관별 대출 잔액, 한도, 신용카드 보유 및 이용현황</td></tr>
<tr><td>연체·불이행 이력</td><td>단기연체(5영업일 이상), 장기연체(3개월 이상), 대위변제, 채무조정 이력</td></tr>
<tr><td>공공정보</td><td>세금 체납정보(500만원 이상), 파산·회생 신청 이력, 소송 관련 기록</td></tr>
<tr><td>부채비율 정보</td><td>DSR(총부채원리금상환비율) 산정에 필요한 부채 현황</td></tr>
</table>

<h2>제4조 (조회 기간)</h2>
<p>본 동의서에 서명한 날로부터 <strong>3개월</strong> 이내에 신용정보가 조회됩니다. 동의 기간 내 조회가 이루어지지 않은 경우 본 동의는 효력을 상실합니다.</p>

<h2>제5조 (동의 거부 권리 및 불이익)</h2>
<p>귀하는 신용정보 조회 동의를 거부할 권리가 있습니다. 다만, 동의를 거부하시는 경우 대출 심사 진행이 불가하며, 이에 따른 불이익(대출 미실행)은 귀하가 부담하게 됩니다.</p>

<div class="warn">
<strong>※ 개인신용평가 대응권 안내 (신용정보법 제36조의2)</strong><br>
자동화 평가(컴퓨터 등 정보처리장치에 의한 신용평가)를 통해 대출 심사가 이루어지는 경우, 귀하는 ①평가 결과 및 주요 기준의 설명 요구, ②평가에 이용된 기초정보의 정정·삭제·재산출 요구 권리를 행사하실 수 있습니다.
</div>

<p style="margin-top:28px;padding-top:16px;border-top:1px solid #ddd;color:#555;font-size:13px">
위 내용을 충분히 숙지하였으며, NICE평가정보(주)에 대한 신용정보 조회에 동의합니다.
</p>
</body></html>'
WHERE terms_code = 'NICE_CREDIT_INQUIRY';

-- ==========================================
-- 3. 서류 징구 및 보관 동의서
-- ==========================================
UPDATE bank_terms SET terms_content = '<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><style>
body{font-family:-apple-system,BlinkMacSystemFont,"Malgun Gothic","맑은 고딕",sans-serif;padding:24px 28px;font-size:14px;line-height:1.9;color:#1a1a1a}
h1{font-size:18px;font-weight:700;border-bottom:2px solid #1a1a1a;padding-bottom:10px;margin-bottom:20px}
h2{font-size:15px;font-weight:700;margin-top:22px;margin-bottom:8px}
p{margin:6px 0}
ul{padding-left:24px;margin:6px 0}
li{margin:4px 0}
table{width:100%;border-collapse:collapse;margin:12px 0;font-size:13px}
th,td{border:1px solid #ccc;padding:8px 10px;text-align:left}
th{background:#f5f7fa;font-weight:700}
.notice{background:#f0fdf4;border-left:4px solid #22c55e;padding:12px 16px;margin:16px 0;font-size:13px}
</style></head><body>
<h1>서류 징구 및 보관 동의서</h1>
<p>우리피산 주식회사(이하 "회사")는 대출 심사, 계약 이행 및 사후 관리를 위하여 아래와 같이 귀하의 서류를 수집·보관합니다. 내용을 충분히 읽고 동의 여부를 결정하여 주시기 바랍니다.</p>

<h2>제1조 (징구 서류 목록)</h2>
<table>
<tr><th>구분</th><th>서류명</th><th>비고</th></tr>
<tr><td>신분확인</td><td>주민등록증 사본 또는 운전면허증 사본</td><td>필수</td></tr>
<tr><td rowspan="3">소득증빙</td><td>근로소득원천징수영수증 (직전연도분)</td><td>근로소득자</td></tr>
<tr><td>건강보험료 납부확인서 (최근 1년)</td><td>근로소득자</td></tr>
<tr><td>사업자등록증 사본, 부가가치세 과세표준증명원</td><td>사업소득자</td></tr>
<tr><td>재직확인</td><td>재직증명서 (발행일 기준 3개월 이내)</td><td>근로소득자</td></tr>
<tr><td>금융정보</td><td>주거래 계좌 통장 사본 (입금 계좌)</td><td>필수</td></tr>
<tr><td>기타</td><td>심사 시 회사가 추가 요청하는 서류</td><td>필요 시</td></tr>
</table>

<h2>제2조 (수집·보관 목적)</h2>
<ul>
<li>대출 심사 시 신청인의 소득·재직 상태 확인</li>
<li>대출 계약 이행 및 원리금 관리</li>
<li>금융사고 조사 및 분쟁 해결 시 증빙 자료 활용</li>
<li>금융당국 감독·검사 시 제출용</li>
<li>관련 법령(금융소비자보호법, 여신전문금융업법 등) 준수</li>
</ul>

<h2>제3조 (보관 기간)</h2>
<table>
<tr><th>서류 구분</th><th>보관 기간</th><th>근거</th></tr>
<tr><td>대출 계약 관련 서류 전반</td><td>대출 원리금 완제일로부터 5년</td><td>전자금융거래법 제22조</td></tr>
<tr><td>신용정보 관련 서류</td><td>채권 소멸 후 3년</td><td>신용정보법 제20조의2</td></tr>
<tr><td>소득·재직 증빙 서류</td><td>계약 종료 후 5년</td><td>여신전문금융업법</td></tr>
</table>

<h2>제4조 (보관 방법 및 폐기)</h2>
<p>수집된 서류는 암호화된 전산시스템 또는 잠금장치가 있는 보안구역에 보관되며, 보관 기간 만료 시 「개인정보 보호법」 제21조에 따라 복원이 불가능한 방법으로 안전하게 파기합니다.</p>
<ul>
<li><strong>전자적 파일</strong>: 복원 불가능한 방법으로 영구 삭제</li>
<li><strong>종이 서류</strong>: 분쇄기를 이용한 파기 또는 소각</li>
</ul>

<h2>제5조 (열람 및 정정 요구 권리)</h2>
<p>귀하는 회사가 보유한 서류의 열람을 요구하거나, 사실과 다른 내용에 대해 정정을 요구할 수 있습니다. 요구 방법: 고객센터(02-2008-5000) 또는 영업점 방문</p>

<div class="notice">
<strong>※ 서류 미제출 시 불이익</strong><br>
필수 서류를 제출하지 않으시는 경우 대출 심사가 지연되거나 거절될 수 있습니다. 심사 거절 시 발생한 비용(등기설정·말소비용 등)은 귀하가 부담합니다.
</div>

<p style="margin-top:28px;padding-top:16px;border-top:1px solid #ddd;color:#555;font-size:13px">
위 내용을 충분히 숙지하였으며, 대출 심사 및 계약 이행을 위한 서류 징구 및 보관에 동의합니다.
</p>
</body></html>'
WHERE terms_code = 'DOCUMENT_COLLECT';

-- ==========================================
-- 4. 대출 기본 약관 (은행여신거래기본약관 준용)
-- ==========================================
UPDATE bank_terms SET terms_content = '<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><style>
body{font-family:-apple-system,BlinkMacSystemFont,"Malgun Gothic","맑은 고딕",sans-serif;padding:24px 28px;font-size:14px;line-height:1.9;color:#1a1a1a}
h1{font-size:18px;font-weight:700;border-bottom:2px solid #1a1a1a;padding-bottom:10px;margin-bottom:20px}
h2{font-size:15px;font-weight:700;margin-top:22px;margin-bottom:8px}
p{margin:6px 0}
ul,ol{padding-left:24px;margin:6px 0}
li{margin:4px 0}
table{width:100%;border-collapse:collapse;margin:12px 0;font-size:13px}
th,td{border:1px solid #ccc;padding:8px 10px;text-align:left}
th{background:#f5f7fa;font-weight:700}
.warn{background:#fff1f2;border-left:4px solid #ef4444;padding:12px 16px;margin:16px 0;font-size:13px}
.info{background:#eff6ff;border-left:4px solid #3b82f6;padding:12px 16px;margin:16px 0;font-size:13px}
</style></head><body>
<h1>대출 기본 약관</h1>
<p>본 약관은 우리피산 주식회사(이하 "회사")와 대출 신청인(이하 "고객") 간의 대출 거래에 관한 기본 사항을 정하며, 은행여신거래기본약관(가계용) 및 「금융소비자 보호에 관한 법률」에 따릅니다.</p>

<h2>제1조 (대출금 지급)</h2>
<p>회사는 대출 계약 체결 및 필요 서류 확인 완료 후, 고객이 지정한 계좌로 대출금을 지급합니다. 대출금은 고객이 신청한 목적에 맞게 사용하여야 하며, 부정 사용 시 기한의 이익이 상실될 수 있습니다.</p>

<h2>제2조 (이자 납부 및 상환)</h2>
<p>고객은 매월 지정된 상환일에 원리금(원금 + 이자)을 납부합니다. 자동이체를 신청한 경우, 잔액 부족으로 이체가 불가한 때에는 은행이 정하는 출금일에 미납금액(지연배상금 포함)을 출금합니다.</p>
<ul>
<li>상환 방법: 원리금균등분할상환</li>
<li>이자 산정: 일(日) 기준 계산, 월 1회 납부</li>
</ul>

<h2>제3조 (연체이자율)</h2>
<p>고객이 원리금을 약정 납부일에 납부하지 않은 경우 연체이자가 발생합니다.</p>
<table>
<tr><th>구분</th><th>내용</th></tr>
<tr><td>연체이자율</td><td>대출이자율 + 연체가산이자율(연 3%)</td></tr>
<tr><td>연체이자율 상한</td><td>최고 연 12%</td></tr>
<tr><td>연체이자 기산일</td><td>약정 납입일 다음 날부터</td></tr>
</table>
<div class="warn">
<strong>※ 연체이자 부과 주의사항</strong><br>
이자 납입 연체로 인하여 연체이율이 적용된 경우, 일부 연체이자만 납입하더라도 연체이자 전액을 납입하기 전까지 대출 잔액에 연체이자율이 적용됩니다.
</div>

<h2>제4조 (기한의 이익 상실)</h2>
<p>다음 각 호에 해당하는 경우 고객은 기한의 이익을 상실하며, 대출금 전액을 즉시 상환하여야 합니다.</p>
<ol>
<li>이자를 납입하기로 약정한 날로부터 1개월(주택담보대출: 2개월)이 경과하도록 납입하지 않은 때</li>
<li>분할상환금을 2회 이상(주택담보대출: 3회) 연속하여 납부하지 않은 때</li>
<li>회사에 대한 예치금 등 각종 채권에 압류명령 또는 체납처분 착수가 있는 때</li>
<li>채무자가 제공한 담보재산에 강제집행 또는 체납처분 착수가 있는 때</li>
<li>파산 또는 개인회생 신청이 있는 때</li>
<li>타 금융기관 대출금을 3개월 이상 연체한 사실이 확인된 때</li>
</ol>
<div class="warn">
<strong>※ 기한의 이익 상실 시 불이익</strong><br>
기한의 이익이 상실되면 ① 대출원금 전액에 대한 연체이자 부담, ② 5영업일 이상 연체 시 단기연체정보 신용정보회사 제공, ③ 3개월 이상 연체 시 장기연체정보 등록(7영업일 이내)으로 개인신용평점이 하락하고 금융거래에 불이익이 발생합니다.
</div>

<h2>제5조 (청약철회권)</h2>
<p>일반금융소비자는 대출 실행일 다음 날로부터 <strong>14일 이내</strong>에 계약에 대한 청약을 철회할 수 있습니다. 청약 철회 시 수령한 대출금과 해당 이자, 은행이 제3자에게 부담한 인지세·근저당권설정비용 등을 반환하여야 하며, 중도상환해약금은 면제됩니다.</p>

<h2>제6조 (금리인하요구권)</h2>
<p>고객은 취업·승진·재산 증가·개인신용평점 상승 등 신용상태가 개선되었다고 판단되는 경우 대출 금리의 인하를 요구할 수 있습니다(은행법 제30조의2). 회사는 요구를 받은 날로부터 10영업일 이내에 수용 여부 및 사유를 통지합니다.</p>

<div class="info">
<strong>※ 민원·분쟁 연락처</strong><br>
우리피산 고객센터: 02-2008-5000 | www.wooribank.com<br>
금융감독원 금융민원센터: www.fcsc.kr | ☎ 1332 (국번 없이)
</div>

<p style="margin-top:28px;padding-top:16px;border-top:1px solid #ddd;color:#555;font-size:13px">
본 약관의 내용을 충분히 숙지하였으며, 이에 동의하고 대출 거래를 신청합니다.
</p>
</body></html>'
WHERE terms_code = 'LOAN_CONTRACT_BASIC';

-- ==========================================
-- 5. 대출 상품 설명서
-- ==========================================
UPDATE bank_terms SET terms_content = '<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><style>
body{font-family:-apple-system,BlinkMacSystemFont,"Malgun Gothic","맑은 고딕",sans-serif;padding:24px 28px;font-size:14px;line-height:1.9;color:#1a1a1a}
h1{font-size:18px;font-weight:700;border-bottom:2px solid #1a1a1a;padding-bottom:10px;margin-bottom:8px}
.subtitle{font-size:13px;color:#6b7280;margin-bottom:20px}
h2{font-size:15px;font-weight:700;margin-top:22px;margin-bottom:8px}
h3{font-size:14px;font-weight:700;margin-top:14px;margin-bottom:6px}
p{margin:6px 0}
ul,ol{padding-left:24px;margin:6px 0}
li{margin:4px 0}
table{width:100%;border-collapse:collapse;margin:12px 0;font-size:13px}
th,td{border:1px solid #ccc;padding:8px 10px;text-align:left}
th{background:#f5f7fa;font-weight:700}
td.center{text-align:center}
.notice{background:#fff8e1;border-left:4px solid #f59e0b;padding:12px 16px;margin:16px 0;font-size:13px}
.highlight{background:#f0fdf4;border:1px solid #bbf7d0;border-radius:6px;padding:12px 16px;margin:12px 0}
</style></head><body>
<h1>대출 상품 설명서</h1>
<p class="subtitle">준법감시인 심의필 2025-9628 (2025.09.23 ~ 2026.12.31) | 「금융소비자 보호에 관한 법률」 제19조에 따른 설명서</p>

<div class="notice">
<strong>◈ 이 설명서는</strong> 금융소비자의 권익 보호 및 대출상품에 대한 이해 증진을 위하여 대출상품의 주요 내용을 쉽게 이해할 수 있도록 작성한 자료이며, 계약서류(약정서 등), 은행여신거래기본약관(가계용)에도 동일하게 적용됩니다.
</div>

<h2>1. 상품 개요</h2>
<table>
<tr><th>상품명</th><td>우리피산 가계신용대출</td></tr>
<tr><th>대출 금리</th><td>연 4.50% ~ 12.00% (고정금리, 개인별 신용도에 따라 결정)</td></tr>
<tr><th>대출 한도</th><td>최소 100만원 ~ 최대 5,000만원</td></tr>
<tr><th>대출 기간</th><td>12개월 / 24개월 / 36개월 (선택)</td></tr>
<tr><th>상환 방식</th><td>원리금균등분할상환</td></tr>
<tr><th>금리 적용 방식</th><td>고정금리 (대출 실행 시 결정된 금리가 만기까지 동일하게 유지)</td></tr>
<tr><th>중도상환해약금</th><td>해당 (최초 취급일로부터 3년 이내 상환 시 부과)</td></tr>
</table>

<h2>2. 대출금리 산정 방식</h2>
<p>대출금리는 아래와 같이 기준금리에 가산금리를 합산하여 결정됩니다.</p>
<div class="highlight">
<strong>최종 적용 금리 = 대출 기준금리 + 가산금리 - 우대금리</strong>
</div>
<table>
<tr><th>구성 요소</th><th>내용</th></tr>
<tr><td>대출 기준금리</td><td>금융채(AAA, 1년물) 또는 COFIX(코픽스) 기준</td></tr>
<tr><td>가산금리</td><td>리스크관리비용, 업무원가(인건비·전산), 기대이익률 등</td></tr>
<tr><td>우대금리</td><td>급여이체, 자동이체 등록, 카드 실적 등 부수거래에 따라 최대 0.5%p 감면</td></tr>
</table>
<p>※ 최종 적용 금리는 대출 심사 완료 후 확정됩니다. 상담 단계의 예상 금리와 다를 수 있습니다.</p>

<h2>3. 상환 방법 비교</h2>
<table>
<tr><th>구분</th><th>원리금균등상환</th><th>원금균등상환</th><th>만기일시상환</th></tr>
<tr><td>방식</td><td>매월 동일 금액 납부</td><td>원금 균등 + 잔액이자</td><td>이자만 납부, 만기에 원금 일시상환</td></tr>
<tr><td>월 납부금</td><td>고정</td><td>점차 감소</td><td>이자만(가장 낮음)</td></tr>
<tr><td>총 이자 부담</td><td>중간</td><td>가장 적음</td><td>가장 많음</td></tr>
<tr><td>본 상품 적용</td><td class="center">✔ 적용</td><td class="center">-</td><td class="center">-</td></tr>
</table>

<h3>▶ 상환 예시 (1억원, 연 5%, 5년 기준)</h3>
<table>
<tr><th>상환기간</th><th>원금</th><th>이자</th><th>상환금액</th><th>대출잔액</th></tr>
<tr><td>1년 차</td><td>1,810만원</td><td>500만원</td><td>2,310만원</td><td>8,190만원</td></tr>
<tr><td>2년 차</td><td>1,900만원</td><td>410만원</td><td>2,310만원</td><td>6,290만원</td></tr>
<tr><td>3년 차</td><td>1,995만원</td><td>315만원</td><td>2,310만원</td><td>4,295만원</td></tr>
<tr><td>4년 차</td><td>2,095만원</td><td>215만원</td><td>2,310만원</td><td>2,200만원</td></tr>
<tr><td>5년 차</td><td>2,200만원</td><td>110만원</td><td>2,310만원</td><td>0원</td></tr>
<tr><td><strong>합계</strong></td><td><strong>1억원</strong></td><td><strong>1,550만원</strong></td><td><strong>1.155억원</strong></td><td>-</td></tr>
</table>
<p>※ 이해를 돕기 위한 단순 예시이며, 실제 납부 원리금은 금리·상환주기에 따라 달라질 수 있습니다.</p>

<h2>4. 금융소비자의 권리</h2>
<table>
<tr><th>권리</th><th>행사 방법 및 기한</th></tr>
<tr><td><strong>청약철회권</strong></td><td>대출 실행일 익일부터 14일 이내 / 영업점·인터넷뱅킹·스마트뱅킹</td></tr>
<tr><td><strong>금리인하요구권</strong></td><td>신용상태 개선 시 언제든지 / 영업점·비대면채널, 10영업일 이내 결과 통보</td></tr>
<tr><td><strong>위법계약해지권</strong></td><td>위반사실 인지 후 1년 이내(계약일로부터 5년 이내) / 계약해지요구서 제출</td></tr>
<tr><td><strong>자료열람요구권</strong></td><td>분쟁조정·소송 목적으로 언제든지 / 열람요구서 제출, 6영업일 이내 열람 가능</td></tr>
</table>

<h2>5. DSR(총부채원리금상환비율) 안내</h2>
<p>대출 심사 시 DSR(모든 대출의 연간 원리금 상환액 ÷ 연소득)을 산출하며, 금융당국 기준 초과 시 대출 한도가 제한될 수 있습니다. 연소득 대비 원리금 상환 부담이 과도한 경우 심사가 거절될 수 있으니 유의하시기 바랍니다.</p>

<p style="margin-top:28px;padding-top:16px;border-top:1px solid #ddd;color:#555;font-size:13px">
본 상품설명서의 내용을 충분히 숙지하였음을 확인합니다.<br>
상품 가입 후 의문사항은 우리피산 고객센터(02-2008-5000) 또는 금융감독원(☎ 1332)에 문의하실 수 있습니다.
</p>
</body></html>'
WHERE terms_code = 'LOAN_PRODUCT_TERMS';

-- ==========================================
-- 6. 금리 변동 위험 고지서
-- ==========================================
UPDATE bank_terms SET terms_content = '<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><style>
body{font-family:-apple-system,BlinkMacSystemFont,"Malgun Gothic","맑은 고딕",sans-serif;padding:24px 28px;font-size:14px;line-height:1.9;color:#1a1a1a}
h1{font-size:18px;font-weight:700;border-bottom:2px solid #1a1a1a;padding-bottom:10px;margin-bottom:20px}
h2{font-size:15px;font-weight:700;margin-top:22px;margin-bottom:8px}
p{margin:6px 0}
ul{padding-left:24px;margin:6px 0}
li{margin:4px 0}
table{width:100%;border-collapse:collapse;margin:12px 0;font-size:13px}
th,td{border:1px solid #ccc;padding:8px 10px;text-align:left;vertical-align:top}
th{background:#f5f7fa;font-weight:700}
.warn{background:#fff1f2;border-left:4px solid #ef4444;padding:12px 16px;margin:16px 0;font-size:13px}
.info{background:#eff6ff;border-left:4px solid #3b82f6;padding:12px 16px;margin:16px 0;font-size:13px}
.tag-fixed{display:inline-block;background:#dbeafe;color:#1d4ed8;font-size:11px;font-weight:700;padding:2px 8px;border-radius:4px}
</style></head><body>
<h1>금리 변동 위험 고지서</h1>
<p>「금융소비자 보호에 관한 법률」 제19조에 따라 대출 금리 유형별 특성 및 위험에 관한 사항을 고지합니다. 고객님께서 선택하신 금리 방식은 <span class="tag-fixed">고정금리</span>입니다.</p>

<h2>1. 금리 유형별 특성 비교</h2>
<table>
<tr><th>구분</th><th>고정금리</th><th>변동금리</th><th>혼합금리</th></tr>
<tr><td><strong>운용 형태</strong></td><td>대출 실행 시 결정된 금리가 만기까지 동일하게 유지</td><td>일정 주기(3/6/12개월)마다 기준금리 변동에 따라 대출금리 변동</td><td>고정금리 + 변동금리 결합 형태</td></tr>
<tr><td><strong>장점</strong></td><td>시장금리 상승 시 추가 이자 부담 없음, 상환 계획 수립 용이</td><td>시장금리 하락 시 이자 부담 감소</td><td>고정·변동의 중간적 형태로 자금계획에 맞춰 운용 가능</td></tr>
<tr><td><strong>단점</strong></td><td>시장금리 하락 시 상대적으로 높은 금리 유지, 변동금리보다 불리</td><td>시장금리 상승 시 이자 부담 증가, 상환 금액 변동</td><td>-</td></tr>
</table>

<h2>2. 고정금리 적용 내용</h2>
<p>본 대출 상품은 고정금리 방식으로, 대출 계약 체결 시 확정된 금리가 대출 만기일까지 동일하게 적용됩니다. 시장금리(COFIX, 금융채 등)의 변동과 무관하게 당초 약정 금리가 유지됩니다.</p>
<ul>
<li>금리 기준: 금융채(AAA) 1년물 기준 + 가산금리</li>
<li>금리 변경 시기: 기한연장·재약정·조건변경 시에만 변경 가능</li>
</ul>

<h2>3. 시장금리 변동에 따른 유의사항</h2>
<div class="warn">
<strong>▶ 금리 하락 시</strong><br>
시장금리가 하락하더라도 약정 고정금리는 변경되지 않아 변동금리 상품 대비 상대적으로 높은 이자 부담이 발생할 수 있습니다. 중도에 금리를 낮추고자 하는 경우 중도상환 후 재대출(중도상환해약금 발생) 또는 금리인하요구권 행사를 검토하시기 바랍니다.
</div>
<div class="info">
<strong>▶ 금리 상승 시</strong><br>
시장금리가 상승하더라도 약정 고정금리는 변경되지 않아 변동금리 상품 대비 상대적으로 유리합니다. 대출 기간 동안 안정적인 상환 계획을 수립하실 수 있습니다.
</div>

<h2>4. 변동금리 기준금리(COFIX) 안내</h2>
<p>변동금리 상품의 경우 아래 기준금리 중 선택할 수 있습니다. (본 상품은 고정금리 적용으로 해당 없으나 참고 안내)</p>
<table>
<tr><th>구분</th><th>COFIX 신규취급 기준</th><th>COFIX 신잔액 기준</th><th>금융채 연동</th></tr>
<tr><td>기준</td><td>전월 신규 취급 조달금리 가중평균</td><td>전월말 잔액 기준 조달금리</td><td>만기 6개월·1년·5년 금융채</td></tr>
<tr><td>고시</td><td>매월 15일 은행연합회 공시</td><td>매월 15일 은행연합회 공시</td><td>매일 공표</td></tr>
<tr><td>특징</td><td>최근 조달금리 반영, 금리 변동에 민감</td><td>금리 상승기 상승 완만, 하락기 하락도 완만</td><td>시장금리 직접 반영</td></tr>
</table>

<h2>5. 금리인하요구권</h2>
<p>취업·승진·재산 증가·개인신용평점 상승 등 신용상태가 개선된 경우, 영업점·인터넷뱅킹·스마트뱅킹을 통해 금리인하를 요구할 수 있습니다(은행법 제30조의2). 회사는 요구일로부터 10영업일 이내에 수용 여부를 통지합니다.</p>

<p style="margin-top:28px;padding-top:16px;border-top:1px solid #ddd;color:#555;font-size:13px">
위 고지 내용을 충분히 읽고 이해하였으며, 금리 관련 위험에 대해 충분히 숙지하였음을 확인합니다.
</p>
</body></html>'
WHERE terms_code = 'INTEREST_RATE_RISK';

-- ==========================================
-- 7. 중도상환수수료 안내
-- ==========================================
UPDATE bank_terms SET terms_content = '<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><style>
body{font-family:-apple-system,BlinkMacSystemFont,"Malgun Gothic","맑은 고딕",sans-serif;padding:24px 28px;font-size:14px;line-height:1.9;color:#1a1a1a}
h1{font-size:18px;font-weight:700;border-bottom:2px solid #1a1a1a;padding-bottom:10px;margin-bottom:20px}
h2{font-size:15px;font-weight:700;margin-top:22px;margin-bottom:8px}
p{margin:6px 0}
ul{padding-left:24px;margin:6px 0}
li{margin:4px 0}
table{width:100%;border-collapse:collapse;margin:12px 0;font-size:13px}
th,td{border:1px solid #ccc;padding:8px 10px;text-align:left}
th{background:#f5f7fa;font-weight:700}
.formula{background:#f8fafc;border:2px solid #e2e8f0;border-radius:8px;padding:14px 18px;margin:14px 0;font-size:15px;font-weight:700;text-align:center;color:#1e40af}
.example{background:#f0fdf4;border:1px solid #bbf7d0;border-radius:6px;padding:14px 16px;margin:12px 0;font-size:13px}
.notice{background:#fff8e1;border-left:4px solid #f59e0b;padding:12px 16px;margin:16px 0;font-size:13px}
</style></head><body>
<h1>중도상환수수료 안내</h1>
<p>중도상환해약금이란 대출의 상환기일이 도래하기 전에 대출금을 상환할 경우 고객이 부담하는 금액입니다. 대출 계약 기간 이전에 원금 전부 또는 일부를 상환하는 경우 아래 기준에 따라 중도상환해약금이 부과됩니다.</p>

<h2>1. 중도상환해약금 산정 방식</h2>
<div class="formula">
중도상환해약금 = 중도상환 원금 × 요율(%) × (대출 잔여일수 ÷ 대출기간일수)
</div>
<p>※ 중도상환해약금 요율은 은행연합회 공시 기준에 따르며, 매년 요율을 재산정하여 은행연합회 홈페이지(www.kfb.or.kr)에 공시합니다.</p>

<h2>2. 적용 기간 및 요율</h2>
<table>
<tr><th>구분</th><th>적용 기간</th><th>적용 요율</th></tr>
<tr><td>신용대출</td><td>최초 대출 취급일로부터 3년 이내</td><td>1.4% (2025년 기준)</td></tr>
<tr><td>3년 초과 상환</td><td>최초 대출 취급일로부터 3년 초과</td><td>면제</td></tr>
<tr><td>만기 3개월 미만</td><td>만기까지 3개월 미만이 남은 경우</td><td>면제</td></tr>
</table>

<div class="notice">
<strong>※ 기간 합산 면제 조건</strong><br>
기존 대출 계약을 해지하고 동일 은행과 사실상 동일한 계약(기존 계약에 따라 지급된 금전 등을 상환받는 새로운 계약)을 체결한 경우, 양 계약의 유지기간을 합하여 3년이 경과한 후 해지할 경우에는 중도상환해약금이 면제됩니다.
</div>

<h2>3. 계산 예시</h2>
<div class="example">
<strong>예시)</strong> 신용대출 5,000만원, 연 6.5% (고정금리), 36개월 만기 대출을 받고 2년(730일) 경과 후 잔여 원금 2,000만원을 중도상환하는 경우<br><br>
잔여일수 = 36개월(1,095일) - 24개월(730일) = <strong>365일</strong><br>
중도상환해약금 = 2,000만원 × 1.4% × (365일 ÷ 1,095일)<br>
= 2,000만원 × 0.014 × 0.333...<br>
= <strong>약 93,333원</strong>
</div>

<h2>4. 인지세 납부 안내</h2>
<p>대출 약정 체결 시 「인지세법」에 의한 인지세가 부과되며, 고객과 회사가 각 50%씩 부담합니다.</p>
<table>
<tr><th>대출 금액</th><th>인지세액</th><th>고객 부담</th><th>회사 부담</th></tr>
<tr><td>5천만원 이하</td><td>비과세</td><td>-</td><td>-</td></tr>
<tr><td>5천만원 초과 ~ 1억원 이하</td><td>7만원</td><td>3만 5천원</td><td>3만 5천원</td></tr>
<tr><td>1억원 초과 ~ 10억원 이하</td><td>15만원</td><td>7만 5천원</td><td>7만 5천원</td></tr>
<tr><td>10억원 초과</td><td>35만원</td><td>17만 5천원</td><td>17만 5천원</td></tr>
</table>

<h2>5. 중도상환 절차</h2>
<ul>
<li>중도상환 신청: 영업점 방문 또는 인터넷뱅킹·스마트뱅킹</li>
<li>중도상환 처리: 신청일 기준 익영업일 처리 원칙</li>
<li>수수료 납부: 중도상환 원금과 함께 동시 납부</li>
<li>영수증: 처리 완료 후 중도상환확인서 발급 가능</li>
</ul>

<h2>6. 중도상환해약금 면제 조건</h2>
<ul>
<li>최초 대출 취급일로부터 3년을 초과하여 상환하는 경우</li>
<li>대출 만기일까지 3개월 미만이 남은 경우</li>
<li>대출계약 철회권 행사(실행일 익일로부터 14일 이내) 시</li>
<li>위법계약해지권 행사가 인정된 경우</li>
</ul>

<p style="margin-top:28px;padding-top:16px;border-top:1px solid #ddd;color:#555;font-size:13px">
위 중도상환해약금 안내 내용을 충분히 숙지하였습니다.<br>
문의: 우리피산 고객센터 02-2008-5000 | www.wooribank.com
</p>
</body></html>'
WHERE terms_code = 'EARLY_REPAYMENT_FEE';
