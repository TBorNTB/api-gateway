# api-gateway

Spring Cloud Gateway 기반의 API Gateway 서비스입니다.

이 프로젝트는 마이크로서비스 아키텍처(MSA)에서 외부 클라이언트의 요청을 내부 서비스(`user-service` 등)로 라우팅하고,  
공통 인증/인가, 로깅, CORS 설정 등의 기능을 중앙에서 처리하는 역할을 합니다.

## 주요 기능

- 요청 라우팅 (Route Mapping)
- 서비스 디스커버리 연동 (Eureka)
- 공통 헤더 및 인증 필터
- CORS 설정
- 응답 로깅

## 관련 서비스

- discovery-service (Eureka)
- user-service
... 추후 확장 예정
## 기술 스택

- Java 21+
- Spring Boot
- Spring Cloud Gateway
- Eureka Client
