package kz.home.RelaySmartSystems.model;

import org.springframework.context.ApplicationEvent;

public class WSEvent extends ApplicationEvent {

    public WSEvent(Object source) {
        super(source);
    }

}
