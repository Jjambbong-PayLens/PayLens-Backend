package com.Jjambbong.PayLens.user.controller;

import com.Jjambbong.PayLens.user.dto.MypageResponseDto;
import com.Jjambbong.PayLens.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<MypageResponseDto> getMypageInfo(@PathVariable Long id) {
        MypageResponseDto responseDto = userService.getMypageInfo(id);
        return ResponseEntity.ok(responseDto);
    }
}