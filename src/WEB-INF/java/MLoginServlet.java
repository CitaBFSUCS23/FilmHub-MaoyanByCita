import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

@WebServlet("/MLogin")
public class MLoginServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        
        try {
            // 从请求头获取用户邮箱
            String userEmail = request.getHeader("X-User-Email");
            if (userEmail == null || userEmail.isEmpty()) {
                result.put("status", "error");
                result.put("message", "请先以用户身份登录");
                response.getWriter().print(result.toString());
                return;
            }
            
            String cinemaId = request.getParameter("cinemaId");
            
            // 验证影院ID格式
            if (cinemaId == null || cinemaId.isEmpty() || !cinemaId.matches("[0-9]{8}")) {
                result.put("status", "error");
                result.put("message", "请输入有效的影院ID（8位数字）");
                response.getWriter().print(result.toString());
                return;
            }
            
            // 解析影院ID
            String provinceCode = cinemaId.substring(0, 2);
            String cityCode = cinemaId.substring(2, 4);
            String cinemaNumber = cinemaId.substring(4);
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            
            try {
                conn = DatabaseUtil.getConnection();
                
                String sql = "SELECT * FROM view_Manager_Login WHERE (Manage_Account_Email, Manage_Cinema_Province_Code, Manage_Cinema_City_Code, Manage_Cinema_ID) = (?, ?, ?, ?)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, userEmail);
                pstmt.setInt(2, Integer.parseInt(provinceCode));
                pstmt.setInt(3, Integer.parseInt(cityCode));
                pstmt.setInt(4, Integer.parseInt(cinemaNumber));
                
                rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    // 验证成功，将影院信息返回给前端
                    // 前端将使用sessionStorage存储这些信息
                    result.put("status", "success");
                    result.put("message", "登录成功");
                    result.put("cinemaProvinceCode", provinceCode);
                    result.put("cinemaCityCode", cityCode);
                    result.put("cinemaId", cinemaNumber);
                    result.put("cinemaFullId", cinemaId);
                } else {
                    result.put("status", "error");
                    result.put("message", "您没有权限管理该影院");
                }
                
            } catch (SQLException e) {
                System.err.println("MLogin数据库错误: " + e.getMessage());
                result.put("status", "error");
                result.put("message", "数据库连接失败: " + e.getMessage());
            } finally {
                DatabaseUtil.closeResources(rs, pstmt, conn);
            }
            
        } catch (Exception e) {
            System.err.println("MLogin处理错误: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "验证失败: " + e.getMessage());
        }
        
        response.getWriter().print(result.toString());
    }
}