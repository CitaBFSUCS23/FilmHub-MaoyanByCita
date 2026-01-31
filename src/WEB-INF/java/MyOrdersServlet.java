import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONArray;

@WebServlet(name = "MyOrdersServlet", urlPatterns = {"/MyOrders"})
public class MyOrdersServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置响应内容类型为JSON
        response.setContentType("application/json; charset=UTF-8");
        // 设置请求编码为UTF-8
        request.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        JSONObject responseData = new JSONObject();
        
        try {
            // 获取用户邮箱（从请求头）
            String userEmail = request.getHeader("X-User-Email");
            
            // 获取请求类型（GET或POST）
            String method = request.getMethod();
            
            if ("GET".equals(method)) {
                // GET请求：获取订单列表
                getOrderList(request, responseData, userEmail);
            } else if ("POST".equals(method)) {
                // POST请求：处理订单操作
                handleOrderAction(request, responseData, userEmail);
            } else {
                responseData.put("success", false);
                responseData.put("message", "不支持的请求方法");
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseData.put("success", false);
            responseData.put("message", "服务器错误：" + e.getMessage());
        } finally {
            out.println(responseData.toString());
            out.close();
        }
    }
    
    // 获取订单列表
    private void getOrderList(HttpServletRequest request, JSONObject responseData, String userEmail) throws SQLException {
        // 获取分页参数
        int currentPage = 1;
        int pageSize = 5;
        
        try {
            currentPage = Integer.parseInt(request.getParameter("page"));
        } catch (NumberFormatException e) {
            currentPage = 1;
        }
        
        try {
            pageSize = Integer.parseInt(request.getParameter("pageSize"));
        } catch (NumberFormatException e) {
            pageSize = 5;
        }
        
        // 获取状态筛选参数
        String statusFilter = request.getParameter("status");
        statusFilter = (statusFilter != null && !statusFilter.trim().isEmpty()) ? statusFilter.trim() : null;
        
        // 计算偏移量
        int offset = (currentPage - 1) * pageSize;
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            // 获取数据库连接
            conn = DatabaseUtil.getConnection();
            
            // 获取总订单数
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM view_Order_Details WHERE Order_Account_Email = ?");
            List<Object> params = new ArrayList<>();
            params.add(userEmail);
            
            if (statusFilter != null) {
                countSql.append(" AND Status_Description = ?");
                params.add(statusFilter);
            }
            
            pstmt = conn.prepareStatement(countSql.toString());
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            rs = pstmt.executeQuery();
            int totalOrders = 0;
            if (rs.next()) {
                totalOrders = rs.getInt(1);
            }
            DatabaseUtil.closeResources(rs, pstmt, null);
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) totalOrders / pageSize);
            
            // 查询订单列表
            StringBuilder sql = new StringBuilder("SELECT Order_Account_Email, Film_Name, Cinema_Name, Schedule_Show_Time, ");
            sql.append("Schedule_Film_Language, Schedule_Visual_Effect, Auditorium_Name, Order_Row_No, Order_Col_No, ");
            sql.append("Schedule_Fare, Status_Description, Order_CreateTime, Schedule_Film_Publisher_ID, ");
            sql.append("Schedule_Film_ID, Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, ");
            sql.append("Schedule_Cinema_ID, Schedule_Auditorium_ID, Schedule_ID, Order_Visual_Effect ");
            sql.append("FROM view_Order_Details WHERE Order_Account_Email = ?");
            
            params = new ArrayList<>();
            params.add(userEmail);
            
            if (statusFilter != null) {
                sql.append(" AND Status_Description = ?");
                params.add(statusFilter);
            }
            
            sql.append(" ORDER BY Order_CreateTime DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add(offset);
            
            pstmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            rs = pstmt.executeQuery();
            
            JSONArray orders = new JSONArray();
            while (rs.next()) {
                JSONObject order = new JSONObject();
                order.put("orderAccountEmail", rs.getString("Order_Account_Email"));
                order.put("filmName", rs.getString("Film_Name"));
                order.put("cinemaName", rs.getString("Cinema_Name"));
                order.put("scheduleShowTime", rs.getTimestamp("Schedule_Show_Time").toString());
                order.put("scheduleFilmLanguage", rs.getString("Schedule_Film_Language"));
                order.put("scheduleVisualEffect", rs.getString("Schedule_Visual_Effect"));
                order.put("auditoriumName", rs.getString("Auditorium_Name"));
                order.put("orderRowNo", rs.getInt("Order_Row_No"));
                order.put("orderColNo", rs.getInt("Order_Col_No"));
                order.put("scheduleFare", rs.getDouble("Schedule_Fare"));
                order.put("statusDescription", rs.getString("Status_Description"));
                order.put("orderCreateTime", rs.getTimestamp("Order_CreateTime").toString());
                order.put("scheduleFilmPublisherId", rs.getInt("Schedule_Film_Publisher_ID"));
                order.put("scheduleFilmId", rs.getInt("Schedule_Film_ID"));
                order.put("scheduleCinemaProvinceCode", rs.getInt("Schedule_Cinema_Province_Code"));
                order.put("scheduleCinemaCityCode", rs.getInt("Schedule_Cinema_City_Code"));
                order.put("scheduleCinemaId", rs.getInt("Schedule_Cinema_ID"));
                order.put("scheduleAuditoriumId", rs.getInt("Schedule_Auditorium_ID"));
                order.put("scheduleId", rs.getInt("Schedule_ID"));
                order.put("orderVisualEffect", rs.getString("Order_Visual_Effect"));
                
                orders.put(order);
            }
            
            // 设置响应数据
            responseData.put("success", true);
            responseData.put("orders", orders);
            responseData.put("totalPages", totalPages);
            responseData.put("currentPage", currentPage);
            responseData.put("totalOrders", totalOrders);
            
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
    }
    
    // 处理订单操作
    private void handleOrderAction(HttpServletRequest request, JSONObject responseData, String userEmail) throws SQLException {
        // 从请求体获取JSON数据
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            java.io.BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            responseData.put("success", false);
            responseData.put("message", "读取请求参数失败");
            return;
        }
        
        JSONObject requestData = new JSONObject(sb.toString());
        
        // 获取请求参数
        String action = requestData.getString("action");
        
        // 获取订单信息
        int filmPublisherId = requestData.getInt("filmPublisherId");
        int filmId = requestData.getInt("filmId");
        String filmLanguage = requestData.getString("filmLanguage");
        String visualEffect = requestData.getString("visualEffect");
        int cinemaProvinceCode = requestData.getInt("cinemaProvinceCode");
        int cinemaCityCode = requestData.getInt("cinemaCityCode");
        int cinemaId = requestData.getInt("cinemaId");
        int auditoriumId = requestData.getInt("auditoriumId");
        int scheduleId = requestData.getInt("scheduleId");
        int rowNo = requestData.getInt("rowNo");
        int colNo = requestData.getInt("colNo");
        String createTimeStr = requestData.getString("createTime");
        
        // 转换创建时间为Timestamp
        Timestamp createTime = Timestamp.valueOf(createTimeStr);
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            // 获取数据库连接
            conn = DatabaseUtil.getConnection();
            
            String updateSql = "";
            String message = "";
            
            if ("cancelOrder".equals(action)) {
                // 取消订单
                updateSql = "UPDATE table_Order SET Order_Status = 'C' WHERE (Order_Film_Publisher_ID, Order_Film_ID, Order_Film_Language, Order_Visual_Effect, Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, Order_Auditorium_ID, Order_Schedule_ID, Order_Row_No, Order_Col_No, Order_CreateTime) = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AND Order_Account_Email = ?";
                message = "订单取消成功";
            } else if ("payOrder".equals(action)) {
                // 支付订单
                updateSql = "UPDATE table_Order SET Order_Status = 'D' WHERE (Order_Film_Publisher_ID, Order_Film_ID, Order_Film_Language, Order_Visual_Effect, Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, Order_Auditorium_ID, Order_Schedule_ID, Order_Row_No, Order_Col_No, Order_CreateTime) = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AND Order_Account_Email = ?";
                message = "支付成功";
            } else if ("refundOrder".equals(action)) {
                // 申请退票
                updateSql = "UPDATE table_Order SET Order_Status = 'R' WHERE (Order_Film_Publisher_ID, Order_Film_ID, Order_Film_Language, Order_Visual_Effect, Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, Order_Auditorium_ID, Order_Schedule_ID, Order_Row_No, Order_Col_No, Order_CreateTime) = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AND Order_Account_Email = ?";
                message = "退票成功！账款将在稍后返还到钱包";
            } else {
                responseData.put("success", false);
                responseData.put("message", "无效的操作类型");
                return;
            }
            
            // 执行更新操作
            pstmt = conn.prepareStatement(updateSql);
            pstmt.setInt(1, filmPublisherId);
            pstmt.setInt(2, filmId);
            pstmt.setString(3, filmLanguage);
            pstmt.setString(4, visualEffect);
            pstmt.setInt(5, cinemaProvinceCode);
            pstmt.setInt(6, cinemaCityCode);
            pstmt.setInt(7, cinemaId);
            pstmt.setInt(8, auditoriumId);
            pstmt.setInt(9, scheduleId);
            pstmt.setInt(10, rowNo);
            pstmt.setInt(11, colNo);
            pstmt.setTimestamp(12, createTime);
            pstmt.setString(13, userEmail);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                responseData.put("success", true);
                responseData.put("message", message);
            } else {
                responseData.put("success", false);
                responseData.put("message", "操作失败，订单不存在或已被修改");
            }
            
        } finally {
            DatabaseUtil.closeResources(null, pstmt, conn);
        }
    }
}