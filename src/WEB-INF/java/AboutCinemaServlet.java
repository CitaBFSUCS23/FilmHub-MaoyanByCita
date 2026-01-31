import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONArray;
import jakarta.servlet.http.Part;
import java.io.InputStream;
import java.nio.file.Files;
import java.io.File;
import java.io.IOException;
import java.sql.*;

@WebServlet("/AboutCinema")
@MultipartConfig(
    maxFileSize = 1024 * 1024 * 10,      // 10MB 最大文件大小
    maxRequestSize = 1024 * 1024 * 50,   // 50MB 最大请求大小
    fileSizeThreshold = 1024 * 1024      // 1MB 文件大小阈值，超过此大小将写入磁盘
)
public class AboutCinemaServlet extends HttpServlet {
    
    // 添加管理人员
    protected void addManager(HttpServletRequest request, HttpServletResponse response, Connection conn, String cinemaProvinceCode, String cinemaCityCode, String cinemaId) throws Exception {
        String email = request.getParameter("email");
        
        email = email.trim();
        
        // 检查目标用户是否存在
        String checkUserSql = "SELECT COUNT(*) FROM table_Account WHERE Account_Email = ?";
        PreparedStatement pstmt = conn.prepareStatement(checkUserSql);
        pstmt.setString(1, email);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next() && rs.getInt(1) == 0) {
            DatabaseUtil.closeResources(rs, pstmt, null);
            throw new Exception("目标用户不存在");
        }
        DatabaseUtil.closeResources(rs, pstmt, null);
        
