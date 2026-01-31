import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.*;

@WebServlet("/GLogin")
public class GLoginServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        
        try {
            String emailLocal = request.getParameter("email-local");
            String emailDomain = request.getParameter("email-domain");
            String password = request.getParameter("password");
            
            String email = emailLocal + "@" + emailDomain;
            
            // 验证用户登录
            boolean loginSuccess = verifyUserLogin(email, password);
            
            if (loginSuccess) {
                // 登录成功，不再设置服务器端Session
                // 前端将使用SessionStorage管理登录状态
                
                result.put("status", "success");
                result.put("message", "登录成功");
                result.put("redirectUrl", "../AboutAccount/AboutAccount.html");
            } else {
                result.put("status", "error");
                result.put("message", "用户名或密码错误");
            }
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "服务器错误: " + e.getMessage());
        }
        
        response.getWriter().print(result.toString());
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 处理登出请求
        String action = request.getParameter("action");
        if ("logout".equals(action)) {
            // 不再使用服务器端Session
            // 前端会通过清除sessionStorage来处理登出
            
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("message", "已登出");
            result.put("redirectUrl", "GLogin.html");
            
            response.setContentType("application/json");
            response.getWriter().print(result.toString());
        } else {
            // 默认显示登录页面
            response.sendRedirect("GLogin.html");
        }
    }
    
    private boolean verifyUserLogin(String email, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // 查询用户
            String sql = "SELECT * FROM view_LnR_Verify WHERE (Account_Email, Account_Password) = (?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();
            
            return rs.next();
            
        } catch (SQLException e) {
            System.err.println("数据库查询错误: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
    }
}