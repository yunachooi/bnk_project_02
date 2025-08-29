# 🏦 BNK 쇼핑환전(외환) 시스템

> **그린컴퓨터아카데미 K-Digital Training**  
> **TEAM 5조 - 우수를 최우수로 바꿔조**

## 📋 프로젝트 개요

BNK 부산은행의 외환 업무를 효율화하고 고객 편의성을 향상시키기 위한 **쇼핑환전(외환) 시스템**입니다.

본 프로젝트는 **그린컴퓨터아카데미 K-Digital Training 과정**의 팀 프로젝트로 진행되었으며, 실무 중심의 개발 역량 강화를 목표로 기획부터 구현까지 전 과정을 팀원들이 직접 수행했습니다. 치열한 경쟁 속에서 **최우수상을 수상**하며 프로젝트의 완성도와 기술적 우수성을 인정받았습니다.

## 👥 팀원 구성

**TEAM 5조 - 우수를 최우수로 바꿔조**

## ✨ 주요 기능

### 🔒 보안 기능
- **카드 번호 마스킹**: 민감정보 노출 방지를 위한 카드 번호 숨기기 기능
  - 카드 번호 일부를 '*' 문자로 대체
  - 사용자 편의성 증대 및 개인정보 보호 강화

- **스위치형 카드 제어**: 고객이 직접 제어할 수 있는 실시간 보안 관리
  - **해외 결제 ON/OFF**: 해외 가맹점에서의 카드 사용 제어
  - **온라인 결제 ON/OFF**: 인터넷 쇼핑몰 등 온라인 거래 차단/허용
  - **특정 국가 차단**: 위험 지역 또는 특정 국가에서의 카드 사용 제한
  - **특정 가맹점 차단**: 개별 가맹점별 사용 제한 설정

- **긴급 상황 대응**: 도난 및 분실 신고 시 즉시 카드 차단
  - 실시간 카드 비활성화
  - 보안 사고 이력 관리

### 💰 외환 거래 기능
- **실시간 환율 조회**: 최신 환율 정보 제공
- **외화 매매**: 다양한 외화에 대한 매매 서비스
- **거래 내역 관리**: 외환 거래 기록 조회 및 관리
- **환율 알림**: 목표 환율 도달 시 알림 서비스

### 👤 사용자 기능
- **직관적인 대시보드**: 한눈에 보는 카드 및 계좌 현황
- **간편 인증**: 생체인식, PIN 등 다양한 인증 방식
- **거래 한도 설정**: 일일/월별 사용 한도 자율 설정
- **사용 통계**: 카드 사용 패턴 분석 및 리포트 제공

### 🛠️ 관리자 기능
- **통합 모니터링**: 실시간 시스템 현황 및 거래 모니터링
- **사용자 계정 관리**: 고객 정보 및 권한 관리
- **위험 거래 탐지**: 이상 거래 패턴 감지 및 알림
- **통계 및 리포팅**: 업무용 데이터 분석 및 보고서 생성
- **시스템 설정**: 환율, 수수료, 정책 등 시스템 파라미터 관리

## 🎥 시연 영상

프로젝트 시연 영상을 통해 주요 기능들을 확인하실 수 있습니다.

## 🚀 기술 스택

### Frontend
- **HTML5/CSS3**: 웹 표준을 준수한 구조적 마크업 및 스타일링
- **JavaScript (ES6+)**: 동적 인터렙션 및 비동기 처리
- **Bootstrap**: 반응형 UI 프레임워크
- **jQuery**: DOM 조작 및 Ajax 통신

### Backend
- **Java**: 안정적이고 확장 가능한 서버사이드 개발
- **Spring Framework**: 
  - Spring MVC: 웹 애플리케이션 아키텍처
  - Spring Security: 인증 및 권한 관리
  - Spring Boot: 빠른 개발 및 배포
- **MyBatis**: 데이터베이스 연동 및 SQL 매핑

### Database
- **Oracle Database**: 대용량 데이터 처리 및 트랜잭션 관리
- **Redis**: 세션 관리 및 캐싱 (선택적)

