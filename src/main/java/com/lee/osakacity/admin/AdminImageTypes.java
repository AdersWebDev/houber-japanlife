package com.lee.osakacity.admin;

/** 파일 앞부분으로 사진 종류를 판별 (파일 이름이나 브라우저가 보낸 종류는 믿지 않음) */
public final class AdminImageTypes {
    private AdminImageTypes() {}

    /** @return image/jpeg, image/png, image/webp, image/gif 또는 null */
    public static String sniff(byte[] d) {
        if (d == null || d.length < 12) return null;
        if ((d[0] & 0xff) == 0xFF && (d[1] & 0xff) == 0xD8 && (d[2] & 0xff) == 0xFF) return "image/jpeg";
        if ((d[0] & 0xff) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return "image/png";
        if (d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F' && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') return "image/webp";
        if (d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8') return "image/gif";
        return null;
    }

    public static String ext(String type) {
        return switch (type) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> throw new IllegalArgumentException(type);
        };
    }
}
