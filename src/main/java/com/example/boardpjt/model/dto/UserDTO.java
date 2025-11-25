package com.example.boardpjt.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 정보를 전달하는 DTO
 *
 * 목적:
 * - 팔로잉/팔로워 목록 조회 시 사용자 정보 반환
 * - Entity 직접 노출 방지로 보안 강화
 * - 필요한 정보만 전달하여 효율성 증대
 *
 * 포함된 정보:
 * - id: 사용자 ID
 * - username: 사용자명
 * - role: 사용자 역할
 *
 * 제외된 정보:
 * - password: 비밀번호 (보안)
 * - followers/following: 관계 정보 (순환 참조 방지)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    /**
     * 사용자 고유 ID
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 사용자명
     */
    @JsonProperty("username")
    private String username;

    /**
     * 사용자 역할/권한
     * 예: "ROLE_USER", "ROLE_ADMIN"
     */
    @JsonProperty("role")
    private String role;
}