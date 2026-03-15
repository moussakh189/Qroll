package com.qroll.database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.qroll.model.Module;


public class ModuleRepository {
    private Connection conn()
    {
        return DatabaseManager.getInstance().getConnection();
    }
    public void save( Module m )
    {
        String sql = "INSERT OR REPLACE INTO modules (module_code, module_name, semester) VALUES (?, ?, ?)";        try(PreparedStatement ps = conn().prepareStatement(sql))
        {
            ps.setString(1 , m.getModuleCode());
            ps.setString(2 , m.getModuleName());
            ps.setInt(3 , m.getSemester());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public Module findByCode(String code) {
        String sql = "SELECT * FROM modules WHERE module_code = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public List<Module> findAll() {
        List<Module> list = new ArrayList<>();
        String sql = "SELECT * FROM modules ORDER BY module_code";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Module mapRow(ResultSet rs) throws SQLException {
        return new Module(
                rs.getString("module_code"),
                rs.getString("module_name"),
                rs.getInt("semester")
        );
    }





}
