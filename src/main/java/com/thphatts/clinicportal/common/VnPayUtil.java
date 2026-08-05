package com.thphatts.clinicportal.common;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class VnPayUtil {

    public static String buildQueryString(Map<String, String> params, boolean encode) {
        // copy dữ liệu vào TreeMap để tự động sắp xếp theo alphabet
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder query = new StringBuilder();
        boolean isFirst = true; // đánh dấu để biết có cần thêm dấu "&" phía trước hay không

        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // Bỏ qua nếu value rỗng hoặc null, theo đúng yêu cầu của VNPay
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (!isFirst) {
                query.append("&"); // chỉ thêm "&" TRƯỚC mỗi cặp, trừ cặp đầu tiên
            }
            query.append(key)
                    .append("=")
                    .append(encode == true ? URLEncoder.encode(value, StandardCharsets.UTF_8) : value);

            isFirst = false;
        }

        return query.toString();
    }
}