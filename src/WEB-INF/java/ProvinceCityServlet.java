import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.*;

@WebServlet("/ProvinceCity")
public class ProvinceCityServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        
        try {
            String provinceCode = request.getParameter("provinceCode");
            
            if (provinceCode == null || provinceCode.trim().isEmpty()) {
                // 获取所有省份
                JSONArray provinces = getProvinces();
                result.put("status", "success");
                result.put("provinces", provinces);
            } else {
                // 获取指定省份的城市
                JSONArray cities = getCities(provinceCode);
                result.put("status", "success");
                result.put("cities", cities);
            }
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "服务器错误: " + e.getMessage());
        }
        
        response.getWriter().print(result.toString());
    }
    
    private JSONArray getProvinces() throws SQLException {
        JSONArray provinces = new JSONArray();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            stmt = conn.createStatement();
            
            // 查询省份数据（假设表名为table_Province）
            String sql = "SELECT Province_Code, Province_Name FROM table_Province ORDER BY Province_Code";
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                JSONObject province = new JSONObject();
                province.put("code", rs.getString("Province_Code"));
                province.put("name", rs.getString("Province_Name"));
                provinces.put(province);
            }
            
        } finally {
            DatabaseUtil.closeResources(rs, stmt, conn);
        }
        
        return provinces;
    }
    
    private JSONArray getCities(String provinceCode) throws SQLException {
        JSONArray cities = new JSONArray();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // 查询城市数据（假设表名为table_City）
            String sql = "SELECT City_Code, City_Name FROM table_City WHERE City_Province_Code = ? ORDER BY City_Code";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, provinceCode);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                JSONObject city = new JSONObject();
                city.put("code", rs.getString("City_Code"));
                city.put("name", rs.getString("City_Name"));
                cities.put(city);
            }
            
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
        
        return cities;
    }
}