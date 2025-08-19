package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.auth.HmacUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/share")
public class ShareController {
    private final HmacUtil hmac;

    /** ✅ 기본값만 잡아둔 것: 기존 값 없으면 이대로 동작 (변경 無) */
    @Value("${share.allowed-paths:/user/shopping/product}")
    private String allowedPathsConf;

    @Value("${share.ttl-sec:3600}")
    private long ttlSec;

    private static final int VERSION = 1;

    private Set<String> allowed() {
        return Arrays.stream(allowedPathsConf.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String canonical(String path, long expSec, int v) {
        return path + "|" + expSec + "|" + v;
    }

    private static String normalizePath(String raw) {
        if (!StringUtils.hasText(raw)) return "/";
        String p = raw.trim();
        if (p.startsWith("http://") || p.startsWith("https://")) {
            throw new IllegalArgumentException("absolute url not allowed");
        }
        if (!p.startsWith("/")) p = "/" + p;
        p = p.replaceAll("/{2,}", "/");
        if (p.contains("..")) throw new IllegalArgumentException("path traversal");
        return p;
    }

    /** ✅ (그대로) 공유 링크 발급: text/plain */
    @GetMapping("/page")
    public ResponseEntity<String> issuePage(@RequestParam("path") String path) {
        String safe = normalizePath(path);
        if (!allowed().contains(safe)) {
            return ResponseEntity.badRequest().body("path not allowed");
        }
        long expSec = Instant.now().getEpochSecond() + ttlSec;
        String sig = hmac.hmacHexLower(canonical(safe, expSec, VERSION));

        String signed = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/share/r")
                .queryParam("path", safe)
                .queryParam("exp", expSec)   // 초 단위
                .queryParam("v", VERSION)
                .queryParam("sig", sig)
                .build(true)
                .toUriString();

        return ResponseEntity.ok()
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body(signed);
    }

    /** ✅ (그대로) 리졸브: 검증 후 302 */
    @GetMapping("/r")
    public ResponseEntity<Void> resolve(
            @RequestParam("path") String path,
            @RequestParam("exp") String expStr,
            @RequestParam("v") int v,
            @RequestParam("sig") String sigProvided
    ) {
        String safe = normalizePath(path);
        if (!allowed().contains(safe)) return ResponseEntity.status(403).build();

        long expParam = Long.parseLong(expStr);
        long expSec = (expParam >= 10_000_000_000L) ? (expParam / 1000L) : expParam; // ms 허용
        long nowSec = Instant.now().getEpochSecond();
        if (expSec < nowSec) return ResponseEntity.status(410).build();

        if (v != VERSION) return ResponseEntity.badRequest().build();

        String expected = hmac.hmacHexLower(canonical(safe, expSec, v));
        if (!slowEquals(expected.getBytes(StandardCharsets.US_ASCII),
                        sigProvided.toLowerCase().getBytes(StandardCharsets.US_ASCII))) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.status(302).location(URI.create(safe)).build();
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= (a[i] ^ b[i]);
        return r == 0;
    }
}