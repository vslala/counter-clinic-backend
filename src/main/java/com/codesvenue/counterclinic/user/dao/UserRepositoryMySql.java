package com.codesvenue.counterclinic.user.dao;

import com.codesvenue.counterclinic.clinic.model.Clinic;
import com.codesvenue.counterclinic.clinic.model.ClinicRoom;
import com.codesvenue.counterclinic.configuration.DateTimeConstants;
import com.codesvenue.counterclinic.qrcode.QRCode;
import com.codesvenue.counterclinic.user.UserConstants;
import com.codesvenue.counterclinic.user.model.*;
import com.codesvenue.counterclinic.walkinappointment.model.WalkInAppointment;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log4j
@Repository
public class UserRepositoryMySql implements UserRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserRepositoryMySql(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Clinic createNewClinic(Clinic clinic) {
        final String SQL = "INSERT INTO clinics (clinic_name) values (:clinicName)";
        SqlParameterSource params = new MapSqlParameterSource().addValue("clinicName", clinic.getClinicName());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(SQL, params, keyHolder);

        Clinic newClinic = Clinic.copyInstance(clinic);
        newClinic.setClinicId(keyHolder.getKey().intValue());

        return newClinic;
    }

    @Override
    public WalkInAppointment createNewWalkInAppointment(WalkInAppointment walkInAppointment) {
        final String sql = "INSERT INTO walkin_appointments (patient_first_name, patient_last_name, appointed_doctor_id, created_at) " +
                "VALUES (:patientFirstName, :patientLastName, :appointedDoctorId, :createdAt)";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("patientFirstName", walkInAppointment.getPatientFirstName())
                .addValue("patientLastName", walkInAppointment.getPatientLastName())
                .addValue("appointedDoctorId", walkInAppointment.getAppointedDoctorId())
                .addValue("createdAt", LocalDateTime.now(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern(DateTimeConstants.MYSQL_DATETIME_PATTERN)));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder);
        return WalkInAppointment.copyInstance(walkInAppointment)
                .walkInAppointmentId(keyHolder.getKey().intValue());
    }

    @Override
    public User findDoctorById(Integer doctorId) {
        final String sql = "SELECT t1.user_id, t1.first_name, t1.last_name, t1.email, t1.mobile, t1.username, t1.preferred_language, t1.created_at,\n" +
                "\t(SELECT t2.meta_value FROM users_meta t2 WHERE t2.meta_key = :userRole AND t2.user_id = :userId) as user_roles,\n" +
                "\t(SELECT t2.meta_value FROM users_meta t2 WHERE t2.meta_key = :assignedClinicRoom AND t2.user_id = :userId) as assigned_clinic_room\n" +
                "FROM users t1\n" +
                "WHERE t1.user_id = :userId";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("userRole", UserConstants.USER_ROLE)
                .addValue("userId", doctorId)
                .addValue("assignedClinicRoom", UserConstants.ASSIGNED_CLINIC_ROOM);
        return jdbcTemplate.queryForObject(sql, params, User.UserRowMapper.newInstance());
    }

    @Override
    public QRCode createNewQRCode(QRCode qrCode) {
        final String sql = "INSERT INTO qrcode_attachments (appointment_id, height, width, image_name, image_file_path, image_url_path, qrcode_data, created_at) " +
                "VALUES (:appointmentId, :height, :width, :imageName, :imageFilePath, :imageUrlPath, :qrCodeData, :createdAt)";
        SqlParameterSource params = null;
        try {
            params = new MapSqlParameterSource()
                    .addValue("appointmentId", qrCode.getAppointmentId())
                    .addValue("height", qrCode.getQrCodeHeight())
                    .addValue("width", qrCode.getQrCodeWidth())
                    .addValue("imageName", qrCode.getQrCodeName())
                    .addValue("imageFilePath", qrCode.getQrCodeFilePath())
                    .addValue("imageUrlPath", qrCode.getQrCodeUrlPath())
                    .addValue("qrCodeData", qrCode.getQrCodeDataInJson())
                    .addValue("createdAt", LocalDateTime.now(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ofPattern(DateTimeConstants.MYSQL_DATETIME_PATTERN)));
        } catch (JsonProcessingException e) {
            log.error("Cannot convert qrcode data to json. Error: " + e.getMessage(), e);
            throw new DatabaseException("Cannot convert qrcode data to json. Error: " + e.getMessage());
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder);

        return QRCode.copyInstance(qrCode).qrCodeId(keyHolder.getKey().intValue());
    }

    @Override
    public WalkInAppointment findAppointmentById(Integer nextAppointmentId) {
        final String sql = "SELECT t1.walkin_appointment_id, t1.patient_first_name, t1.patient_last_name, " +
                "t1.appointed_doctor_id, t1.created_at FROM `walkin_appointments` t1 " +
                "WHERE t1.walkin_appointment_id = :walkInAppointmentId";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("walkInAppointmentId", nextAppointmentId);
        return jdbcTemplate.queryForObject(sql, params, WalkInAppointment.WalkInAppointmentRowMapper.newInstance());
    }

    @Override
    public Clinic createNewClinicRoom(Clinic newClinic) {
        final String SQL = "INSERT INTO clinic_rooms (clinic_id, room_name) values (:clinicId, :roomName)";
        List<ClinicRoom> clinicRooms = new ArrayList<>();
        newClinic.getRooms().forEach(room -> {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(SQL, new MapSqlParameterSource()
                    .addValue("clinicId", newClinic.getClinicId())
                    .addValue("roomName", room.getName()),
                    keyHolder);
            clinicRooms.add(ClinicRoom.copyInstance(room));
                });
        newClinic.setRooms(clinicRooms);
        return newClinic;
    }

    @Override
    public User createNewUser(User user) {
        final String sql = "INSERT INTO users (first_name, last_name, email, mobile, username, preferred_language, created_at) " +
                "values (:patientFirstName, :patientLastName, :email, :mobile, :username, :preferredLanguage)";
        System.out.println("Preferred Language: "  + user.getPreferredLanguage());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("patientFirstName", user.getFirstName())
                .addValue("patientLastName", user.getLastName())
                .addValue("email", user.getEmail())
                .addValue("mobile", user.getMobile())
                .addValue("username", user.getUsername())
                .addValue("preferredLanguage", user.getPreferredLanguage().toString())
                .addValue("createdAt", LocalDateTime.now(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern(DateTimeConstants.MYSQL_DATETIME_PATTERN)));
        jdbcTemplate.update(sql, params, keyHolder);
        return User.copyInstance(user).userId(keyHolder.getKey().intValue());
    }

    @Override
    public UserLogin createNewUserLogin(UserLogin userLogin) {
        final String sql = "INSERT INTO users_login (user_id, username, password, logged_in_at) " +
                "values (:userId, :username, :password,  :loggedInAt)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userLogin.getUserId())
                .addValue("username", userLogin.getUsername())
                .addValue("password", userLogin.getPassword())
                .addValue("loggedInAt", LocalDateTime.now(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern(DateTimeConstants.MYSQL_DATETIME_PATTERN)));
        jdbcTemplate.update(sql, params, keyHolder);
        return UserLogin.copyInstance(userLogin).id(keyHolder.getKey().intValue());
    }

    @Override
    public ClinicRoom findClinicRoomById(Integer clinicRoomId) {
        final String sql = "SELECT clinic_room_id, clinic_id, room_name FROM clinic_rooms WHERE clinic_room_id = :clinicRoomId";
        return jdbcTemplate.queryForObject(sql,
                new MapSqlParameterSource().addValue("clinicRoomId", clinicRoomId),
                Clinic.ClinicRoomRowMapper.newInstance());
    }

    @Override
    public UserMeta updateUserMeta(Integer userId, String metaKey, String metaValue) {
        final String sql = "INSERT INTO users_meta (user_id, meta_key, meta_value) " +
                "VALUES (:userId, :metaKey, :metaValue) " +
                "ON DUPLICATE KEY UPDATE " +
                "user_id = :userId, meta_key=:metaKey, meta_value=:metaValue";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("metaKey", metaKey)
                .addValue("metaValue", metaValue);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected = jdbcTemplate.update(sql, params, keyHolder);
        return UserMeta.newInstance()
                .metaId( Objects.isNull(keyHolder.getKeys()) ? 0 : (int)keyHolder.getKeys().getOrDefault("meta_id", 0))
                .userId(userId)
                .metaKey(metaKey)
                .metaValue(metaValue);
    }

    @Override
    public List<User> findAllUsersByRole(UserRole userRole) {
        final String sql = "SELECT t1.user_id, t1.first_name, t1.last_name, t1.email, t1.mobile, t1.username, t1.preferred_language, t1.created_at, " +
                " (SELECT meta_value FROM users_meta WHERE meta_key = 'assigned_clinic_room') as assigned_clinic_room,\n" +
                " ( SELECT GROUP_CONCAT(DISTINCT t2.role_name) as user_role FROM user_roles t2 WHERE t2.user_id = t1.user_id ) as user_roles\n" +
                "FROM users t1 WHERE t1.user_id IN (SELECT t3.user_id FROM user_roles t3 WHERE t3.role_name = :userRole);";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("userRole", userRole.toString());
        List<User> users = jdbcTemplate.query(sql, params, User.UserRowMapper.newInstance());
        return users;
    }

    @Override
    public User findUserById(int userId) {
        final String sql = "SELECT t1.user_id, t1.first_name, t1.last_name, t1.email, t1.mobile, t1.username, t1.preferred_language, t1.created_at, " +
                "   ( " +
                "       SELECT GROUP_CONCAT(DISTINCT t2.role_name) as user_role " +
                "       FROM user_roles t2 WHERE t2.user_id = t1.user_id " +
                "   ) as user_roles,\n" +
                " (SELECT meta_value FROM users_meta WHERE meta_key = 'assigned_clinic_room') as assigned_clinic_room\n" +
                "FROM users t1 WHERE user_id = :userId";
        SqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        return jdbcTemplate.queryForObject(sql, params, User.UserRowMapper.newInstance());
    }
}