import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;

@WebServlet("/Auditoriums")
public class AuditoriumsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置响应内容类型
        response.setContentType("application/json; charset=UTF-8");
        // 设置请求编码，防止中文乱码
        request.setCharacterEncoding("UTF-8");
        
        // 获取请求头中的影院信息
        String cinemaProvinceCode = request.getHeader("X-Cinema-Province-Code");
        String cinemaCityCode = request.getHeader("X-Cinema-City-Code");
        String cinemaId = request.getHeader("X-Cinema-ID");
        
        // 获取请求参数
        String action = request.getParameter("action");
        
        // 处理获取视觉效果的请求（不需要影院信息）
        if ("getVisualEffects".equals(action)) {
            getVisualEffects(request, response);
            return;
        }

        // 根据action参数处理不同请求
        if ("newId".equals(action)) {
            getNewAuditoriumId(request, response, cinemaProvinceCode, cinemaCityCode, cinemaId);
        } else {
            getAuditoriumList(request, response, cinemaProvinceCode, cinemaCityCode, cinemaId);
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置响应内容类型
        response.setContentType("application/json; charset=UTF-8");
        // 设置请求编码，防止中文乱码
        request.setCharacterEncoding("UTF-8");
        
        // 获取表单参数
        String cinemaProvinceCode = request.getParameter("cinemaProvinceCode");
        String cinemaCityCode = request.getParameter("cinemaCityCode");
        String cinemaId = request.getParameter("cinemaId");
        String auditoriumId = request.getParameter("auditoriumId");
        String auditoriumName = request.getParameter("auditoriumName");
        String rowsStr = request.getParameter("rows");
        String colsStr = request.getParameter("cols");
        String selectedVisualEffects = request.getParameter("selectedVisualEffects");
        String auditoriumLayout = request.getParameter("auditoriumLayout");

        // 解析行数和列数
        int rows = 0;
        int cols = 0;
        try {
            rows = Integer.parseInt(rowsStr);
            cols = Integer.parseInt(colsStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "error");
            errorResponse.put("message", "行数和列数必须为正整数");
            response.getWriter().write(errorResponse.toString());
            return;
        }
        
        // 解析JSON格式的视觉效果数组
        List<String> visualEffectsList = new ArrayList<>();
        try {
            if (selectedVisualEffects != null && !selectedVisualEffects.isEmpty() && !"[]".equals(selectedVisualEffects)) {
                // 使用org.json.JSONArray更专业地解析JSON数组
                JSONArray effectsArray = new JSONArray(selectedVisualEffects);
                for (int i = 0; i < effectsArray.length(); i++) {
                    String effect = effectsArray.getString(i);
                    if (effect != null && !effect.trim().isEmpty()) {
                        visualEffectsList.add(effect.trim());
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败时保持为空列表
            e.printStackTrace();
        }
        
        // 数据库连接
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // 开始事务
            conn.setAutoCommit(false);
            
            // 插入新影厅到table_Auditorium表
            String insertSql = "INSERT INTO table_Auditorium (Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code,Auditorium_Cinema_ID, Auditorium_ID, Auditorium_Name, Auditorium_Row_Count, Auditorium_Col_Count) VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            pstmt = conn.prepareStatement(insertSql);
            pstmt.setString(1, cinemaProvinceCode);
            pstmt.setString(2, cinemaCityCode);
            pstmt.setString(3, cinemaId);
            pstmt.setString(4, auditoriumId);
            pstmt.setString(5, auditoriumName);
            pstmt.setInt(6, rows);
            pstmt.setInt(7, cols);
            pstmt.executeUpdate();
            
            // 插入支持的视觉效果到table_Auditorium_Visual_Effects_Association表
            String visualEffectSql = "INSERT INTO table_Auditorium_Visual_Effects_Association (Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID, Auditorium_ID, Auditorium_Visual_Effect) VALUES (?, ?, ?, ?, ?)";
            
            pstmt = conn.prepareStatement(visualEffectSql);
            for (String effect : visualEffectsList) {
                pstmt.setString(1, cinemaProvinceCode);
                pstmt.setString(2, cinemaCityCode);
                pstmt.setString(3, cinemaId);
                pstmt.setString(4, auditoriumId);
                pstmt.setString(5, effect);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            
            // 创建布局文件目录 本地文件系统路径（用于文件上传）
            String uploadDir = new File("uploads/cinemas/" + cinemaProvinceCode + "/" + cinemaCityCode + "/" + cinemaId + "/").getAbsolutePath();
            File layoutDir = new File(uploadDir);
            if (!layoutDir.exists()) {
                layoutDir.mkdirs();
            }
            
            // 保存布局文件 - 仅存储被禁用的座位坐标，使用3位影厅号（补零）
            String formattedAuditoriumId = String.format("%03d", Integer.parseInt(auditoriumId));
            String layoutFilePath = uploadDir + File.separator + formattedAuditoriumId + ".json";
            FileWriter writer = new FileWriter(layoutFilePath);
            writer.write(auditoriumLayout);
            writer.close();
            
            // 提交事务
            conn.commit();
            
            // 构建成功响应
            JSONObject responseObj = new JSONObject();
            responseObj.put("status", "success");
            responseObj.put("message", "影厅添加成功");
            response.getWriter().write(responseObj.toString());
            
        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            
            e.printStackTrace();
            
            // 构建错误响应
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "error");
            errorResponse.put("message", "添加失败: " + e.getMessage());
            response.getWriter().write(errorResponse.toString());
            
        } finally {
            // 关闭资源
            DatabaseUtil.closeResources(null, pstmt, conn);
        }
    }
    
    // 获取影厅列表
    private void getAuditoriumList(HttpServletRequest request, HttpServletResponse response, String cinemaProvinceCode, String cinemaCityCode, String cinemaId) throws ServletException, IOException {
        // 数据库连接
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> auditoriumList = new ArrayList<>();
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // 查询当前影院的所有影厅及其支持的视觉效果和排片信息
            String sql = "SELECT Auditorium_ID, Auditorium_Name, Auditorium_Row_Count, Auditorium_Col_Count, Auditorium_Capacity, Visual_Effect_Names, Next_Film_Name, Next_Show_Start_Time, Next_Show_End_Time, Next_Film_Language, Schedule_Fare FROM view_Auditorium_Schedule WHERE (Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID) = (?, ?, ?) ORDER BY Auditorium_ID";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, cinemaProvinceCode);
            pstmt.setString(2, cinemaCityCode);
            pstmt.setString(3, cinemaId);
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> auditorium = new HashMap<>();
                auditorium.put("auditoriumId", rs.getString("Auditorium_ID"));
                auditorium.put("auditoriumName", rs.getString("Auditorium_Name"));
                auditorium.put("rowCount", rs.getInt("Auditorium_Row_Count"));
                auditorium.put("colCount", rs.getInt("Auditorium_Col_Count"));
                auditorium.put("capacity", rs.getInt("Auditorium_Capacity"));
                auditorium.put("visualEffects", rs.getString("Visual_Effect_Names"));
                auditorium.put("nextFilmName", rs.getString("Next_Film_Name"));
                auditorium.put("nextShowStartTime", rs.getTimestamp("Next_Show_Start_Time"));
                auditorium.put("nextShowEndTime", rs.getTimestamp("Next_Show_End_Time"));
                auditorium.put("nextFilmLanguage", rs.getString("Next_Film_Language"));
                auditorium.put("scheduleFare", rs.getDouble("Schedule_Fare"));
                auditoriumList.add(auditorium);
            }
            
            // 构建JSON响应
            JSONObject responseObj = new JSONObject();
            responseObj.put("status", "success");
            responseObj.put("auditoriumList", new JSONArray(auditoriumList));
            
            // 发送响应
            response.getWriter().write(responseObj.toString());
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "error");
            errorResponse.put("message", "数据库查询错误");
            response.getWriter().write(errorResponse.toString());
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
    }
    
    // 获取新影厅ID
    private void getNewAuditoriumId(HttpServletRequest request, HttpServletResponse response, String cinemaProvinceCode, String cinemaCityCode, String cinemaId) throws ServletException, IOException {
        // 数据库连接
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int newAuditoriumId = 0;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // 获取新影厅的ID
            String maxIdSql = "SELECT MAX(Auditorium_ID) FROM table_Auditorium WHERE (Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID) = (?, ?, ?) ";
    
            pstmt = conn.prepareStatement(maxIdSql);
            pstmt.setString(1, cinemaProvinceCode);
            pstmt.setString(2, cinemaCityCode);
            pstmt.setString(3, cinemaId);
            
            rs = pstmt.executeQuery();
            if (rs.next()) {
                Integer maxId = rs.getInt(1);
                newAuditoriumId = rs.wasNull() ? 1 : maxId + 1;
            }
            
            // 计算完整的影厅号 (2位省号+2位市号+4位影院号+3位影厅数字)
            String fullAuditoriumId = String.format("%02d%02d%04d%03d", 
                Integer.parseInt(cinemaProvinceCode), 
                Integer.parseInt(cinemaCityCode), 
                Integer.parseInt(cinemaId), 
                newAuditoriumId);
            
            // 构建JSON响应
            JSONObject responseObj = new JSONObject();
            responseObj.put("status", "success");
            responseObj.put("auditoriumId", newAuditoriumId);
            responseObj.put("fullAuditoriumId", fullAuditoriumId);
            
            // 发送响应
            response.getWriter().write(responseObj.toString());
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "error");
            errorResponse.put("message", "数据库查询错误");
            response.getWriter().write(errorResponse.toString());
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
    }
    
    // 获取所有视觉效果
    private void getVisualEffects(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 数据库连接
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<String> visualEffects = new ArrayList<>();
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // 查询所有视觉效果
            String sql = "SELECT Visual_Effect_Name FROM table_Visual_Effect ORDER BY Visual_Effect_Name";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                visualEffects.add(rs.getString("Visual_Effect_Name"));
            }
            
            // 构建JSON响应
            JSONObject responseObj = new JSONObject();
            responseObj.put("status", "success");
            responseObj.put("visualEffects", new JSONArray(visualEffects));
            
            // 发送响应
            response.getWriter().write(responseObj.toString());
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "error");
            errorResponse.put("message", "数据库查询错误");
            response.getWriter().write(errorResponse.toString());
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
    }
}