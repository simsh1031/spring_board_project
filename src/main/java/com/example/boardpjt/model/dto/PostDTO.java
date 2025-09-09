package com.example.boardpjt.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 게시물 관련 데이터 전송 객체 (Data Transfer Object)
 * 클라이언트와 서버 간의 게시물 데이터 교환을 위한 DTO 클래스들을 포함
 * Entity와 View 계층 사이의 데이터 전달 역할을 담당하며, 보안과 캡슐화를 제공
 *
 * 설계 원칙:
 * - Entity 직접 노출 방지로 보안 강화
 * - 계층 간 결합도 감소
 * - API 응답 구조의 일관성 보장
 * - 클라이언트 요구사항에 맞춘 데이터 형태 제공
 */
public class PostDTO {

    /**
     * 게시물 생성/수정 요청을 위한 DTO 클래스
     * 클라이언트에서 서버로 게시물 데이터를 전송할 때 사용
     *
     * 사용 시나리오:
     * - 새 게시물 작성 시 폼 데이터 전달
     * - 기존 게시물 수정 시 변경된 데이터 전달
     * - HTML 폼의 input 필드와 매핑
     *
     * 보안 고려사항:
     * - username은 클라이언트에서 전송되지만 서버에서 인증된 사용자 정보로 덮어씀
     * - 악의적인 사용자가 다른 사용자 이름으로 게시물 작성 시도 방지
     */
    @Getter // Lombok: 모든 필드에 대한 getter 메서드 자동 생성
    @Setter // Lombok: 모든 필드에 대한 setter 메서드 자동 생성
    public static class Request {

        /**
         * 게시물 제목
         * 필수 입력 항목으로 HTML 폼의 title 필드와 매핑
         *
         * 검증 고려사항:
         * - @NotBlank: 빈 값이나 공백만 있는 경우 방지
         * - @Size(max = 100): 제목 길이 제한
         * - HTML 태그 이스케이프 처리로 XSS 방지
         */
        private String title;

        /**
         * 게시물 내용
         * 필수 입력 항목으로 HTML 폼의 content 필드와 매핑
         *
         * 검증 고려사항:
         * - @NotBlank: 빈 내용 방지
         * - @Size(max = 5000): 내용 길이 제한
         * - 개행 문자 처리 및 HTML 태그 이스케이프
         */
        private String content;

        /**
         * 작성자 사용자명
         * 보안상 클라이언트 값을 신뢰하지 않고 서버에서 재설정
         *
         * 처리 과정:
         * 1. 클라이언트에서 전송된 값 (신뢰하지 않음)
         * 2. 컨트롤러에서 authentication.getName()으로 덮어씀
         * 3. 서비스에서 실제 UserAccount 엔티티와 연결
         *
         * 보안 중요사항:
         * - 절대 클라이언트에서 전송된 username을 그대로 사용하지 않음
         * - JWT 토큰에서 추출한 인증된 사용자 정보만 신뢰
         */
        private String username;

        // === 추가 필드 고려사항 ===
        // 향후 확장 시 추가할 수 있는 필드들:
        // private String category;        // 게시물 카테고리
        // private List<String> tags;      // 태그 목록
        // private boolean isPrivate;      // 비공개 게시물 여부
        // private Long parentId;          // 답글인 경우 부모 게시물 ID
    }

