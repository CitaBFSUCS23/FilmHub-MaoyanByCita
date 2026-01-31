import org.json.JSONObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;



@WebServlet(name = "CheckinServlet", value = "/Checkin")
public class CheckinServlet extends HttpServlet {

    // 状态描述映射
    private static final Map<String, String> statusMap = new HashMap<>();
    static {
        statusMap.put("D", "已付");
        statusMap.put("I", "检票");
        statusMap.put("T", "暂离");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置响应类型
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject result = new JSONObject();

        try {
            // 获取请求体
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JSONObject requestData = new JSONObject(sb.toString());

            // 获取订单号
            String orderNumber = requestData.getString("orderNumber");

            // 验证订单号长度

                Connection conn = null;
                PreparedStatement selectPstmt = null;
                ResultSet rs = null;
                PreparedStatement updatePstmt = null;

                try {
                // 解析订单号
                int publisherId = Integer.parseInt(orderNumber.substring(0, 6));
                int filmId = Integer.parseInt(orderNumber.substring(6, 12));
                int provinceCode = Integer.parseInt(orderNumber.substring(12, 14));
                int cityCode = Integer.parseInt(orderNumber.substring(14, 16));
                int cinemaId = Integer.parseInt(orderNumber.substring(16, 20));
                int auditoriumId = Integer.parseInt(orderNumber.substring(20, 23));
                int scheduleId = Integer.parseInt(orderNumber.substring(23, 26));
                int rowNo = Integer.parseInt(orderNumber.substring(26, 28));
                int colNo = Integer.parseInt(orderNumber.substring(28, 30));
                String createTimeStr = orderNumber.substring(30);
                String year = createTimeStr.substring(0, 4);
                String month = createTimeStr.substring(4, 6);
                String day = createTimeStr.substring(6, 8);
                String hour = createTimeStr.substring(8, 10);
                String minute = createTimeStr.substring(10, 12);
                String second = createTimeStr.substring(12, 14);
                String microsecond = createTimeStr.substring(14);

                // 构建Timestamp需要的格式
                String formattedTime = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + "." + microsecond;
                
                Timestamp createTime = Timestamp.valueOf(formattedTime);

                // 获取数据库连接
                conn = DatabaseUtil.getConnection();

                // 获取当前订单状态
                String selectSql = "SELECT Order_Status, Order_Film_Language, Order_Visual_Effect FROM table_Order WHERE (Order_Film_Publisher_ID, Order_Film_ID, Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, Order_Auditorium_ID, Order_Schedule_ID, Order_Row_No, Order_Col_No, Order_CreateTime) = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                selectPstmt = conn.prepareStatement(selectSql);
                // 正确设置所有10个参数，按照SQL语句中占位符的顺序
                selectPstmt.setInt(1, publisherId);    // Order_Film_Publisher_ID
                selectPstmt.setInt(2, filmId);          // Order_Film_ID
                selectPstmt.setInt(3, provinceCode);    // Order_Cinema_Province_Code
                selectPstmt.setInt(4, cityCode);        // Order_Cinema_City_Code
                selectPstmt.setInt(5, cinemaId);        // Order_Cinema_ID
                selectPstmt.setInt(6, auditoriumId);    // Order_Auditorium_ID
                selectPstmt.setInt(7, scheduleId);      // Order_Schedule_ID
                selectPstmt.setInt(8, rowNo);           // Order_Row_No
                selectPstmt.setInt(9, colNo);           // Order_Col_No
                selectPstmt.setTimestamp(10, createTime); // Order_CreateTime

                rs = selectPstmt.executeQuery();
                if (rs.next()) {
                    String currentStatus = rs.getString("Order_Status");
                    String filmLanguage = rs.getString("Order_Film_Language");
                    String visualEffect = rs.getString("Order_Visual_Effect");

                    // 确定下一个状态
                    String nextStatus = "";
                    if (currentStatus.equals("D")) {
                        nextStatus = "I"; // 已付 → 检票
                    } else if (currentStatus.equals("I")) {
                        nextStatus = "T"; // 检票 → 暂离
                    } else if (currentStatus.equals("T")) {
                        nextStatus = "I"; // 暂离 → 检票
                    } else {
                        // 其他状态不允许验票
                        result.put("success", false);
                        result.put("message", "该订单当前状态不允许验票");
                        response.getWriter().write(result.toString());
                        return;
                    }

                    // 更新订单状态
                    String updateSql = "UPDATE table_Order SET Order_Status = ? WHERE (Order_Film_Publisher_ID, Order_Film_ID, Order_Film_Language, Order_Visual_Effect, Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, Order_Auditorium_ID, Order_Schedule_ID, Order_Row_No, Order_Col_No, Order_CreateTime) = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    updatePstmt = conn.prepareStatement(updateSql);
                    updatePstmt.setString(1, nextStatus);
                    updatePstmt.setInt(2, publisherId);
                    updatePstmt.setInt(3, filmId);
                    updatePstmt.setString(4, filmLanguage);
                    updatePstmt.setString(5, visualEffect);
                    updatePstmt.setInt(6, provinceCode);
                    updatePstmt.setInt(7, cityCode);
                    updatePstmt.setInt(8, cinemaId);
                    updatePstmt.setInt(9, auditoriumId);
                    updatePstmt.setInt(10, scheduleId);
                    updatePstmt.setInt(11, rowNo);
                    updatePstmt.setInt(12, colNo);
                    updatePstmt.setTimestamp(13, createTime);

                    int rowsAffected = updatePstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        String successMessage = "验票成功！订单状态已从 " + statusMap.get(currentStatus) + " 更新为 " + statusMap.get(nextStatus);
                        result.put("success", true);
                        result.put("message", successMessage);
                    } else {
                        result.put("success", false);
                        result.put("message", "验票失败，未找到匹配的订单");
                    }

                } else {
                    result.put("success", false);
                    result.put("message", "订单号无效，未找到匹配的订单");
                }

            } finally {
                // 关闭资源
                DatabaseUtil.closeResources(rs, selectPstmt, conn);
                if (updatePstmt != null) {
                    DatabaseUtil.closeResources(null, updatePstmt, null);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "验票失败：" + e.getMessage());
        }

        response.getWriter().write(result.toString());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // GET请求直接重定向到HTML页面
        response.sendRedirect("/Manager/Checkin/Checkin.html");
    }
}