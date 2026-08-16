package com.mugunghwa.goodquestion.admin.global.error;

import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 없는 경로의 응답.
 *
 * <p>이 검사가 있는 이유: 없는 경로가 500 으로 나오던 때가 있었다. 주소를 잘못 친
 * 것뿐인데 서버가 고장 난 것처럼 보여서, 배포를 확인하다가 없는 장애를 한참 쫓았다.
 *
 * <p>로그인해서 호출한다. 인증이 없으면 보안 필터가 먼저 401 을 돌려줘서 이 문제가
 * 드러나지 않는다. 토큰을 들고 주소를 잘못 치는 것이 실제로 겪는 상황이다.
 */
@IntegrationTest
@AutoConfigureMockMvc
class UnknownPathTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String body = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@goodquestion.kr","password":"admin1234!"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("accessToken").asString();
    }

    @Test
    @DisplayName("없는 경로는 404 로 답한다")
    void unknownPathReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("있는 경로는 그대로 동작한다")
    void knownPathStillWorks() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