        // 添加管理员
        String insertSql = "INSERT INTO table_Manage (Manage_Cinema_Province_Code, Manage_Cinema_City_Code, Manage_Cinema_ID, Manage_Account_Email) VALUES (?, ?, ?, ?)";
        pstmt = conn.prepareStatement(insertSql);
        pstmt.setInt(1, Integer.parseInt(cinemaProvinceCode));
        pstmt.setInt(2, Integer.parseInt(cinemaCityCode));
        pstmt.setInt(3, Integer.parseInt(cinemaId));
        pstmt.setString(4, email);
        pstmt.executeUpdate();
        DatabaseUtil.closeResources(null, pstmt, null);
    }
    
    // 删除管理人员
    protected void deleteManager(HttpServletRequest request, HttpServletResponse response, Connection conn, String cinemaProvinceCode, String cinemaCityCode, String cinemaId) throws Exception {
        String email = request.getParameter("email");  
        email = email.trim();
        
        // 检查是否尝试删除自己
        // 从请求头获取当前用户邮箱
        String currentUserEmail = request.getHeader("X-User-Email");
        
        if (email.equals(currentUserEmail)) {
            throw new Exception("不能删除自己");
        }
        
        // 删除管理员
        String sql = "DELETE FROM table_Manage WHERE (Manage_Cinema_Province_Code, Manage_Cinema_City_Code, Manage_Cinema_ID, Manage_Account_Email) = (?, ?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, Integer.parseInt(cinemaProvinceCode));
        pstmt.setInt(2, Integer.parseInt(cinemaCityCode));
        pstmt.setInt(3, Integer.parseInt(cinemaId));
        pstmt.setString(4, email);
        pstmt.executeUpdate();
        DatabaseUtil.closeResources(null, pstmt, null);
    }
    
    // 获取管理人员列表
    protected JSONArray getManagerList(Connection conn, String cinemaProvinceCode, String cinemaCityCode, String cinemaId) throws SQLException {
        JSONArray managerList = new JSONArray();
        String sql = "SELECT Manage_Account_Email FROM view_About_Cinema WHERE (Manage_Cinema_Province_Code, Manage_Cinema_City_Code, Manage_Cinema_ID) = (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, Integer.parseInt(cinemaProvinceCode));
        pstmt.setInt(2, Integer.parseInt(cinemaCityCode));
        pstmt.setInt(3, Integer.parseInt(cinemaId));
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            JSONObject manager = new JSONObject();
            manager.put("email", rs.getString("Manage_Account_Email"));
            managerList.put(manager);
        }
        
        DatabaseUtil.closeResources(rs, pstmt, null);
        return managerList;
    }
    

    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        
        try {
            // 从请求参数获取影院信息
            String cinemaProvinceCode = request.getParameter("cinemaProvinceCode");
            String cinemaCityCode = request.getParameter("cinemaCityCode");
            String cinemaId = request.getParameter("cinemaId");
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            
            try {
                conn = DatabaseUtil.getConnection();
                
                // 获取影院基本信息
                String sql = "SELECT Cinema_Name, Cinema_Detailed_Address FROM view_About_Cinema WHERE (Manage_Cinema_Province_Code, Manage_Cinema_City_Code, Manage_Cinema_ID) = (?, ?, ?)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, Integer.parseInt(cinemaProvinceCode));
                pstmt.setInt(2, Integer.parseInt(cinemaCityCode));
                pstmt.setInt(3, Integer.parseInt(cinemaId));
                rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    JSONObject cinemaInfo = new JSONObject();
                    cinemaInfo.put("cinemaProvinceCode", cinemaProvinceCode);
                    cinemaInfo.put("cinemaCityCode", cinemaCityCode);
                    cinemaInfo.put("cinemaId", cinemaId);
                    cinemaInfo.put("cinemaName", rs.getString("Cinema_Name"));
                    cinemaInfo.put("cinemaAddress", rs.getString("Cinema_Detailed_Address"));
                    
                    // 构建图片路径
                    String imageWebPath = request.getContextPath() + "/uploads/cinemas/" + cinemaProvinceCode + "/" + cinemaCityCode + "/" + cinemaId + "/album.jpg";
                    cinemaInfo.put("cinemaImagePath", imageWebPath);
                    
                    // 获取管理人员列表
                    JSONArray managerList = getManagerList(conn, cinemaProvinceCode, cinemaCityCode, cinemaId);
                    cinemaInfo.put("managers", managerList);
                    
                    result.put("status", "success");
                    result.put("cinemaInfo", cinemaInfo);
                } else {
                    result.put("status", "error");
                    result.put("message", "影院信息不存在");
                }
                
            } catch (SQLException e) {
                System.err.println("AboutCinema数据库错误: " + e.getMessage());
                result.put("status", "error");
                result.put("message", "数据库查询失败: " + e.getMessage());
            } finally {
                DatabaseUtil.closeResources(rs, pstmt, conn);
            }
            
        } catch (Exception e) {
            System.err.println("AboutCinema处理错误: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "获取影院信息失败: " + e.getMessage());
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
            // 从请求参数或请求头获取影院信息
            String cinemaProvinceCode = request.getParameter("cinemaProvinceCode") 
                                      != null ? request.getParameter("cinemaProvinceCode") 
                                      : request.getHeader("X-Cinema-Province-Code");
            
            String cinemaCityCode = request.getParameter("cinemaCityCode") 
                                    != null ? request.getParameter("cinemaCityCode") 
                                    : request.getHeader("X-Cinema-City-Code");
            
            String cinemaId = request.getParameter("cinemaId") 
                              != null ? request.getParameter("cinemaId") 
                              : request.getHeader("X-Cinema-Id");
            
            // 检查是否为文件上传请求
            Part filePart = request.getPart("cinemaImage");
            boolean hasFileUpload = filePart != null && filePart.getSize() > 0;
            
            if (hasFileUpload) {
                // 文件上传请求，将已获取的参数封装成数组传递给上传方法
                // 上传方法会尝试从表单字段中提取参数，如果提取失败则使用传入的参数
                String[] cinemaParams = {cinemaProvinceCode, cinemaCityCode, cinemaId};
                
                // 处理图片上传和基本信息更新
                try {
                    boolean imageUploaded = false;
                    String cinemaName = request.getParameter("cinemaName");
                    String cinemaAddress = request.getParameter("cinemaAddress");
                    
                    // 从表单字段中提取影院信息（如果提供了）
                    String formProvinceCode = request.getParameter("cinemaProvinceCode");
                    String formCityCode = request.getParameter("cinemaCityCode");
                    String formCinemaId = request.getParameter("cinemaId");
                    
                    if (formProvinceCode != null) cinemaParams[0] = formProvinceCode;
                    if (formCityCode != null) cinemaParams[1] = formCityCode;
                    if (formCinemaId != null) cinemaParams[2] = formCinemaId;
                    
                    // 处理文件上传
                    if (filePart != null && filePart.getSize() > 0) {
                        // 构建上传路径
                        String uploadDir = new File("uploads/cinemas/" + cinemaParams[0] + "/" + cinemaParams[1] + "/" + cinemaParams[2] + "/").getAbsolutePath();
                        File dir = new File(uploadDir);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        
                        // 保存图片
                        File uploadedFile = new File(uploadDir + File.separator + "album.jpg");
                        try (InputStream input = filePart.getInputStream()) {
                            Files.copy(input, uploadedFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                        
                        // 添加延迟确保文件写入完成
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            // 忽略中断异常
                        }
                        
                        imageUploaded = true;
                    }
                    
                    // 第三阶段：更新影院基本信息
                    boolean infoUpdated = false;
                    Connection conn = DatabaseUtil.getConnection();
                    try {
                        // 更新影院名称
                        if (cinemaName != null && !cinemaName.trim().isEmpty()) {
                            String sql = "UPDATE table_Cinema SET Cinema_Name = ? WHERE (Cinema_Province_Code, Cinema_City_Code, Cinema_ID) = (?, ?, ?)";
                            PreparedStatement pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, cinemaName.trim());
                            pstmt.setInt(2, Integer.parseInt(cinemaParams[0]));
                            pstmt.setInt(3, Integer.parseInt(cinemaParams[1]));
                            pstmt.setInt(4, Integer.parseInt(cinemaParams[2]));
                            pstmt.executeUpdate();
                            DatabaseUtil.closeResources(null, pstmt, null);
                            infoUpdated = true;
                        }
                        
                        // 更新影院地址
                        if (cinemaAddress != null && !cinemaAddress.trim().isEmpty()) {
                            String sql = "UPDATE table_Cinema SET Cinema_Detailed_Address = ? WHERE (Cinema_Province_Code, Cinema_City_Code, Cinema_ID) = (?, ?, ?)";
                            PreparedStatement pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, cinemaAddress.trim());
                            pstmt.setInt(2, Integer.parseInt(cinemaParams[0]));
                            pstmt.setInt(3, Integer.parseInt(cinemaParams[1]));
                            pstmt.setInt(4, Integer.parseInt(cinemaParams[2]));
                            pstmt.executeUpdate();
                            DatabaseUtil.closeResources(null, pstmt, null);
                            infoUpdated = true;
                        }
                    } finally {
                        DatabaseUtil.closeResources(null, null, conn);
                    }
                    
                    if (imageUploaded && infoUpdated) {
                        result.put("status", "success");
                        result.put("message", "影院信息和图片更新成功");
                    } else if (imageUploaded) {
                        result.put("status", "success");
                        result.put("message", "图片上传成功");
                    } else if (infoUpdated) {
                        result.put("status", "success");
                        result.put("message", "影院信息更新成功");
                    } else {
                        result.put("status", "error");
                        result.put("message", "未找到有效的更新内容");
                    }
                } catch (Exception e) {
                    System.err.println("AboutCinema处理错误: " + e.getMessage());
                    result.put("status", "error");
                    result.put("message", "操作失败: " + e.getMessage());
                }
            } else {
                // 处理普通表单请求
                String action = request.getParameter("action");
                Connection conn = DatabaseUtil.getConnection();
                
                try {
                    if ("addManager".equals(action)) {
                        // 添加管理人员
                        addManager(request, response, conn, cinemaProvinceCode, cinemaCityCode, cinemaId);
                        result.put("status", "success");
                        result.put("message", "管理人员添加成功");
                        
                    } else if ("deleteManager".equals(action)) {
                        // 删除管理人员
                        deleteManager(request, response, conn, cinemaProvinceCode, cinemaCityCode, cinemaId);
                        result.put("status", "success");
                        result.put("message", "管理人员删除成功");
                        
                    } else {
                        // 默认操作：更新影院信息
                        String cinemaName = request.getParameter("cinemaName");
                        String cinemaAddress = request.getParameter("cinemaAddress");
                        
                        // 更新影院名称
                        if (cinemaName != null && !cinemaName.trim().isEmpty()) {
                            String sql = "UPDATE table_Cinema SET Cinema_Name = ? WHERE (Cinema_Province_Code, Cinema_City_Code, Cinema_ID) = (?, ?, ?)";
                            PreparedStatement pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, cinemaName.trim());
                            pstmt.setInt(2, Integer.parseInt(cinemaProvinceCode));
                            pstmt.setInt(3, Integer.parseInt(cinemaCityCode));
                            pstmt.setInt(4, Integer.parseInt(cinemaId));
                            pstmt.executeUpdate();
                            DatabaseUtil.closeResources(null, pstmt, null);
                        }
                        
                        // 更新影院地址
                        if (cinemaAddress != null && !cinemaAddress.trim().isEmpty()) {
                            String sql = "UPDATE table_Cinema SET Cinema_Detailed_Address = ? WHERE (Cinema_Province_Code, Cinema_City_Code, Cinema_ID) = (?, ?, ?)";
                            PreparedStatement pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, cinemaAddress.trim());
                            pstmt.setInt(2, Integer.parseInt(cinemaProvinceCode));
                            pstmt.setInt(3, Integer.parseInt(cinemaCityCode));
                            pstmt.setInt(4, Integer.parseInt(cinemaId));
                            pstmt.executeUpdate();
                            DatabaseUtil.closeResources(null, pstmt, null);
                        }
                        
                        result.put("status", "success");
                        result.put("message", "影院信息更新成功");
                    }
                    
                } catch (Exception e) {
                    System.err.println("AboutCinema数据库错误: " + e.getMessage());
                    result.put("status", "error");
                    result.put("message", "操作失败: " + e.getMessage());
                } finally {
                    DatabaseUtil.closeResources(null, null, conn);
                }
            }
            
        } catch (Exception e) {
            System.err.println("AboutCinema处理错误: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "操作失败: " + e.getMessage());
        }
        
        response.getWriter().print(result.toString());
    }
}