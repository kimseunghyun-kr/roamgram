package com.example.travelDiary.common.auth;

import com.example.travelDiary.common.auth.dto.Token;
import com.example.travelDiary.common.auth.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException{
        OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();
        UserDto userDto = new UserDto(oAuth2User);

        log.info("Principal에서 꺼낸 OAuth2User = {}", oAuth2User);
        // 최초 로그인이라면 회원가입 처리를 한다.
        String targetUrl;
        log.info("토큰 발행 시작");

        Token token = tokenService.generateToken(userDto.getEmail(), "USER");
        log.info("{}", token);
        response.setHeader("Authorization", token.getToken());
        targetUrl = UriComponentsBuilder.fromUriString("/home")
                .queryParam("Authorization", token.getToken())
                .build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}