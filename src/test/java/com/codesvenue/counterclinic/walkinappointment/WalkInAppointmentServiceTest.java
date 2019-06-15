package com.codesvenue.counterclinic.walkinappointment;

import com.codesvenue.counterclinic.user.User;
import com.codesvenue.counterclinic.user.UserRole;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.*;
import java.util.ArrayList;

public class WalkInAppointmentServiceTest {

    private WalkInAppointmentService walkInAppointmentService;

    @Before
    public void setup() {
        AppointmentRepository appointmentRepository = new FakeAppointmentRepository();
        SimpMessagingTemplate simpMessagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        walkInAppointmentService = new WalkInAppointmentServiceImpl(appointmentRepository, simpMessagingTemplate);
        TestData.appointmentStatusList = new ArrayList<>();
    }

    @Test
    public void itShouldFetchTheAvgWaitingTimeOfThePatientAppointment_DoctorHasNotStartedTakingPatient() {
        Integer appointmentId = 5;
        Integer doctorId = 1;

        AppointmentStatus appointmentStatus = walkInAppointmentService.getAppointmentStatus(appointmentId, doctorId, LocalDateTime.now(ZoneOffset.UTC));
        Assert.assertNotNull(appointmentStatus);
        Assert.assertEquals(60, (int) appointmentStatus.getAvgWaitingTime());
    }

    @Test
    public void fetchAvgWaitingTimeWhenTwoPatientsHaveBeenChecked() {
        Integer doctorId = 1;
        Integer appointmentId = 5;

        AppointmentStatus appointmentStatus1 = TestData.firstPersonGoesInsideTheDoctorCabin();
        AppointmentStatus appointmentStatus2 = TestData.secondPersonGoesInsideTheDoctorCabin();

        TestData.store(appointmentStatus1);
        TestData.store(appointmentStatus2);

        LocalDateTime inquiryTime = LocalDateTime.of(LocalDate.of(2019, Month.JUNE, 6), LocalTime.of(11, 30));
        AppointmentStatus appointmentStatus = walkInAppointmentService.getAppointmentStatus(appointmentId, doctorId, inquiryTime);
        Assert.assertNotNull(appointmentStatus);
        Assert.assertEquals(45, (int)appointmentStatus.getAvgWaitingTime());
    }

    @Test
    public void itShouldAddTheDoctorBreakTimeToTheAppointmentAvgWaitingTime() {
        Integer doctorId = 1;
        Integer appointmentId = 5;

        AppointmentStatus appointmentStatus1 = TestData.firstPersonGoesInsideTheDoctorCabin();
        AppointmentStatus appointmentStatus2 = TestData.secondPersonGoesInsideTheDoctorCabin();

        TestData.store(appointmentStatus1);
        TestData.store(appointmentStatus2);
        // have to update the doctor break time in the last appointment of the queue
        TestData.appointmentStatusList.get(TestData.appointmentStatusList.size()-1).setDoctorBreakDuration(10);

        LocalDateTime inquiryTime = LocalDateTime.of(LocalDate.of(2019, Month.JUNE, 6), LocalTime.of(11, 30));
        AppointmentStatus appointmentStatus = walkInAppointmentService.getAppointmentStatus(appointmentId, doctorId, inquiryTime);
        Assert.assertNotNull(appointmentStatus);
        Assert.assertEquals(55, (int)appointmentStatus.getAvgWaitingTime());
    }

    @Test
    public void itShouldNotifyReceptionAndCreateNewAppointmentStatus() {
        User user = User.newInstance().roles(UserRole.DOCTOR).userId(1);
        AppointmentStatus newAppointmentStatus = walkInAppointmentService.callNextPatient(user);
        Assert.assertNotNull(newAppointmentStatus);
        Assert.assertEquals(1, (int) newAppointmentStatus.getCurrentAppointmentId());

        newAppointmentStatus = walkInAppointmentService.callNextPatient(user);
        Assert.assertEquals(2, (int) newAppointmentStatus.getCurrentAppointmentId());
    }
}
