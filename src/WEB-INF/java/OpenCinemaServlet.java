import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.*;

@WebServlet("/OpenCinema")
public class OpenCinemaServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        
        try {
            // 从请求头获取用户邮箱
            String userEmail = request.getHeader("X-User-Email");
            
            String cinemaName = request.getParameter("cinemaName");
            String provinceCode = request.getParameter("province");
            String cityCode = request.getParameter("city");
            String address = request.getParameter("address");
            
            Connection conn = null;
            PreparedStatement maxStmt = null;
            PreparedStatement insertStmt = null;
            PreparedStatement manageStmt = null;
            ResultSet maxRs = null;
            
            try {
                conn = DatabaseUtil.getConnection();
                
                // 获取该省市已有的最大影院号
                String maxCinemaIdSql = "SELECT MAX(Cinema_ID) FROM table_Cinema WHERE (Cinema_Province_Code, Cinema_City_Code) = (?, ?)";
                maxStmt = conn.prepareStatement(maxCinemaIdSql);
                maxStmt.setInt(1, Integer.parseInt(provinceCode));
                maxStmt.setInt(2, Integer.parseInt(cityCode));
                maxRs = maxStmt.executeQuery();
                
                int newCinemaNumber = 1;
                if (maxRs.next()) {
                    Integer maxCinemaId = maxRs.getInt(1);
                    if (maxCinemaId != null && maxCinemaId > 0) {
                        newCinemaNumber = maxCinemaId + 1;
                    }
                }
                
                // 生成4位影院号
                String cinemaNumber = String.format("%04d", newCinemaNumber);
                String cinemaId = String.format("%02d", Integer.parseInt(provinceCode)) + 
                                 String.format("%02d", Integer.parseInt(cityCode)) + cinemaNumber;
                
                // 插入影院信息
                String insertCinemaSql = "INSERT INTO table_Cinema (Cinema_Province_Code, Cinema_City_Code, Cinema_ID, Cinema_Name, Cinema_Detailed_Address) VALUES (?, ?, ?, ?, ?)";
                insertStmt = conn.prepareStatement(insertCinemaSql);
                insertStmt.setInt(1, Integer.parseInt(provinceCode));
                insertStmt.setInt(2, Integer.parseInt(cityCode));
                insertStmt.setInt(3, newCinemaNumber);
                insertStmt.setString(4, cinemaName.trim());
                insertStmt.setString(5, address.trim());
                insertStmt.executeUpdate();
                
                // 插入管理关系
                String insertManageSql = "INSERT INTO table_Manage (Manage_Account_Email, Manage_Cinema_Province_Code, Manage_Cinema_City_Code, Manage_Cinema_ID) VALUES (?, ?, ?, ?)";
                manageStmt = conn.prepareStatement(insertManageSql);
                manageStmt.setString(1, userEmail);
                manageStmt.setInt(2, Integer.parseInt(provinceCode));
                manageStmt.setInt(3, Integer.parseInt(cityCode));
                manageStmt.setInt(4, newCinemaNumber);
                manageStmt.executeUpdate();
                
                result.put("status", "success");
                result.put("message", "影院登记成功");
                result.put("cinemaId", cinemaId);
                
            } catch (SQLException e) {
                System.err.println("OpenCinema数据库错误: " + e.getMessage());
                result.put("status", "error");
                result.put("message", "数据库操作失败: " + e.getMessage());
            } finally {
                DatabaseUtil.closeResources(maxRs, maxStmt, conn);
                DatabaseUtil.closeResources(null, insertStmt, null);
                DatabaseUtil.closeResources(null, manageStmt, null);
            }
            
        } catch (Exception e) {
            System.err.println("OpenCinema处理错误: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "影院登记失败: " + e.getMessage());
        }
        
        response.getWriter().print(result.toString());
    }
}