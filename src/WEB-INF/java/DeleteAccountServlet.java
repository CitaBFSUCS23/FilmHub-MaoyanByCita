import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.json.JSONObject;

@WebServlet("/DeleteAccountServlet")
public class DeleteAccountServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        
        try {
            // 从请求头获取用户邮箱
            String currentUserEmail = request.getHeader("X-User-Email");
            
            // 获取数据库连接
            Connection conn = DatabaseUtil.getConnection();
            
            // 先删除用户上传的头像文件
            deleteAvatarFile(currentUserEmail);
            
            // 删除账户（级联删除由数据库约束处理）
            boolean deleteSuccess = deleteUserAccount(conn, currentUserEmail);
            
            if (deleteSuccess) {
                result.put("status", "success");
                result.put("message", "账号已成功注销");
            } else {
                result.put("status", "error");
                result.put("message", "删除账户失败，请稍后重试");
            }
            
            // 关闭数据库连接
            DatabaseUtil.closeResources(null, null, conn);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "error");
            result.put("message", "服务器内部错误：" + e.getMessage());
        }
        
        response.getWriter().print(result.toString());
    }
    
    /**
     * 删除用户头像文件
     */
    private void deleteAvatarFile(String userEmail) {
        try {
            // 获取头像文件路径
            String avatarFileName = userEmail + ".jpg";
            String uploadsDir = new File("uploads/avatars/").getAbsolutePath();
            File avatarFile = new File(uploadsDir, avatarFileName);
            
            // 如果头像文件存在，则删除
            if (avatarFile.exists()) {
                if (avatarFile.delete()) {
                    System.out.println("成功删除头像文件：" + avatarFile.getAbsolutePath());
                } else {
                    System.out.println("删除头像文件失败：" + avatarFile.getAbsolutePath());
                }
            } else {
                System.out.println("头像文件不存在：" + avatarFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("删除头像文件时发生错误：" + e.getMessage());
            // 头像文件删除失败不影响主流程，继续执行账户删除
        }
    }
    
    /**
     * 删除用户账户
     */
    private boolean deleteUserAccount(Connection conn, String userEmail) {
        PreparedStatement pstmt = null;
        
        try {
            // 删除账户（数据库级联删除会处理相关记录）
            String sql = "DELETE FROM table_Account WHERE Account_Email = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userEmail);
            
            int rowsAffected = pstmt.executeUpdate();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            // 清理资源
            DatabaseUtil.closeResources(null, pstmt, null);
        }
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // GET请求返回错误信息
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        result.put("status", "error");
        result.put("message", "请使用POST方法请求");
        
        response.getWriter().print(result.toString());
    }
}