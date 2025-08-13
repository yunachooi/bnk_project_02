package com.example.bnk_project_02s.auth;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.lang.reflect.Field;

/**
 * @HmacOf 평문 필드 → 목적지(to) 필드에 HMAC(hex) 세팅.
 * - merge 경로에서 @Transient 값이 null일 수 있으므로 null/빈값이면 건너뜀
 * - 어떤 오류도 던지지 않고 로깅만 함(서비스 HOTFIX가 최종 보장)
 */
public class HmacEntityListener {

    @PrePersist @PreUpdate
    public void onWrite(Object entity){
        Class<?> clazz = entity.getClass();

        for (Field srcField : getAllDeclaredFields(clazz)) {
            HmacOf ann = srcField.getAnnotation(HmacOf.class);
            if (ann == null) continue;

            try {
                srcField.setAccessible(true);
                Object plain = srcField.get(entity);
                if (plain == null) continue;
                String plainStr = plain.toString();
                if (plainStr.isEmpty()) continue;

                String src = ann.domain() + plainStr;

                Field dstField = findFieldRecursive(clazz, ann.to());
                if (dstField == null) {
                    System.err.println("[HMAC] 대상 필드 없음: " + ann.to() + " in " + clazz.getName());
                    continue;
                }
                dstField.setAccessible(true);
                if (dstField.getType() != String.class) {
                    System.err.println("[HMAC] 대상 필드 타입 String 아님: " + ann.to());
                    continue;
                }
                if (CryptoBeans.HMAC == null) {
                    System.err.println("[HMAC] HMAC Bean 미초기화 — CryptoBeans.HMAC == null");
                    continue;
                }

                String hex = CryptoBeans.HMAC.hmacHex(src);
                if (hex == null) {
                    System.err.println("[HMAC] hmacHex 결과 null: src=" + src);
                    continue;
                }
                dstField.set(entity, hex);

            } catch (Exception e) {
                System.err.println("[HMAC] 처리 실패: entity=" + clazz.getName()
                        + ", sourceField=" + srcField.getName()
                        + ", to=" + ann.to() + ", domain=" + ann.domain());
                e.printStackTrace();
                // 예외를 던지지 않음 — 서비스 HOTFIX가 최종값을 채움
            }
        }
    }

    private static Field findFieldRecursive(Class<?> type, String name) {
        Class<?> cur = type;
        while (cur != null && cur != Object.class) {
            try { return cur.getDeclaredField(name); }
            catch (NoSuchFieldException ignore) { cur = cur.getSuperclass(); }
        }
        return null;
    }
    private static Iterable<Field> getAllDeclaredFields(Class<?> type) {
        return () -> new java.util.Iterator<>() {
            private Class<?> cur = type;
            private Field[] buf = (cur != null ? cur.getDeclaredFields() : new Field[0]);
            private int idx = 0;
            @Override public boolean hasNext() { advanceIfNeeded(); return cur != null && idx < buf.length; }
            @Override public Field next() { advanceIfNeeded(); return buf[idx++]; }
            private void advanceIfNeeded() {
                while (cur != null && (buf == null || idx >= buf.length)) {
                    cur = cur.getSuperclass();
                    if (cur == null || cur == Object.class) break;
                    buf = cur.getDeclaredFields();
                    idx = 0;
                }
            }
        };
    }
}