    /**
     * 게시물 조회 응답을 위한 DTO 레코드 클래스
     * 서버에서 클라이언트로 게시물 데이터를 전달할 때 사용
     * Java 14+의 Record 문법을 활용하여 불변 객체로 구성
     *
     * Record의 장점:
     * - 불변성 보장 (모든 필드가 final)
     * - 자동 생성자, getter, equals, hashCode, toString 제공
     * - 간결한 코드로 데이터 클래스 정의
     * - 컴파일 타임 안전성 보장
     *
     * 사용 시나리오:
     * - 게시물 목록 조회 시 각 게시물의 요약 정보 제공
     * - 게시물 상세 조회 시 완전한 정보 제공
     * - JSON API 응답 구조의 일관성 보장
     */
    public record Response(
            /**
             * 게시물 고유 식별자
             * 데이터베이스의 Primary Key 값
             * URL 경로나 수정/삭제 요청 시 사용
             */
            Long id,

            /**
             * 게시물 제목
             * 목록 화면과 상세 화면에서 표시
             * XSS 방지를 위해 HTML 이스케이프 처리됨
             */
            String title,

            /**
             * 게시물 내용
             * 상세 화면에서 표시되며, 목록에서는 요약본 사용 고려
             *
             * 표시 고려사항:
             * - 목록 화면: 첫 100자 정도로 요약 표시
             * - 상세 화면: 전체 내용 표시
             * - 개행 문자를 HTML <br> 태그로 변환
             */
            String content,

            /**
             * 작성자 사용자명
             * Post 엔티티의 author.username에서 추출
             *
             * 보안 고려사항:
             * - 작성자의 다른 민감 정보 (이메일, 실명 등)는 노출하지 않음
             * - 사용자명만 공개 정보로 제공
             */
            String username,

            /**
             * 게시물 작성일시 (문자열 형태)
             * Post 엔티티의 createdAt 필드를 문자열로 변환
             *
             * 날짜 형식 고려사항:
             * - 현재: toString() 메서드 사용 (기본 형식)
             * - 개선: DateTimeFormatter로 사용자 친화적 형식 적용
             * - 예: "2024-01-15 14:30:25" 또는 "2024년 1월 15일 오후 2:30"
             *
             * 클라이언트 처리:
             * - JavaScript에서 Date 객체로 파싱 가능
             * - 상대 시간 표시 ("3시간 전", "2일 전" 등) 구현 가능
             */
            String createdAt

            // === 추가 필드 고려사항 ===
            // 향후 확장 시 추가할 수 있는 필드들:
            // String updatedAt,           // 수정일시
            // int viewCount,              // 조회수
            // int likeCount,              // 좋아요 수
            // int commentCount,           // 댓글 수
            // String category,            // 카테고리
            // List<String> tags,          // 태그 목록
            // boolean isMyPost            // 현재 사용자의 게시물 여부
    ) {
        // === Record 추가 메서드 정의 가능 ===

        /**
         * 게시물 내용 요약 메서드 (구현 예정)
         * 목록 화면에서 긴 내용을 요약하여 표시할 때 사용
         */
        /*
        public String getContentSummary(int maxLength) {
            if (content.length() <= maxLength) {
                return content;
            }
            return content.substring(0, maxLength) + "...";
        }
        */

        /**
         * 상대 시간 표시 메서드 (구현 예정)
         * "3시간 전", "2일 전" 형태로 시간 표시
         */
        /*
        public String getRelativeTime() {
            LocalDateTime postTime = LocalDateTime.parse(createdAt);
            Duration duration = Duration.between(postTime, LocalDateTime.now());

            if (duration.toDays() > 0) {
                return duration.toDays() + "일 전";
            } else if (duration.toHours() > 0) {
                return duration.toHours() + "시간 전";
            } else {
                return duration.toMinutes() + "분 전";
            }
        }
        */
    }

    // === DTO 사용 패턴 ===

    /**
     * Entity → DTO 변환 유틸리티 메서드 (구현 예정)
     * 서비스나 컨트롤러에서 Entity를 DTO로 변환할 때 사용
     */
    /*
    public static Response fromEntity(Post post) {
        return new Response(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getAuthor().getUsername(),
            post.getCreatedAt().toString()
        );
    }
    */

    /**
     * DTO → Entity 변환 유틸리티 메서드 (구현 예정)
     * Request DTO를 Entity로 변환할 때 사용 (새 게시물 생성 시)
     */
    /*
    public static Post toEntity(Request request, UserAccount author) {
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setAuthor(author);
        return post;
    }
    */
}

// === DTO 설계 원칙 및 고려사항 ===

/**
 * 1. 보안 고려사항:
 * - Entity 직접 노출 방지로 내부 구조 보호
 * - 민감한 정보 필터링 (비밀번호, 내부 ID 등)
 * - XSS 방지를 위한 데이터 검증 및 이스케이프
 * - 사용자 권한에 따른 필드 노출 제어
 *
 * 2. 성능 고려사항:
 * - 필요한 데이터만 전송하여 네트워크 부하 감소
 * - 지연 로딩 문제 해결 (N+1 쿼리 방지)
 * - JSON 직렬화 최적화
 * - 캐싱 전략 적용 용이
 *
 * 3. 유지보수성:
 * - Entity 변경이 API에 미치는 영향 최소화
 * - 버전 관리 및 하위 호환성 보장
 * - 명확한 API 계약 정의
 * - 테스트 용이성 향상
 *
 * 4. 확장성:
 * - 새로운 기능 추가 시 유연한 대응
 * - 다양한 클라이언트 요구사항 수용
 * - API 버전별 DTO 분리 가능
 * - 국제화(i18n) 지원 용이
 */

// === 검증 어노테이션 추가 예시 ===

/**
 * Bean Validation을 활용한 입력 검증 강화 방안:
 *
 * @Getter
 * @Setter
 * public static class Request {
 *     @NotBlank(message = "제목은 필수입니다")
 *     @Size(min = 1, max = 100, message = "제목은 1~100자 사이여야 합니다")
 *     private String title;
 *
 *     @NotBlank(message = "내용은 필수입니다")
 *     @Size(min = 1, max = 5000, message = "내용은 1~5000자 사이여야 합니다")
 *     private String content;
 *
 *     @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "사용자명 형식이 올바르지 않습니다")
 *     private String username;
 * }
 */

// === API 응답 형태 예시 ===

/**
 * 목록 조회 API 응답:
 * {
 *   "posts": [
 *     {
 *       "id": 1,
 *       "title": "첫 번째 게시물",
 *       "content": "게시물 내용입니다...",
 *       "username": "user123",
 *       "createdAt": "2024-01-15T14:30:25"
 *     },
 *     ...
 *   ]
 * }
 *
 * 상세 조회 API 응답:
 * {
 *   "post": {
 *     "id": 1,
 *     "title": "첫 번째 게시물",
 *     "content": "게시물 전체 내용입니다...",
 *     "username": "user123",
 *     "createdAt": "2024-01-15T14:30:25"
 *   }
 * }
 */