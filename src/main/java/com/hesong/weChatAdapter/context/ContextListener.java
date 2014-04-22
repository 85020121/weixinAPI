package com.hesong.weChatAdapter.context;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.quartz.SchedulerException;


public class ContextListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        try {
            if (UpdateAccessTokenRunner.scheduler != null
                    || UpdateAccessTokenRunner.scheduler.isStarted()
                    || UpdateAccessTokenRunner.scheduler.isInStandbyMode()) {
                UpdateAccessTokenRunner.scheduler.shutdown(true);
                UpdateAccessTokenRunner.scheduler = null;
                ContextPreloader.ContextLog.info("Quartz scheduler shuted down.");
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        
     // This manually deregisters JDBC driver, which prevents Tomcat 7 from complaining about memory leaks wrto this class
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                ContextPreloader.ContextLog.info(String.format("deregistering jdbc driver: %s", driver));
            } catch (SQLException e) {
                ContextPreloader.ContextLog.error(String.format("Error deregistering driver %s, exception: %s", driver, e));
            }

        }
        System.out.println("contextDestroyed");
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        // TODO Auto-generated method stub
        System.out.println("contextInitialized");
    }

}
