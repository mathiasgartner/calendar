package at.gartner.calendar.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import at.gartner.calendar.CalendarUserDetailsService;
import at.gartner.calendar.CalendarUserPrincipal;
import at.gartner.calendar.data.entity.ActivityType;
import at.gartner.calendar.data.entity.Appointment;
import at.gartner.calendar.data.entity.Project;
import at.gartner.calendar.data.entity.User;
import at.gartner.calendar.data.repository.ActivityTypeRepository;
import at.gartner.calendar.data.repository.AppointmentRepository;
import at.gartner.calendar.data.repository.ProjectRepository;
import at.gartner.calendar.data.repository.UserRepository;
import at.gartner.calendar.data.viewmodel.SchedulerAppointment;
import at.gartner.calendar.data.viewmodel.WeeklySummaryItem;

@RestController
@RequestMapping("/calendar")
public class CalendarController {
	
	@Autowired
	private ProjectRepository projectRepository;
	@Autowired
	private ActivityTypeRepository activityTypeRepository;
	@Autowired
	private AppointmentRepository appointmentRepository;
	@Autowired
	private UserRepository userRepository;

	@RequestMapping(value = "/loadAppointmentsForWeek/{startDate}")
	public Iterable<Appointment> LoadAppointmentsForWeek(Authentication authentication, @PathVariable(name = "startDate") @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime startDate)
	{
		CalendarUserPrincipal p = (CalendarUserPrincipal)authentication.getPrincipal();
		
		//TODO: only find by week (for simplicity just date +- 7 days)		
		Iterable<Appointment> appointments = appointmentRepository.findByUserId(p.getUserId());
		return appointments;
	}
		
	@RequestMapping(value = "/appointment/insert", method = RequestMethod.POST)
	public ResponseEntity<SchedulerAppointment> InsertAppointment(Authentication authentication, @RequestBody SchedulerAppointment appointment)
	{
		CalendarUserPrincipal p = (CalendarUserPrincipal)authentication.getPrincipal();
		User u = userRepository.findById(p.getUserId()).orElse(null);

		Project project = projectRepository.findById(appointment.getProjectId()).orElse(null);
		ActivityType activityType = activityTypeRepository.findById(appointment.getActivityTypeId()).orElse(null);
		Appointment data = new Appointment(appointment.getStartDate(), appointment.getEndDate(), appointment.getSubject(), appointment.getBody());		
		data.setProject(project);
		data.setActivityType(activityType);
		data.setUser(u);
		appointmentRepository.save(data);
        return new ResponseEntity<SchedulerAppointment>(appointment, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/appointment/update/{id}", method = RequestMethod.POST)
	public ResponseEntity<SchedulerAppointment> UpdateAppointment(Authentication authentication, @PathVariable(name = "id") Long id, @RequestBody SchedulerAppointment appointment)
	{
		CalendarUserPrincipal p = (CalendarUserPrincipal)authentication.getPrincipal();
		User u = userRepository.findById(p.getUserId()).orElse(null);
		
		//TODO: check if appointment is owned by User u
		Project project = projectRepository.findById(appointment.getProjectId()).orElse(null);
		ActivityType activityType = activityTypeRepository.findById(appointment.getActivityTypeId()).orElse(null);
		Appointment data = new Appointment(appointment.getStartDate(), appointment.getEndDate(), appointment.getSubject(), appointment.getBody());
		data.setId(appointment.getId());
		data.setProject(project);
		data.setActivityType(activityType);
		data.setUser(u);
		appointmentRepository.save(data);
        return new ResponseEntity<SchedulerAppointment>(appointment, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/appointment/delete/{id}")
	public String DeleteAppointment(Authentication authentication, @PathVariable(name = "id") Long id)
	{
		CalendarUserPrincipal p = (CalendarUserPrincipal)authentication.getPrincipal();
		Appointment a = appointmentRepository.findById(id).orElse(null);
		if (a != null && a.getUserId() == p.getUserId())
		{
			appointmentRepository.deleteById(id);
			return "deleted";	
		}
		else
		{
			return "failed";
		}
	}

	@RequestMapping(value = "/loadWeeklySummary/{startDate}")
	public List<WeeklySummaryItem> LoadWeeklySummary(Authentication authentication, @PathVariable(name = "startDate") @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime startDate)
	{	
		CalendarUserPrincipal p = (CalendarUserPrincipal)authentication.getPrincipal();

		//TODO: write custom repository method for fetching only weekly apointments!
		LocalDateTime monday = startDate;
	    while (monday.getDayOfWeek() != DayOfWeek.MONDAY)
	    {
	      monday = monday.minusDays(1);
	    }

	    LocalDateTime sunday = startDate;
	    while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY)
	    {
	      sunday = sunday.plusDays(1);
	    }
		List<WeeklySummaryItem> items = new ArrayList<>();
		Iterable<Appointment> appointments = appointmentRepository.findByUserId(p.getUserId());
		for(Appointment a : appointments)
		{
			if (a.getStartDate().toLocalDateTime().isAfter(monday) && a.getEndDate().toLocalDateTime().isBefore(sunday))
			{
				items.add(new WeeklySummaryItem(a.getHours(), a.getProject().getName() + " - " + a.getActivityType().getName()));
			}
		}
		Map<String, Double> groupedItems = items.stream().collect(Collectors.groupingBy(i -> i.getDescription(), Collectors.summingDouble(i -> i.getTotalHours())));
		items.clear();
		for (Map.Entry<String, Double> gi : groupedItems.entrySet())
		{
			items.add(new WeeklySummaryItem(gi.getValue(), gi.getKey()));
		}
		return items;
	}

	@RequestMapping(value = "/testpost", method = RequestMethod.POST)
	public String testpost(@RequestBody String s) {
		return "test";
	}
	
	@RequestMapping(value = "test/")
	public String test() {
		return "test";
	}

	@RequestMapping(value = "test/{terminId}")
	public String test1(@PathVariable(name = "terminId") Long terminId) {
		appointmentRepository.save(new Appointment(ZonedDateTime.now(), ZonedDateTime.now().plusHours(3), "app" + terminId, "tes test test"));
		return "saved " + terminId;
	}

	@RequestMapping(value = "show/{id}", method = RequestMethod.GET, produces = "application/json")
	public Appointment show(@PathVariable(name = "id") Long id) {
		Appointment app = appointmentRepository.findById(id).orElse(new Appointment(ZonedDateTime.now(), ZonedDateTime.now().plusHours(3), "not found", "appointment could not be found"));
		return app;
	}

	@RequestMapping(value = "test1/", method = RequestMethod.GET)
	public String test2(@RequestParam String test1) {
		return "test " + test1;
	}

	@RequestMapping(value = "test/", method = RequestMethod.POST)
	public String test3(@RequestBody BigDecimal m) {
		return "test";
	}

	@RequestMapping(value = "app/")
	public Appointment app() {
		return new Appointment(ZonedDateTime.now(), ZonedDateTime.now().plusHours(3), "test", "aödslfkjöasdkljföaklfj");
	}
}
