# AI가 진짜 최고 사랑하는 구조 - 최종

GEMINI.md                    # [Global] 전체 라우팅 맵, 브랜치/PR 전략
prompt/
├── docs/                        # [Global Rules] 직군별 전사 공통 규칙
│   ├── RULE_FE_STYLE.md         # 프론트엔드 네이밍, 폴더 구조, 상태관리 원칙
│   └── RULE_BE_STYLE.md         # 백엔드 네이밍, 예외처리, 트랜잭션 규칙
│
├── platform/                    # [Domain: 중계 플랫폼]
│   ├── DOMAIN_PLATFORM.md       # (공통) mTLS, FAPI 보안, JWS 서명/검증 표준
│   ├── backend/                 
│   │   └── STACK_PLATFORM_BE.md # (로컬) 플랫폼 BE 전용 라이브러리 및 설정
│   └── frontend/                
│       └── STACK_PLATFORM_FE.md # (로컬) 플랫폼 FE 전용 라이브러리 및 마스킹 로직
│
├── bank/                        # [Domain: 은행 코어]
│   ├── DOMAIN_BANK.md           # (공통) 원장 정합성, 이체 멱등성, 금융 비즈니스 룰
│   └── backend/                 
│       └── STACK_BANK_BE.md     # (로컬) Spring Batch, DB 격리수준 설정
│
└── monitoring/                  # [Domain: 관측 인프라]
├── DOMAIN_MONITOR.md        # (공통) 금융 서비스 모니터링 임계치 지표
├── backend/                 
│   └── STACK_MONITOR_BE.md  # (로컬) Collector(Prometheus) 스택
└── frontend/                
└── STACK_MONITOR_FE.md  # (로컬) 대시보드(Grafana/D3) 스택