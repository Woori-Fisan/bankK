# [우리FISA 6기] 클라우드 서비스 개발 과정 5팀

## 1\. 프로젝트 개요
  * **주제**: (BaaS 기반 임베디드 금융 서비스)
  * **프로젝트 기획 배경**: 
  * **기술 스택**
      * 백엔드: Java 17, Spring Boot 3.2, MySQL 8.0, Redis
      * 프론트엔드: 

   
        
## 2\. 아키텍쳐

### 2-1. 시스템 아키텍쳐
<img width="8683" height="6242" alt="아키텍처(PROD)" src="https://github.com/user-attachments/assets/be9718ce-e3df-4e60-8e8c-f265d20de85a" />



### 설명
GitHub과 Jenkins를 연계한 CI/CD 파이프라인을 통해 코드 변경 시 자동으로 빌드 및 배포가 이루어지는 구조입니다.
Frontend(React)와 Backend(FastAPI)는 Docker 기반으로 구성되어 있으며, MySQL, S3, OpenSearch와 연동하여 데이터 저장 및 검색 기능을 수행합니다.
또한 Prometheus, Grafana, Elasticsearch, Kibana를 활용한 모니터링 및 로그 관리 환경을 통해 시스템의 안정적인 운영을 지원합니다.

### 2-2. 소프트웨어 아키텍처
<img width="1148" height="705" alt="프레젠테이션2" src="https://github.com/user-attachments/assets/c3c2f677-bd1f-4277-83cb-59dcb5897e63" />

### 설명

해당 아키텍처는 Presentation부터 Database까지 계층적으로 구성된 Layered 구조로, 각 레이어가 역할에 따라 분리되어 있습니다.
요청은 상위 레이어에서 하위 레이어로 순차적으로 전달되며, Controller–Service–Component–DBIO를 거쳐 데이터 처리 및 비즈니스 로직이 수행됩니다.
또한 Utility와 Interface 영역을 통해 외부 시스템 연동 및 공통 기능을 분리하여, 확장성과 유지보수성을 고려한 구조로 설계되었습니다.


### 2-3. ERD 다이어그램
- Platform ERD
<img width="1260" height="884" alt="image" src="https://github.com/user-attachments/assets/4a8149e6-f786-4c71-89cd-9be5b0271297" />

- Bank ERD
<img width="2278" height="1186" alt="image" src="https://github.com/user-attachments/assets/6e84326f-3b30-4167-9705-98532151b6ad" />


## 3\. 주요 기능 소개

### 3-1. 핵심 기술 구성
<img width="1280" height="720" alt="슬라이드4" src="https://github.com/user-attachments/assets/6f65ae06-91dc-441f-8439-6857ef98e04a" />

### 3-2. 통합 워크플로우 다이어그램
<img width="3454" height="2633" alt="api흐름_120배율 (1)" src="https://github.com/user-attachments/assets/133f49dd-19b0-409c-aaeb-5184450efca5" />


### 3-3. 세부 기능 소개

#### [기능 1] Redis 분산 락을 이용한 재고 감소 로직

  * **기능 설명**: 동일한 상품에 1,000명이 동시에 결제를 시도할 때, 재고가 마이너스가 되지 않도록 Redisson 라이브러리를 활용해 원자성을 보장했습니다.
  * **핵심 코드**:

<!-- end list -->

```java
public void decreaseStock(Long productId, Long quantity) {
    RLock lock = redissonClient.getLock("stock_lock:" + productId);
    try {
        if (lock.tryLock(5, 1, TimeUnit.SECONDS)) { // 5초 대기, 1초 점유
            Product product = productRepository.findById(productId).orElseThrow();
            product.removeStock(quantity);
        }
    } catch (InterruptedException e) {
        throw new BusinessException("락 획득 실패");
    } finally {
        lock.unlock();
    }
}
```

  * **코드 링크**: OrderService.java

### [기능 2] ...
### [기능 3] ...
### [기능 4] ...
### [기능 5] ...
