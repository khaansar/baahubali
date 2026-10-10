package com.example.payment.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.nio.ByteBuffer;
import java.util.UUID;

@Repository
public class CouponUsageJdbc {
    private final JdbcTemplate jdbc;
    public CouponUsageJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Returns true iff the per-user counter moved. Row-level atomic: concurrent calls serialize on the PK row. */
    public boolean tryIncrementUser(UUID couponId, UUID userId, Integer limit) {
        jdbc.update("INSERT IGNORE INTO coupon_user_usage (coupon_id, user_id, used_count) VALUES (?,?,0)", bytes(couponId), bytes(userId));
        int rows = (limit == null)
            ? jdbc.update("UPDATE coupon_user_usage SET used_count = used_count + 1 WHERE coupon_id=? AND user_id=?", bytes(couponId), bytes(userId))
            : jdbc.update("UPDATE coupon_user_usage SET used_count = used_count + 1 WHERE coupon_id=? AND user_id=? AND used_count < ?",
                          bytes(couponId), bytes(userId), limit);
        return rows == 1;
    }
    public void decrementUser(UUID couponId, UUID userId) {
        jdbc.update("UPDATE coupon_user_usage SET used_count = used_count - 1 WHERE coupon_id=? AND user_id=? AND used_count > 0",
            bytes(couponId), bytes(userId));
    }
    static byte[] bytes(UUID u) {
        ByteBuffer b = ByteBuffer.allocate(16);
        b.putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits());
        return b.array();
    }
}