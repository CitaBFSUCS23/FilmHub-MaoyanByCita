import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/Recharge")
public class RechargeServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");
        
        try (PrintWriter out = response.getWriter()) {
            JSONObject result = new JSONObject();
            
            // 从请求头获取用户邮箱
            String userEmail = request.getHeader("X-User-Email");
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            
            try {
                conn = DatabaseUtil.getConnection();
                
                // 获取当前余额
                String sql = "SELECT Account_Wallet FROM table_Account WHERE Account_Email = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, userEmail);
                rs = pstmt.executeQuery();
                
                double wallet = 0.00;
                if (rs.next()) {
                    wallet = rs.getDouble("Account_Wallet");
                }
                
                result.put("status", "success");
                result.put("balance", wallet);
                
            } catch (SQLException e) {
                result.put("status", "error");
                result.put("message", "数据库操作失败: " + e.getMessage());
            } finally {
                DatabaseUtil.closeResources(rs, pstmt, conn);
            }
            
            out.print(result.toString());
            
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", "服务器错误: " + e.getMessage());
            response.getWriter().print(error.toString());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");
        
        try (PrintWriter out = response.getWriter()) {
            JSONObject result = new JSONObject();
            
            // 从请求头获取用户邮箱
            String userEmail = request.getHeader("X-User-Email");
            if (userEmail == null || userEmail.isEmpty()) {
                result.put("status", "error");
                result.put("message", "未提供用户邮箱");
                out.print(result.toString());
                return;
            }
            
            // 获取充值金额
            String amountStr = request.getParameter("amount");
            if (amountStr == null || amountStr.isEmpty()) {
                result.put("status", "error");
                result.put("message", "请输入充值金额");
                out.print(result.toString());
                return;
            }
            
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                result.put("status", "error");
                result.put("message", "请输入有效的充值金额");
                out.print(result.toString());
                return;
            }
            
            // 验证金额范围
            if (amount < 0.01 || amount > 999.99) {
                result.put("status", "error");
                result.put("message", "充值金额必须在0.01-999.99元之间");
                out.print(result.toString());
                return;
            }
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            
            try {
                conn = DatabaseUtil.getConnection();
                conn.setAutoCommit(false);
                
                // 更新钱包余额
                String updateSql = "UPDATE table_Account SET Account_Wallet = Account_Wallet + ? WHERE Account_Email = ?";
                pstmt = conn.prepareStatement(updateSql);
                pstmt.setDouble(1, amount);
                pstmt.setString(2, userEmail);
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    conn.commit();
                    result.put("status", "success");
                    result.put("message", "充值成功");
                    result.put("amount", amount);
                } else {
                    conn.rollback();
                    result.put("status", "error");
                    result.put("message", "充值失败，未找到用户");
                }
                
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                result.put("status", "error");
                result.put("message", "数据库操作失败: " + e.getMessage());
            } finally {
                DatabaseUtil.closeResources(null, pstmt, conn);
            }
            
            out.print(result.toString());
            
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", "服务器错误: " + e.getMessage());
            response.getWriter().print(error.toString());
        }
    }
}