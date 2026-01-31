import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * PlayMoviesServlet
 */
@WebServlet("/Manager/PlayMovies")
public class PlayMoviesServlet extends HttpServlet {
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
                responseObj.put("status", "error");
                responseObj.put("message", "未指定操作类型");
                out.write(responseObj.toString());
                return;
            }
            
            switch (action) {
                case "getAuditoriums":
                    getAuditoriums(request, response, out, responseObj);
                    break;
                case "getAvailableFilms":
                    getAvailableFilms(request, response, out, responseObj);
                    break;
                default:
                    responseObj.put("status", "error");
                    responseObj.put("message", "未知操作类型: " + action);
                    out.write(responseObj.toString());
                    break;
            }
        } catch (Exception e) {
            responseObj.put("status", "error");
            responseObj.put("message", "操作失败: " + e.getMessage());
            out.write(responseObj.toString());
            e.printStackTrace();
        } finally {
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
            // 获取操作类型
            String action = request.getParameter("action");
            
            if (action == null) {
                responseObj.put("status", "error");
                responseObj.put("message", "未指定操作类型");
                out.write(responseObj.toString());
                return;
            }
            
            switch (action) {
                case "submitSchedule":
                    submitSchedule(request, response, out, responseObj);
                    break;
                default:
                    responseObj.put("status", "error");
                    responseObj.put("message", "未知操作类型: " + action);
                    out.write(responseObj.toString());
                    break;
            }
        } catch (Exception e) {
            responseObj.put("status", "error");
            responseObj.put("message", "操作失败: " + e.getMessage());
            out.write(responseObj.toString());
            e.printStackTrace();
        } finally {
            out.close();
        }
    }
    
    /**
     * 获取影厅列表
     */
    private void getAuditoriums(HttpServletRequest request, HttpServletResponse response, PrintWriter out, JSONObject responseObj) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> auditoriumList = new ArrayList<>();
        
        try {
            // 从请求头获取影院信息
            String cinemaProvinceCode = request.getHeader("X-Cinema-Province-Code");
            String cinemaCityCode = request.getHeader("X-Cinema-City-Code");
            String cinemaId = request.getHeader("X-Cinema-Id");
            
            // 验证影院信息
            if (cinemaProvinceCode == null || cinemaCityCode == null || cinemaId == null) {
                responseObj.put("status", "error");
                responseObj.put("message", "未获取到影院信息");
                out.write(responseObj.toString());
                return;
            }
            
            // 查询当前影院的所有影厅
            String sql = "SELECT Auditorium_ID, Auditorium_Name FROM table_Auditorium WHERE (Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID) = (?, ?, ?) ORDER BY Auditorium_ID";
            
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, cinemaProvinceCode);
            pstmt.setString(2, cinemaCityCode);
            pstmt.setString(3, cinemaId);
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> auditorium = new HashMap<>();
                auditorium.put("auditoriumId", rs.getString("Auditorium_ID"));
                auditorium.put("auditoriumName", rs.getString("Auditorium_Name"));
                auditoriumList.add(auditorium);
            }
            
            // 构建响应
            responseObj.put("status", "success");
            responseObj.put("auditoriums", new JSONArray(auditoriumList));
            out.write(responseObj.toString());
            
        } catch (SQLException e) {
            responseObj.put("status", "error");
            responseObj.put("message", "查询影厅失败: " + e.getMessage());
            out.write(responseObj.toString());
            e.printStackTrace();
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);;
        }
    }
    
    /**
     * 获取可放映影片
     */
    private void getAvailableFilms(HttpServletRequest request, HttpServletResponse response, PrintWriter out, JSONObject responseObj) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> availableFilms = new ArrayList<>();
        
        try {
            // 从请求头获取影院信息
            String cinemaProvinceCode = request.getHeader("X-Cinema-Province-Code");
            String cinemaCityCode = request.getHeader("X-Cinema-City-Code");
            String cinemaId = request.getHeader("X-Cinema-Id");
            
            // 获取请求参数
            String auditoriumId = request.getParameter("auditoriumId");
            String date = request.getParameter("date");
            
            // 验证参数
            if (cinemaProvinceCode == null || cinemaCityCode == null || cinemaId == null) {
                responseObj.put("status", "error");
                responseObj.put("message", "未获取到影院信息");
                out.write(responseObj.toString());
                return;
            }
            
            if (auditoriumId == null || date == null || date.trim().isEmpty()) {
                responseObj.put("status", "error");
                responseObj.put("message", "影厅ID和日期不能为空");
                out.write(responseObj.toString());
                return;
            }
            
            // 查询可放映的影片
            String sql = "SELECT DISTINCT Film_Publisher_ID, Film_ID, Film_Name, Film_Duration_Min, " +
                       "Film_Release_Date, Film_Finished_Date, Publisher_Name, Film_Min_Fare, " +
                       "Film_Language_Name, Visual_Effect_Name " +
                       "FROM view_Play_Movies " +
                       "WHERE (Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID, Auditorium_ID) = (?, ?, ?, ?)" +
                       "AND ? BETWEEN Film_Release_Date AND Film_Finished_Date";
            
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, cinemaProvinceCode);
            pstmt.setString(2, cinemaCityCode);
            pstmt.setString(3, cinemaId);
            pstmt.setString(4, auditoriumId);
            pstmt.setString(5, date);
            
            rs = pstmt.executeQuery();
            
            Map<String, Map<String, Object>> filmMap = new HashMap<>();
            while (rs.next()) {
                String filmKey = rs.getString("Film_Publisher_ID") + "_" + rs.getString("Film_ID");
                
                if (!filmMap.containsKey(filmKey)) {
                    Map<String, Object> film = new HashMap<>();
                    film.put("publisherId", rs.getString("Film_Publisher_ID"));
                    film.put("filmId", rs.getString("Film_ID"));
                    film.put("filmName", rs.getString("Film_Name"));
                    film.put("duration", rs.getInt("Film_Duration_Min"));
                    film.put("releaseDate", rs.getDate("Film_Release_Date"));
                    film.put("finishedDate", rs.getDate("Film_Finished_Date"));
                    film.put("publisherName", rs.getString("Publisher_Name"));
                    film.put("Film_Min_Fare", rs.getDouble("Film_Min_Fare"));
                    film.put("languages", new HashSet<String>());
                    film.put("visualEffects", new HashSet<String>());
                    filmMap.put(filmKey, film);
                }
                
                Map<String, Object> film = filmMap.get(filmKey);
                ((Set<String>) film.get("languages")).add(rs.getString("Film_Language_Name"));
                ((Set<String>) film.get("visualEffects")).add(rs.getString("Visual_Effect_Name"));
            }
            
            availableFilms = new ArrayList<>(filmMap.values());
            
            // 将Set转换为List以支持JSON序列化
            for (Map<String, Object> film : availableFilms) {
                film.put("languages", new ArrayList<>((Set<String>) film.get("languages")));
                film.put("visualEffects", new ArrayList<>((Set<String>) film.get("visualEffects")));
            }
            
            // 构建响应
            responseObj.put("status", "success");
            responseObj.put("films", new JSONArray(availableFilms));
            out.write(responseObj.toString());
            
        } catch (SQLException e) {
            responseObj.put("status", "error");
            responseObj.put("message", "查询影片失败: " + e.getMessage());
            out.write(responseObj.toString());
            e.printStackTrace();
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);;
        }
    }
    
    /**
     * 提交排片信息
     */
    private void submitSchedule(HttpServletRequest request, HttpServletResponse response, PrintWriter out, JSONObject responseObj) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            // 从请求头获取影院信息
            String cinemaProvinceCode = request.getHeader("X-Cinema-Province-Code");
            String cinemaCityCode = request.getHeader("X-Cinema-City-Code");
            String cinemaId = request.getHeader("X-Cinema-Id");
            
            // 设置请求字符编码为UTF-8
            request.setCharacterEncoding("UTF-8");
            
            // 解析请求体
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            JSONObject requestBody = new JSONObject(sb.toString());
            
            // 获取表单参数
            String selectedAuditorium = requestBody.getString("selectedAuditorium");
            String selectedDate = requestBody.getString("selectedDate");
            String selectedFilm = requestBody.getString("selectedFilm");
            String selectedLanguage = requestBody.getString("selectedLanguage");
            String visualEffect = requestBody.getString("visualEffect");
            String fareStr = requestBody.getString("fare");
            String startTime = requestBody.getString("startTime");
            
            // 解析参数
            String auditoriumId = selectedAuditorium;
            
            // 解析selectedFilm为发行商ID和影片ID
            String[] filmParts = selectedFilm.split("_");
            String publisherId = filmParts[0];
            String filmId = filmParts[1];
            
            // 解析票价
            java.math.BigDecimal fare = new java.math.BigDecimal(fareStr);
            
            // 构建完整的放映时间
            String showTimeStr = selectedDate + " " + startTime;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            java.util.Date showTimeUtil = sdf.parse(showTimeStr);
            Timestamp showTime = new Timestamp(showTimeUtil.getTime());
            
            // 开始事务
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);
            
            // 查询当前影厅的最大排片ID
            String maxIdSql = "SELECT MAX(Schedule_ID) FROM table_Schedule WHERE (Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, Schedule_Cinema_ID, Schedule_Auditorium_ID) = (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(maxIdSql);
            pstmt.setString(1, cinemaProvinceCode);
            pstmt.setString(2, cinemaCityCode);
            pstmt.setString(3, cinemaId);
            pstmt.setString(4, auditoriumId);
            
            rs = pstmt.executeQuery();
            int nextScheduleId = 1;
            if (rs.next()) {
                Integer maxId = rs.getInt(1);
                if (!rs.wasNull()) {
                    nextScheduleId = maxId + 1;
                }
            }
            
            // 插入排片数据
            String insertScheduleSql = "INSERT INTO table_Schedule (Schedule_Film_Publisher_ID, Schedule_Film_ID, Schedule_Film_Language, Schedule_Visual_Effect, Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, Schedule_Cinema_ID, Schedule_Auditorium_ID, Schedule_ID, Schedule_Fare, Schedule_Show_Time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            pstmt = conn.prepareStatement(insertScheduleSql);
            pstmt.setString(1, publisherId);
            pstmt.setString(2, filmId);
            pstmt.setString(3, selectedLanguage);
            pstmt.setString(4, visualEffect);
            pstmt.setString(5, cinemaProvinceCode);
            pstmt.setString(6, cinemaCityCode);
            pstmt.setString(7, cinemaId);
            pstmt.setString(8, auditoriumId);
            pstmt.setInt(9, nextScheduleId);
            pstmt.setBigDecimal(10, fare);
            pstmt.setTimestamp(11, showTime);
            
            // 提交事务
            conn.commit();
            
            // 构建响应
            responseObj.put("status", "success");
            responseObj.put("message", "排片成功！");
            out.write(responseObj.toString());
            
        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            
            responseObj.put("status", "error");
            responseObj.put("message", "排片失败: " + e.getMessage());
            out.write(responseObj.toString());
            e.printStackTrace();
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
    }
}