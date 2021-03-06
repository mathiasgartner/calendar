package at.gartner.calendar.data.repository;

import org.springframework.data.repository.CrudRepository;

import at.gartner.calendar.data.entity.ActivityType;
import at.gartner.calendar.data.entity.User;

public interface ActivityTypeRepository extends CrudRepository<ActivityType, Long> {
}