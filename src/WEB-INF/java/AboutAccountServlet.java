import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.Part;
import java.io.InputStream;
import java.nio.file.Files;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.sql.*;

@WebServlet("/AboutAccount")
@MultipartConfig(
    maxFileSize = 1024 * 1024 * 10,      // 10MB 最大文件大小
    maxRequestSize = 1024 * 1024 * 50,   // 50MB 最大请求大小
    fileSizeThreshold = 1024 * 1024      // 1MB 文件大小阈值，超过此大小将写入磁盘
)
public class AboutAccountServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        
        try {
            // 从请求头获取用户邮箱（替代服务器端Session）
            String currentUserEmail = request.getHeader("X-User-Email");  
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            
            try {
                conn = DatabaseUtil.getConnection();
                
                String sql = "SELECT Account_Nickname, Account_Gender, Account_Tel, Account_Province_Code, Account_City_Code, Account_SelfIntro, Account_Wallet, Account_CreateTime FROM view_Account_Info WHERE Account_Email = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, currentUserEmail);
                rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    result.put("status", "success");
                    result.put("email", currentUserEmail);
                    result.put("nickname", rs.getString("Account_Nickname"));
                    result.put("gender", rs.getString("Account_Gender"));
                    result.put("tel", rs.getString("Account_Tel"));
                    result.put("provinceCode", rs.getObject("Account_Province_Code"));
                    result.put("cityCode", rs.getObject("Account_City_Code"));
                    result.put("selfIntro", rs.getString("Account_SelfIntro"));
                    result.put("wallet", rs.getDouble("Account_Wallet"));
                    result.put("createTime", rs.getTimestamp("Account_CreateTime").toString());
                } else {
                    result.put("status", "error");
                    result.put("message", "用户信息不存在");
                }
                
            } catch (SQLException e) {
                System.err.println("获取用户信息数据库错误: " + e.getMessage());
                result.put("status", "error");
                result.put("message", "数据库错误: " + e.getMessage());
            } finally {
                DatabaseUtil.closeResources(rs, pstmt, conn);
            }
            
        } catch (Exception e) {
            System.err.println("获取用户信息错误: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "服务器错误: " + e.getMessage());
        }
        
        response.getWriter().print(result.toString());
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        
        try {
            // 从请求头获取用户邮箱（替代服务器端Session）
            String currentUserEmail = request.getHeader("X-User-Email");
            
            // 检查是否是文件上传（头像上传）
            boolean isMultipart = request.getContentType() != null && request.getContentType().toLowerCase().startsWith("multipart/");
            if (isMultipart) {
                Part filePart = request.getPart("avatar");
                if (filePart != null && filePart.getSize() > 0) {
                    handleAvatarUpload(request, response, currentUserEmail);
                    return;
                }
            }
            
            // 处理其他POST请求
            String action = request.getParameter("action");
            
            if ("update".equals(action)) {
                handleInfoUpdate(request, response, currentUserEmail);
            } else if ("changePassword".equals(action)) {
                handlePasswordChange(request, response, currentUserEmail);
            } else {
                result.put("status", "error");
                result.put("message", "无效的操作");
                response.getWriter().print(result.toString());
            }
            
        } catch (Exception e) {
            System.err.println("处理请求错误: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "服务器错误: " + e.getMessage());
            response.getWriter().print(result.toString());
        }
    }
    
    private void handleAvatarUpload(HttpServletRequest request, HttpServletResponse response, String currentUserEmail) throws Exception {
        JSONObject result = new JSONObject();
        
        boolean avatarUploaded = false;
        
        // 使用Jakarta Servlet内置的Part接口处理文件上传
        Part filePart = request.getPart("avatar");
        if (filePart != null && filePart.getSize() > 0) {
            // 处理头像上传
            String uploadDir = new File("uploads/avatars").getAbsolutePath();
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String fileName = currentUserEmail + ".jpg";
            File uploadedFile = new File(uploadDir, fileName);
            
            // 使用Files.copy来保存文件
            try (InputStream input = filePart.getInputStream()) {
                Files.copy(input, uploadedFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            avatarUploaded = true;
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (avatarUploaded) {
            result.put("status", "success");
            result.put("message", "头像更新成功");
        } else {
            result.put("status", "error");
            result.put("message", "头像上传失败");
        }
        
        response.getWriter().print(result.toString());
    }
    
    private void handleInfoUpdate(HttpServletRequest request, HttpServletResponse response, String currentUserEmail) throws Exception {
        JSONObject result = new JSONObject();
        
        String newNickname = request.getParameter("nickname");
        String newTel = request.getParameter("tel");
        String newGender = request.getParameter("gender");
        String newProvinceCodeStr = request.getParameter("province");
        Integer newProvinceCode = (newProvinceCodeStr == null || newProvinceCodeStr.isEmpty()) ? null : Integer.parseInt(newProvinceCodeStr);
        String newCityCodeStr = request.getParameter("city");
        Integer newCityCode = (newCityCodeStr == null || newCityCodeStr.isEmpty()) ? null : Integer.parseInt(newCityCodeStr);
        String newSelfIntro = request.getParameter("self-intro");
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            String updateSql = "UPDATE table_Account SET Account_Nickname = ?, Account_Gender = ?, Account_Tel = ?, Account_Province_Code = ?, Account_City_Code = ?, Account_SelfIntro = ? WHERE Account_Email = ?";
            pstmt = conn.prepareStatement(updateSql);
            pstmt.setString(1, newNickname);
            pstmt.setString(2, newGender);
            pstmt.setString(3, newTel);
            pstmt.setInt(4, newProvinceCode);
            pstmt.setInt(5, newCityCode); 
            pstmt.setString(6, newSelfIntro);
            pstmt.setString(7, currentUserEmail);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                result.put("status", "success");
                result.put("message", "基本信息更新成功");
            } else {
                result.put("status", "error");
                result.put("message", "更新失败，请重试");
            }
            
        } catch (SQLException e) {
            System.err.println("更新用户信息数据库错误: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "数据库操作失败: " + e.getMessage());
        } finally {
            DatabaseUtil.closeResources(null, pstmt, conn);
        }
        
        response.getWriter().print(result.toString());
    }
    
    private void handlePasswordChange(HttpServletRequest request, HttpServletResponse response, String currentUserEmail) throws Exception {
        JSONObject result = new JSONObject();
        
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        
        // 验证密码一致性
        if (!newPassword.equals(confirmPassword)) {
            result.put("status", "error");
            result.put("message", "两次输入的密码不一致");
            response.getWriter().print(result.toString());
            return;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // 更新密码
            String updatePasswordSql = "UPDATE table_Account SET Account_Password = ? WHERE Account_Email = ?";
            pstmt = conn.prepareStatement(updatePasswordSql);
            pstmt.setString(1, newPassword);
            pstmt.setString(2, currentUserEmail);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                result.put("status", "success");
                result.put("message", "密码修改成功");
            } else {
                result.put("status", "error");
                result.put("message", "密码修改失败，请重试");
            }
            
        } catch (SQLException e) {
            System.err.println("修改密码数据库错误: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "数据库操作失败: " + e.getMessage());
        } finally {
            DatabaseUtil.closeResources(null, pstmt, conn);
        }
        
        response.getWriter().print(result.toString());
    }
}