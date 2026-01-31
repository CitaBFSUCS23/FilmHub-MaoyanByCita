import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.*;

@WebServlet("/Register")
public class RegisterServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");
        
        String step = request.getParameter("step");
        
        if ("1".equals(step)) {
            // 步骤1：邮箱验证
            handleEmailVerification(request, response);
        } else if ("2".equals(step)) {
            // 步骤2：注册提交
            handleRegistration(request, response);
        } else {
            response.setContentType("text/plain");
            response.getWriter().print("invalid-step");
        }
    }
    
    private void handleEmailVerification(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("email");
        
        if (email == null || email.trim().isEmpty()) {
            response.getWriter().print("invalid-email");
            return;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // 查询用户是否已存在
            String sql = "SELECT * FROM view_LnR_Verify WHERE Account_Email = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                response.getWriter().print("user-exists");
            } else {
                response.getWriter().print("email-valid");
            }
            
        } catch (SQLException e) {
            System.err.println("邮箱验证数据库错误: " + e.getMessage());
            response.getWriter().print("database-error");
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
    }
    
    private void handleRegistration(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject result = new JSONObject();
        
        try {
            String email = request.getParameter("email");
            String nickname = request.getParameter("nickname");
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirm-password");
            String genderStr = request.getParameter("gender");
            String gender = (genderStr == null || genderStr.isEmpty()) ? "U" : genderStr;
            String tel = request.getParameter("tel");
            String provinceCodeStr = request.getParameter("province");
            Integer provinceCode = (provinceCodeStr == null || provinceCodeStr.isEmpty()) ? null : Integer.parseInt(provinceCodeStr);
            String cityCodeStr = request.getParameter("city");
            Integer cityCode = (cityCodeStr == null || cityCodeStr.isEmpty()) ? null : Integer.parseInt(cityCodeStr);
            String selfIntro = request.getParameter("self-intro");
            
            // 验证密码一致性
            if (!password.equals(confirmPassword)) {
                result.put("status", "error");
                result.put("message", "两次输入的密码不一致");
                response.getWriter().print(result.toString());
                return;
            }
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            
            try {
                conn = DatabaseUtil.getConnection();
                
                // 插入用户数据
                String sql = "INSERT INTO table_Account (Account_Email, Account_Nickname, Account_Password, Account_Gender, Account_Tel, Account_Province_Code, Account_City_Code, Account_SelfIntro) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, email);
                pstmt.setString(2, nickname);
                pstmt.setString(3, password);
                pstmt.setString(4, gender);
                pstmt.setString(5, tel);     
                pstmt.setInt(6, provinceCode);
                pstmt.setInt(7, cityCode);
                pstmt.setString(8, selfIntro);
                
                if (pstmt.executeUpdate() > 0) {
                    result.put("status", "success");
                    result.put("message", "注册成功");
                } else {
                    result.put("status", "error");
                    result.put("message", "注册失败，请重试");
                }
                
            } catch (SQLException e) {
                System.err.println("注册数据库错误: " + e.getMessage());
                result.put("status", "error");
                result.put("message", "数据库操作失败: " + e.getMessage());
            } finally {
                DatabaseUtil.closeResources(null, pstmt, conn);
            }
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "服务器错误: " + e.getMessage());
        }
        
        response.getWriter().print(result.toString());
    }
}