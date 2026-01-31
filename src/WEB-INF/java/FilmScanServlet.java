import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONArray;

@WebServlet("/FilmScan")
public class FilmScanServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置响应内容类型为JSON
        response.setContentType("application/json; charset=UTF-8");
        // 设置请求编码为UTF-8
        request.setCharacterEncoding("UTF-8");
        
        // 获取请求参数
        String pageStr = request.getParameter("page");
        String pageSizeStr = request.getParameter("pageSize");
        String statusFilter = request.getParameter("status");
        String searchKeyword = request.getParameter("search");
        
        // 解析参数
        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 5;
        searchKeyword = (searchKeyword != null && !searchKeyword.trim().isEmpty()) ? searchKeyword.trim() : null;
        statusFilter = (statusFilter != null && !statusFilter.trim().isEmpty()) ? statusFilter.trim() : null;
        
        // 计算偏移量
        int offset = (page - 1) * pageSize;
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            // 获取数据库连接
            conn = DatabaseUtil.getConnection();
            
            // 构建SQL查询
            StringBuilder whereClause = new StringBuilder();
            List<Object> params = new ArrayList<>();
            
            // 添加搜索条件
            if (searchKeyword != null) {
                whereClause.append(" WHERE Film_Name LIKE ?");
                params.add("%" + searchKeyword + "%");
            }
            
            // 添加状态筛选条件
            if (statusFilter != null) {
                if (whereClause.length() == 0) {
                    whereClause.append(" WHERE");
                } else {
                    whereClause.append(" AND");
                }
                whereClause.append(" Release_Status = ?");
                params.add(statusFilter);
            }
            
            // 查询总记录数
            String countSql = "SELECT COUNT(*) FROM view_Film_Hot" + whereClause.toString();
            pstmt = conn.prepareStatement(countSql);
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            rs = pstmt.executeQuery();
            int totalFilms = 0;
            if (rs.next()) {
                totalFilms = rs.getInt(1);
            }
            DatabaseUtil.closeResources(rs, pstmt, null);
            
            // 查询当前页的电影数据
            String filmSql = "SELECT Film_Publisher_ID, Film_ID, Film_Name, Film_Intro, Film_Duration_Min, Film_Box_Office, Film_Type_Names, Release_Status FROM view_Film_Hot" + whereClause.toString() + " ORDER BY Film_Name ASC LIMIT ? OFFSET ?";
            pstmt = conn.prepareStatement(filmSql);
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            pstmt.setInt(params.size() + 1, pageSize);
            pstmt.setInt(params.size() + 2, offset);
            rs = pstmt.executeQuery();
            
            // 处理查询结果
            List<Map<String, Object>> filmList = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> film = new HashMap<>();
                film.put("publisherId", rs.getString("Film_Publisher_ID"));
                film.put("filmId", rs.getString("Film_ID"));
                film.put("filmName", rs.getString("Film_Name"));
                film.put("synopsis", rs.getString("Film_Intro"));
                film.put("duration", rs.getInt("Film_Duration_Min"));
                film.put("boxOffice", rs.getDouble("Film_Box_Office"));
                film.put("filmTypes", rs.getString("Film_Type_Names"));
                film.put("releaseStatus", rs.getString("Release_Status"));
                filmList.add(film);
            }
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) totalFilms / pageSize);
            
            // 构建响应数据
            JSONObject responseData = new JSONObject();
            JSONArray filmsArray = new JSONArray();
            
            // 将电影列表转换为JSONArray
            for (Map<String, Object> film : filmList) {
                JSONObject filmObj = new JSONObject(film);
                filmsArray.put(filmObj);
            }
            
            // 设置响应数据
            responseData.put("films", filmsArray);
            responseData.put("totalFilms", totalFilms);
            responseData.put("totalPages", totalPages);
            
            // 将响应数据转换为JSON格式并发送
            response.getWriter().write(responseData.toString());
            
        } catch (SQLException e) {
            // 处理数据库异常
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "error");
            errorResponse.put("message", "数据库查询错误: " + e.getMessage());
            response.getWriter().write(errorResponse.toString());
        } finally {
            // 关闭数据库资源
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
    }
}