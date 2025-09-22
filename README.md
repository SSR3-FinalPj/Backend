# 🚀 SSJ3PJ - Meaire Backend

본 프로젝트는 "AI, 세상의 반응을 듣다"라는 슬로건을 가진 AI 기반 소셜 미디어 콘텐츠 통합 관리 솔루션 **Meaire**의 백엔드 서버입니다. Spring Boot를 기반으로 구축되었으며, 사용자 인증, 데이터 수집, AI 콘텐츠 생성 요청, 소셜 미디어 연동 및 대시보드 데이터 제공 등 다양한 기능을 담당합니다.

## ✨ 주요 기능 (Features)

- **사용자 인증**: JWT(Access Token + HttpOnly Cookie Refresh Token)를 사용한 안전한 인증 시스템.
- **OAuth 2.0 연동**: Google (YouTube), Reddit 과의 OAuth 2.0 연동을 통해 사용자 계정으로 각 플랫폼의 API를 호출.
- **비동기 작업 처리**: Kafka를 이용해 AI 미디어(이미지/비디오) 생성과 같은 시간이 많이 소요되는 작업을 비동기적으로 처리.
- **실시간 알림**: SSE(Server-Sent Events)를 통해 콘텐츠 생성 완료 등 주요 이벤트를 클라이언트에 실시간으로 알림.
- **데이터 수집 파이프라인**: Kafka를 통해 서울시 실시간 도시 데이터 등 외부 데이터를 수집하고 Elasticsearch에 저장.
- **통합 데이터 조회**: Elasticsearch를 활용하여 YouTube, Reddit, 도시 데이터 등 다양한 소스의 데이터를 집계하고 대시보드 및 분석 API를 제공.
- **콘텐츠 게시**: 사용자를 대신하여 생성된 미디어를 YouTube, Reddit에 업로드.
- **보안 파일 관리**: AWS S3 Presigned URL을 사용하여 클라이언트가 안전하게 파일을 업로드하고 다운로드할 수 있도록 지원.
- **API 문서화**: SpringDoc (OpenAPI 3)을 통해 API 명세를 자동으로 생성하고, Swagger UI를 제공.
- **CI/CD**: GitHub Actions를 통해 Docker 이미지 빌드, ECR 푸시, ArgoCD 매니페스트 업데이트를 자동화.

## 🛠️ 기술 스택 (Tech Stack)

- **Framework**: Spring Boot 3
- **Language**: Java 17
- **Build Tool**: Maven
- **Database**: PostgreSQL (JPA/Hibernate)
- **Cache / Session**: Redis
- **Search / Analytics**: Elasticsearch
- **Message Queue**: Apache Kafka
- **Authentication**: Spring Security, JWT
- **Storage**: AWS S3
- **API Documentation**: SpringDoc (OpenAPI 3)
- **Containerization**: Docker

## 🏛️ 아키텍처 (Architecture)

- **RESTful API**: 표준 HTTP 메서드를 따르는 RESTful API를 제공합니다.
- **Controller-Service-Repository 패턴**: 각 계층의 역할을 명확히 분리하여 코드의 유지보수성과 테스트 용이성을 높였습니다.
- **이벤트 기반 아키텍처**: Kafka를 메시지 브로커로 사용하여 서비스 간의 결합도를 낮추고, AI 처리와 같은 비동기 작업을 효율적으로 관리합니다.
- **스케줄링**: Spring Scheduler를 사용하여 주기적으로 만료되는 OAuth 토큰을 자동 갱신합니다.
- **외부 서비스 연동**: Python 기반의 AI 서비스(Bridge)와 RestTemplate을 통해 통신하여 미디어 생성을 요청합니다.

## 📁 디렉토리 구조 (Directory Structure)

```
.
├── .github/workflows/deploy.yaml  # CI/CD 파이프라인
├── src
│   ├── main
│   │   ├── java/org/example/ssj3pj
│   │   │   ├── config/          # Spring, 외부 서비스(AWS, Kafka, Redis 등) 설정
│   │   │   ├── controller/      # API 엔드포인트 정의 (Controller)
│   │   │   ├── dto/             # 데이터 전송 객체 (Request/Response DTOs)
│   │   │   ├── entity/          # JPA 엔티티 (데이터베이스 테이블 매핑)
│   │   │   ├── kafka/           # Kafka Producer/Consumer
│   │   │   ├── redis/           # Redis 관련 서비스
│   │   │   ├── repository/      # Spring Data JPA 리포지토리
│   │   │   ├── scheduler/       # 주기적 작업 (토큰 갱신 등)
│   │   │   ├── security/        # Spring Security, JWT 관련 설정
│   │   │   └── services/        # 비즈니스 로직 (Service)
│   │   └── resources
│   │       └── application.yml  # 메인 애플리케이션 설정
│   └── test/                    # 테스트 코드
├── Dockerfile                   # Docker 이미지 빌드 설정
├── pom.xml                      # Maven 프로젝트 설정 및 의존성 관리
└── README.md                    # 프로젝트 문서
```

## ⚙️ 시작하기 (Getting Started)

### 1. 사전 요구사항

- Java 17
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL
- Redis
- Elasticsearch
- Kafka

### 2. 설정

1.  프로젝트 루트의 `application.yml` 파일을 환경에 맞게 수정합니다. 특히 다음 항목들은 반드시 설정해야 합니다.
    - **Database**: `spring.datasource`의 `url`, `username`, `password`
    - **JWT**: `jwt.secret`
    - **External Services**: `prompt.server.base` (AI 브릿지 서버 주소)
    - **OAuth Credentials**: `google.*`, `reddit.*` (각 플랫폼에서 발급받은 클라이언트 ID/Secret)
    - **AWS**: `aws.region`, `aws.access_key_id`, `aws.secret_access_key`, `aws.s3.bucket`

2.  필요한 외부 서비스(PostgreSQL, Redis, Elasticsearch, Kafka)를 직접 설치하거나 Docker Compose를 이용하여 실행합니다.

### 3. 빌드 및 실행

**Maven으로 직접 실행:**

```bash
# 프로젝트 빌드 (테스트 스킵)
./mvnw clean package -DskipTests

# 애플리케이션 실행
java -jar target/SSJ3PJ-0.0.1-SNAPSHOT.jar
```

**Spring Boot 플러그인으로 실행:**

```bash
./mvnw spring-boot:run
```

## 🐳 Docker

프로젝트 루트에 포함된 `Dockerfile`을 사용하여 애플리케이션을 컨테이너화할 수 있습니다.

```bash
# 1. Docker 이미지 빌드
docker build -t ssj3pj-backend .

# 2. Docker 컨테이너 실행
# .env 파일을 통해 환경변수를 주입하는 것을 권장합니다.
docker run -p 8080:8080 --env-file .env ssj3pj-backend
```

## 📖 API 문서

애플리케이션 실행 후, 다음 주소에서 API 문서를 확인할 수 있습니다.

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

인증이 필요한 API는 Swagger UI의 `Authorize` 버튼을 통해 JWT Access Token을 `Bearer <token>` 형식으로 입력하여 테스트할 수 있습니다.
