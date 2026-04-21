package pl.chrono.dbmanager.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.chrono.dbmanager.model.Holiday;
import pl.chrono.dbmanager.repository.HolidayRepository;

@RestController
@RequestMapping("/api/holiday")
public class HolidayController extends BaseController<Holiday, Integer> {

    private final HolidayRepository holidayRepository;

    public HolidayController(HolidayRepository holidayRepository) {
        super(holidayRepository);
        this.holidayRepository = holidayRepository;
    }
}