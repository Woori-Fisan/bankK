# 아키텍처


### 운영 환경
<img width="100%" height="auto" alt="아키텍처(운영)" src="https://github.com/user-attachments/assets/8ae1d879-483a-4f79-9504-0b5e3458538b" />
<p align="center">
    <ui>
        <li>플랫폼 서비스는 AWS 기반 운영</li>
        <li>은행 시스템은 기존 On-Premise 유지</li>
        <li>WireGuard VPN 기반 내부 통신 구성</li>
    </ui>
</p>

---

### 테스트 환경
<img width="100%" height="auto" alt="아키텍처(테스트)" src="https://github.com/user-attachments/assets/d907d6f4-de66-4932-9a9a-0cb7bbcb730d" />
<p align="center">
    <ui>
        <li>운영 환경과 유사한 테스트 환경 구축</li>
        <li>기능 정상 동작 여부 검증</li>
        <li>운영 배포 전 장애 위험 최소화</li>
    </ui>
</p>

---

## 전체 구성 개요

BankK는 **AWS 클라우드(플랫폼·모니터링)** 와 **온프레미스 은행 망** 이 WireGuard VPN으로 연결된 하이브리드 아키텍처입니다.  
멀티 AZ 구성으로 플랫폼 서버의 가용성을 확보하며, 모니터링과 데이터 레이어는 단일 AZ에 배치합니다.

### 통신 흐름

```
사용자 (대행업자)
    │  HTTPS
    ▼
Route 53 → CloudFront → Internet Gateway → ALB
    │
    ▼
플랫폼 WAS (Private-Platform-Subnet, AZ-A / AZ-C, Auto Scaling)
    │
    ├── [BaaS API]  WireGuard VPN 터널  →  On-Premise 은행 WAS  (mTLS)
    │
    ├── [로그]      Fluent Bit HTTP OUTPUT  →  모니터링 WAS  →  Loki / DB
    │
    └── [메트릭]    Prometheus scrape  →  Grafana 대시보드
```

- **외부 → 플랫폼:** CloudFront가 정적 자산 캐싱 및 WAF 필터링 처리 후 ALB로 전달. ALB는 AZ-A/C의 플랫폼 WAS에 라운드로빈.
- **플랫폼 → 은행:** VPN Subnet의 WireGuard Host를 경유해 사설 터널로 은행 온프레미스 망에 접근. 터널 안에서 추가로 mTLS 적용.
- **플랫폼 → 모니터링:** 각 WAS에 사이드카로 붙은 Fluent Bit이 로그를 HTTP로 모니터링 WAS에 전송. 모니터링 WAS는 DB 저장 + 조회 API 제공.
- **장기 보관:** CloudWatch → S3로 로그 아카이빙.

---

## 컴포넌트 설명

### 엣지 / 네트워크

| 컴포넌트 | 설명 |
|----------|------|
| **Amazon Route 53** | 도메인 DNS 라우팅 |
| **AWS WAF** | SQL Injection·XSS 등 L7 공격 차단 |
| **Amazon CloudFront** | 정적 자산 글로벌 캐싱 및 HTTPS 엔드포인트 |
| **AWS Certificate Manager** | CloudFront·ALB용 TLS 인증서 자동 갱신 관리 |
| **Internet Gateway** | VPC ↔ 퍼블릭 인터넷 연결 |
| **Application Load Balancer** | 플랫폼 WAS AZ-A / AZ-C 간 트래픽 분산 |
| **NAT Gateway** | 프라이빗 서브넷 인스턴스의 아웃바운드 인터넷 접근 |

### 관리 / 접근

| 컴포넌트 | 설명 |
|----------|------|
| **EC2 Bastion Host** | 운영자 SSH 점프 서버 (Public-Mgmt-Subnet) |
| **EC2 WireGuard Host** | 클라우드 ↔ 온프레미스 은행 망 VPN 터널 엔드포인트 |

### 플랫폼 (Private-Platform-Subnet, AZ-A / AZ-C)

| 컴포넌트 | 설명 |
|----------|------|
| **플랫폼 WAS** | BaaS 중계 플랫폼 서버 (Spring Boot); Auto Scaling 그룹으로 수평 확장 |
| **Fluent Bit** | 플랫폼 WAS 사이드카 로그 수집기; OUTPUT을 모니터링 WAS HTTP 엔드포인트로 전송 |

### 데이터 (Private-Data-Subnet, AZ-A)

| 컴포넌트 | 설명 |
|----------|------|
| **Amazon RDS** | 플랫폼 거래 원장·감사 로그 저장 (MySQL) |
| **Redis** | 리프레시 토큰·멱등키·분산 락 저장소 |
| **Qdrant** | RAG 기반 AI 업무 보조용 벡터 DB |

### 모니터링 (Private-Monitor-Subnet, AZ-A)

| 컴포넌트 | 설명 |
|----------|------|
| **모니터링 WAS** | Fluent Bit OUTPUT 수신, DB 저장, 로그 조회 API 제공 (Spring Boot) |
| **Loki** | 로그 집계 및 인덱싱 (Grafana 연동) |
| **Prometheus** | 플랫폼·모니터링 WAS 메트릭 수집 |
| **Grafana** | Prometheus·Loki 데이터 기반 실시간 관제 대시보드 |

### 기타

| 컴포넌트 | 설명 |
|----------|------|
| **Amazon CloudWatch** | AWS 리소스 메트릭·알람 수집 |
| **Amazon S3** | Loki 로그 장기 저장소 + CloudWatch 로그 아카이빙 |

### On-Premise 은행 망

| 컴포넌트 | 설명 |
|----------|------|
| **은행 WAS** | 코어뱅킹 BaaS API 서버. WireGuard VPN + mTLS로 플랫폼과 통신 |
| **Redis** | 멱등키 저장소 |
| **MySQL** | 은행 원장 DB |