### Development Tools
- **Maven**: 프로젝트 빌드 및 의존성 관리
- **Git**: 버전 관리 시스템
- **Eclipse/IntelliJ IDEA**: 통합 개발 환경

### Server & Deployment
- **Apache Tomcat**: WAS (Web Application Server)
- **Linux/Windows Server**: 운영체제

## 📁 프로젝트 구조

```
bnk_project_02/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── bnk/
│   │   │           ├── controller/          # 컨트롤러 레이어
│   │   │           │   ├── UserController.java
│   │   │           │   ├── AdminController.java
│   │   │           │   ├── CardController.java
│   │   │           │   └── ExchangeController.java
│   │   │           ├── service/             # 서비스 레이어
│   │   │           │   ├── UserService.java
│   │   │           │   ├── CardService.java
│   │   │           │   ├── SecurityService.java
│   │   │           │   └── ExchangeService.java
│   │   │           ├── dao/                 # 데이터 접근 레이어
│   │   │           │   ├── UserDao.java
│   │   │           │   ├── CardDao.java
│   │   │           │   └── ExchangeDao.java
│   │   │           ├── dto/                 # 데이터 전송 객체
│   │   │           │   ├── UserDto.java
│   │   │           │   ├── CardDto.java
│   │   │           │   └── ExchangeDto.java
│   │   │           ├── config/              # 설정 클래스
│   │   │           │   ├── SecurityConfig.java
│   │   │           │   └── DatabaseConfig.java
│   │   │           └── util/                # 유틸리티 클래스
│   │   │               ├── SecurityUtil.java
│   │   │               └── ExchangeUtil.java
│   │   ├── resources/
│   │   │   ├── mybatis/                     # MyBatis 매핑 파일
│   │   │   │   ├── mapper/
│   │   │   │   │   ├── UserMapper.xml
│   │   │   │   │   ├── CardMapper.xml
│   │   │   │   │   └── ExchangeMapper.xml
│   │   │   │   └── mybatis-config.xml
│   │   │   ├── static/                      # 정적 리소스
│   │   │   │   ├── css/
│   │   │   │   │   ├── common.css
│   │   │   │   │   ├── user.css
│   │   │   │   │   └── admin.css
│   │   │   │   ├── js/
│   │   │   │   │   ├── common.js
│   │   │   │   │   ├── card-control.js
│   │   │   │   │   ├── exchange.js
│   │   │   │   │   └── security.js
│   │   │   │   └── images/
│   │   │   │       ├── logo/
│   │   │   │       ├── icons/
│   │   │   │       └── bg/
│   │   │   └── application.properties       # 애플리케이션 설정
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           ├── views/                    # JSP 뷰 파일
│   │           │   ├── user/
│   │           │   │   ├── dashboard.jsp
│   │           │   │   ├── card-manage.jsp
│   │           │   │   └── exchange.jsp
│   │           │   ├── admin/
│   │           │   │   ├── dashboard.jsp
│   │           │   │   ├── user-manage.jsp
│   │           │   │   └── system-manage.jsp
│   │           │   └── common/
│   │           │       ├── header.jsp
│   │           │       ├── footer.jsp
│   │           │       └── login.jsp
│   │           ├── web.xml                  # 웹 애플리케이션 설정
│   │           └── spring/
│   │               ├── appServlet-servlet.xml
│   │               └── root-context.xml
│   └── test/                                # 테스트 코드
│       └── java/
├── docs/                                    # 프로젝트 문서
│   ├── api/                                 # API 문서
│   ├── database/                            # DB 스키마 및 ERD
│   └── presentation/                        # 발표 자료
├── sql/                                     # 데이터베이스 스크립트
│   ├── schema.sql                           # 테이블 생성 스크립트
│   ├── data.sql                             # 초기 데이터
│   └── procedures.sql                       # 저장 프로시저
├── pom.xml                                  # Maven 의존성 관리
└── README.md
```



---

**© 2025 그린컴퓨터아카데미 K-Digital Training TEAM 5조**
