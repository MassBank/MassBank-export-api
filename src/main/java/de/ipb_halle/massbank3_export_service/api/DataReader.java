package de.ipb_halle.massbank3_export_service.api;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DataReader {
    @EventListener(ApplicationReadyEvent.class)
    public void readDataAfterStartup() {
        System.out.println("hello world, I have just started up");
    }
}

