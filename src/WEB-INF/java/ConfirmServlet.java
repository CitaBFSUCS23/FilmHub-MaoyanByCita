import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/General/Confirm")
public class ConfirmServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    /**
     * 处理GET请求
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置响应内容类型
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        JSONObject responseObj = new JSONObject();
        
        try {
            // 获取操作类型
            String action = request.getParameter("action");
            
            if (action == null) {
                responseObj.put("success", false);
                responseObj.put("message", "无效的请求操作");
                out.println(responseObj.toString());
                return;
            }
            
            switch (action) {
                default:
                    responseObj.put("success", false);
                    responseObj.put("message", "无效的请求操作");
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseObj.put("success", false);
            responseObj.put("message", "服务器内部错误");
        } finally {
            out.println(responseObj.toString());
            out.close();
        }
    }
    
    /**
     * 处理POST请求
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置响应内容类型
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        JSONObject responseObj = new JSONObject();
        
        try {
            // 设置请求字符编码
            request.setCharacterEncoding("UTF-8");
            
            // 获取操作类型
            String action = null;
            JSONObject requestBody = null;
            
            // 获取请求内容类型
            String contentType = request.getContentType();
            
            // 如果是JSON类型的请求体，尝试解析
            if (contentType != null && contentType.contains("application/json")) {
                // 解析JSON请求体
                StringBuilder sb = new StringBuilder();
                String line;
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                if (!sb.isEmpty()) {
                    requestBody = new JSONObject(sb.toString());
                    if (requestBody.has("action")) {
                        action = requestBody.getString("action");
                    }
                }
            }
            
            // 获取请求头中的用户信息
            String userEmail = request.getHeader("X-User-Email");
            if (userEmail == null) {
                responseObj.put("success", false);
                responseObj.put("message", "请先登录");
                out.println(responseObj.toString());
                return;
            }
            
            switch (action) {
                case "getAuditoriumInfo":
                    // 获取影厅信息
                    getAuditoriumInfo(requestBody, responseObj, userEmail);
                    break;
                case "submitOrder":
                    // 提交订单
                    submitOrder(request, responseObj, userEmail);
                    break;
                default:
                    responseObj.put("success", false);
                    responseObj.put("message", "无效的请求操作");
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseObj.put("success", false);
            responseObj.put("message", "服务器内部错误");
        } finally {
            out.println(responseObj.toString());
            out.close();
        }
    }
    
    /**
     * 获取影厅信息、禁用座位和已售座位
     */
    private void getAuditoriumInfo(JSONObject requestBody, JSONObject responseObj, String userEmail) throws SQLException, IOException {
        // 从请求体中获取参数
        String publisherId = requestBody.getString("publisherId");
        String filmId = requestBody.getString("filmId");
        String filmLanguage = requestBody.getString("filmLanguage");
        String visualEffect = requestBody.getString("visualEffect");
        String cinemaProvinceCode = requestBody.getString("cinemaProvinceCode");
        String cinemaCityCode = requestBody.getString("cinemaCityCode");
        String cinemaId = requestBody.getString("cinemaId");
        String auditoriumId = requestBody.getString("auditoriumId");
        String scheduleId = requestBody.getString("scheduleId");
        
        // 获取影厅、电影和影院信息（从view_Confirm_Details视图）
        Map<String, Object> confirmInfo = getConfirmDetails(publisherId, filmId, filmLanguage, visualEffect, cinemaProvinceCode, cinemaCityCode, cinemaId, auditoriumId, scheduleId);
        if (confirmInfo == null) {
            responseObj.put("success", false);
            responseObj.put("message", "获取影厅信息失败");
            return;
        }
        
        // 获取禁用座位（从JSON文件）
        Set<String> disabledSeats = getDisabledSeats(cinemaProvinceCode, cinemaCityCode, cinemaId, auditoriumId);
        
        // 获取已售座位（从view_Unselectable_Seats视图）
        Set<String> occupiedSeats = getOccupiedSeats(publisherId, filmId, filmLanguage, visualEffect, cinemaProvinceCode, cinemaCityCode, cinemaId, auditoriumId, scheduleId);
        
        // 构建响应数据
        responseObj.put("success", true);
        responseObj.put("filmInfo", new JSONObject()
            .put("filmName", confirmInfo.get("Film_Name"))
            .put("cinemaName", confirmInfo.get("Cinema_Name")));
        responseObj.put("auditoriumInfo", new JSONObject()
            .put("auditoriumId", auditoriumId)
            .put("auditoriumRowCount", confirmInfo.get("Auditorium_Row_Count"))
            .put("auditoriumColCount", confirmInfo.get("Auditorium_Col_Count"))
            .put("scheduleFare", confirmInfo.get("Schedule_Fare")));
        responseObj.put("disabledSeats", new JSONArray(disabledSeats));
        responseObj.put("occupiedSeats", new JSONArray(occupiedSeats));
    }
    
    /**
     * 从view_Confirm_Details视图获取影厅、电影和影院信息
     */
    private Map<String, Object> getConfirmDetails(String publisherId, String filmId, String filmLanguage, String visualEffect, String cinemaProvinceCode, String cinemaCityCode, String cinemaId, String auditoriumId, String scheduleId) throws SQLException {
        Map<String, Object> confirmInfo = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            String sql = "SELECT * FROM view_Confirm_Details WHERE (Schedule_Film_Publisher_ID, Schedule_Film_ID, Schedule_Film_Language, Schedule_Visual_Effect, Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, Schedule_Cinema_ID, Schedule_Auditorium_ID, Schedule_ID) = (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, publisherId);
            pstmt.setString(2, filmId);
            pstmt.setString(3, filmLanguage);
            pstmt.setString(4, visualEffect);
            pstmt.setString(5, cinemaProvinceCode);
            pstmt.setString(6, cinemaCityCode);
            pstmt.setString(7, cinemaId);
            pstmt.setString(8, auditoriumId);
            pstmt.setString(9, scheduleId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                confirmInfo = new java.util.HashMap<>();
                confirmInfo.put("Film_Name", rs.getString("Film_Name"));
                confirmInfo.put("Cinema_Name", rs.getString("Cinema_Name"));
                confirmInfo.put("Schedule_Fare", rs.getInt("Schedule_Fare"));
                confirmInfo.put("Auditorium_Row_Count", rs.getInt("Auditorium_Row_Count"));
                confirmInfo.put("Auditorium_Col_Count", rs.getInt("Auditorium_Col_Count"));
            }
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
        
        return confirmInfo;
    }
    
    /**
     * 从JSON文件获取禁用座位
     */
    private Set<String> getDisabledSeats(String cinemaProvinceCode, String cinemaCityCode, String cinemaId, String auditoriumId) throws IOException {
        Set<String> disabledSeats = new HashSet<>();

        String formattedProvinceCode = String.format("%02d", Integer.parseInt(cinemaProvinceCode));
        String formattedCityCode = String.format("%02d", Integer.parseInt(cinemaCityCode));
        String formattedCinemaId = String.format("%04d", Integer.parseInt(cinemaId));
        String formattedAuditoriumId = String.format("%03d", Integer.parseInt(auditoriumId));
        String jsonFilePath = new File("uploads/cinemas/" + formattedProvinceCode + "/" + formattedCityCode + "/" + formattedCinemaId + "/" + formattedAuditoriumId + ".json").getAbsolutePath();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFilePath))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            
            JSONArray jsonArray = new JSONArray(jsonContent.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject seat = jsonArray.getJSONObject(i);
                int row = seat.getInt("row");
                int col = seat.getInt("col");
                String seatKey = row + "_" + col;
                disabledSeats.add(seatKey);
            }
        } catch (Exception e) {
            
        }
        
        return disabledSeats;
    }
    
    /**
     * 从view_Unselectable_Seats视图获取已售座位
     */
    private Set<String> getOccupiedSeats(String publisherId, String filmId, String filmLanguage, String visualEffect, String cinemaProvinceCode, String cinemaCityCode, String cinemaId, String auditoriumId, String scheduleId) throws SQLException {
        Set<String> occupiedSeats = new HashSet<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // 查询已售座位
            String sql = "SELECT Order_Row_No, Order_Col_No FROM view_Unselectable_Seats WHERE (Order_Film_Publisher_ID, Order_Film_ID, Order_Film_Language, Order_Visual_Effect, Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, Order_Auditorium_ID, Order_Schedule_ID) = (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, publisherId);
            pstmt.setString(2, filmId);
            pstmt.setString(3, filmLanguage);
            pstmt.setString(4, visualEffect);
            pstmt.setString(5, cinemaProvinceCode);
            pstmt.setString(6, cinemaCityCode);
            pstmt.setString(7, cinemaId);
            pstmt.setString(8, auditoriumId);
            pstmt.setString(9, scheduleId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int row = rs.getInt("Order_Row_No");
                int col = rs.getInt("Order_Col_No");
                String seatKey = row + "_" + col;
                occupiedSeats.add(seatKey);
            }
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
        
        return occupiedSeats;
    }
    
    /**
     * 提交订单
     */
    private void submitOrder(HttpServletRequest request, JSONObject responseObj, String userEmail) throws SQLException {
        // 获取参数
        String publisherId = request.getParameter("publisherId");
        String filmId = request.getParameter("filmId");
        String filmLanguage = request.getParameter("filmLanguage");
        String visualEffect = request.getParameter("visualEffect");
        String cinemaProvinceCode = request.getParameter("cinemaProvinceCode");
        String cinemaCityCode = request.getParameter("cinemaCityCode");
        String cinemaId = request.getParameter("cinemaId");
        String auditoriumId = request.getParameter("auditoriumId");
        String scheduleId = request.getParameter("scheduleId");
        String selectedSeats = request.getParameter("selectedSeats");
        
        Connection queryConn = null;
        PreparedStatement queryStmt = null;
        ResultSet queryRs = null;
        
        try {
            // 获取数据库连接
            queryConn = DatabaseUtil.getConnection();
            
            String querySql = "SELECT * FROM table_Schedule WHERE (Schedule_Film_Publisher_ID, Schedule_Film_ID,  Schedule_Film_Language, Schedule_Visual_Effect, Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, Schedule_Cinema_ID, Schedule_Auditorium_ID, Schedule_ID) = (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            queryStmt = queryConn.prepareStatement(querySql);
            queryStmt.setInt(1, Integer.parseInt(publisherId));
            queryStmt.setInt(2, Integer.parseInt(filmId));
            queryStmt.setString(3, filmLanguage);
            queryStmt.setString(4, visualEffect);
            queryStmt.setInt(5, Integer.parseInt(cinemaProvinceCode));
            queryStmt.setInt(6, Integer.parseInt(cinemaCityCode));
            queryStmt.setInt(7, Integer.parseInt(cinemaId));
            queryStmt.setInt(8, Integer.parseInt(auditoriumId));
            queryStmt.setInt(9, Integer.parseInt(scheduleId));
            queryRs = queryStmt.executeQuery();
        } finally {
            // 关闭查询资源
            DatabaseUtil.closeResources(queryRs, queryStmt, queryConn);
        }
        
        // 解析选中的座位
        String[] seats = selectedSeats.split(",");
        if (seats.length == 0 || (seats.length == 1 && seats[0].isEmpty())) {
            responseObj.put("success", false);
            responseObj.put("message", "请选择座位");
            return;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            // 获取数据库连接
            conn = DatabaseUtil.getConnection();
            
            // 关闭自动提交，开启事务
            conn.setAutoCommit(false);
            
            // 为每个选中的座位创建订单记录
            String insertSql = "INSERT INTO table_Order (Order_Film_Publisher_ID, Order_Film_ID, Order_Film_Language, Order_Visual_Effect, Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, Order_Auditorium_ID, Order_Schedule_ID, Order_Row_No, Order_Col_No, Order_Account_Email, Order_Status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            pstmt = conn.prepareStatement(insertSql);
            
            int insertedCount = 0;
            
            for (String seat : seats) {
                if (seat.trim().isEmpty()) continue;
                
                // 解析座位信息（格式：行号_列号）
                String[] seatParts = seat.split("_");
                if (seatParts.length != 2) continue;
                
                String rowNo = seatParts[0];
                String colNo = seatParts[1];
                
                // 设置参数
                pstmt.setInt(1, Integer.parseInt(publisherId));
                pstmt.setInt(2, Integer.parseInt(filmId));
                pstmt.setString(3, filmLanguage);
                pstmt.setString(4, visualEffect);
                pstmt.setInt(5, Integer.parseInt(cinemaProvinceCode));
                pstmt.setInt(6, Integer.parseInt(cinemaCityCode));
                pstmt.setInt(7, Integer.parseInt(cinemaId));
                pstmt.setInt(8, Integer.parseInt(auditoriumId));
                pstmt.setInt(9, Integer.parseInt(scheduleId));
                pstmt.setInt(10, Integer.parseInt(rowNo));
                pstmt.setInt(11, Integer.parseInt(colNo));
                pstmt.setString(12, userEmail);
                pstmt.setString(13, "P"); // 订单状态：P（未支付）
                
                pstmt.addBatch();
            }
        
            // 执行批次更新
            int[] results = pstmt.executeBatch();
            
            // 计算成功插入的记录数
            for (int result : results) {
                insertedCount += result;
            }
            
            conn.commit();
            responseObj.put("success", true);
            responseObj.put("message", "订单提交成功！共创建" + insertedCount + "个订单，状态为未支付。");
            responseObj.put("redirect", "../MyOrders/MyOrders.html");
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            
            // 检查是否是票数限制的错误
            if (e.getMessage().contains("trigger_Order_Ticket_Limit")) {
                responseObj.put("success", false);
                responseObj.put("message", "每个放映场次最多只能购买6张票");
            } else {
                responseObj.put("success", false);
                responseObj.put("message", "订单提交失败：" + e.getMessage());
            }
        } finally {
            DatabaseUtil.closeResources(null, pstmt, conn);
        }
    }
}