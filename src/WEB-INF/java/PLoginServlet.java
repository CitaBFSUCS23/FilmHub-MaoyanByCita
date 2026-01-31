import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.*;

@WebServlet("/PLogin")
public class PLoginServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        
        try {
            // 从请求头获取用户邮箱      
            String publisherId = request.getParameter("publisherId");
            String password = request.getParameter("password");
            
            // 验证发行商ID格式
            if (publisherId == null || publisherId.isEmpty() || !publisherId.matches("[0-9]{6}")) {
                result.put("status", "error");
                result.put("message", "请输入有效的发行商ID（6位数字）");
                response.getWriter().print(result.toString());
                return;
            }
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            
            try {
                conn = DatabaseUtil.getConnection();
                
                String sql = "SELECT * FROM view_Publisher_Login WHERE (Publisher_ID, Publisher_Password) = (?, ?)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, publisherId);
                pstmt.setString(2, password);
                
                rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    // 验证成功，将发行方ID返回给前端
                    // 前端将使用sessionStorage存储这些信息
                    result.put("status", "success");
                    result.put("message", "登录成功");
                    result.put("publisherId", publisherId);
                } else {
                    result.put("status", "error");
                    result.put("message", "发行商ID或密码错误");
                }
                
            } catch (SQLException e) {
                System.err.println("PLogin数据库错误: " + e.getMessage());
                result.put("status", "error");
                result.put("message", "数据库连接失败: " + e.getMessage());
            } finally {
                DatabaseUtil.closeResources(rs, pstmt, conn);
            }
            
        } catch (Exception e) {
            System.err.println("PLogin处理错误: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "验证失败: " + e.getMessage());
        }
        
        response.getWriter().print(result.toString());
    }
}