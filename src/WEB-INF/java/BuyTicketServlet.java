import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONArray;

@WebServlet(name = "BuyTicketServlet", urlPatterns = {"/BuyTicket"})
public class BuyTicketServlet extends HttpServlet {
    
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
            // 获取请求参数
            String action = request.getParameter("action");
            JSONObject requestBody = null;
            
            // 如果是POST请求，尝试从请求体中获取JSON数据
            if (request.getMethod().equalsIgnoreCase("POST")) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = request.getReader().readLine()) != null) {
                    sb.append(line);
                }
                if (!sb.isEmpty()) {
                    requestBody = new JSONObject(sb.toString());
                    // 如果请求体中有action参数，优先使用
                    if (requestBody.has("action")) {
                        action = requestBody.getString("action");
                    }
                }
            }
            
            if ("getFilmDetails".equals(action)) {
                // 获取电影详细信息
                getFilmDetails(request, responseData);
            } else if ("searchSchedule".equals(action)) {
                // 查询场次信息
                searchSchedule(request, responseData, requestBody);
            } else {
                responseData.put("success", false);
                responseData.put("message", "无效的请求操作");
            }
        } catch (Exception e) {
            responseData.put("success", false);
            responseData.put("message", "服务器错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 发送响应
            out.println(responseData.toString());
            out.close();
        }
    }
    
    // 获取电影详细信息
    private void getFilmDetails(HttpServletRequest request, JSONObject responseData) throws SQLException {
        // 获取请求参数
        String publisherId = request.getParameter("publisherId");
        String filmId = request.getParameter("filmId");
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            // 获取数据库连接
            conn = DatabaseUtil.getConnection();
            
            // 查询电影详细信息
            String sql = "SELECT * FROM view_Film_All WHERE (Film_Publisher_ID, Film_ID) = (?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, publisherId);
            pstmt.setString(2, filmId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                JSONObject filmInfo = new JSONObject();
                filmInfo.put("filmName", rs.getString("Film_Name"));
                filmInfo.put("releaseDate", new SimpleDateFormat("yyyy-MM-dd").format(rs.getDate("Film_Release_Date")));
                filmInfo.put("filmTypes", rs.getString("Film_Type_Names"));
                filmInfo.put("languages", rs.getString("Film_Language_Names"));
                filmInfo.put("visualEffects", rs.getString("Visual_Effect_Names"));
                filmInfo.put("duration", rs.getInt("Film_Duration_Min"));
                filmInfo.put("boxOffice", new DecimalFormat("0.00").format(rs.getDouble("Film_Box_Office")));
                filmInfo.put("synopsis", rs.getString("Film_Intro"));
                filmInfo.put("publisherId", rs.getString("Film_Publisher_ID"));
                filmInfo.put("filmId", rs.getString("Film_ID"));
                
                responseData.put("success", true);
                responseData.put("filmInfo", filmInfo);
            } else {
                responseData.put("success", false);
                responseData.put("message", "未找到指定电影信息");
            }
        } finally {
            // 关闭数据库资源
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
    }
    
    // 查询场次信息
    private void searchSchedule(HttpServletRequest request, JSONObject responseData, JSONObject requestBody) throws SQLException {
        // 获取请求参数
        String publisherId = request.getParameter("publisherId");
        String filmId = request.getParameter("filmId");
        String province = request.getParameter("province");
        String city = request.getParameter("city");
        String date = request.getParameter("date");
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            // 获取数据库连接
            conn = DatabaseUtil.getConnection();
            
            // 查询指定日期的场次
            String sql = "SELECT * FROM view_Schedule_BuyTicket WHERE (Schedule_Film_Publisher_ID, Schedule_Film_ID, Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, TO_CHAR(Schedule_Show_Time, 'YYYY-MM-DD')) = (?, ?, ?, ?, ?) ORDER BY Schedule_Show_Time ASC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, publisherId);
            pstmt.setString(2, filmId);
            pstmt.setString(3, province);
            pstmt.setString(4, city);
            pstmt.setString(5, date);
            rs = pstmt.executeQuery();
            
            // 按影院分组
            Map<String, List<JSONObject>> cinemaSchedules = new HashMap<>();
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            DecimalFormat fareFormat = new DecimalFormat("0.00");
            
            while (rs.next()) {
                String cinemaName = rs.getString("Cinema_Name");
                JSONObject schedule = new JSONObject();
                
                schedule.put("publisherId", rs.getString("Schedule_Film_Publisher_ID"));
                schedule.put("filmId", rs.getString("Schedule_Film_ID"));
                schedule.put("cinemaProvinceCode", rs.getString("Schedule_Cinema_Province_Code"));
                schedule.put("cinemaCityCode", rs.getString("Schedule_Cinema_City_Code"));
                schedule.put("cinemaId", rs.getString("Schedule_Cinema_ID"));
                schedule.put("auditoriumId", rs.getString("Schedule_Auditorium_ID"));
                schedule.put("scheduleId", rs.getString("Schedule_ID"));
                schedule.put("showTime", rs.getTimestamp("Schedule_Show_Time"));
                schedule.put("language", rs.getString("Schedule_Film_Language"));
                schedule.put("fare", rs.getBigDecimal("Schedule_Fare"));
                schedule.put("visualEffect", rs.getString("Schedule_Visual_Effect"));
                
                // 格式化时间和票价
                schedule.put("formattedTime", timeFormat.format(rs.getTimestamp("Schedule_Show_Time")));
                schedule.put("formattedFare", fareFormat.format(rs.getBigDecimal("Schedule_Fare")));
                
                if (!cinemaSchedules.containsKey(cinemaName)) {
                    cinemaSchedules.put(cinemaName, new ArrayList<>());
                }
                cinemaSchedules.get(cinemaName).add(schedule);
            }
            
            // 将结果转换为JSON格式
            JSONObject cinemaSchedulesJson = new JSONObject();
            for (Map.Entry<String, List<JSONObject>> entry : cinemaSchedules.entrySet()) {
                JSONArray schedulesArray = new JSONArray();
                for (JSONObject schedule : entry.getValue()) {
                    schedulesArray.put(schedule);
                }
                cinemaSchedulesJson.put(entry.getKey(), schedulesArray);
            }
            
            responseData.put("success", true);
            responseData.put("cinemaSchedules", cinemaSchedulesJson);
        } finally {
            // 关闭数据库资源
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
    }
}