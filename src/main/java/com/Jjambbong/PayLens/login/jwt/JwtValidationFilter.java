package com.Jjambbong.PayLens.login.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtValidationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            // 토큰 파싱/검증 (여기서 예외 터질 수 있음)
            jwtProvider.validate(token);

            // userId 추출
            Long userId = jwtProvider.getUserId(token);

            // 인증 등록
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,                     // principal(주체): userId
                            null,                       // 비밀번호같은 민감값: 없음
                            Collections.emptyList()     // 권한(role): 없음
                    );

            // 시큐리티 컨텍스트에 등록: @AuthenticationPrincipal 같은 걸로 인증 정보 접근 가능해짐.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            // 잘못된 서명 / 위조 / 형식 오류
            sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
        } catch (ExpiredJwtException e) {
            // 토큰 만료
            sendErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            // 지원하지 않는 형식의 토큰
            sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
        } catch (IllegalArgumentException e) {
            // 널 / 공백 등 잘못된 입력
            sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
        } catch (Exception e) {
            // 예기치 못한 오류
            sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
        }
    }

    // 표준 에러 응답 -> JSON 반환
    // 상태 코드 401 통일로 되어있어서 에러 코드에 따라 상태 코드 출력 되도록 수정함
    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());
        response.getWriter().write(
                new ObjectMapper().writeValueAsString(ApiResponse.onFailure(errorCode, null))
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // reissue 엔드포인트만 필터 제외
        return uri.startsWith("/api/auth/reissue");
    }
}
