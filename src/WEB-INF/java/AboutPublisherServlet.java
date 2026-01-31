import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;

@WebServlet("/AboutPublisher")
public class AboutPublisherServlet extends HttpServlet {
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 检查请求参数
        String action = request.getParameter("action");
        if ("info".equals(action)) {
            // 获取发行商信息
            getPublisherInfo(request, response);
            return;
        }
        
        // 重定向到静态HTML页面
        response.sendRedirect("../Publisher/AboutPublisher/AboutPublisher.html");
    }
    
    private void getPublisherInfo(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 从请求头获取发行商ID
        String publisherId = request.getHeader("X-Publisher-Id");
        
        // 获取发行商信息
        try {
            if (!DatabaseUtil.loadJDBCDriver()) {
                response.setContentType("application/json;charset=UTF-8");
                JSONObject error = new JSONObject();
                error.put("error", "数据库驱动加载失败");
                response.getWriter().write(error.toString());
                return;
            }
            
            // 数据库连接
            Connection conn = DatabaseUtil.getConnection();
            
            // 获取发行商信息
            String sql = "SELECT * FROM view_About_Publisher WHERE Publisher_ID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, publisherId);
            ResultSet rs = pstmt.executeQuery();
            
            String publisherName = "";
            String publisherNationality = "";
            
            if (rs.next()) {
                publisherName = rs.getString("Publisher_Name");
                publisherNationality = rs.getString("Publisher_Nationality");
            }
            DatabaseUtil.closeResources(rs, pstmt, null);
            
            // 获取所有国籍
            List<String> nationalities = new ArrayList<>();
            sql = "SELECT Nationality_Name FROM table_Nationality ORDER BY Nationality_Name";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                nationalities.add(rs.getString("Nationality_Name"));
            }
            DatabaseUtil.closeResources(rs, pstmt, conn);
            
            // 生成JSON
            response.setContentType("application/json;charset=UTF-8");
            JSONObject result = new JSONObject();
            result.put("publisherId", publisherId);
            result.put("publisherName", publisherName);
            result.put("publisherNationality", publisherNationality);
            
            JSONArray nationalitiesArray = new JSONArray();
            for (String nationality : nationalities) {
                nationalitiesArray.put(nationality);
            }
            result.put("nationalities", nationalitiesArray);
            
            response.getWriter().write(result.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            response.setContentType("application/json;charset=UTF-8");
            JSONObject error = new JSONObject();
            error.put("error", "数据库连接失败: " + e.getMessage());
            response.getWriter().write(error.toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json;charset=UTF-8");
            JSONObject error = new JSONObject();
            error.put("error", "加载失败：" + e.getMessage());
            response.getWriter().write(error.toString());
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置请求和响应的字符编码
        request.setCharacterEncoding("UTF-8");
        
        // 处理表单提交
        // 从请求头获取发行商ID
        String publisherId = request.getHeader("X-Publisher-Id");
        
        try {
            if (!DatabaseUtil.loadJDBCDriver()) {
                response.setContentType("application/json;charset=UTF-8");
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "数据库驱动加载失败");
                response.getWriter().write(error.toString());
                return;
            }
            
            // 数据库连接
            Connection conn = DatabaseUtil.getConnection();
            
            // 获取表单参数，包括原始值
            String publisherName = request.getParameter("publisherName");
            String publisherNationality = request.getParameter("publisherNationality");
            String newPassword = request.getParameter("newPassword");
            String originalPublisherName = request.getParameter("originalPublisherName");
            String originalPublisherNationality = request.getParameter("originalPublisherNationality");
            
            // 参数验证和处理
            if (publisherName == null || publisherName.trim().isEmpty()) {
                publisherName = originalPublisherName;
            }
            
            if (publisherNationality == null || publisherNationality.trim().isEmpty()) {
                publisherNationality = originalPublisherNationality;
            }
            
            PreparedStatement pstmt;
            
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                String sql = "UPDATE table_Publisher SET Publisher_Name = ?, Publisher_Nationality = ?, Publisher_Password = ? WHERE Publisher_ID = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, publisherName);
                pstmt.setString(2, publisherNationality);
                pstmt.setString(3, newPassword);
                pstmt.setString(4, publisherId);
            } else {
                String sql = "UPDATE table_Publisher SET Publisher_Name = ?, Publisher_Nationality = ? WHERE Publisher_ID = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, publisherName);
                pstmt.setString(2, publisherNationality);
                pstmt.setString(3, publisherId);
            }
            pstmt.executeUpdate();
            DatabaseUtil.closeResources(null, pstmt, conn);
            
            // 返回成功响应
            response.setContentType("application/json;charset=UTF-8");
            JSONObject success = new JSONObject();
            success.put("status", "success");
            success.put("message", "信息更新成功");
            response.getWriter().write(success.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            response.setContentType("application/json;charset=UTF-8");
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", "数据库错误: " + e.getMessage());
            response.getWriter().write(error.toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json;charset=UTF-8");
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", "保存失败: " + e.getMessage());
            response.getWriter().write(error.toString());
        }
    }
}