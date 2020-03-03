import {LoggerConfig} from "@uukmm/ng-logger";
import {NgxLoggerLevel} from "ngx-logger";

const loggerConfig: LoggerConfig = {
  level: NgxLoggerLevel.DEBUG,
  serverLogLevel: NgxLoggerLevel.OFF,
  serverLoggingUrl: "https://edmopencdsdev.med.utah.edu:8443/kmm-client-logger/log",
  disableConsoleLogging: true
};


export const environment = {
  production: true,
  serverEndpoint: "http://127.0.0.1/emerse-it-server-prod",
  loggerConfig
};
